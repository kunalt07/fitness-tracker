package com.example.fitness_tracker.data

import kotlinx.serialization.Serializable

/**
 * One AI-suggested "what to eat" meal from the Diet day plan. Not a Room entity —
 * the day plan is persisted as JSON in `cached_diet_plan` (per day) and shared
 * across screens via [FitnessRepository.dayPlan]. Serializable for that storage.
 */
@Serializable
data class PlannedMeal(
    val category: String,
    val dietType: String,
    val name: String,
    val description: String,
    val calories: Int,
    val proteinG: Int,
    val ingredients: List<String>,
    val steps: List<String>,
)
