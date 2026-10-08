package com.vaultguard.app.ui.screens.generator

import androidx.lifecycle.ViewModel
import com.vaultguard.app.domain.model.PasswordGeneratorConfig
import com.vaultguard.app.domain.usecase.GeneratePasswordUseCase
import com.vaultguard.app.domain.usecase.PasswordStrength
import com.vaultguard.app.security.ClipboardHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class GeneratorUiState(
    val password: String = "",
    val length: Int = 18,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val strength: PasswordStrength = PasswordStrength(4, "Very Strong", 80.0)
)

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val generatePasswordUseCase: GeneratePasswordUseCase,
    private val clipboardHelper: ClipboardHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    init {
        regenerate()
    }

    fun onLengthChanged(newLength: Int) {
        _uiState.value = _uiState.value.copy(length = newLength)
        regenerate()
    }

    fun toggleUppercase(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeLowercase && !_uiState.value.includeNumbers && !_uiState.value.includeSymbols) return
        _uiState.value = _uiState.value.copy(includeUppercase = enabled)
        regenerate()
    }

    fun toggleLowercase(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeUppercase && !_uiState.value.includeNumbers && !_uiState.value.includeSymbols) return
        _uiState.value = _uiState.value.copy(includeLowercase = enabled)
        regenerate()
    }

    fun toggleNumbers(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeUppercase && !_uiState.value.includeLowercase && !_uiState.value.includeSymbols) return
        _uiState.value = _uiState.value.copy(includeNumbers = enabled)
        regenerate()
    }

    fun toggleSymbols(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeUppercase && !_uiState.value.includeLowercase && !_uiState.value.includeNumbers) return
        _uiState.value = _uiState.value.copy(includeSymbols = enabled)
        regenerate()
    }

    fun regenerate() {
        val state = _uiState.value
        val config = PasswordGeneratorConfig(
            length = state.length,
            includeUppercase = state.includeUppercase,
            includeLowercase = state.includeLowercase,
            includeNumbers = state.includeNumbers,
            includeSymbols = state.includeSymbols
        )
        val password = generatePasswordUseCase(config)
        val strength = generatePasswordUseCase.calculateStrength(password)
        _uiState.value = state.copy(password = password, strength = strength)
    }

    fun copyPassword() {
        val currentPassword = _uiState.value.password
        clipboardHelper.copyToClipboard(
            label = "Generated Password",
            text = currentPassword,
            isSensitive = true,
            autoClearSeconds = 30
        )
    }
}
