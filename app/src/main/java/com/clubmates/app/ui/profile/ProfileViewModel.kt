package com.clubmates.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubmates.app.data.repository.AuthRepository
import com.clubmates.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { authRepository.getMe() }
                .onSuccess { profile -> _uiState.update { it.copy(profile = profile, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            runCatching { authRepository.updateProfile(profile) }
                .onSuccess { saved -> _uiState.update { it.copy(profile = saved, isSaving = false, saveSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }
}
