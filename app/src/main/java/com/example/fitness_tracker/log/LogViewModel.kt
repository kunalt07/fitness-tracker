package com.example.fitness_tracker.log

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_tracker.data.Exercise
import com.example.fitness_tracker.data.ExerciseKind
import com.example.fitness_tracker.data.FitnessRepository
import com.example.fitness_tracker.UiState
import com.example.fitness_tracker.data.SetWithExerciseRow
import com.example.fitness_tracker.data.VolumeTotals
import com.example.fitness_tracker.data.WorkoutSession
import com.example.fitness_tracker.data.WorkoutTemplate
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SESSION_RESUME_WINDOW_MS = 4 * 60 * 60 * 1000L  // 4 hours

data class LastSet(
    val reps: Int,
    val weightKg: Double,
    val durationSec: Int,
    val distanceMeters: Int,
    val performedAt: Long,
)

data class TodayTotals(
    val sets: Int = 0,
    val exercises: Int = 0,
    val volume: VolumeTotals = VolumeTotals(0.0, 0L, 0L, 0, 0),
)

private fun startOfTodayMs(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

class LogViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FitnessRepository.get(app)

    private val _activeSession = MutableStateFlow<WorkoutSession?>(null)
    val activeSession: StateFlow<WorkoutSession?> = _activeSession.asStateFlow()

    val exercises: StateFlow<List<Exercise>> =
        repo.exercises.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val plannedExerciseIds: StateFlow<List<Long>> =
        repo.pendingPlan.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val templates: StateFlow<List<WorkoutTemplate>> =
        repo.templates.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val templateMuscleByTemplateId: StateFlow<Map<Long, String>> =
        repo.templateDominantGroups
            .map { rows -> rows.mapNotNull { r -> r.muscleGroup?.let { r.templateId to it } }.toMap() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Live totals for sets logged today (across all sessions of the day). */
    val todayTotals: StateFlow<TodayTotals> = run {
        val midnight = startOfTodayMs()
        combine(
            repo.observeVolumeSince(midnight),
            repo.observeDistinctExercisesSince(midnight),
        ) { vol, distinct ->
            TodayTotals(
                sets = vol.totalSets,
                exercises = distinct,
                volume = vol,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TodayTotals())
    }

    private val _saveTemplatePrompt = MutableStateFlow<Long?>(null)
    val saveTemplatePrompt: StateFlow<Long?> = _saveTemplatePrompt.asStateFlow()

    private val _critique = MutableStateFlow<UiState>(UiState.Initial)
    val critique: StateFlow<UiState> = _critique.asStateFlow()

    private val critiqueModel = Firebase
        .ai(backend = GenerativeBackend.googleAI())
        .generativeModel(modelName = "gemini-2.5-flash")

    sealed interface QuickLogState {
        data object Idle : QuickLogState
        data object Parsing : QuickLogState
        data class Preview(val parsed: List<ParsedSet>) : QuickLogState
        data class Error(val message: String) : QuickLogState
    }

    data class ParsedSet(
        val exerciseId: Long,
        val exerciseName: String,
        val kind: ExerciseKind,
        val reps: Int,
        val weightKg: Double,
        val durationSec: Int,
        val distanceMeters: Int,
    )

    private val _quickLog = MutableStateFlow<QuickLogState>(QuickLogState.Idle)
    val quickLog: StateFlow<QuickLogState> = _quickLog.asStateFlow()

    @Suppress("OPT_IN_USAGE")
    val sets: StateFlow<List<SetWithExerciseRow>> =
        _activeSession.flatMapLatest { s ->
            if (s == null) flowOf(emptyList()) else repo.observeSetsForSession(s.id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _restRemainingSec = MutableStateFlow(0)
    val restRemainingSec: StateFlow<Int> = _restRemainingSec.asStateFlow()
    private var restJob: Job? = null

    init {
        viewModelScope.launch { repo.seedDefaultExercisesIfEmpty() }
        viewModelScope.launch { repo.pruneStalePlan() }
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - SESSION_RESUME_WINDOW_MS
            repo.mostRecentOpenSessionSince(cutoff)?.let { _activeSession.value = it }
        }
        viewModelScope.launch {
            // When a plan arrives from the Plan tab and no session is active,
            // start one automatically so the planned chips are immediately usable.
            repo.pendingPlan.collect { ids ->
                if (ids.isNotEmpty() && _activeSession.value == null) {
                    val now = System.currentTimeMillis()
                    val id = repo.startSession(now)
                    _activeSession.value = WorkoutSession(id = id, startedAt = now)
                }
            }
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = repo.startSession(now)
            _activeSession.value = WorkoutSession(id = id, startedAt = now)
        }
    }

    fun endSession() {
        viewModelScope.launch {
            val current = _activeSession.value ?: return@launch
            val setCount = repo.setCount(current.id)
            if (setCount == 0) {
                repo.deleteSession(current.id)
                _activeSession.value = null
                cancelRest()
                // Plan stays — user might want it for the next session today.
            } else {
                // Prompt the user to save this as a template before fully ending.
                _saveTemplatePrompt.value = current.id
            }
        }
    }

    /** Called from the save-template dialog. Pass blank/null name to skip saving. */
    fun confirmEndSession(saveAsName: String?) {
        viewModelScope.launch {
            val sessionId = _saveTemplatePrompt.value ?: return@launch
            val now = System.currentTimeMillis()
            val sessionSets = repo.setsForSession(sessionId)
            if (!saveAsName.isNullOrBlank()) {
                val exerciseIds = sessionSets.map { it.exerciseId }.distinct()
                repo.saveTemplate(saveAsName.trim(), exerciseIds, now)
            }
            repo.endSessionMark(sessionId, now)
            _activeSession.value = null
            _saveTemplatePrompt.value = null
            cancelRest()
            // Plan stays — user might want it for the next session today.

            requestCritique(sessionSets)
        }
    }

    private fun requestCritique(sessionSets: List<SetWithExerciseRow>) {
        if (sessionSets.isEmpty()) return
        _critique.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = repo.summarizeRecentHistory()
                val prompt = buildString {
                    appendLine("You are a thoughtful strength coach. The user just finished this workout:")
                    appendLine()
                    sessionSets.forEach { s ->
                        val detail = when (s.exerciseKind.name) {
                            "REPS" -> "${s.reps} reps" + if (s.weightKg > 0) " @ ${formatKg(s.weightKg)} kg" else ""
                            "TIME" -> "${s.durationSec}s"
                            "DISTANCE" -> "${s.distanceMeters} m"
                            else -> "${s.reps} reps"
                        }
                        appendLine("- ${s.exerciseName}: $detail")
                    }
                    if (history != null) {
                        appendLine()
                        appendLine(history)
                    }
                    appendLine()
                    appendLine(
                        "Write a short, warm post-workout note in 3-4 sentences. " +
                            "Mention one thing they did well, one specific suggestion (e.g. progressive overload, " +
                            "muscle group balance, rep-range tweak), and end with one concrete next-session focus. " +
                            "No bullet points, no markdown, no headers — just plain prose.",
                    )
                }
                val resp = critiqueModel.generateContent(content { text(prompt) })
                _critique.value = resp.text?.let { UiState.Success(it.trim()) }
                    ?: UiState.Error("Empty response.")
            } catch (e: Exception) {
                Log.e("LogViewModel", "critique failed", e)
                _critique.value = UiState.Error(e.localizedMessage ?: "Couldn't reach the coach.")
            }
        }
    }

    fun dismissCritique() {
        _critique.value = UiState.Initial
    }

    fun parseQuickLog(text: String) {
        if (text.isBlank()) return
        _quickLog.value = QuickLogState.Parsing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val knownNames = exercises.value.joinToString(", ") { it.name }
                val prompt = buildString {
                    appendLine("Parse the user's free-text workout log into structured JSON.")
                    appendLine()
                    appendLine("Known exercises (prefer matching one of these): $knownNames")
                    appendLine()
                    appendLine("Output ONLY a JSON object of the form:")
                    appendLine("{ \"sets\": [ { \"name\": \"Bench Press\", \"reps\": 8, \"weight_kg\": 60, \"duration_sec\": 0, \"distance_m\": 0 }, ... ] }")
                    appendLine()
                    appendLine("Rules:")
                    appendLine("- Expand a phrase like \"3 sets x 10 reps at 80kg\" into 3 separate set objects.")
                    appendLine("- For pure-time exercises (planks, holds), set duration_sec, leave reps=0.")
                    appendLine("- For distance exercises (run, bike), set distance_m (meters), leave reps=0.")
                    appendLine("- Use 0 (zero) for any unused field; never null. Weights are in kilograms.")
                    appendLine("- Use the user's wording for exercise names if it doesn't match the known list.")
                    appendLine("- Output JSON only — no markdown, no commentary.")
                    appendLine()
                    appendLine("User: $text")
                }
                val resp = critiqueModel.generateContent(content { text(prompt) })
                val raw = resp.text?.trim().orEmpty()
                val parsed = parseQuickLogJson(raw)
                if (parsed.isEmpty()) {
                    _quickLog.value = QuickLogState.Error("Couldn't make sense of that — try \"3 sets bench at 60\".")
                } else {
                    _quickLog.value = QuickLogState.Preview(parsed)
                }
            } catch (e: Exception) {
                Log.e("LogViewModel", "quick log parse failed", e)
                _quickLog.value = QuickLogState.Error(e.localizedMessage ?: "Couldn't reach the parser.")
            }
        }
    }

    private suspend fun parseQuickLogJson(raw: String): List<ParsedSet> {
        val jsonText = stripCodeFences(raw)
        val root = runCatching { org.json.JSONObject(jsonText) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("sets") ?: return emptyList()
        val out = mutableListOf<ParsedSet>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val name = item.optString("name").trim()
            if (name.isBlank()) continue
            val id = repo.resolveOrCreateExercise(name) ?: continue
            val ex = repo.getExercise(id) ?: continue
            out += ParsedSet(
                exerciseId = id,
                exerciseName = ex.name,
                kind = ex.kind,
                reps = item.optInt("reps", 0).coerceAtLeast(0),
                weightKg = item.optDouble("weight_kg", 0.0).coerceAtLeast(0.0),
                durationSec = item.optInt("duration_sec", 0).coerceAtLeast(0),
                distanceMeters = item.optInt("distance_m", 0).coerceAtLeast(0),
            )
        }
        return out
    }

    private fun stripCodeFences(s: String): String {
        val trimmed = s.trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed.removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
    }

    fun confirmQuickLog() {
        val state = _quickLog.value as? QuickLogState.Preview ?: return
        val session = _activeSession.value
        viewModelScope.launch {
            // Auto-start a session if none is active so the user can quick-log
            // without clicking Start first.
            val sid = session?.id ?: run {
                val now = System.currentTimeMillis()
                val newId = repo.startSession(now)
                _activeSession.value = WorkoutSession(id = newId, startedAt = now)
                newId
            }
            state.parsed.forEach { p ->
                repo.logSet(
                    sessionId = sid,
                    exerciseId = p.exerciseId,
                    reps = p.reps,
                    weightKg = p.weightKg,
                    durationSec = p.durationSec,
                    distanceMeters = p.distanceMeters,
                    now = System.currentTimeMillis(),
                )
            }
            _quickLog.value = QuickLogState.Idle
        }
    }

    fun dismissQuickLog() {
        _quickLog.value = QuickLogState.Idle
    }

    private fun formatKg(kg: Double): String =
        if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.1f".format(kg)

    fun dismissSaveTemplatePrompt() {
        _saveTemplatePrompt.value = null
    }

    fun startFromTemplate(templateId: Long) {
        viewModelScope.launch {
            val ids = repo.exerciseIdsForTemplate(templateId)
            if (ids.isNotEmpty()) repo.stagePlan(ids)
            // Auto-start happens via the `pendingPlan` collector in init {}.
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repo.deleteTemplate(id) }
    }

    fun logSet(
        exerciseId: Long,
        reps: Int,
        weightKg: Double,
        durationSec: Int,
        distanceMeters: Int,
        restSeconds: Int = 90,
    ) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            repo.logSet(
                sessionId = session.id,
                exerciseId = exerciseId,
                reps = reps,
                weightKg = weightKg,
                durationSec = durationSec,
                distanceMeters = distanceMeters,
                now = System.currentTimeMillis(),
            )
            startRest(restSeconds)
        }
    }

    fun updateSet(
        id: Long,
        reps: Int,
        weightKg: Double,
        durationSec: Int,
        distanceMeters: Int,
    ) {
        viewModelScope.launch {
            repo.updateSet(id, reps, weightKg, durationSec, distanceMeters)
        }
    }

    fun deleteSet(id: Long) {
        viewModelScope.launch { repo.deleteSet(id) }
    }

    /**
     * Delete a set and return a restore lambda. Caller (the screen) hooks the
     * lambda to a snackbar's Undo action.
     */
    suspend fun deleteSetWithUndo(id: Long): (suspend () -> Unit)? {
        val snapshot = repo.getSetById(id) ?: return null
        repo.deleteSet(id)
        return { repo.restoreSet(snapshot) }
    }

    suspend fun deleteTemplateWithUndo(id: Long): (suspend () -> Unit)? {
        val snapshot = repo.snapshotTemplate(id) ?: return null
        repo.deleteTemplate(id)
        return { repo.restoreTemplate(snapshot) }
    }

    fun updateExercise(id: Long, name: String, muscleGroup: String, kind: ExerciseKind) {
        viewModelScope.launch {
            repo.updateExercise(id, name, muscleGroup, kind)
        }
    }

    suspend fun deleteExerciseWithUndo(id: Long): (suspend () -> Unit)? {
        val snapshot = repo.snapshotExercise(id) ?: return null
        repo.deleteExercise(id)
        return { repo.restoreExercise(snapshot) }
    }

    /** One-tap "again" — insert an identical set and start the rest timer. */
    fun repeatLastSet() {
        val session = _activeSession.value ?: return
        val last = sets.value.lastOrNull() ?: return
        viewModelScope.launch {
            repo.logSet(
                sessionId = session.id,
                exerciseId = last.exerciseId,
                reps = last.reps,
                weightKg = last.weightKg,
                durationSec = last.durationSec,
                distanceMeters = last.distanceMeters,
                now = System.currentTimeMillis(),
            )
            startRest(90)
        }
    }

    suspend fun lastSetFor(exerciseId: Long): LastSet? {
        val s = repo.lastSetForExercise(exerciseId) ?: return null
        return LastSet(
            reps = s.reps,
            weightKg = s.weightKg,
            durationSec = s.durationSec,
            distanceMeters = s.distanceMeters,
            performedAt = s.performedAt,
        )
    }

    suspend fun createExercise(name: String, kind: ExerciseKind): Exercise? {
        val titled = name.trim().split(Regex("\\s+"))
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        if (titled.isBlank()) return null
        val id = repo.addExercise(titled, "Custom", kind).takeIf { it > 0 }
            ?: repo.resolveOrCreateExercise(titled)
            ?: return null
        return repo.getExercise(id)
    }

    fun startRest(seconds: Int) {
        cancelRest()
        if (seconds <= 0) return
        restJob = viewModelScope.launch {
            _restRemainingSec.value = seconds
            while (_restRemainingSec.value > 0) {
                delay(1_000)
                _restRemainingSec.value -= 1
            }
            buzz()
        }
    }

    fun cancelRest() {
        restJob?.cancel()
        restJob = null
        _restRemainingSec.value = 0
    }

    private fun buzz() {
        val ctx = getApplication<Application>().applicationContext
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1),
        )
    }

    override fun onCleared() {
        super.onCleared()
        restJob?.cancel()
    }
}
