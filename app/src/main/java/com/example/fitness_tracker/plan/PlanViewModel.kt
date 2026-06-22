package com.example.fitness_tracker.plan

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_tracker.UiState
import com.example.fitness_tracker.data.FitnessRepository
import com.example.fitness_tracker.data.ReminderSettings
import com.example.fitness_tracker.data.WeeklySplitDay
import com.example.fitness_tracker.reminder.ReminderScheduler
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class PlanViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FitnessRepository.get(app)

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val generativeModel = Firebase
        .ai(backend = GenerativeBackend.googleAI())
        .generativeModel(modelName = "gemini-2.5-flash")

    private val _hasHistory = MutableStateFlow(false)
    val hasHistory: StateFlow<Boolean> = _hasHistory.asStateFlow()

    val weeklySplit: StateFlow<List<WeeklySplitDay>> = repo.weeklySplit
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reminderSettings: StateFlow<ReminderSettings?> = repo.reminderSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.saveReminderSettings(
                ReminderSettings(id = 0, enabled = enabled, hour = hour, minute = minute),
            )
            val ctx = getApplication<Application>().applicationContext
            if (enabled) ReminderScheduler.scheduleNext(ctx, hour, minute)
            else ReminderScheduler.cancel(ctx)
        }
    }

    init {
        viewModelScope.launch {
            _hasHistory.value = repo.summarizeRecentHistory() != null
        }
        // Wipe any previously-cached plan so the AI result doesn't reappear after
        // the app is closed and reopened. The Plan tab now always starts blank;
        // the user explicitly taps "Generate plan" to produce a fresh one.
        viewModelScope.launch {
            repo.clearCachedPlan()
        }
    }

    fun todayDayOfWeek(): Int =
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    fun focusForDay(dayOfWeek: Int): String? =
        weeklySplit.value.firstOrNull { it.dayOfWeek == dayOfWeek }?.focus
            ?.takeIf { it.isNotBlank() }

    /**
     * Generate a plan. If [overrideFocus] is non-null/non-blank it overrides the
     * day-of-week focus injected from the weekly split.
     */
    fun generatePlan(
        goal: String,
        durationMin: Int,
        equipment: String,
        notes: String,
        overrideFocus: String? = null,
    ) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val history = repo.summarizeRecentHistory()
            val weightGoal = repo.weightGoalContext()
            val nutrition = repo.nutritionContext()
            _hasHistory.value = history != null
            val focus = overrideFocus?.takeIf { it.isNotBlank() }
                ?: focusForDay(todayDayOfWeek())

            val prompt = buildString {
                appendLine("You are a certified personal trainer. Build a single workout for the user.")
                appendLine()
                if (weightGoal != null) {
                    appendLine(weightGoal)
                    appendLine()
                }
                if (nutrition != null) {
                    appendLine(nutrition)
                    appendLine()
                }
                if (history != null) {
                    appendLine(history)
                    appendLine()
                    appendLine("Use the history to balance muscle groups (avoid hitting recently-trained areas) and to suggest weights consistent with the user's recent loads.")
                    appendLine()
                }
                if (focus != null) {
                    appendLine("Today's focus: $focus")
                    appendLine("All Main-section exercises must target the muscles named in today's focus. If the focus is a rest day or mobility, return a short recovery routine instead.")
                    appendLine()
                }
                appendLine("Goal: $goal")
                appendLine("Time available: $durationMin minutes")
                appendLine("Equipment: ${equipment.ifBlank { "bodyweight only" }}")
                if (notes.isNotBlank()) {
                    appendLine("Notes / constraints: $notes")
                }
                appendLine()
                appendLine("Output a Markdown workout plan with these sections (use exact headers):")
                appendLine("## Warm-up")
                appendLine("## Main")
                appendLine("## Cool-down")
                appendLine("Under Main, list 4-6 exercises as bullets in the form: \"Exercise Name — sets x reps\".")
                appendLine("End with one coaching tip line. Keep it concise.")
            }

            try {
                val response = generativeModel.generateContent(content { text(prompt) })
                val text = response.text
                if (text != null) {
                    // Plan results live in memory only — by design, they're discarded
                    // when the app is closed so the user always starts from a blank tab.
                    _uiState.value = UiState.Success(text)
                } else {
                    _uiState.value = UiState.Error("Empty response from AI.")
                }
            } catch (e: Exception) {
                Log.e("PlanViewModel", "generateContent failed", e)
                _uiState.value = UiState.Error(
                    com.example.fitness_tracker.ai.friendlyAiError(e),
                )
            }
        }
    }

    fun saveWeeklySplit(rows: List<WeeklySplitDay>) {
        viewModelScope.launch { repo.saveWeeklySplit(rows) }
    }

    suspend fun applyPlan(): Int {
        val text = (_uiState.value as? UiState.Success)?.outputText ?: return 0
        val names = extractExercisesFromPlan(text)
        if (names.isEmpty()) return 0
        val ids = repo.resolveOrCreateExercises(names)
        repo.stagePlan(ids)
        return ids.size
    }

    fun reset() {
        _uiState.value = UiState.Initial
    }
}
