package com.example.fitness_tracker.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_tracker.data.FitnessRepository
import com.example.fitness_tracker.data.SetWithExerciseRow
import com.example.fitness_tracker.data.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FitnessRepository.get(app)

    val sessions: StateFlow<List<WorkoutSession>> =
        repo.sessions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _expandedSessionId = MutableStateFlow<Long?>(null)
    val expandedSessionId: StateFlow<Long?> = _expandedSessionId.asStateFlow()

    @Suppress("OPT_IN_USAGE")
    val expandedSets: StateFlow<List<SetWithExerciseRow>> =
        _expandedSessionId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.observeSetsForSession(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggle(sessionId: Long) {
        _expandedSessionId.value = if (_expandedSessionId.value == sessionId) null else sessionId
    }

    fun delete(sessionId: Long) {
        viewModelScope.launch {
            if (_expandedSessionId.value == sessionId) _expandedSessionId.value = null
            repo.deleteSession(sessionId)
        }
    }

    suspend fun deleteWithUndo(sessionId: Long): (suspend () -> Unit)? {
        val snapshot = repo.snapshotSession(sessionId) ?: return null
        if (_expandedSessionId.value == sessionId) _expandedSessionId.value = null
        repo.deleteSession(sessionId)
        return { repo.restoreSession(snapshot) }
    }
}
