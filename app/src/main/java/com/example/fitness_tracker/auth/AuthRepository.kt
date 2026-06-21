package com.example.fitness_tracker.auth

import android.content.Context
import com.example.fitness_tracker.data.FitnessRepository
import com.example.fitness_tracker.data.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Local-only "auth": just a single profile row in Room. No network, no Firebase.
 * Existence of a profile = signed in.
 */
class AuthRepository(private val repo: FitnessRepository) {

    val profile: Flow<UserProfile?> = repo.userProfile

    suspend fun snapshot(): UserProfile? = repo.getUserProfile()

    suspend fun saveProfile(name: String, email: String) {
        repo.saveUserProfile(
            UserProfile(
                id = 0,
                name = name.trim(),
                email = email.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clearProfile() {
        repo.clearUserProfile()
    }

    companion object {
        @Volatile private var instance: AuthRepository? = null

        fun get(context: Context): AuthRepository =
            instance ?: synchronized(this) {
                instance ?: AuthRepository(FitnessRepository.get(context)).also { instance = it }
            }
    }
}
