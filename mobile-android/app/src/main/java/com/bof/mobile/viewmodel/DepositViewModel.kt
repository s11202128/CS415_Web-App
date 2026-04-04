package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.data.repository.FeatureRepository
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.VerifyTransferOtpRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DepositUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val customerAccounts: List<AccountItem> = emptyList(),
    val customerAccountsLoaded: Boolean = false,
    val depositFromAccountId: String = "",
    val depositDestinationAccountId: String = "",
    val depositAmount: String = "",
    val depositNote: String = "",
    val depositOtp: String = "",
    val depositTransferId: String? = null,
    val showDepositOtpField: Boolean = false
)

class DepositViewModel(
    private val accountRepository: AccountRepository,
    private val featureRepository: FeatureRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DepositUiState())
    val uiState: StateFlow<DepositUiState> = _uiState

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
                    val selectedFromAccount = result.data.firstOrNull()
                    val selectedDestinationAccount = result.data.getOrNull(1) ?: selectedFromAccount
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customerAccounts = result.data,
                            customerAccountsLoaded = true,
                            depositFromAccountId = selectedFromAccount?.id?.toString().orEmpty(),
                            depositDestinationAccountId = selectedDestinationAccount?.id?.toString().orEmpty(),
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

    fun onDepositFromAccountIdChanged(value: String) = _uiState.update {
        it.copy(depositFromAccountId = value, errorMessage = null, successMessage = null)
    }

    fun onDepositDestinationAccountIdChanged(value: String) = _uiState.update {
        it.copy(depositDestinationAccountId = value, errorMessage = null, successMessage = null)
    }

    fun onDepositAmountChanged(value: String) = _uiState.update {
        it.copy(depositAmount = value, errorMessage = null, successMessage = null)
    }

    fun onDepositNoteChanged(value: String) = _uiState.update {
        it.copy(depositNote = value, errorMessage = null, successMessage = null)
    }

    fun onDepositOtpChanged(value: String) = _uiState.update {
        it.copy(depositOtp = value, errorMessage = null, successMessage = null)
    }

    fun deposit() {
        val state = _uiState.value
        val fromAccountId = state.depositFromAccountId.toIntOrNull()
        val destinationAccountId = state.depositDestinationAccountId.toIntOrNull()
        val amount = state.depositAmount.toDoubleOrNull()

        if (fromAccountId == null || fromAccountId <= 0) {
            return setError("Please select a valid from account")
        }

        if (destinationAccountId == null || destinationAccountId <= 0) {
            return setError("Please select a valid destination account")
        }

        if (fromAccountId == destinationAccountId) {
            return setError("From and destination accounts must be different")
        }

        if (amount == null || amount <= 0) {
            return setError("Amount must be greater than 0")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (
                val result = featureRepository.depositBetweenAccounts(
                    fromAccountId = fromAccountId,
                    destinationAccountId = destinationAccountId,
                    amount = amount,
                    note = state.depositNote.ifBlank { null }
                )
            ) {
                is ApiResult.Success -> {
                    if (result.data.requiresOtp) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                depositTransferId = result.data.transferId,
                                showDepositOtpField = true,
                                successMessage = result.data.message.ifBlank { "OTP verification required" },
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                depositAmount = "",
                                depositNote = "",
                                depositOtp = "",
                                depositTransferId = null,
                                showDepositOtpField = false,
                                successMessage = "Deposit successful - FJD ${String.format("%.2f", amount)}",
                                errorMessage = null
                            )
                        }
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun verifyDepositOtp() {
        val state = _uiState.value
        val transferId = state.depositTransferId
        if (transferId.isNullOrBlank()) {
            return setError("Deposit transfer ID not found")
        }
        if (state.depositOtp.isBlank()) {
            return setError("OTP is required")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (
                val result = featureRepository.verifyTransferOtp(
                    VerifyTransferOtpRequest(transferId = transferId, otp = state.depositOtp.trim())
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            depositAmount = "",
                            depositNote = "",
                            depositOtp = "",
                            depositTransferId = null,
                            showDepositOtpField = false,
                            successMessage = "Deposit successful${result.data.amount?.let { amt -> " - FJD ${String.format("%.2f", amt)}" } ?: ""}",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }
}