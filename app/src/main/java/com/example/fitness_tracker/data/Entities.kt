package com.example.fitness_tracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseKind { REPS, TIME, DISTANCE }

@Serializable
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    @ColumnInfo(defaultValue = "REPS") val kind: ExerciseKind = ExerciseKind.REPS,
)

@Serializable
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    @ColumnInfo(defaultValue = "NULL") val endedAt: Long? = null,
    val notes: String = "",
)

@Entity(
    tableName = "pending_plan",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
@Serializable
data class PendingPlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val position: Int,
    /** epoch ms / 86_400_000 — same scheme the streak math uses. */
    val dayKey: Long,
)

@Serializable
enum class Readiness { TIRED, OK, STRONG }

@Serializable
@Entity(tableName = "body_weight_entry")
data class BodyWeightEntry(
    @PrimaryKey val dayKey: Long,
    val weightKg: Double,
    val performedAt: Long,
)

@Serializable
@Entity(tableName = "food_entry")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayKey: Long,
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val loggedAt: Long,
)

@Serializable
enum class DietType { VEG, NON_VEG, VEGAN, EGGETARIAN }

@Serializable
enum class MealCategory { BREAKFAST, LUNCH, DINNER, SNACK, HIGH_PROTEIN }

@Serializable
@Entity(tableName = "meal")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    /** Newline-separated ingredient lines. */
    val ingredients: String,
    /** Newline-separated step lines. */
    val steps: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val category: MealCategory,
    /** Comma-separated DietType names this meal fits (e.g. "VEG,VEGAN"). */
    val dietTypes: String,
    /** True for AI-generated meals; lets us scope deletion if needed. */
    val isAiGenerated: Boolean = false,
    val createdAt: Long = 0,
)

@Serializable
@Entity(tableName = "diet_preference")
data class DietPreference(
    /** Single-row table; always id=0. */
    @PrimaryKey val id: Int = 0,
    val type: DietType,
)

@Serializable
@Entity(tableName = "cached_diet_plan")
data class CachedDietPlan(
    @PrimaryKey val dayKey: Long,
    val direction: String,
    val markdown: String,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "calorie_goal")
data class CalorieGoal(
    /** Single-row table; always id=0. */
    @PrimaryKey val id: Int = 0,
    val targetCalories: Int,
    val proteinTargetG: Int?,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "weight_goal")
data class WeightGoal(
    /** Single-row table; always id=0. */
    @PrimaryKey val id: Int = 0,
    val targetKg: Double,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "readiness_entry")
data class ReadinessEntry(
    @PrimaryKey val dayKey: Long,
    val level: Readiness,
    val performedAt: Long,
)

@Serializable
@Entity(tableName = "water_entry")
data class WaterEntry(
    @PrimaryKey val dayKey: Long,
    val glasses: Int,
)

@Serializable
@Entity(tableName = "user_profile")
data class UserProfile(
    /** Single-row table; always id=0. */
    @PrimaryKey val id: Int = 0,
    val name: String,
    val email: String,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "reminder_settings")
data class ReminderSettings(
    /** Single-row table; always id=0. */
    @PrimaryKey val id: Int = 0,
    val enabled: Boolean,
    /** 0–23. */
    val hour: Int,
    /** 0–59. */
    val minute: Int,
)

@Serializable
@Entity(tableName = "cached_plan")
data class CachedPlan(
    @PrimaryKey val dayKey: Long,
    val focus: String,
    val markdown: String,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "weekly_split")
data class WeeklySplitDay(
    /** Calendar.DAY_OF_WEEK: 1 = Sunday … 7 = Saturday. */
    @PrimaryKey val dayOfWeek: Int,
    /** Free text — e.g. "Chest + Triceps", or empty for a rest day. */
    val focus: String,
)

@Serializable
@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "template_exercises",
    primaryKeys = ["templateId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
@Serializable
data class TemplateExercise(
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
)

@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
@Serializable
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val reps: Int,
    val weightKg: Double,
    @ColumnInfo(defaultValue = "0") val durationSec: Int = 0,
    @ColumnInfo(defaultValue = "0") val distanceMeters: Int = 0,
    val performedAt: Long,
)
