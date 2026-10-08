package com.vaultguard.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.VaultItem
import com.vaultguard.app.domain.repository.VaultRepository
import com.vaultguard.app.domain.usecase.DeleteVaultItemUseCase
import com.vaultguard.app.domain.usecase.SaveVaultItemUseCase
import com.vaultguard.app.security.ClipboardHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val item: VaultItem? = null,
    val isPasswordRevealed: Boolean = false,
    val isLoading: Boolean = true
)

sealed class DetailEvent {
    data object ItemDeleted : DetailEvent()
    data class ShowToast(val message: String) : DetailEvent()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val deleteVaultItemUseCase: DeleteVaultItemUseCase,
    private val saveVaultItemUseCase: SaveVaultItemUseCase,
    private val clipboardHelper: ClipboardHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>()
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            vaultRepository.getItemById(itemId).collect { item ->
                _uiState.value = _uiState.value.copy(
                    item = item,
                    isLoading = false
                )
            }
        }
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordRevealed = !_uiState.value.isPasswordRevealed
        )
    }

    fun toggleFavorite() {
        val current = _uiState.value.item ?: return
        viewModelScope.launch {
            saveVaultItemUseCase(current.copy(isFavorite = !current.isFavorite))
        }
    }

    fun copyField(label: String, value: String, isSensitive: Boolean) {
        val current = _uiState.value.item
        if (current != null) {
            viewModelScope.launch {
                vaultRepository.updateLastUsed(current.id, System.currentTimeMillis())
            }
        }
        clipboardHelper.copyToClipboard(
            label = label,
            text = value,
            isSensitive = isSensitive,
            autoClearSeconds = if (isSensitive) 30 else 0
        )
    }

    fun deleteItem() {
        val current = _uiState.value.item ?: return
        viewModelScope.launch {
            deleteVaultItemUseCase(current)
            _events.emit(DetailEvent.ItemDeleted)
        }
    }
}
