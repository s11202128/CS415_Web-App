package com.bof.mobile.model

data class DepositRequest(
    val accountId: Int,
    val amount: Double,
    val note: String? = null
)

data class DepositResponse(
    val success: Boolean,
    val message: String,
    val transactionId: Long,
    val balanceAfter: Double,
    val amount: Double
)

data class WithdrawRequest(
    val accountId: Int,
    val amount: Double
)

data class WithdrawResponse(
    val success: Boolean,
    val message: String,
    val requiresOtp: Boolean = false,
    val withdrawalId: String? = null,
    val expiresInSeconds: Int? = null,
    val attemptsRemaining: Int? = null,
    val transactionId: Long? = null,
    val balanceAfter: Double? = null,
    val amount: Double? = null
)

data class VerifyWithdrawalRequest(
    val withdrawalId: String,
    val otp: String
)
