package com.example.fitness_tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun fromKind(value: ExerciseKind): String = value.name
    @TypeConverter fun toKind(value: String): ExerciseKind =
        runCatching { ExerciseKind.valueOf(value) }.getOrDefault(ExerciseKind.REPS)

    @TypeConverter fun fromReadiness(value: Readiness): String = value.name
    @TypeConverter fun toReadiness(value: String): Readiness =
        runCatching { Readiness.valueOf(value) }.getOrDefault(Readiness.OK)

    @TypeConverter fun fromDietType(value: DietType): String = value.name
    @TypeConverter fun toDietType(value: String): DietType =
        runCatching { DietType.valueOf(value) }.getOrDefault(DietType.VEG)

    @TypeConverter fun fromMealCategory(value: MealCategory): String = value.name
    @TypeConverter fun toMealCategory(value: String): MealCategory =
        runCatching { MealCategory.valueOf(value) }.getOrDefault(MealCategory.BREAKFAST)
}

@Database(
    entities = [
        Exercise::class,
        WorkoutSession::class,
        SetEntry::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
        WeeklySplitDay::class,
        PendingPlanItem::class,
        CachedPlan::class,
        ReminderSettings::class,
        UserProfile::class,
        BodyWeightEntry::class,
        ReadinessEntry::class,
        WaterEntry::class,
        WeightGoal::class,
        FoodEntry::class,
        CalorieGoal::class,
        CachedDietPlan::class,
        Meal::class,
        DietPreference::class,
    ],
    version = 13,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setEntryDao(): SetEntryDao
    abstract fun templateDao(): TemplateDao
    abstract fun aggregateDao(): AggregateDao
    abstract fun weeklySplitDao(): WeeklySplitDao
    abstract fun pendingPlanDao(): PendingPlanDao
    abstract fun cachedPlanDao(): CachedPlanDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dailyTrackerDao(): DailyTrackerDao
    abstract fun weightGoalDao(): WeightGoalDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun calorieGoalDao(): CalorieGoalDao
    abstract fun cachedDietPlanDao(): CachedDietPlanDao
    abstract fun mealDao(): MealDao
    abstract fun dietPreferenceDao(): DietPreferenceDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile private var instance: FitnessDatabase? = null

        fun get(context: Context): FitnessDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE exercises ADD COLUMN kind TEXT NOT NULL DEFAULT 'REPS'"
                )
                db.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN endedAt INTEGER DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE set_entries ADD COLUMN durationSec INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE set_entries ADD COLUMN distanceMeters INTEGER NOT NULL DEFAULT 0"
                )
                // Best-effort kind tagging for the seeded defaults that already exist.
                db.execSQL("UPDATE exercises SET kind = 'TIME' WHERE name IN ('Plank')")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS workout_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS template_exercises (
                        templateId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        PRIMARY KEY(templateId, exerciseId),
                        FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_template_exercises_exerciseId " +
                        "ON template_exercises(exerciseId)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_split (
                        dayOfWeek INTEGER PRIMARY KEY NOT NULL,
                        focus TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_plan (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        dayKey INTEGER NOT NULL,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pending_plan_exerciseId " +
                        "ON pending_plan(exerciseId)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_plan (
                        dayKey INTEGER PRIMARY KEY NOT NULL,
                        focus TEXT NOT NULL,
                        markdown TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminder_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        enabled INTEGER NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meal (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        ingredients TEXT NOT NULL,
                        steps TEXT NOT NULL,
                        calories INTEGER NOT NULL,
                        proteinG INTEGER NOT NULL,
                        carbsG INTEGER NOT NULL,
                        fatG INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        dietTypes TEXT NOT NULL,
                        isAiGenerated INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS diet_preference (
                        id INTEGER PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_diet_plan (
                        dayKey INTEGER PRIMARY KEY NOT NULL,
                        direction TEXT NOT NULL,
                        markdown TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_entry (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dayKey INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        calories INTEGER NOT NULL,
                        proteinG INTEGER NOT NULL,
                        carbsG INTEGER NOT NULL,
                        fatG INTEGER NOT NULL,
                        loggedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calorie_goal (
                        id INTEGER PRIMARY KEY NOT NULL,
                        targetCalories INTEGER NOT NULL,
                        proteinTargetG INTEGER,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weight_goal (
                        id INTEGER PRIMARY KEY NOT NULL,
                        targetKg REAL NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS body_weight_entry (
                        dayKey INTEGER PRIMARY KEY NOT NULL,
                        weightKg REAL NOT NULL,
                        performedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS readiness_entry (
                        dayKey INTEGER PRIMARY KEY NOT NULL,
                        level TEXT NOT NULL,
                        performedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS water_entry (
                        dayKey INTEGER PRIMARY KEY NOT NULL,
                        glasses INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
