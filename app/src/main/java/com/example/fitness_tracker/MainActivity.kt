package com.example.fitness_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.auth.AuthViewModel
import com.example.fitness_tracker.auth.WelcomeScreen
import com.example.fitness_tracker.onboarding.OnboardingFlow
import com.example.fitness_tracker.onboarding.OnboardingStore
import com.example.fitness_tracker.ui.theme.FitnessTrackerTheme
import com.example.fitness_tracker.ui.theme.ThemeModeStore
import com.example.fitness_tracker.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Observe the user's theme preference. Re-renders the whole tree
            // when they pick a different mode in Settings.
            val themeMode by ThemeModeStore.get(applicationContext)
                .mode.collectAsState()
            FitnessTrackerTheme(darkTheme = themeMode.resolveDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    val profile by authViewModel.profile.collectAsState()
                    val onboardingStore = OnboardingStore.get(applicationContext)
                    val onboardingComplete by onboardingStore.complete.collectAsState()
                    when {
                        profile == null -> WelcomeScreen(viewModel = authViewModel)
                        !onboardingComplete -> OnboardingFlow(
                            onFinish = { onboardingStore.markComplete() },
                        )
                        else -> FitnessApp()
                    }
                }
            }
        }
    }
}
