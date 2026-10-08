package com.vaultguard.app.ui.screens.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.repository.SettingsRepository
import com.vaultguard.app.domain.usecase.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnlockUiState(
    val isFirstTimeSetup: Boolean = false,
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isBiometricAvailable: Boolean = false
)

sealed class UnlockEvent {
    data object UnlockSuccess : UnlockEvent()
    data class ShowToast(val message: String) : UnlockEvent()
}

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val lockManager: LockManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UnlockEvent>()
    val events: SharedFlow<UnlockEvent> = _events.asSharedFlow()

    init {
        checkSetupStatus()
    }

    private fun checkSetupStatus() {
        viewModelScope.launch {
            val isConfigured = settingsRepository.isMasterPasswordConfigured()
            val settings = settingsRepository.getSettings()
            _uiState.value = _uiState.value.copy(
                isFirstTimeSetup = !isConfigured,
                isBiometricAvailable = settings.isBiometricEnabled
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(passwordInput = password, errorMessage = null)
    }

    fun onConfirmPasswordChanged(confirm: String) {
        _uiState.value = _uiState.value.copy(confirmPasswordInput = confirm, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.passwordInput.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password cannot be empty")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                if (state.isFirstTimeSetup) {
                    if (state.passwordInput.length < 6) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Master password must be at least 6 characters"
                        )
                        return@launch
                    }
                    if (state.passwordInput != state.confirmPasswordInput) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Passwords do not match"
                        )
                        return@launch
                    }

                    val success = lockManager.setupMasterPassword(state.passwordInput)
                    if (success) {
                        _events.emit(UnlockEvent.UnlockSuccess)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to setup master password"
                        )
                    }
                } else {
                    val success = lockManager.unlockWithMasterPassword(state.passwordInput)
                    if (success) {
                        _events.emit(UnlockEvent.UnlockSuccess)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Incorrect master password"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Authentication error"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun unlockWithBiometric() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val success = lockManager.unlockWithBiometrics()
                if (success) {
                    _events.emit(UnlockEvent.UnlockSuccess)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Biometric unlock failed. Please enter master password."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Biometric authentication failed"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
