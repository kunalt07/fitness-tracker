package com.example.fitness_tracker.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_tracker.data.BodyWeightEntry
import com.example.fitness_tracker.data.FitnessRepository
import com.example.fitness_tracker.data.Readiness
import com.example.fitness_tracker.data.ReadinessEntry
import com.example.fitness_tracker.data.WaterEntry
import com.example.fitness_tracker.data.WeightGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DailyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FitnessRepository.get(app)

    val bodyWeightToday: StateFlow<BodyWeightEntry?> =
        repo.observeBodyWeightForDay()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val readinessToday: StateFlow<ReadinessEntry?> =
        repo.observeReadinessForDay()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val waterToday: StateFlow<WaterEntry?> =
        repo.observeWaterForDay()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val weightGoal: StateFlow<WeightGoal?> =
        repo.weightGoal.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveBodyWeight(weightKg: Double, targetKg: Double?) {
        viewModelScope.launch {
            repo.saveBodyWeight(weightKg)
            if (targetKg != null && targetKg > 0) {
                repo.saveWeightGoal(targetKg)
            }
        }
    }

    fun saveReadiness(level: Readiness) {
        viewModelScope.launch { repo.saveReadiness(level) }
    }

    fun bumpWater(delta: Int) {
        val current = waterToday.value?.glasses ?: 0
        viewModelScope.launch { repo.setWaterGlasses(current + delta) }
    }
}
