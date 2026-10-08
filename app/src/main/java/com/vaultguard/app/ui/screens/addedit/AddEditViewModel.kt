package com.vaultguard.app.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.model.FieldMapping
import com.vaultguard.app.domain.model.PasswordGeneratorConfig
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.VaultItem
import com.vaultguard.app.domain.repository.VaultRepository
import com.vaultguard.app.domain.usecase.GeneratePasswordUseCase
import com.vaultguard.app.domain.usecase.SaveVaultItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddEditUiState(
    val id: String = UUID.randomUUID().toString(),
    val isEditMode: Boolean = false,
    val type: SecretType = SecretType.LOGIN,
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val urlOrPackage: String = "",
    val apiKey: String = "",
    val notes: String = "",
    val folderOrTag: String = "",
    val isFavorite: Boolean = false,
    val customIconUri: String? = null,
    val customFieldMappings: List<FieldMapping> = emptyList(),
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)

sealed class AddEditEvent {
    data object SaveSuccess : AddEditEvent()
    data class ShowToast(val message: String) : AddEditEvent()
}

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val saveVaultItemUseCase: SaveVaultItemUseCase,
    private val generatePasswordUseCase: GeneratePasswordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String? = savedStateHandle["itemId"]

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditEvent>()
    val events: SharedFlow<AddEditEvent> = _events.asSharedFlow()

    init {
        if (!itemId.isNullOrBlank() && itemId != "new") {
            loadItem(itemId)
        }
    }

    private fun loadItem(id: String) {
        viewModelScope.launch {
            val item = vaultRepository.getItemByIdSync(id)
            if (item != null) {
                _uiState.value = AddEditUiState(
                    id = item.id,
                    isEditMode = true,
                    type = item.type,
                    name = item.name,
                    username = item.username,
                    password = item.password,
                    urlOrPackage = item.urlOrPackage,
                    apiKey = item.apiKey,
                    notes = item.notes,
                    folderOrTag = item.folderOrTag,
                    isFavorite = item.isFavorite,
                    customIconUri = item.customIconUri,
                    customFieldMappings = item.customFieldMappings
                )
            }
        }
    }

    fun onTypeChanged(type: SecretType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name, errorMessage = null)
    }

    fun onUsernameChanged(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onUrlOrPackageChanged(url: String) {
        _uiState.value = _uiState.value.copy(urlOrPackage = url)
    }

    fun onApiKeyChanged(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey)
    }

    fun onNotesChanged(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onFolderOrTagChanged(tag: String) {
        _uiState.value = _uiState.value.copy(folderOrTag = tag)
    }

    fun onFavoriteToggle() {
        _uiState.value = _uiState.value.copy(isFavorite = !_uiState.value.isFavorite)
    }

    fun onCustomIconUriChanged(uri: String?) {
        _uiState.value = _uiState.value.copy(customIconUri = uri)
    }

    fun addFieldMapping(viewId: String, hint: String, inputType: Int = 0) {
        val mapping = FieldMapping(
            viewId = viewId.trim(),
            hint = hint.trim(),
            inputType = inputType,
            targetField = if (_uiState.value.type == SecretType.API_KEY) "apiKey" else "password"
        )
        val updated = _uiState.value.customFieldMappings + mapping
        _uiState.value = _uiState.value.copy(customFieldMappings = updated)
    }

    fun removeFieldMapping(index: Int) {
        if (index in _uiState.value.customFieldMappings.indices) {
            val updated = _uiState.value.customFieldMappings.toMutableList()
            updated.removeAt(index)
            _uiState.value = _uiState.value.copy(customFieldMappings = updated)
        }
    }

    fun generatePassword(config: PasswordGeneratorConfig = PasswordGeneratorConfig()) {
        val generated = generatePasswordUseCase(config)
        if (_uiState.value.type == SecretType.API_KEY) {
            _uiState.value = _uiState.value.copy(apiKey = generated)
        } else {
            _uiState.value = _uiState.value.copy(password = generated)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name is required")
            return
        }

        viewModelScope.launch {
            try {
                val item = VaultItem(
                    id = state.id,
                    type = state.type,
                    name = state.name.trim(),
                    username = state.username.trim(),
                    password = state.password,
                    urlOrPackage = state.urlOrPackage.trim(),
                    apiKey = state.apiKey.trim(),
                    notes = state.notes.trim(),
                    folderOrTag = state.folderOrTag.trim(),
                    isFavorite = state.isFavorite,
                    customIconUri = state.customIconUri,
                    customFieldMappings = state.customFieldMappings,
                    updatedAt = System.currentTimeMillis()
                )
                saveVaultItemUseCase(item)
                _events.emit(AddEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Failed to save")
            }
        }
    }
}
