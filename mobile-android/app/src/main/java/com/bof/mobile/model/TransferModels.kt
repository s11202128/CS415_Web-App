package com.bof.mobile.model

enum class TransferMode {
    INTERNAL,
    EXTERNAL
}

data class ValidateDestinationRequest(
    val fromAccountId: Int,
    val toAccountNumber: String
)

data class ValidateDestinationResponse(
    val accountNumber: String,
    val customerId: Int,
    val customerName: String
)

data class InitiateTransferRequest(
    val fromAccountId: Int,
    val toAccountNumber: String,
    val amount: Double,
    val description: String
)

data class InitiateTransferResponse(
    val highValueThreshold: Double,
    val status: String,
    val requiresOtp: Boolean,
    val transferId: String?,
    val otp: String?
)

data class VerifyTransferRequest(
    val transferId: String,
    val otp: String
)

data class VerifyTransferResponse(
    val status: String,
    val transferId: String?
)

data class TransferMoneyRequest(
    val fromAccount: Int,
    val transferType: String,
    val toAccount: Int? = null,
    val recipientName: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val amount: Double,
    val note: String? = null
)

data class TransferMoneyResponse(
    val success: Boolean,
    val message: String,
    val requiresOtp: Boolean = false,
    val transferId: String? = null,
    val otp: String? = null,
    val transactionId: Long? = null,
    val creditTransactionId: Long? = null,
    val balanceAfter: Double? = null,
    val amount: Double? = null,
    val attemptsRemaining: Int? = null
)

data class VerifyTransferOtpRequest(
    val transferId: String,
    val otp: String
)
