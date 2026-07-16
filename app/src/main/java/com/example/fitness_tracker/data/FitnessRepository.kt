@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.fitness_tracker.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class FitnessRepository(
    private val exerciseDao: ExerciseDao,
    private val sessionDao: SessionDao,
    private val setEntryDao: SetEntryDao,
    private val templateDao: TemplateDao,
    private val aggregateDao: AggregateDao,
    private val weeklySplitDao: WeeklySplitDao,
    private val pendingPlanDao: PendingPlanDao,
    private val cachedPlanDao: CachedPlanDao,
    private val reminderSettingsDao: ReminderSettingsDao,
    private val userProfileDao: UserProfileDao,
    private val dailyTrackerDao: DailyTrackerDao,
    private val weightGoalDao: WeightGoalDao,
    private val foodEntryDao: FoodEntryDao,
    private val calorieGoalDao: CalorieGoalDao,
    private val cachedDietPlanDao: CachedDietPlanDao,
    private val mealDao: MealDao,
    private val dietPreferenceDao: DietPreferenceDao,
) {
    /**
     * Current day key, re-checked every minute so any "today" query rolls over
     * at midnight even while the process stays warm. distinctUntilChanged means
     * downstream Room queries only re-subscribe when the day actually changes.
     * Declared first so the "today" observers below can build off it.
     */
    private val dayKeyFlow: Flow<Long> = flow {
        while (true) {
            emit(todayKey())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    val meals: Flow<List<Meal>> = mealDao.observeAll()
    val dietPreference: Flow<DietPreference?> = dietPreferenceDao.observe()

    suspend fun seedDefaultMealsIfEmpty() {
        if (mealDao.count() == 0) {
            mealDao.insertAll(SeedMeals.list())
        }
    }

    suspend fun saveDietPreference(type: DietType) {
        dietPreferenceDao.upsert(DietPreference(id = 0, type = type))
    }

    suspend fun getDietPreference(): DietPreference? = dietPreferenceDao.get()

    suspend fun addMeal(meal: Meal): Long = mealDao.insert(meal)

    suspend fun deleteMeal(id: Long) = mealDao.delete(id)

    suspend fun getMeal(id: Long): Meal? = mealDao.getById(id)
    suspend fun cachedDietPlanForToday(): CachedDietPlan? =
        cachedDietPlanDao.getForDay(todayKey())

    suspend fun cacheDietPlan(direction: String, markdown: String) {
        cachedDietPlanDao.upsert(
            CachedDietPlan(
                dayKey = todayKey(),
                direction = direction,
                markdown = markdown,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun pruneStaleDietPlan() = cachedDietPlanDao.clearStale(todayKey())

    /**
     * Pulls current weight + target if both exist; null otherwise.
     * Used to derive the diet direction (cut/bulk/maintain).
     */
    suspend fun currentWeightAndTarget(): Pair<Double, Double>? {
        val current = dailyTrackerDao.getMostRecentBodyWeight() ?: return null
        val goal = weightGoalDao.get() ?: return null
        return current.weightKg to goal.targetKg
    }
    fun observeFoodsForDay(): Flow<List<FoodEntry>> =
        dayKeyFlow.flatMapLatest { foodEntryDao.observeForDay(it) }

    fun observeMacroTotalsForDay(): Flow<MacroTotals> =
        dayKeyFlow.flatMapLatest { foodEntryDao.observeTotalsForDay(it) }

    suspend fun addFood(
        name: String,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        now: Long = System.currentTimeMillis(),
    ): Long = foodEntryDao.insert(
        FoodEntry(
            dayKey = todayKey(),
            name = name,
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            loggedAt = now,
        ),
    )

    suspend fun deleteFood(id: Long) = foodEntryDao.delete(id)

    suspend fun getFood(id: Long): FoodEntry? = foodEntryDao.getById(id)

    val calorieGoal: Flow<CalorieGoal?> = calorieGoalDao.observe()

    suspend fun getCalorieGoal(): CalorieGoal? = calorieGoalDao.get()

    suspend fun saveCalorieGoal(targetCalories: Int, proteinTargetG: Int?) {
        calorieGoalDao.upsert(
            CalorieGoal(
                id = 0,
                targetCalories = targetCalories,
                proteinTargetG = proteinTargetG,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** One-line nutrition summary for the AI plan prompt; null if nothing logged. */
    suspend fun nutritionContext(): String? {
        val totals = foodEntryDao.getTotalsForDay(todayKey())
        if (totals.calories == 0 && totals.protein == 0) return null
        val goal = calorieGoalDao.get()
        return if (goal != null) {
            "Today's nutrition: ${totals.calories} / ${goal.targetCalories} kcal " +
                "(${totals.protein}g protein)."
        } else {
            "Today's nutrition: ${totals.calories} kcal (${totals.protein}g protein)."
        }
    }
    val weightGoal: Flow<WeightGoal?> = weightGoalDao.observe()

    suspend fun getWeightGoal(): WeightGoal? = weightGoalDao.get()

    suspend fun saveWeightGoal(targetKg: Double, now: Long = System.currentTimeMillis()) {
        weightGoalDao.upsert(WeightGoal(id = 0, targetKg = targetKg, updatedAt = now))
    }

    suspend fun clearWeightGoal() = weightGoalDao.clear()

    /**
     * Build a one-line context string for the AI plan prompt based on current
     * weight + target. Returns null if either piece is missing.
     *
     * Two-state: bulk vs cut, with maintenance only when current == target.
     */
    suspend fun weightGoalContext(): String? {
        val goal = weightGoalDao.get() ?: return null
        val current = dailyTrackerDao.getMostRecentBodyWeight() ?: return null
        return formatWeightGoalContext(currentKg = current.weightKg, targetKg = goal.targetKg)
    }
    fun observeBodyWeightForDay(): Flow<BodyWeightEntry?> =
        dayKeyFlow.flatMapLatest { dailyTrackerDao.observeBodyWeightForDay(it) }

    fun observeBodyWeightHistory(): Flow<List<BodyWeightEntry>> =
        dailyTrackerDao.observeBodyWeightHistory()

    suspend fun saveBodyWeight(weightKg: Double, now: Long = System.currentTimeMillis()) {
        dailyTrackerDao.upsertBodyWeight(
            BodyWeightEntry(dayKey = todayKey(), weightKg = weightKg, performedAt = now),
        )
    }

    fun observeReadinessForDay(): Flow<ReadinessEntry?> =
        dayKeyFlow.flatMapLatest { dailyTrackerDao.observeReadinessForDay(it) }

    suspend fun saveReadiness(level: Readiness, now: Long = System.currentTimeMillis()) {
        dailyTrackerDao.upsertReadiness(
            ReadinessEntry(dayKey = todayKey(), level = level, performedAt = now),
        )
    }

    fun observeWaterForDay(): Flow<WaterEntry?> =
        dayKeyFlow.flatMapLatest { dailyTrackerDao.observeWaterForDay(it) }

    suspend fun setWaterGlasses(glasses: Int) {
        dailyTrackerDao.upsertWater(
            WaterEntry(dayKey = todayKey(), glasses = glasses.coerceAtLeast(0)),
        )
    }
    val reminderSettings: Flow<ReminderSettings?> = reminderSettingsDao.observe()

    suspend fun getReminderSettings(): ReminderSettings? = reminderSettingsDao.get()

    suspend fun saveReminderSettings(settings: ReminderSettings) =
        reminderSettingsDao.upsert(settings)

    val userProfile: Flow<UserProfile?> = userProfileDao.observe()

    suspend fun getUserProfile(): UserProfile? = userProfileDao.get()

    suspend fun saveUserProfile(profile: UserProfile) = userProfileDao.upsert(profile)

    suspend fun clearUserProfile() = userProfileDao.clear()
    val exercises: Flow<List<Exercise>> = exerciseDao.observeAll()
    val sessions: Flow<List<WorkoutSession>> = sessionDao.observeAll()
    val allSetTimestamps: Flow<List<Long>> = setEntryDao.observeAllSetTimestamps()
    val templates: Flow<List<WorkoutTemplate>> = templateDao.observeAll()
    val templateDominantGroups: Flow<List<TemplateMuscleRow>> = templateDao.observeDominantGroups()
    val weeklySplit: Flow<List<WeeklySplitDay>> = weeklySplitDao.observeAll()

    /** Today's pending plan exercise IDs in order. Persisted; auto-clears on date change. */
    val pendingPlan: Flow<List<Long>> =
        dayKeyFlow.flatMapLatest { pendingPlanDao.observeForDay(it) }

    suspend fun stagePlan(exerciseIds: List<Long>) {
        val day = todayKey()
        // Drop any prior day's entries and replace today's with the new list.
        pendingPlanDao.clearAll()
        if (exerciseIds.isEmpty()) return
        pendingPlanDao.insertAll(
            exerciseIds.distinct().mapIndexed { idx, exId ->
                PendingPlanItem(exerciseId = exId, position = idx, dayKey = day)
            },
        )
    }

    suspend fun clearPlan() {
        pendingPlanDao.clearAll()
    }

    suspend fun pruneStalePlan() {
        pendingPlanDao.clearStale(keepDayKey = todayKey())
    }

    suspend fun cachedPlanForToday(): CachedPlan? = cachedPlanDao.getForDay(todayKey())

    suspend fun cachePlan(focus: String, markdown: String, now: Long = System.currentTimeMillis()) {
        cachedPlanDao.upsert(
            CachedPlan(
                dayKey = todayKey(),
                focus = focus,
                markdown = markdown,
                createdAt = now,
            ),
        )
    }

    suspend fun pruneStaleCachedPlan() {
        cachedPlanDao.clearStale(keepDayKey = todayKey())
    }

    suspend fun clearCachedPlan() {
        cachedPlanDao.clearAll()
    }

    private fun todayKey(): Long = System.currentTimeMillis() / (24L * 60 * 60 * 1000)

    private fun formatWeightGoalContext(currentKg: Double, targetKg: Double): String {
        val gap = targetKg - currentKg
        val cur = formatKg(currentKg)
        val tgt = formatKg(targetKg)
        return when {
            gap < -0.05 -> "User weight: $cur kg, target $tgt kg (cutting). " +
                "Bias the workout toward higher rep ranges (10-15), shorter rests (~60s), " +
                "and include one short cardio finisher."
            gap > 0.05 -> "User weight: $cur kg, target $tgt kg (bulking). " +
                "Bias the workout toward heavier compound lifts and lower rep ranges (4-8) " +
                "with longer rests (90-120s)."
            else -> "User weight: $cur kg (maintenance). Balanced rep ranges and rest times."
        }
    }

    private fun formatKg(kg: Double): String =
        if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.1f".format(kg)

    suspend fun seedDefaultExercisesIfEmpty() {
        // Backfill any default that isn't already present (case-insensitive by
        // name), so existing installs pick up the full per-muscle-group library.
        val existing = exerciseDao.allNames().map { it.lowercase() }.toSet()
        val missing = DEFAULT_EXERCISES.filter { it.name.lowercase() !in existing }
        if (missing.isNotEmpty()) exerciseDao.insertAll(missing)
    }

    suspend fun saveWeeklySplit(rows: List<WeeklySplitDay>) {
        weeklySplitDao.upsertAll(rows)
    }

    suspend fun focusForDay(dayOfWeek: Int): String? =
        weeklySplitDao.get(dayOfWeek)?.focus?.takeIf { it.isNotBlank() }

    suspend fun startSession(now: Long): Long =
        sessionDao.insert(WorkoutSession(startedAt = now))

    suspend fun endSessionMark(id: Long, now: Long) = sessionDao.markEnded(id, now)

    suspend fun deleteSession(id: Long) = sessionDao.delete(id)

    suspend fun getSession(id: Long): WorkoutSession? = sessionDao.getById(id)

    suspend fun mostRecentOpenSessionSince(sinceMs: Long): WorkoutSession? =
        sessionDao.mostRecentOpenSince(sinceMs)

    suspend fun lastSetForExercise(exerciseId: Long, excludeSessionId: Long = -1L): SetEntry? =
        setEntryDao.lastForExercise(exerciseId, excludeSessionId)

    suspend fun getExercise(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun resolveOrCreateExercises(names: List<String>): List<Long> =
        names.mapNotNull { resolveOrCreateExercise(it.trim()) }

    suspend fun resolveOrCreateExercise(name: String): Long? {
        if (name.isBlank()) return null
        val match = exerciseDao.findByNameCaseInsensitive(name)
        if (match != null) return match.id
        val titled = name.split(Regex("\\s+"))
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        val newId = exerciseDao.insert(Exercise(name = titled, muscleGroup = "Custom"))
        if (newId > 0) return newId
        return exerciseDao.findByNameCaseInsensitive(name)?.id
    }

    fun observeSetsForSession(sessionId: Long): Flow<List<SetWithExerciseRow>> =
        setEntryDao.observeForSession(sessionId)

    suspend fun getSetById(id: Long): SetEntry? = setEntryDao.getById(id)

    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        reps: Int,
        weightKg: Double,
        durationSec: Int,
        distanceMeters: Int,
        now: Long,
    ): Long = setEntryDao.insert(
        SetEntry(
            sessionId = sessionId,
            exerciseId = exerciseId,
            reps = reps,
            weightKg = weightKg,
            durationSec = durationSec,
            distanceMeters = distanceMeters,
            performedAt = now,
        )
    )

    suspend fun updateSet(
        id: Long,
        reps: Int,
        weightKg: Double,
        durationSec: Int,
        distanceMeters: Int,
    ) {
        val existing = setEntryDao.getById(id) ?: return
        setEntryDao.update(
            existing.copy(
                reps = reps,
                weightKg = weightKg,
                durationSec = durationSec,
                distanceMeters = distanceMeters,
            )
        )
    }

    suspend fun deleteSet(id: Long) = setEntryDao.delete(id)

    suspend fun setCount(sessionId: Long): Int = setEntryDao.countForSession(sessionId)

    suspend fun addExercise(name: String, muscleGroup: String, kind: ExerciseKind): Long =
        exerciseDao.insert(Exercise(name = name, muscleGroup = muscleGroup, kind = kind))

    suspend fun updateExercise(id: Long, name: String, muscleGroup: String, kind: ExerciseKind) {
        val existing = exerciseDao.getById(id) ?: return
        exerciseDao.update(
            existing.copy(name = name.trim().ifBlank { existing.name },
                muscleGroup = muscleGroup.trim().ifBlank { existing.muscleGroup },
                kind = kind),
        )
    }

    /** Snapshot exercise + every set referencing it before deletion (cascade
     *  will remove the sets — undo needs to put them back). */
    data class ExerciseSnapshot(val exercise: Exercise, val sets: List<SetEntry>)

    suspend fun snapshotExercise(id: Long): ExerciseSnapshot? {
        val ex = exerciseDao.getById(id) ?: return null
        val sets = setEntryDao.allForExercise(id)
        return ExerciseSnapshot(ex, sets)
    }

    suspend fun deleteExercise(id: Long) = exerciseDao.delete(id)

    suspend fun restoreExercise(snapshot: ExerciseSnapshot) {
        exerciseDao.insert(snapshot.exercise)
        snapshot.sets.forEach { setEntryDao.insert(it) }
    }

    suspend fun saveTemplate(name: String, exerciseIds: List<Long>, now: Long): Long {
        val templateId = templateDao.insertTemplate(
            WorkoutTemplate(name = name, createdAt = now),
        )
        if (exerciseIds.isNotEmpty()) {
            templateDao.insertItems(
                exerciseIds.distinct().mapIndexed { idx, exId ->
                    TemplateExercise(templateId = templateId, exerciseId = exId, position = idx)
                },
            )
        }
        return templateId
    }

    /** Rename a template and replace its exercise list in one shot. */
    suspend fun updateTemplate(id: Long, name: String, exerciseIds: List<Long>) {
        templateDao.rename(id, name)
        templateDao.clearItems(id)
        if (exerciseIds.isNotEmpty()) {
            templateDao.insertItems(
                exerciseIds.distinct().mapIndexed { idx, exId ->
                    TemplateExercise(templateId = id, exerciseId = exId, position = idx)
                },
            )
        }
    }

    suspend fun deleteTemplate(id: Long) = templateDao.delete(id)

    /** Restore a previously-deleted set. Re-uses its original id. */
    suspend fun restoreSet(set: SetEntry): Long = setEntryDao.insert(set)

    /** Snapshot session + its sets *before* deleting (call this from VM, then deleteSession). */
    data class SessionSnapshot(val session: WorkoutSession, val sets: List<SetEntry>)

    suspend fun snapshotSession(id: Long): SessionSnapshot? {
        val session = sessionDao.getById(id) ?: return null
        val sets = setEntryDao.setsForSession(id).map { row ->
            SetEntry(
                id = row.id,
                sessionId = row.sessionId,
                exerciseId = row.exerciseId,
                reps = row.reps,
                weightKg = row.weightKg,
                durationSec = row.durationSec,
                distanceMeters = row.distanceMeters,
                performedAt = row.performedAt,
            )
        }
        return SessionSnapshot(session, sets)
    }

    suspend fun restoreSession(snapshot: SessionSnapshot) {
        sessionDao.insert(snapshot.session)
        snapshot.sets.forEach { setEntryDao.insert(it) }
    }

    /** Snapshot a template (header + exercise links) before deleting. */
    data class TemplateSnapshot(val template: WorkoutTemplate, val items: List<TemplateExercise>)

    suspend fun snapshotTemplate(id: Long): TemplateSnapshot? {
        val tpl = templateDao.getById(id) ?: return null
        val items = templateDao.exerciseIdsForTemplate(id).mapIndexed { idx, exId ->
            TemplateExercise(templateId = id, exerciseId = exId, position = idx)
        }
        return TemplateSnapshot(tpl, items)
    }

    suspend fun restoreTemplate(snapshot: TemplateSnapshot) {
        templateDao.insertTemplate(snapshot.template)
        templateDao.insertItems(snapshot.items)
    }

    /**
     * Compact text summary of the user's last [days] of training, suitable
     * for prepending to an LLM prompt. Returns null if nothing has been logged.
     */
    suspend fun summarizeRecentHistory(days: Int = 14, now: Long = System.currentTimeMillis()): String? {
        val since = now - days * 24L * 60 * 60 * 1000
        val groups = aggregateDao.muscleGroupTalliesSince(since)
        val top = aggregateDao.topExercisesSince(since, limit = 5)
        if (groups.isEmpty()) return null

        return buildString {
            appendLine("Recent training (last $days days):")
            groups.forEach { g ->
                val daysAgo = ((now - g.lastTrainedAt) / (24L * 60 * 60 * 1000)).coerceAtLeast(0)
                val freshness = when (daysAgo) {
                    0L -> "today"
                    1L -> "yesterday"
                    else -> "${daysAgo}d ago"
                }
                appendLine("- ${g.muscleGroup}: ${g.sets} sets (last $freshness)")
            }
            if (top.isNotEmpty()) {
                appendLine("Most-used exercises:")
                top.forEach { ex ->
                    val weight = if (ex.lastWeightKg > 0)
                        " (last ${formatHistoryWeight(ex.lastWeightKg)} kg × ${ex.lastReps})"
                    else ""
                    appendLine("- ${ex.name}: ${ex.sets} sets$weight")
                }
            }
        }.trimEnd()
    }

    private fun formatHistoryWeight(kg: Double): String =
        if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.1f".format(kg)

    suspend fun setsForSession(sessionId: Long): List<SetWithExerciseRow> =
        setEntryDao.setsForSession(sessionId)

    suspend fun volumeSince(sinceMs: Long): VolumeTotals =
        aggregateDao.volumeSince(sinceMs)

    fun observeVolumeSince(sinceMs: Long): Flow<VolumeTotals> =
        aggregateDao.observeVolumeSince(sinceMs)

    fun observeDistinctExercisesSince(sinceMs: Long): Flow<Int> =
        aggregateDao.observeDistinctExercisesSince(sinceMs)

    suspend fun muscleGroupTalliesSince(sinceMs: Long): List<MuscleGroupTally> =
        aggregateDao.muscleGroupTalliesSince(sinceMs)

    suspend fun personalRecords(limit: Int = 5): List<PersonalRecord> =
        aggregateDao.personalRecords(limit)

    /** IDs of best-1RM sets per exercise. Reactive — flips on new PRs. */
    val prSetIds: Flow<List<Long>> = aggregateDao.observePrSetIds()

    suspend fun seriesForExercise(exerciseId: Long): List<ExerciseSeriesPoint> =
        aggregateDao.seriesForExercise(exerciseId)

    suspend fun exerciseIdsForTemplate(templateId: Long): List<Long> =
        templateDao.exerciseIdsForTemplate(templateId)

    companion object {
        @Volatile private var instance: FitnessRepository? = null

        fun get(context: Context): FitnessRepository =
            instance ?: synchronized(this) {
                instance ?: run {
                    val db = FitnessDatabase.get(context)
                    FitnessRepository(
                        db.exerciseDao(),
                        db.sessionDao(),
                        db.setEntryDao(),
                        db.templateDao(),
                        db.aggregateDao(),
                        db.weeklySplitDao(),
                        db.pendingPlanDao(),
                        db.cachedPlanDao(),
                        db.reminderSettingsDao(),
                        db.userProfileDao(),
                        db.dailyTrackerDao(),
                        db.weightGoalDao(),
                        db.foodEntryDao(),
                        db.calorieGoalDao(),
                        db.cachedDietPlanDao(),
                        db.mealDao(),
                        db.dietPreferenceDao(),
                    ).also { instance = it }
                }
            }

        private val DEFAULT_EXERCISES = listOf(
            // Chest
            Exercise(name = "Bench Press", muscleGroup = "Chest", kind = ExerciseKind.REPS),
            Exercise(name = "Incline Bench Press", muscleGroup = "Chest", kind = ExerciseKind.REPS),
            Exercise(name = "Dumbbell Bench Press", muscleGroup = "Chest", kind = ExerciseKind.REPS),
            Exercise(name = "Push-Up", muscleGroup = "Chest", kind = ExerciseKind.REPS),
            Exercise(name = "Chest Fly", muscleGroup = "Chest", kind = ExerciseKind.REPS),
            Exercise(name = "Cable Crossover", muscleGroup = "Chest", kind = ExerciseKind.REPS),
            Exercise(name = "Dip", muscleGroup = "Chest", kind = ExerciseKind.REPS),
            // Triceps
            Exercise(name = "Tricep Pushdown", muscleGroup = "Triceps", kind = ExerciseKind.REPS),
            Exercise(name = "Overhead Tricep Extension", muscleGroup = "Triceps", kind = ExerciseKind.REPS),
            Exercise(name = "Close-Grip Bench Press", muscleGroup = "Triceps", kind = ExerciseKind.REPS),
            Exercise(name = "Skull Crusher", muscleGroup = "Triceps", kind = ExerciseKind.REPS),
            Exercise(name = "Tricep Kickback", muscleGroup = "Triceps", kind = ExerciseKind.REPS),
            // Legs
            Exercise(name = "Back Squat", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            Exercise(name = "Front Squat", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            Exercise(name = "Lunge", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            Exercise(name = "Leg Press", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            Exercise(name = "Romanian Deadlift", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            Exercise(name = "Leg Extension", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            Exercise(name = "Leg Curl", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            Exercise(name = "Calf Raise", muscleGroup = "Legs", kind = ExerciseKind.REPS),
            // Shoulders
            Exercise(name = "Overhead Press", muscleGroup = "Shoulders", kind = ExerciseKind.REPS),
            Exercise(name = "Dumbbell Shoulder Press", muscleGroup = "Shoulders", kind = ExerciseKind.REPS),
            Exercise(name = "Lateral Raise", muscleGroup = "Shoulders", kind = ExerciseKind.REPS),
            Exercise(name = "Front Raise", muscleGroup = "Shoulders", kind = ExerciseKind.REPS),
            Exercise(name = "Rear Delt Fly", muscleGroup = "Shoulders", kind = ExerciseKind.REPS),
            Exercise(name = "Arnold Press", muscleGroup = "Shoulders", kind = ExerciseKind.REPS),
            Exercise(name = "Upright Row", muscleGroup = "Shoulders", kind = ExerciseKind.REPS),
            // Biceps
            Exercise(name = "Bicep Curl", muscleGroup = "Biceps", kind = ExerciseKind.REPS),
            Exercise(name = "Hammer Curl", muscleGroup = "Biceps", kind = ExerciseKind.REPS),
            Exercise(name = "Preacher Curl", muscleGroup = "Biceps", kind = ExerciseKind.REPS),
            Exercise(name = "Concentration Curl", muscleGroup = "Biceps", kind = ExerciseKind.REPS),
            Exercise(name = "Cable Curl", muscleGroup = "Biceps", kind = ExerciseKind.REPS),
            Exercise(name = "Chin-Up", muscleGroup = "Biceps", kind = ExerciseKind.REPS),
            // Core
            Exercise(name = "Plank", muscleGroup = "Core", kind = ExerciseKind.TIME),
            Exercise(name = "Crunch", muscleGroup = "Core", kind = ExerciseKind.REPS),
            Exercise(name = "Hanging Leg Raise", muscleGroup = "Core", kind = ExerciseKind.REPS),
            Exercise(name = "Russian Twist", muscleGroup = "Core", kind = ExerciseKind.REPS),
            Exercise(name = "Sit-Up", muscleGroup = "Core", kind = ExerciseKind.REPS),
            Exercise(name = "Bicycle Crunch", muscleGroup = "Core", kind = ExerciseKind.REPS),
            Exercise(name = "Dead Bug", muscleGroup = "Core", kind = ExerciseKind.REPS),
            // Back
            Exercise(name = "Deadlift", muscleGroup = "Back", kind = ExerciseKind.REPS),
            Exercise(name = "Barbell Row", muscleGroup = "Back", kind = ExerciseKind.REPS),
            Exercise(name = "Pull-Up", muscleGroup = "Back", kind = ExerciseKind.REPS),
            Exercise(name = "Lat Pulldown", muscleGroup = "Back", kind = ExerciseKind.REPS),
            Exercise(name = "Seated Cable Row", muscleGroup = "Back", kind = ExerciseKind.REPS),
            Exercise(name = "Face Pull", muscleGroup = "Back", kind = ExerciseKind.REPS),
            // Cardio
            Exercise(name = "Run", muscleGroup = "Cardio", kind = ExerciseKind.DISTANCE),
            Exercise(name = "Bike", muscleGroup = "Cardio", kind = ExerciseKind.DISTANCE),
            Exercise(name = "Row", muscleGroup = "Cardio", kind = ExerciseKind.DISTANCE),
            Exercise(name = "Elliptical", muscleGroup = "Cardio", kind = ExerciseKind.DISTANCE),
            Exercise(name = "Jump Rope", muscleGroup = "Cardio", kind = ExerciseKind.TIME),
        )
    }
}
