package com.example.fitness_tracker.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_tracker.data.Exercise
import com.example.fitness_tracker.data.ExerciseSeriesPoint
import com.example.fitness_tracker.data.FitnessRepository
import com.example.fitness_tracker.data.MuscleGroupTally
import com.example.fitness_tracker.data.PersonalRecord
import com.example.fitness_tracker.data.VolumeTotals
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class StatsWindow(val days: Int?, val label: String) {
    Week(7, "Week"),
    Month(30, "Month"),
    All(null, "All time"),
}

data class StatsUiState(
    val window: StatsWindow = StatsWindow.Week,
    val totalSessions: Int = 0,
    val totalSets: Int = 0,
    val streakDays: Int = 0,
    val volume: VolumeTotals = VolumeTotals(0.0, 0L, 0L, 0, 0),
    val muscleBalance: List<MuscleGroupTally> = emptyList(),
    val weeklySetsPerDay: List<Int> = List(7) { 0 },
    val weekDayLabels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FitnessRepository.get(app)

    private val _window = MutableStateFlow(StatsWindow.Week)
    private val _windowState = MutableStateFlow(WindowState())

    private data class WindowState(
        val volume: VolumeTotals = VolumeTotals(0.0, 0L, 0L, 0, 0),
        val balance: List<MuscleGroupTally> = emptyList(),
    )

    private val streakAndWeekly: StateFlow<Pair<Int, List<Int>>> =
        repo.allSetTimestamps
            .map { computeStreakAndWeekly(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0 to List(7) { 0 })

    val state: StateFlow<StatsUiState> = combine(
        _window,
        _windowState,
        streakAndWeekly,
    ) { window, windowState, (streak, weekly) ->
        StatsUiState(
            window = window,
            totalSessions = windowState.volume.totalSessions,
            totalSets = windowState.volume.totalSets,
            streakDays = streak,
            volume = windowState.volume,
            muscleBalance = windowState.balance,
            weeklySetsPerDay = weekly,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())

    private val _personalRecords = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val personalRecords: StateFlow<List<PersonalRecord>> = _personalRecords.asStateFlow()

    /** Exercises that have at least one logged set with weight + reps — only these
     *  are pickable in the progress chart. */
    val chartableExercises: StateFlow<List<Exercise>> =
        combine(repo.exercises, repo.allSetTimestamps) { exercises, _ ->
            // Cheap filter: ask the DB which ones have ≥1 chartable point.
            exercises.filter { it.kind.name == "REPS" }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    private val _exerciseSeries = MutableStateFlow<List<ExerciseSeriesPoint>>(emptyList())
    val exerciseSeries: StateFlow<List<ExerciseSeriesPoint>> = _exerciseSeries.asStateFlow()

    private var refreshJob: Job? = null
    private var seriesJob: Job? = null

    init {
        refreshWindow()
        refreshAlwaysOn()
        // Re-fetch when set list grows so totals stay live.
        viewModelScope.launch {
            repo.allSetTimestamps.collect {
                refreshWindow()
                refreshAlwaysOn()
            }
        }
    }

    fun selectWindow(window: StatsWindow) {
        if (_window.value == window) return
        _window.value = window
        refreshWindow()
    }

    fun selectExercise(id: Long?) {
        _selectedExerciseId.value = id
        seriesJob?.cancel()
        if (id == null) {
            _exerciseSeries.value = emptyList()
            return
        }
        seriesJob = viewModelScope.launch {
            _exerciseSeries.value = repo.seriesForExercise(id)
        }
    }

    private fun refreshAlwaysOn() {
        viewModelScope.launch {
            _personalRecords.value = repo.personalRecords(limit = 5)
            _selectedExerciseId.value?.let { id ->
                _exerciseSeries.value = repo.seriesForExercise(id)
            }
        }
    }

    private fun refreshWindow() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val window = _window.value
            val sinceMs = window.days?.let {
                System.currentTimeMillis() - it * DAY_MS
            } ?: 0L
            val volume = repo.volumeSince(sinceMs)
            val balance = repo.muscleGroupTalliesSince(sinceMs)
            _windowState.value = WindowState(volume = volume, balance = balance)
        }
    }

    private fun computeStreakAndWeekly(timestamps: List<Long>): Pair<Int, List<Int>> {
        val now = System.currentTimeMillis()
        val mondayMidnight = mondayMidnightMs(now)
        val weekly = IntArray(7)
        timestamps.forEach { ts ->
            if (ts >= mondayMidnight) {
                val dayOffset = ((ts - mondayMidnight) / DAY_MS).toInt()
                if (dayOffset in 0..6) weekly[dayOffset] += 1
            }
        }

        val daysWithSets = timestamps.map { dayKey(it) }.toSet()
        val todayKey = dayKey(now)
        var streak = 0
        var probe = todayKey
        if (todayKey !in daysWithSets) probe = todayKey - 1
        while (probe in daysWithSets) {
            streak += 1
            probe -= 1
        }
        return streak to weekly.toList()
    }

    private fun dayKey(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / DAY_MS
    }

    private fun mondayMidnightMs(nowMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val daysFromMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        return cal.timeInMillis
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
