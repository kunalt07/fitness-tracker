package com.example.fitness_tracker.onboarding

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists whether the user has finished the first-launch onboarding flow
 * (split / weight goal / diet). Mirrors [com.example.fitness_tracker.ui.theme.ThemeModeStore]:
 * SharedPreferences for durability, StateFlow for UI reactivity.
 *
 *   OnboardingStore.get(context).complete.collectAsState()  // observe
 *   OnboardingStore.get(context).markComplete()             // finish flow
 */
class OnboardingStore private constructor(private val prefs: SharedPreferences) {

    private val _complete = MutableStateFlow(prefs.getBoolean(KEY, false))
    val complete: StateFlow<Boolean> = _complete.asStateFlow()

    fun markComplete() {
        prefs.edit().putBoolean(KEY, true).apply()
        _complete.value = true
    }

    companion object {
        private const val PREFS = "onboarding_prefs"
        private const val KEY = "onboarding_complete"

        @Volatile private var instance: OnboardingStore? = null

        fun get(context: Context): OnboardingStore {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val prefs = context.applicationContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                return OnboardingStore(prefs).also { instance = it }
            }
        }
    }
}
