package com.vaultguard.app.ui.screens.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultguard.app.domain.repository.VaultRepository
import com.vaultguard.app.domain.usecase.AuditIssue
import com.vaultguard.app.domain.usecase.AuditIssueType
import com.vaultguard.app.domain.usecase.SecurityAuditor
import com.vaultguard.app.domain.usecase.VaultAuditReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuditUiState(
    val report: VaultAuditReport = VaultAuditReport(
        overallScore = 100,
        totalItems = 0,
        totalLogins = 0,
        weakCount = 0,
        reusedCount = 0,
        staleCount = 0,
        passkeyCount = 0,
        issues = emptyList()
    ),
    val selectedFilter: AuditIssueType? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AuditViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    init {
        observeVaultItems()
    }

    private fun observeVaultItems() {
        viewModelScope.launch(Dispatchers.Default) {
            vaultRepository.getAllItems().collect { items ->
                val auditReport = SecurityAuditor.audit(items)
                _uiState.value = _uiState.value.copy(
                    report = auditReport,
                    isLoading = false
                )
            }
        }
    }

    fun setFilter(filter: AuditIssueType?) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }
}
