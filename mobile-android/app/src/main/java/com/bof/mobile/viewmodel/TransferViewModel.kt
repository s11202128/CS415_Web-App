package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.TransferRepository
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
    val dailyLimit: Double = 10000.0
)

class TransferViewModel(private val transferRepository: TransferRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState

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
            successMessage = null
        )
    }

    fun onFromAccountIdChanged(value: String) = _uiState.update { it.copy(fromAccountId = value) }
    fun onInternalDestinationAccountIdChanged(value: String) = _uiState.update { it.copy(internalDestinationAccountId = value) }
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
            return setError("Please select a valid source account")
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
                return setError("Please select a destination account")
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
                            errorMessage = null
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
            state.fromAccountId.isNotBlank() && state.internalDestinationAccountId.isNotBlank() && state.internalDestinationAccountId != state.fromAccountId
        } else {
            state.fromAccountId.isNotBlank() && state.recipientName.isNotBlank() && state.bankName.isNotBlank() && state.externalAccountNumber.isNotBlank()
        }
    }

    private fun handleTransferSuccess(response: TransferMoneyResponse, amount: Double) {
        if (response.requiresOtp) {
            _uiState.update {
                it.copy(
                    transferId = response.transferId,
                    requiresOtp = true,
                    successMessage = response.message,
                    errorMessage = null
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
                    errorMessage = null
                )
            }
        }
    }

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message, successMessage = null) }
    }
}
