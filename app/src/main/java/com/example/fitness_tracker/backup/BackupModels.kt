package com.example.fitness_tracker.backup

import com.example.fitness_tracker.data.BodyWeightEntry
import com.example.fitness_tracker.data.CachedDietPlan
import com.example.fitness_tracker.data.CachedPlan
import com.example.fitness_tracker.data.CalorieGoal
import com.example.fitness_tracker.data.DietPreference
import com.example.fitness_tracker.data.Exercise
import com.example.fitness_tracker.data.FoodEntry
import com.example.fitness_tracker.data.Meal
import com.example.fitness_tracker.data.PendingPlanItem
import com.example.fitness_tracker.data.ReadinessEntry
import com.example.fitness_tracker.data.ReminderSettings
import com.example.fitness_tracker.data.SetEntry
import com.example.fitness_tracker.data.TemplateExercise
import com.example.fitness_tracker.data.UserProfile
import com.example.fitness_tracker.data.WaterEntry
import com.example.fitness_tracker.data.WeeklySplitDay
import com.example.fitness_tracker.data.WeightGoal
import com.example.fitness_tracker.data.WorkoutSession
import com.example.fitness_tracker.data.WorkoutTemplate
import kotlinx.serialization.Serializable

/**
 * Top-level backup payload. Bumps [schemaVersion] whenever the wire shape
 * changes — restore checks this and bails on unknown majors.
 *
 * The shape is "every Room table as a flat list, keyed by table name". Lets
 * future-us write a forward-compatible reader by simply ignoring fields it
 * doesn't recognise (kotlinx-serialization does this with [ignoreUnknownKeys]).
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val exercises: List<Exercise> = emptyList(),
    val workoutSessions: List<WorkoutSession> = emptyList(),
    val setEntries: List<SetEntry> = emptyList(),
    val workoutTemplates: List<WorkoutTemplate> = emptyList(),
    val templateExercises: List<TemplateExercise> = emptyList(),
    val weeklySplit: List<WeeklySplitDay> = emptyList(),
    val pendingPlan: List<PendingPlanItem> = emptyList(),
    val cachedPlans: List<CachedPlan> = emptyList(),
    val reminderSettings: List<ReminderSettings> = emptyList(),
    val userProfile: List<UserProfile> = emptyList(),
    val bodyWeights: List<BodyWeightEntry> = emptyList(),
    val readiness: List<ReadinessEntry> = emptyList(),
    val water: List<WaterEntry> = emptyList(),
    val weightGoals: List<WeightGoal> = emptyList(),
    val foodEntries: List<FoodEntry> = emptyList(),
    val calorieGoals: List<CalorieGoal> = emptyList(),
    val cachedDietPlans: List<CachedDietPlan> = emptyList(),
    val meals: List<Meal> = emptyList(),
    val dietPreferences: List<DietPreference> = emptyList(),
)

/** Bumps when the wire format breaks compatibility. v1 is the initial release. */
internal const val BACKUP_SCHEMA_VERSION = 1

/** Mirrors versionName in app/build.gradle.kts. Bump in lockstep. */
internal const val APP_VERSION_NAME = "1.0"
