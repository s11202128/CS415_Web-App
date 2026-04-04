package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.data.repository.TransferRepository
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.TransferMoneyRequest
import com.bof.mobile.model.TransferMode
import com.bof.mobile.model.TransferMoneyResponse
import com.bof.mobile.model.VerifyTransferOtpRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferUiState(
    val accounts: List<AccountItem> = emptyList(),
    val accountsLoading: Boolean = false,
    val accountsLoaded: Boolean = false,
    val transferMode: TransferMode = TransferMode.INTERNAL,
    val fromAccountId: String = "",
    val internalDestinationAccountId: String = "",
    val recipientName: String = "",
    val bankName: String = "",
    val externalAccountNumber: String = "",
    val amount: String = "",
    val note: String = "",
    val otp: String = "",
    val transferId: String? = null,
    val requiresOtp: Boolean = false,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val dailyLimit: Double = 10000.0,
    val latestServerResponse: TransferMoneyResponse? = null,
    val lastUpdatedAtEpochMs: Long? = null
)

class TransferViewModel(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(accountsLoading = true, accountsLoaded = false) }
            when (val result = accountRepository.getAccounts()) {
                is ApiResult.Success -> {
                    val firstActive = result.data.firstOrNull { it.status.equals("active", ignoreCase = true) }
                    _uiState.update {
                        it.copy(
                            accounts = result.data,
                            fromAccountId = it.fromAccountId.ifBlank { firstActive?.id?.toString().orEmpty() },
                            accountsLoading = false,
                            accountsLoaded = true,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            accountsLoading = false,
                            accountsLoaded = true,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    fun onTransferModeChanged(mode: TransferMode) = _uiState.update {
        it.copy(
            transferMode = mode,
            internalDestinationAccountId = "",
            recipientName = "",
            bankName = "",
            externalAccountNumber = "",
            otp = "",
            transferId = null,
            requiresOtp = false,
            errorMessage = null,
            successMessage = null,
            latestServerResponse = null
        )
    }

    fun onFromAccountIdChanged(value: String) = _uiState.update {
        it.copy(fromAccountId = normalizeResolvedAccountId(value))
    }

    fun onInternalDestinationAccountIdChanged(value: String) = _uiState.update {
        it.copy(internalDestinationAccountId = normalizeResolvedAccountId(value))
    }
    fun onRecipientNameChanged(value: String) = _uiState.update { it.copy(recipientName = value) }
    fun onBankNameChanged(value: String) = _uiState.update { it.copy(bankName = value) }
    fun onExternalAccountNumberChanged(value: String) = _uiState.update { it.copy(externalAccountNumber = value) }
    fun onAmountChanged(value: String) = _uiState.update { it.copy(amount = value) }
    fun onNoteChanged(value: String) = _uiState.update { it.copy(note = value) }
    fun onOtpChanged(value: String) = _uiState.update { it.copy(otp = value) }

    fun submitTransfer() {
        val state = _uiState.value
        val fromAccountId = state.fromAccountId.toIntOrNull()
        val amount = state.amount.toDoubleOrNull()

        if (fromAccountId == null || fromAccountId <= 0) {
            return setError("Source account must be resolved to a valid account ID")
        }
        if (amount == null || amount <= 0) {
            return setError("Amount must be greater than 0")
        }
        if (amount > state.dailyLimit) {
            return setError("Amount exceeds daily transfer limit")
        }

        if (state.transferMode == TransferMode.INTERNAL) {
            val destinationAccountId = state.internalDestinationAccountId.toIntOrNull()
            if (destinationAccountId == null || destinationAccountId <= 0) {
                return setError("Destination account must be resolved to a valid account ID")
            }
            if (destinationAccountId == fromAccountId) {
                return setError("Source and destination accounts must be different")
            }
        } else {
            if (state.recipientName.isBlank()) return setError("Recipient name is required")
            if (state.bankName.isBlank()) return setError("Bank name is required")
            if (state.externalAccountNumber.isBlank()) return setError("Account number is required")
        }

        viewModelScope.launch {
            setLoading(true)
            val request = TransferMoneyRequest(
                fromAccount = fromAccountId,
                transferType = if (state.transferMode == TransferMode.INTERNAL) "internal" else "external",
                toAccount = state.internalDestinationAccountId.toIntOrNull(),
                recipientName = state.recipientName.ifBlank { null },
                bankName = state.bankName.ifBlank { null },
                accountNumber = state.externalAccountNumber.ifBlank { null },
                amount = amount,
                note = state.note.ifBlank { null }
            )

            when (val result = transferRepository.transfer(request)) {
                is ApiResult.Success -> handleTransferSuccess(result.data, amount)
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        val transferId = state.transferId
        if (transferId.isNullOrBlank()) {
            return setError("Transfer ID not found")
        }
        if (state.otp.isBlank()) {
            return setError("OTP is required")
        }

        viewModelScope.launch {
            setLoading(true)
            when (val result = transferRepository.verifyTransferOtp(VerifyTransferOtpRequest(transferId, state.otp.trim()))) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            fromAccountId = "",
                            internalDestinationAccountId = "",
                            recipientName = "",
                            bankName = "",
                            externalAccountNumber = "",
                            amount = "",
                            note = "",
                            otp = "",
                            transferId = null,
                            requiresOtp = false,
                            successMessage = result.data.message,
                            errorMessage = null,
                            latestServerResponse = result.data,
                            lastUpdatedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun validateTransferReady(): Boolean {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return false
        if (amount <= 0 || amount > state.dailyLimit) return false
        return if (state.transferMode == TransferMode.INTERNAL) {
            val fromId = state.fromAccountId.toIntOrNull()
            val destinationId = state.internalDestinationAccountId.toIntOrNull()
            fromId != null && fromId > 0 && destinationId != null && destinationId > 0 && destinationId != fromId
        } else {
            val fromId = state.fromAccountId.toIntOrNull()
            fromId != null && fromId > 0 && state.recipientName.isNotBlank() && state.bankName.isNotBlank() && state.externalAccountNumber.isNotBlank()
        }
    }

    private fun handleTransferSuccess(response: TransferMoneyResponse, amount: Double) {
        if (response.requiresOtp) {
            _uiState.update {
                it.copy(
                    transferId = response.transferId,
                    requiresOtp = true,
                    successMessage = response.message,
                    errorMessage = null,
                    latestServerResponse = response,
                    lastUpdatedAtEpochMs = System.currentTimeMillis()
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    fromAccountId = "",
                    internalDestinationAccountId = "",
                    recipientName = "",
                    bankName = "",
                    externalAccountNumber = "",
                    amount = "",
                    note = "",
                    otp = "",
                    transferId = null,
                    requiresOtp = false,
                    successMessage = response.message.ifBlank { "Transfer successful - FJD ${String.format("%.2f", amount)}" },
                    errorMessage = null,
                    latestServerResponse = response,
                    lastUpdatedAtEpochMs = System.currentTimeMillis()
                )
            }
        }
    }

    private fun normalizeResolvedAccountId(value: String): String {
        val trimmed = value.trim()
        val parsed = trimmed.toIntOrNull() ?: return ""
        return if (parsed > 0) parsed.toString() else ""
    }

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message, successMessage = null) }
    }
}
