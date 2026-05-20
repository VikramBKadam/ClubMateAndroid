package com.clubmates.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubmates.app.data.repository.AuthRepository
import com.clubmates.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsProfile: Boolean = false,
    val currentProfile: UserProfile? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isLoggedIn) {
            loadCurrentUser()
        }
    }

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { authRepository.sendOtp(phone) }
                .onSuccess { _uiState.update { it.copy(isLoading = false, otpSent = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun verifyOtp(phone: String, code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { authRepository.verifyOtp(phone, code) }
                .onSuccess { response ->
                    if (response.user.profileCompleted) {
                        loadCurrentUser()
                    } else {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true, needsProfile = true) }
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { authRepository.updateProfile(profile) }
                .onSuccess { saved ->
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, needsProfile = false, currentProfile = saved) }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun resetOtpSent() = _uiState.update { it.copy(otpSent = false) }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            runCatching { authRepository.getMe() }
                .onSuccess { profile ->
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, needsProfile = false, currentProfile = profile) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
