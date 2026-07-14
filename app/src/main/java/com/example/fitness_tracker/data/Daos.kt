package com.example.fitness_tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByNameCaseInsensitive(name: String): Exercise?

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Update
    suspend fun update(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSession?

    @Query(
        "SELECT * FROM workout_sessions " +
            "WHERE startedAt >= :sinceMs AND endedAt IS NULL " +
            "ORDER BY startedAt DESC LIMIT 1"
    )
    suspend fun mostRecentOpenSince(sinceMs: Long): WorkoutSession?

    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Query("UPDATE workout_sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun markEnded(id: Long, endedAt: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SetEntryDao {
    @Insert
    suspend fun insert(set: SetEntry): Long

    @Update
    suspend fun update(set: SetEntry)

    @Query(
        "SELECT s.*, e.name AS exerciseName, e.kind AS exerciseKind, e.muscleGroup AS muscleGroup " +
            "FROM set_entries s " +
            "INNER JOIN exercises e ON e.id = s.exerciseId " +
            "WHERE s.sessionId = :sessionId ORDER BY s.performedAt ASC"
    )
    fun observeForSession(sessionId: Long): Flow<List<SetWithExerciseRow>>

    @Query("SELECT COUNT(*) FROM set_entries WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: Long): Int

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun getById(id: Long): SetEntry?

    @Query(
        "SELECT * FROM set_entries WHERE exerciseId = :exerciseId " +
            "AND sessionId != :excludeSessionId " +
            "ORDER BY performedAt DESC LIMIT 1"
    )
    suspend fun lastForExercise(exerciseId: Long, excludeSessionId: Long): SetEntry?

    @Query("SELECT * FROM set_entries WHERE exerciseId = :exerciseId")
    suspend fun allForExercise(exerciseId: Long): List<SetEntry>

    @Query("SELECT performedAt FROM set_entries ORDER BY performedAt ASC")
    fun observeAllSetTimestamps(): Flow<List<Long>>

    @Query(
        "SELECT s.*, e.name AS exerciseName, e.kind AS exerciseKind, e.muscleGroup AS muscleGroup " +
            "FROM set_entries s INNER JOIN exercises e ON e.id = s.exerciseId " +
            "WHERE s.performedAt >= :sinceMs ORDER BY s.performedAt ASC"
    )
    suspend fun setsSince(sinceMs: Long): List<SetWithExerciseRow>

    @Query(
        "SELECT s.*, e.name AS exerciseName, e.kind AS exerciseKind, e.muscleGroup AS muscleGroup " +
            "FROM set_entries s INNER JOIN exercises e ON e.id = s.exerciseId " +
            "WHERE s.sessionId = :sessionId ORDER BY s.performedAt ASC"
    )
    suspend fun setsForSession(sessionId: Long): List<SetWithExerciseRow>

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PendingPlanDao {
    @Query("SELECT exerciseId FROM pending_plan WHERE dayKey = :dayKey ORDER BY position ASC")
    fun observeForDay(dayKey: Long): Flow<List<Long>>

    @Query("DELETE FROM pending_plan")
    suspend fun clearAll()

    @Query("DELETE FROM pending_plan WHERE dayKey != :keepDayKey")
    suspend fun clearStale(keepDayKey: Long)

    @Insert
    suspend fun insertAll(items: List<PendingPlanItem>)
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE dayKey = :dayKey ORDER BY loggedAt DESC")
    fun observeForDay(dayKey: Long): Flow<List<FoodEntry>>

    @Query(
        "SELECT " +
            "COALESCE(SUM(calories), 0) AS calories, " +
            "COALESCE(SUM(proteinG), 0) AS protein, " +
            "COALESCE(SUM(carbsG), 0) AS carbs, " +
            "COALESCE(SUM(fatG), 0) AS fat " +
            "FROM food_entry WHERE dayKey = :dayKey"
    )
    fun observeTotalsForDay(dayKey: Long): Flow<MacroTotals>

    @Query(
        "SELECT " +
            "COALESCE(SUM(calories), 0) AS calories, " +
            "COALESCE(SUM(proteinG), 0) AS protein, " +
            "COALESCE(SUM(carbsG), 0) AS carbs, " +
            "COALESCE(SUM(fatG), 0) AS fat " +
            "FROM food_entry WHERE dayKey = :dayKey"
    )
    suspend fun getTotalsForDay(dayKey: Long): MacroTotals

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Query("DELETE FROM food_entry WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM food_entry WHERE id = :id")
    suspend fun getById(id: Long): FoodEntry?
}

data class MacroTotals(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

@Dao
interface MealDao {
    @Query("SELECT * FROM meal ORDER BY category ASC, isAiGenerated ASC, id ASC")
    fun observeAll(): Flow<List<Meal>>

    @Query("SELECT COUNT(*) FROM meal")
    suspend fun count(): Int

    @Query("SELECT * FROM meal WHERE id = :id")
    suspend fun getById(id: Long): Meal?

    @Insert
    suspend fun insert(meal: Meal): Long

    @Insert
    suspend fun insertAll(meals: List<Meal>)

    @Query("DELETE FROM meal WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DietPreferenceDao {
    @Query("SELECT * FROM diet_preference WHERE id = 0 LIMIT 1")
    fun observe(): Flow<DietPreference?>

    @Query("SELECT * FROM diet_preference WHERE id = 0 LIMIT 1")
    suspend fun get(): DietPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: DietPreference)
}

@Dao
interface CachedDietPlanDao {
    @Query("SELECT * FROM cached_diet_plan WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getForDay(dayKey: Long): CachedDietPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: CachedDietPlan)

    @Query("DELETE FROM cached_diet_plan WHERE dayKey != :keepDayKey")
    suspend fun clearStale(keepDayKey: Long)
}

@Dao
interface CalorieGoalDao {
    @Query("SELECT * FROM calorie_goal WHERE id = 0 LIMIT 1")
    fun observe(): Flow<CalorieGoal?>

    @Query("SELECT * FROM calorie_goal WHERE id = 0 LIMIT 1")
    suspend fun get(): CalorieGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: CalorieGoal)

    @Query("DELETE FROM calorie_goal")
    suspend fun clear()
}

@Dao
interface WeightGoalDao {
    @Query("SELECT * FROM weight_goal WHERE id = 0 LIMIT 1")
    fun observe(): Flow<WeightGoal?>

    @Query("SELECT * FROM weight_goal WHERE id = 0 LIMIT 1")
    suspend fun get(): WeightGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: WeightGoal)

    @Query("DELETE FROM weight_goal")
    suspend fun clear()
}

@Dao
interface DailyTrackerDao {
    // Body weight
    @Query("SELECT * FROM body_weight_entry WHERE dayKey = :dayKey LIMIT 1")
    fun observeBodyWeightForDay(dayKey: Long): Flow<BodyWeightEntry?>

    @Query("SELECT * FROM body_weight_entry ORDER BY dayKey DESC LIMIT 30")
    fun observeBodyWeightHistory(): Flow<List<BodyWeightEntry>>

    @Query("SELECT * FROM body_weight_entry ORDER BY dayKey DESC LIMIT 1")
    suspend fun getMostRecentBodyWeight(): BodyWeightEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBodyWeight(entry: BodyWeightEntry)

    // Readiness
    @Query("SELECT * FROM readiness_entry WHERE dayKey = :dayKey LIMIT 1")
    fun observeReadinessForDay(dayKey: Long): Flow<ReadinessEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadiness(entry: ReadinessEntry)

    // Water
    @Query("SELECT * FROM water_entry WHERE dayKey = :dayKey LIMIT 1")
    fun observeWaterForDay(dayKey: Long): Flow<WaterEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWater(entry: WaterEntry)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    fun observe(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    suspend fun get(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Dao
interface ReminderSettingsDao {
    @Query("SELECT * FROM reminder_settings WHERE id = 0 LIMIT 1")
    fun observe(): Flow<ReminderSettings?>

    @Query("SELECT * FROM reminder_settings WHERE id = 0 LIMIT 1")
    suspend fun get(): ReminderSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ReminderSettings)
}

@Dao
interface CachedPlanDao {
    @Query("SELECT * FROM cached_plan WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getForDay(dayKey: Long): CachedPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: CachedPlan)

    @Query("DELETE FROM cached_plan WHERE dayKey != :keepDayKey")
    suspend fun clearStale(keepDayKey: Long)

    @Query("DELETE FROM cached_plan")
    suspend fun clearAll()
}

@Dao
interface WeeklySplitDao {
    @Query("SELECT * FROM weekly_split ORDER BY dayOfWeek ASC")
    fun observeAll(): Flow<List<WeeklySplitDay>>

    @Query("SELECT * FROM weekly_split WHERE dayOfWeek = :dayOfWeek")
    suspend fun get(dayOfWeek: Int): WeeklySplitDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<WeeklySplitDay>)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM workout_templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WorkoutTemplate>>

    @Query("SELECT COUNT(*) FROM workout_templates")
    suspend fun count(): Int

    @Insert
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Insert
    suspend fun insertItems(items: List<TemplateExercise>)

    @Query(
        "SELECT exerciseId FROM template_exercises " +
            "WHERE templateId = :templateId ORDER BY position ASC"
    )
    suspend fun exerciseIdsForTemplate(templateId: Long): List<Long>

    @Query(
        "SELECT te.templateId AS templateId, " +
            "(SELECT e.muscleGroup FROM template_exercises te2 " +
            " INNER JOIN exercises e ON e.id = te2.exerciseId " +
            " WHERE te2.templateId = te.templateId " +
            " GROUP BY e.muscleGroup ORDER BY COUNT(*) DESC LIMIT 1) AS muscleGroup " +
            "FROM template_exercises te GROUP BY te.templateId"
    )
    fun observeDominantGroups(): Flow<List<TemplateMuscleRow>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getById(id: Long): WorkoutTemplate?

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun delete(id: Long)
}

data class TemplateMuscleRow(
    val templateId: Long,
    val muscleGroup: String?,
)

data class MuscleGroupTally(
    val muscleGroup: String,
    val sets: Int,
    val lastTrainedAt: Long,
)

data class ExerciseTally(
    val name: String,
    val sets: Int,
    val lastWeightKg: Double,
    val lastReps: Int,
)

data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRm: Double,
    val performedAt: Long,
)

data class ExerciseSeriesPoint(
    val performedAt: Long,
    val reps: Int,
    val weightKg: Double,
    val estimatedOneRm: Double,
)

data class VolumeTotals(
    val weightKgReps: Double,    // Σ weightKg * reps  (REPS exercises)
    val durationSec: Long,       // Σ durationSec      (TIME exercises)
    val distanceMeters: Long,    // Σ distanceMeters   (DISTANCE exercises)
    val totalSets: Int,
    val totalSessions: Int,
)

@Dao
interface AggregateDao {
    @Query(
        "SELECT e.muscleGroup AS muscleGroup, COUNT(*) AS sets, " +
            "MAX(s.performedAt) AS lastTrainedAt " +
            "FROM set_entries s INNER JOIN exercises e ON e.id = s.exerciseId " +
            "WHERE s.performedAt >= :sinceMs " +
            "GROUP BY e.muscleGroup ORDER BY sets DESC"
    )
    suspend fun muscleGroupTalliesSince(sinceMs: Long): List<MuscleGroupTally>

    @Query(
        "SELECT " +
            "COALESCE(SUM(s.weightKg * s.reps), 0) AS weightKgReps, " +
            "COALESCE(SUM(s.durationSec), 0) AS durationSec, " +
            "COALESCE(SUM(s.distanceMeters), 0) AS distanceMeters, " +
            "COUNT(*) AS totalSets, " +
            "COUNT(DISTINCT s.sessionId) AS totalSessions " +
            "FROM set_entries s WHERE s.performedAt >= :sinceMs"
    )
    suspend fun volumeSince(sinceMs: Long): VolumeTotals

    @Query("SELECT COUNT(DISTINCT s.exerciseId) FROM set_entries s WHERE s.performedAt >= :sinceMs")
    fun observeDistinctExercisesSince(sinceMs: Long): Flow<Int>

    @Query(
        "SELECT " +
            "COALESCE(SUM(s.weightKg * s.reps), 0) AS weightKgReps, " +
            "COALESCE(SUM(s.durationSec), 0) AS durationSec, " +
            "COALESCE(SUM(s.distanceMeters), 0) AS distanceMeters, " +
            "COUNT(*) AS totalSets, " +
            "COUNT(DISTINCT s.sessionId) AS totalSessions " +
            "FROM set_entries s WHERE s.performedAt >= :sinceMs"
    )
    fun observeVolumeSince(sinceMs: Long): Flow<VolumeTotals>

    @Query(
        "SELECT e.name AS name, COUNT(*) AS sets, " +
            "(SELECT s2.weightKg FROM set_entries s2 WHERE s2.exerciseId = e.id ORDER BY s2.performedAt DESC LIMIT 1) AS lastWeightKg, " +
            "(SELECT s2.reps FROM set_entries s2 WHERE s2.exerciseId = e.id ORDER BY s2.performedAt DESC LIMIT 1) AS lastReps " +
            "FROM set_entries s INNER JOIN exercises e ON e.id = s.exerciseId " +
            "WHERE s.performedAt >= :sinceMs " +
            "GROUP BY e.id ORDER BY sets DESC LIMIT :limit"
    )
    suspend fun topExercisesSince(sinceMs: Long, limit: Int): List<ExerciseTally>

    /**
     * Best estimated-1RM set for each exercise (REPS-kind only), ranked desc.
     * Epley estimate: w * (1 + reps/30).
     */
    @Query(
        """
        SELECT t.exerciseId AS exerciseId,
               e.name AS exerciseName,
               e.muscleGroup AS muscleGroup,
               t.reps AS reps,
               t.weightKg AS weightKg,
               t.estimatedOneRm AS estimatedOneRm,
               t.performedAt AS performedAt
        FROM (
            SELECT s.exerciseId AS exerciseId,
                   s.reps AS reps,
                   s.weightKg AS weightKg,
                   s.weightKg * (1.0 + s.reps / 30.0) AS estimatedOneRm,
                   s.performedAt AS performedAt,
                   ROW_NUMBER() OVER (
                       PARTITION BY s.exerciseId
                       ORDER BY s.weightKg * (1.0 + s.reps / 30.0) DESC, s.performedAt DESC
                   ) AS rk
            FROM set_entries s
            INNER JOIN exercises e ON e.id = s.exerciseId
            WHERE s.weightKg > 0 AND s.reps > 0 AND e.kind = 'REPS'
        ) t
        INNER JOIN exercises e ON e.id = t.exerciseId
        WHERE t.rk = 1
        ORDER BY t.estimatedOneRm DESC
        LIMIT :limit
        """
    )
    suspend fun personalRecords(limit: Int): List<PersonalRecord>

    @Query(
        """
        SELECT s.performedAt AS performedAt,
               s.reps AS reps,
               s.weightKg AS weightKg,
               s.weightKg * (1.0 + s.reps / 30.0) AS estimatedOneRm
        FROM set_entries s
        WHERE s.exerciseId = :exerciseId AND s.weightKg > 0 AND s.reps > 0
        ORDER BY s.performedAt ASC
        """
    )
    suspend fun seriesForExercise(exerciseId: Long): List<ExerciseSeriesPoint>

    /**
     * IDs of the best-1RM set for each exercise — same Epley ranking as
     * [personalRecords] but returns just the set_entries.id so the Log row
     * renderer can flag PRs inline. Reactive: re-emits whenever a new set
     * is logged that beats the current PR for that exercise.
     */
    @Query(
        """
        SELECT t.id AS id
        FROM (
            SELECT s.id AS id,
                   ROW_NUMBER() OVER (
                       PARTITION BY s.exerciseId
                       ORDER BY s.weightKg * (1.0 + s.reps / 30.0) DESC, s.performedAt DESC
                   ) AS rk
            FROM set_entries s
            INNER JOIN exercises e ON e.id = s.exerciseId
            WHERE s.weightKg > 0 AND s.reps > 0 AND e.kind = 'REPS'
        ) t
        WHERE t.rk = 1
        """
    )
    fun observePrSetIds(): Flow<List<Long>>
}

data class SetWithExerciseRow(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val reps: Int,
    val weightKg: Double,
    val durationSec: Int,
    val distanceMeters: Int,
    val performedAt: Long,
    val exerciseName: String,
    val exerciseKind: ExerciseKind,
    val muscleGroup: String,
)
