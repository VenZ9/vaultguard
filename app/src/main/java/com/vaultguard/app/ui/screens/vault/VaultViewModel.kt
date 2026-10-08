package com.vaultguard.app.ui.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.SortOrder
import com.vaultguard.app.domain.model.VaultItem
import com.vaultguard.app.domain.usecase.DeleteVaultItemUseCase
import com.vaultguard.app.domain.usecase.GetVaultItemsUseCase
import com.vaultguard.app.domain.usecase.SaveVaultItemUseCase
import com.vaultguard.app.security.ClipboardHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VaultFilterChip {
    ALL,
    LOGINS,
    API_KEYS,
    APP_PASSWORDS,
    FAVORITES
}

data class VaultListUiState(
    val items: List<VaultItem> = emptyList(),
    val searchQuery: String = "",
    val activeChip: VaultFilterChip = VaultFilterChip.ALL,
    val sortOrder: SortOrder = SortOrder.NAME,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val isLoading: Boolean = false
)

sealed class VaultListEvent {
    data class ItemDeletedWithUndo(val item: VaultItem) : VaultListEvent()
    data class ShowToast(val message: String) : VaultListEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val getVaultItemsUseCase: GetVaultItemsUseCase,
    private val saveVaultItemUseCase: SaveVaultItemUseCase,
    private val deleteVaultItemUseCase: DeleteVaultItemUseCase,
    private val clipboardHelper: ClipboardHelper
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeChip = MutableStateFlow(VaultFilterChip.ALL)
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _selectedTag = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(VaultListUiState())
    val uiState: StateFlow<VaultListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VaultListEvent>()
    val events: SharedFlow<VaultListEvent> = _events.asSharedFlow()

    private var recentlyDeletedItem: VaultItem? = null

    init {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _activeChip,
                _sortOrder,
                _selectedTag
            ) { query, chip, sort, tag ->
                val type = when (chip) {
                    VaultFilterChip.LOGINS -> SecretType.LOGIN
                    VaultFilterChip.API_KEYS -> SecretType.API_KEY
                    VaultFilterChip.APP_PASSWORDS -> SecretType.APP_PASSWORD
                    else -> null
                }
                val favoriteOnly = chip == VaultFilterChip.FAVORITES

                Tuple4(query, type, favoriteOnly, sort, tag)
            }.flatMapLatest { tuple ->
                getVaultItemsUseCase(
                    query = tuple.query,
                    typeFilter = tuple.type,
                    favoriteOnly = tuple.favoriteOnly,
                    tagFilter = tuple.tag,
                    sortOrder = tuple.sort
                )
            }.collect { items ->
                val allTags = items.map { it.folderOrTag }.filter { it.isNotBlank() }.distinct()
                _uiState.value = _uiState.value.copy(
                    items = items,
                    searchQuery = _searchQuery.value,
                    activeChip = _activeChip.value,
                    sortOrder = _sortOrder.value,
                    selectedTag = _selectedTag.value,
                    availableTags = allTags,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChipSelected(chip: VaultFilterChip) {
        _activeChip.value = chip
    }

    fun onSortOrderSelected(order: SortOrder) {
        _sortOrder.value = order
    }

    fun onTagSelected(tag: String?) {
        _selectedTag.value = tag
    }

    fun toggleFavorite(item: VaultItem) {
        viewModelScope.launch {
            saveVaultItemUseCase(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun deleteItem(item: VaultItem) {
        recentlyDeletedItem = item
        viewModelScope.launch {
            deleteVaultItemUseCase(item)
            _events.emit(VaultListEvent.ItemDeletedWithUndo(item))
        }
    }

    fun undoDelete() {
        val itemToRestore = recentlyDeletedItem ?: return
        viewModelScope.launch {
            saveVaultItemUseCase(itemToRestore)
            recentlyDeletedItem = null
        }
    }

    fun copyPassword(item: VaultItem) {
        val secret = when (item.type) {
            SecretType.API_KEY -> item.apiKey
            else -> item.password
        }
        clipboardHelper.copyToClipboard(item.name, secret, isSensitive = true, autoClearSeconds = 30)
    }

    fun copyUsername(item: VaultItem) {
        clipboardHelper.copyToClipboard(item.name, item.username, isSensitive = false, autoClearSeconds = 0)
    }

    private data class Tuple4(
        val query: String,
        val type: SecretType?,
        val favoriteOnly: Boolean,
        val sort: SortOrder,
        val tag: String?
    )
}
