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

sealed class OtpState {
    data object Idle : OtpState()
    data object Sending : OtpState()
    data class OtpSent(val transferId: String, val message: String? = null) : OtpState()
    data object Verifying : OtpState()
    data class Verified(val message: String) : OtpState()
    data class Error(val message: String) : OtpState()
}

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
    val pendingTransactionId: Long? = null,
    val requiresOtp: Boolean = false,
    val otpState: OtpState = OtpState.Idle,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val transferSuccessDialogMessage: String? = null,
    val dailyLimit: Double = 10000.0,
    val latestServerResponse: TransferMoneyResponse? = null,
    val lastUpdatedAtEpochMs: Long? = null
)

class TransferViewModel(
    private val transferRepository: TransferRepository,
    private val accountRepository: AccountRepository,
    private val loggedInCustomerId: Int? = null
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
                    val scopedAccounts = result.data.filter { account ->
                        val matchesCustomer = loggedInCustomerId?.let { it > 0 && account.customerId == it } ?: true
                        matchesCustomer
                    }
                    val firstActive = scopedAccounts.firstOrNull { it.status.equals("active", ignoreCase = true) }
                    _uiState.update {
                        it.copy(
                            accounts = scopedAccounts,
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

    fun clearMessages() = _uiState.update {
        it.copy(
            errorMessage = null,
            successMessage = null,
            transferSuccessDialogMessage = null
        )
    }

    fun dismissTransferSuccessPopup() {
        _uiState.update { it.copy(transferSuccessDialogMessage = null) }
    }

    fun onTransferModeChanged(mode: TransferMode) = _uiState.update {
        it.copy(
            transferMode = mode,
            internalDestinationAccountId = "",
            recipientName = "",
            bankName = "",
            externalAccountNumber = "",
            otp = "",
            transferId = null,
            pendingTransactionId = null,
            requiresOtp = false,
            otpState = OtpState.Idle,
            errorMessage = null,
            successMessage = null,
            transferSuccessDialogMessage = null,
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
            _uiState.update { it.copy(otpState = OtpState.Sending, errorMessage = null, successMessage = null) }
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
                is ApiResult.Error -> {
                    _uiState.update { it.copy(otpState = OtpState.Error(result.message)) }
                    setError(result.message)
                }
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
            _uiState.update { it.copy(otpState = OtpState.Verifying, errorMessage = null) }
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
                            pendingTransactionId = null,
                            requiresOtp = false,
                            otpState = OtpState.Verified(result.data.message.ifBlank { "Transfer verified successfully" }),
                            successMessage = result.data.message,
                            transferSuccessDialogMessage = result.data.message.ifBlank { "Transfer verified successfully" },
                            errorMessage = null,
                            latestServerResponse = result.data,
                            lastUpdatedAtEpochMs = System.currentTimeMillis()
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            otpState = OtpState.Error(result.message),
                            requiresOtp = true,
                            errorMessage = result.message,
                            successMessage = null
                        )
                    }
                }
            }
            setLoading(false)
        }
    }

    fun resetOtpFlow() {
        _uiState.update {
            it.copy(
                otp = "",
                transferId = null,
                pendingTransactionId = null,
                requiresOtp = false,
                otpState = OtpState.Idle,
                errorMessage = null,
                successMessage = null,
                transferSuccessDialogMessage = null
            )
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
        val otpRequired = response.otpRequired ?: response.requiresOtp
        if (otpRequired) {
            val transferId = response.transferId
            if (transferId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        otpState = OtpState.Error("OTP required but transfer reference was missing"),
                        errorMessage = "OTP required but transfer reference was missing",
                        successMessage = null,
                        requiresOtp = false
                    )
                }
                return
            }
            _uiState.update {
                it.copy(
                    transferId = transferId,
                    pendingTransactionId = response.transactionId,
                    requiresOtp = true,
                    otpState = OtpState.OtpSent(transferId = transferId, message = response.message),
                    successMessage = response.message,
                    errorMessage = null,
                    latestServerResponse = response,
                    lastUpdatedAtEpochMs = System.currentTimeMillis()
                )
            }
        } else {
            val resolvedSuccessMessage = response.message.ifBlank { "Transfer successful - FJD ${String.format("%.2f", amount)}" }
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
                    pendingTransactionId = null,
                    requiresOtp = false,
                    otpState = OtpState.Verified(response.message.ifBlank { "Transfer successful - FJD ${String.format("%.2f", amount)}" }),
                    successMessage = resolvedSuccessMessage,
                    transferSuccessDialogMessage = resolvedSuccessMessage,
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
        _uiState.update { it.copy(errorMessage = message, successMessage = null, transferSuccessDialogMessage = null) }
    }
}
