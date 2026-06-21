package com.example.fitness_tracker.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_tracker.data.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository.get(app)

    val profile: StateFlow<UserProfile?> = repo.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun saveProfile(name: String, email: String) {
        viewModelScope.launch { repo.saveProfile(name, email) }
    }

    fun clearProfile() {
        viewModelScope.launch { repo.clearProfile() }
    }
}
