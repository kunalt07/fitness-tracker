package com.example.fitness_tracker.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-selected theme preference. Persisted in SharedPreferences and exposed
 * as a StateFlow so the whole UI tree reacts the moment the user picks a new
 * mode in Settings.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Single source of truth for the user's theme preference.
 *
 * Backed by SharedPreferences (no Room migration / DataStore overhead — this
 * is a single int, not structured data). Read once at construction; writes
 * push to both prefs and the in-memory StateFlow so UI reflects the change
 * synchronously.
 *
 *   ThemeModeStore.get(context).mode.collectAsState()  // observe
 *   ThemeModeStore.get(context).set(ThemeMode.DARK)    // change
 */
class ThemeModeStore private constructor(prefs: SharedPreferences) {
    private val prefs: SharedPreferences = prefs

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(newMode: ThemeMode) {
        prefs.edit().putString(KEY, newMode.name).apply()
        _mode.value = newMode
    }

    private fun read(): ThemeMode {
        val raw = prefs.getString(KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    companion object {
        private const val PREFS = "theme_mode_prefs"
        private const val KEY = "theme_mode"

        @Volatile private var instance: ThemeModeStore? = null

        fun get(context: Context): ThemeModeStore {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val prefs = context.applicationContext
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                return ThemeModeStore(prefs).also { instance = it }
            }
        }
    }
}
