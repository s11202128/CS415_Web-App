package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.data.repository.FeatureRepository
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.VerifyWithdrawalRequest
import com.bof.mobile.model.WithdrawRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WithdrawUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val customerAccounts: List<AccountItem> = emptyList(),
    val customerAccountsLoaded: Boolean = false,
    val withdrawAccountId: String = "",
    val withdrawAmount: String = "",
    val withdrawNote: String = "",
    val withdrawOtp: String = "",
    val showWithdrawOtpField: Boolean = false,
    val withdrawalId: String? = null
)

class WithdrawViewModel(
    private val accountRepository: AccountRepository,
    private val featureRepository: FeatureRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WithdrawUiState())
    val uiState: StateFlow<WithdrawUiState> = _uiState

    init {
        loadAccounts()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, customerAccountsLoaded = false) }
            when (val result = accountRepository.getAccounts()) {
                is ApiResult.Success -> {
                    val selected = result.data.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customerAccounts = result.data,
                            customerAccountsLoaded = true,
                            withdrawAccountId = it.withdrawAccountId.ifBlank { selected?.id?.toString().orEmpty() },
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customerAccountsLoaded = true,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun onWithdrawAccountIdChanged(value: String) = _uiState.update {
        it.copy(withdrawAccountId = value, errorMessage = null, successMessage = null)
    }

    fun onWithdrawAmountChanged(value: String) = _uiState.update {
        it.copy(withdrawAmount = value, errorMessage = null, successMessage = null)
    }

    fun onWithdrawNoteChanged(value: String) = _uiState.update {
        it.copy(withdrawNote = value, errorMessage = null, successMessage = null)
    }

    fun onWithdrawOtpChanged(value: String) = _uiState.update {
        it.copy(withdrawOtp = value, errorMessage = null, successMessage = null)
    }

    fun withdraw() {
        val state = _uiState.value
        val accountId = state.withdrawAccountId.toIntOrNull()
        val amount = state.withdrawAmount.toDoubleOrNull()

        if (accountId == null || accountId <= 0) {
            return setError("Please select a valid account")
        }

        if (amount == null || amount <= 0) {
            return setError("Amount must be greater than 0")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = featureRepository.withdraw(WithdrawRequest(accountId, amount, state.withdrawNote.ifBlank { null }))) {
                is ApiResult.Success -> {
                    if (result.data.requiresOtp) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showWithdrawOtpField = true,
                                withdrawalId = result.data.withdrawalId,
                                successMessage = "OTP verification required",
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                withdrawAmount = "",
                                withdrawNote = "",
                                withdrawOtp = "",
                                showWithdrawOtpField = false,
                                withdrawalId = null,
                                successMessage = "Withdrawal successful - FJD ${String.format("%.2f", amount)}",
                                errorMessage = null
                            )
                        }
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun verifyWithdrawalOtp() {
        val state = _uiState.value
        val withdrawalId = state.withdrawalId

        if (withdrawalId.isNullOrBlank()) {
            return setError("Withdrawal ID not found")
        }

        if (state.withdrawOtp.isBlank()) {
            return setError("OTP is required")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = featureRepository.verifyWithdrawal(VerifyWithdrawalRequest(withdrawalId, state.withdrawOtp))) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            withdrawAmount = "",
                            withdrawNote = "",
                            withdrawOtp = "",
                            showWithdrawOtpField = false,
                            withdrawalId = null,
                            successMessage = "Withdrawal verified and completed successfully",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message, successMessage = null) }
    }
}