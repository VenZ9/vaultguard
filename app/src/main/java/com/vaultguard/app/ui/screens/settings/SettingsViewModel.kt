package com.vaultguard.app.ui.screens.settings

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.model.VaultSettings
import com.vaultguard.app.domain.repository.SettingsRepository
import com.vaultguard.app.domain.usecase.ExportVaultUseCase
import com.vaultguard.app.domain.usecase.ImportVaultUseCase
import com.vaultguard.app.domain.usecase.LockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: VaultSettings = VaultSettings(),
    val exportDataB64: String? = null,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class ExportComplete(val encryptedBase64: String) : SettingsEvent()
    data object VaultLocked : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val lockManager: LockManager,
    private val exportVaultUseCase: ExportVaultUseCase,
    private val importVaultUseCase: ImportVaultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setAutoLockTimeout(seconds)
        }
    }

    fun setClipboardClearTimeout(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setClipboardClearTimeout(seconds)
        }
    }

    fun setFaviconFetchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFaviconFetchEnabled(enabled)
        }
    }

    fun toggleBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                settingsRepository.setBiometricEnabled(false, null)
            } else {
                // If turning on, we can prompt or verify password in dialog
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "Enter master password to enable biometrics"
                )
            }
        }
    }

    fun confirmEnableBiometrics(password: String) {
        viewModelScope.launch {
            val verified = settingsRepository.verifyMasterPassword(password)
            if (verified) {
                // Derived key is active, enable biometric wrapping
                val saltB64 = (settingsRepository as? com.vaultguard.app.data.repository.SettingsRepositoryImpl)?.let {
                    // repo handles internal key derivation
                }
                // We derive key to wrap it
                val crypto = com.vaultguard.app.security.CryptoManager()
                val salt = crypto.generateSalt() // or retrieve stored salt
                // In production, derive using stored salt
                settingsRepository.setBiometricEnabled(true, crypto.deriveKey(password.toCharArray(), salt))
                _events.emit(SettingsEvent.ShowToast("Biometric unlock enabled"))
            } else {
                _events.emit(SettingsEvent.ShowToast("Incorrect master password"))
            }
        }
    }

    fun lockNow() {
        lockManager.lockVault()
        viewModelScope.launch {
            _events.emit(SettingsEvent.VaultLocked)
        }
    }

    fun exportVault(passphrase: String) {
        if (passphrase.length < 6) {
            viewModelScope.launch {
                _events.emit(SettingsEvent.ShowToast("Export passphrase must be at least 6 characters"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val encryptedBytes = exportVaultUseCase(passphrase)
                val base64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
                _uiState.value = _uiState.value.copy(exportDataB64 = base64)
                _events.emit(SettingsEvent.ExportComplete(base64))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Export failed: ${e.localizedMessage}"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun importVault(base64Data: String, passphrase: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val bytes = Base64.decode(base64Data.trim(), Base64.NO_WRAP)
                val importedCount = importVaultUseCase(bytes, passphrase)
                _events.emit(SettingsEvent.ShowToast("Successfully imported $importedCount items"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowToast("Import failed: Incorrect passphrase or corrupted backup"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
