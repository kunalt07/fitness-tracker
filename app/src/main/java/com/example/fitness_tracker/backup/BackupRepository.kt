package com.example.fitness_tracker.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.fitness_tracker.data.BackupDao
import com.example.fitness_tracker.data.FitnessDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Sealed result so the UI layer can distinguish "user picked a bad file"
 * from "the file is fine but a row failed to insert". Both surface as
 * snackbars — the message just differs.
 */
sealed class BackupResult {
    data class Success(val rowCount: Int) : BackupResult()
    data class Failure(val message: String) : BackupResult()
}

/**
 * Exports every Room table to a single JSON document, and restores from
 * one. Uses [androidx.room.withTransaction] so a partially-failing restore
 * leaves the database untouched.
 *
 * Restore wipes everything and replaces it with the file's contents — there
 * is no "merge" mode. The Profile screen confirms with the user before
 * calling [import].
 */
class BackupRepository private constructor(private val db: FitnessDatabase) {

    private val dao: BackupDao = db.backupDao()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true       // forward-compat: tolerate unknown fields
        encodeDefaults = true
    }

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            appVersion = APP_VERSION_NAME,
            exportedAt = System.currentTimeMillis(),
            exercises = dao.allExercises(),
            workoutSessions = dao.allSessions(),
            setEntries = dao.allSetEntries(),
            workoutTemplates = dao.allTemplates(),
            templateExercises = dao.allTemplateExercises(),
            weeklySplit = dao.allWeeklySplit(),
            pendingPlan = dao.allPendingPlan(),
            cachedPlans = dao.allCachedPlans(),
            reminderSettings = dao.allReminderSettings(),
            userProfile = dao.allUserProfiles(),
            bodyWeights = dao.allBodyWeights(),
            readiness = dao.allReadiness(),
            water = dao.allWater(),
            weightGoals = dao.allWeightGoals(),
            foodEntries = dao.allFoodEntries(),
            calorieGoals = dao.allCalorieGoals(),
            cachedDietPlans = dao.allCachedDietPlans(),
            meals = dao.allMeals(),
            dietPreferences = dao.allDietPreferences(),
        )
        json.encodeToString(BackupPayload.serializer(), payload)
    }

    suspend fun import(jsonText: String): BackupResult = withContext(Dispatchers.IO) {
        val payload = runCatching {
            json.decodeFromString(BackupPayload.serializer(), jsonText)
        }.getOrElse { e ->
            return@withContext BackupResult.Failure("Couldn't read backup: ${e.message ?: "invalid JSON"}")
        }

        if (payload.schemaVersion > BACKUP_SCHEMA_VERSION) {
            return@withContext BackupResult.Failure(
                "Backup is from a newer app version (schema ${payload.schemaVersion}). Update Vector and try again."
            )
        }

        runCatching {
            db.withTransaction {
                // Wipe order: child tables before parents, mirroring FK direction.
                dao.clearSetEntries()
                dao.clearTemplateExercises()
                dao.clearPendingPlan()
                dao.clearSessions()
                dao.clearTemplates()
                dao.clearExercises()
                dao.clearWeeklySplit()
                dao.clearCachedPlans()
                dao.clearReminderSettings()
                dao.clearUserProfile()
                dao.clearBodyWeights()
                dao.clearReadiness()
                dao.clearWater()
                dao.clearWeightGoals()
                dao.clearFoodEntries()
                dao.clearCalorieGoals()
                dao.clearCachedDietPlans()
                dao.clearMeals()
                dao.clearDietPreferences()

                // Insert order: parents before children.
                dao.insertExercises(payload.exercises)
                dao.insertSessions(payload.workoutSessions)
                dao.insertSetEntries(payload.setEntries)
                dao.insertTemplates(payload.workoutTemplates)
                dao.insertTemplateExercises(payload.templateExercises)
                dao.insertWeeklySplit(payload.weeklySplit)
                dao.insertPendingPlan(payload.pendingPlan)
                dao.insertCachedPlans(payload.cachedPlans)
                dao.insertReminderSettings(payload.reminderSettings)
                dao.insertUserProfiles(payload.userProfile)
                dao.insertBodyWeights(payload.bodyWeights)
                dao.insertReadiness(payload.readiness)
                dao.insertWater(payload.water)
                dao.insertWeightGoals(payload.weightGoals)
                dao.insertFoodEntries(payload.foodEntries)
                dao.insertCalorieGoals(payload.calorieGoals)
                dao.insertCachedDietPlans(payload.cachedDietPlans)
                dao.insertMeals(payload.meals)
                dao.insertDietPreferences(payload.dietPreferences)
            }
        }.fold(
            onSuccess = { BackupResult.Success(rowCount = payload.totalRows()) },
            onFailure = { BackupResult.Failure("Restore failed: ${it.message ?: "unknown error"}") },
        )
    }

    companion object {
        @Volatile private var instance: BackupRepository? = null

        fun get(context: Context): BackupRepository {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: BackupRepository(FitnessDatabase.get(context)).also { instance = it }
            }
        }
    }
}

private fun BackupPayload.totalRows(): Int = exercises.size + workoutSessions.size +
    setEntries.size + workoutTemplates.size + templateExercises.size + weeklySplit.size +
    pendingPlan.size + cachedPlans.size + reminderSettings.size + userProfile.size +
    bodyWeights.size + readiness.size + water.size + weightGoals.size + foodEntries.size +
    calorieGoals.size + cachedDietPlans.size + meals.size + dietPreferences.size
