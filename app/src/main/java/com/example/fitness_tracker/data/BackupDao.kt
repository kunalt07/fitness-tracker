package com.example.fitness_tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Bulk read/write surface used only by the backup/restore flow. Centralizes
 * the queries the regular DAOs don't expose — full-table scans for export,
 * full-table deletes for restore. No app code outside [com.example.fitness_tracker.backup]
 * should call these.
 */
@Dao
interface BackupDao {
    // --- Full-table reads -------------------------------------------------

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<Exercise>

    @Query("SELECT * FROM workout_sessions")
    suspend fun allSessions(): List<WorkoutSession>

    @Query("SELECT * FROM set_entries")
    suspend fun allSetEntries(): List<SetEntry>

    @Query("SELECT * FROM workout_templates")
    suspend fun allTemplates(): List<WorkoutTemplate>

    @Query("SELECT * FROM template_exercises")
    suspend fun allTemplateExercises(): List<TemplateExercise>

    @Query("SELECT * FROM weekly_split")
    suspend fun allWeeklySplit(): List<WeeklySplitDay>

    @Query("SELECT * FROM pending_plan")
    suspend fun allPendingPlan(): List<PendingPlanItem>

    @Query("SELECT * FROM cached_plan")
    suspend fun allCachedPlans(): List<CachedPlan>

    @Query("SELECT * FROM reminder_settings")
    suspend fun allReminderSettings(): List<ReminderSettings>

    @Query("SELECT * FROM user_profile")
    suspend fun allUserProfiles(): List<UserProfile>

    @Query("SELECT * FROM body_weight_entry")
    suspend fun allBodyWeights(): List<BodyWeightEntry>

    @Query("SELECT * FROM readiness_entry")
    suspend fun allReadiness(): List<ReadinessEntry>

    @Query("SELECT * FROM water_entry")
    suspend fun allWater(): List<WaterEntry>

    @Query("SELECT * FROM weight_goal")
    suspend fun allWeightGoals(): List<WeightGoal>

    @Query("SELECT * FROM food_entry")
    suspend fun allFoodEntries(): List<FoodEntry>

    @Query("SELECT * FROM calorie_goal")
    suspend fun allCalorieGoals(): List<CalorieGoal>

    @Query("SELECT * FROM cached_diet_plan")
    suspend fun allCachedDietPlans(): List<CachedDietPlan>

    @Query("SELECT * FROM meal")
    suspend fun allMeals(): List<Meal>

    @Query("SELECT * FROM diet_preference")
    suspend fun allDietPreferences(): List<DietPreference>

    // --- Full-table deletes ----------------------------------------------
    // Order matters at the call site: child rows go first because of FKs.

    @Query("DELETE FROM set_entries") suspend fun clearSetEntries()
    @Query("DELETE FROM template_exercises") suspend fun clearTemplateExercises()
    @Query("DELETE FROM pending_plan") suspend fun clearPendingPlan()
    @Query("DELETE FROM workout_sessions") suspend fun clearSessions()
    @Query("DELETE FROM workout_templates") suspend fun clearTemplates()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM weekly_split") suspend fun clearWeeklySplit()
    @Query("DELETE FROM cached_plan") suspend fun clearCachedPlans()
    @Query("DELETE FROM reminder_settings") suspend fun clearReminderSettings()
    @Query("DELETE FROM user_profile") suspend fun clearUserProfile()
    @Query("DELETE FROM body_weight_entry") suspend fun clearBodyWeights()
    @Query("DELETE FROM readiness_entry") suspend fun clearReadiness()
    @Query("DELETE FROM water_entry") suspend fun clearWater()
    @Query("DELETE FROM weight_goal") suspend fun clearWeightGoals()
    @Query("DELETE FROM food_entry") suspend fun clearFoodEntries()
    @Query("DELETE FROM calorie_goal") suspend fun clearCalorieGoals()
    @Query("DELETE FROM cached_diet_plan") suspend fun clearCachedDietPlans()
    @Query("DELETE FROM meal") suspend fun clearMeals()
    @Query("DELETE FROM diet_preference") suspend fun clearDietPreferences()

    // --- Bulk inserts ----------------------------------------------------
    // REPLACE so a re-run of restore is idempotent.

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(rows: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(rows: List<WorkoutSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetEntries(rows: List<SetEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(rows: List<WorkoutTemplate>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(rows: List<TemplateExercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySplit(rows: List<WeeklySplitDay>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingPlan(rows: List<PendingPlanItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedPlans(rows: List<CachedPlan>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderSettings(rows: List<ReminderSettings>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfiles(rows: List<UserProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeights(rows: List<BodyWeightEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadiness(rows: List<ReadinessEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(rows: List<WaterEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightGoals(rows: List<WeightGoal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntries(rows: List<FoodEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalorieGoals(rows: List<CalorieGoal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedDietPlans(rows: List<CachedDietPlan>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(rows: List<Meal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietPreferences(rows: List<DietPreference>)
}
