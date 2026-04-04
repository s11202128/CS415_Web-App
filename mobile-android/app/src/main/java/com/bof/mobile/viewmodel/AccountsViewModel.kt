package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.model.AccountDetailsResponse
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.TransactionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val isLoadingAccounts: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val isLoadingTransactions: Boolean = false,
    val accounts: List<AccountItem> = emptyList(),
    val selectedAccountId: Int? = null,
    val selectedAccountDetails: AccountDetailsResponse? = null,
    val transactions: List<TransactionItem> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val typeFilter: String? = null,
    val errorMessage: String? = null,
    val lastUpdatedAtEpochMs: Long? = null
)

class AccountsViewModel(private val accountRepository: AccountRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState
    private var lastLoadAccountsAtEpochMs: Long = 0L

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun upsertAccount(account: AccountItem) {
        _uiState.update { state ->
            val existingIndex = state.accounts.indexOfFirst { it.id == account.id }
            val updatedAccounts = if (existingIndex >= 0) {
                state.accounts.toMutableList().apply { this[existingIndex] = account }
            } else {
                listOf(account) + state.accounts
            }

            state.copy(
                accounts = updatedAccounts,
                selectedAccountId = state.selectedAccountId ?: account.id,
                lastUpdatedAtEpochMs = System.currentTimeMillis()
            )
        }
    }

    fun selectAccount(accountId: Int) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
        loadAccountDetails(accountId)
        loadTransactions(accountId = accountId, page = 1, typeFilter = _uiState.value.typeFilter)
    }

    fun setTypeFilter(filter: String?) {
        val accountId = _uiState.value.selectedAccountId ?: return
        _uiState.update { it.copy(typeFilter = filter) }
        loadTransactions(accountId = accountId, page = 1, typeFilter = filter)
    }

    fun loadAccounts() {
        if (_uiState.value.isLoadingAccounts) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastLoadAccountsAtEpochMs < 700) {
            return
        }
        lastLoadAccountsAtEpochMs = now

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAccounts = true,
                    // Preserve visible account data while refresh is in-flight.
                    errorMessage = if (it.accounts.isNotEmpty()) it.errorMessage else null
                )
            }
            when (val result = accountRepository.getAccounts()) {
                is ApiResult.Success -> {
                    val previousAccounts = _uiState.value.accounts
                    val refreshedAccounts = result.data
                    val stableAccounts = if (refreshedAccounts.isEmpty() && previousAccounts.isNotEmpty()) {
                        previousAccounts
                    } else {
                        refreshedAccounts
                    }
                    val selected = _uiState.value.selectedAccountId ?: stableAccounts.firstOrNull()?.id
                    _uiState.update {
                        it.copy(
                            isLoadingAccounts = false,
                            accounts = stableAccounts,
                            selectedAccountId = selected,
                            errorMessage = null,
                            lastUpdatedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                    selected?.let { id ->
                        loadAccountDetails(id)
                        loadTransactions(accountId = id, page = 1, typeFilter = _uiState.value.typeFilter)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingAccounts = false,
                            accounts = it.accounts,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadAccountDetails(accountId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true, errorMessage = null) }
            when (val result = accountRepository.getAccountDetails(accountId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingDetails = false,
                            selectedAccountDetails = result.data,
                            errorMessage = null,
                            lastUpdatedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingDetails = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadTransactions(accountId: Int, page: Int = 1, typeFilter: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransactions = true, errorMessage = null) }
            when (val result = accountRepository.getTransactions(accountId, page = page, typeFilter = typeFilter)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingTransactions = false,
                            transactions = result.data.items,
                            page = result.data.page,
                            totalPages = result.data.totalPages.coerceAtLeast(1),
                            typeFilter = typeFilter,
                            errorMessage = null,
                            lastUpdatedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingTransactions = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        val accountId = state.selectedAccountId ?: return
        if (state.page >= state.totalPages || state.isLoadingTransactions) return
        loadTransactions(accountId = accountId, page = state.page + 1, typeFilter = state.typeFilter)
    }

    fun loadPreviousPage() {
        val state = _uiState.value
        val accountId = state.selectedAccountId ?: return
        if (state.page <= 1 || state.isLoadingTransactions) return
        loadTransactions(accountId = accountId, page = state.page - 1, typeFilter = state.typeFilter)
    }
}
