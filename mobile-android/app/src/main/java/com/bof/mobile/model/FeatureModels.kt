package com.bof.mobile.model

data class BillPaymentRequest(
    val accountId: Int,
    val payee: String,
    val amount: Double,
    val scheduledDate: String? = null
)

data class ScheduledBillItem(
    val id: Int,
    val accountId: Int,
    val customerId: Int,
    val payee: String,
    val amount: Double,
    val scheduledDate: String,
    val status: String,
    val createdAt: String
)

data class BillHistoryItem(
    val id: Int,
    val accountId: Int,
    val customerId: Int,
    val payee: String,
    val amount: Double,
    val scheduledDate: String?,
    val status: String,
    val createdAt: String,
    val description: String?
)

data class StatementRequestPayload(
    val accountId: Int,
    val accountNumber: String,
    val fullName: String,
    val accountHolder: String,
    val fromDate: String,
    val toDate: String
)

data class StatementRequestItem(
    val id: Int,
    val customerId: Int,
    val accountId: Int?,
    val fullName: String,
    val accountHolder: String,
    val accountNumber: String,
    val fromDate: String,
    val toDate: String,
    val status: String,
    val adminNote: String?,
    val reviewedBy: String?,
    val reviewedAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class StatementRowItem(
    val id: Int,
    val accountId: Int?,
    val accountNumber: String,
    val type: String?,
    val kind: String?,
    val amount: Double,
    val description: String,
    val status: String,
    val createdAt: String
)

data class NotificationItem(
    val id: Int,
    val userId: Int,
    val phoneNumber: String,
    val message: String,
    val notificationType: String,
    val deliveryStatus: String,
    val providerMessageId: String?,
    val timestamp: String
)

data class ProfileResponse(
    val id: Int,
    val fullName: String,
    val mobile: String,
    val nationalId: String,
    val residencyStatus: String,
    val tin: String,
    val identityVerified: Boolean,
    val registrationStatus: String,
    val status: String,
    val email: String,
    val emailVerified: Boolean,
    val failedLoginAttempts: Int,
    val lockedUntil: String?,
    val lastLoginAt: String?
)

data class UpdateProfileRequest(
    val customerId: Int,
    val fullName: String,
    val mobile: String,
    val nationalId: String,
    val residencyStatus: String,
    val tin: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ForgotPasswordResponse(
    val resetId: String,
    val message: String
)

data class ResetPasswordRequest(
    val email: String,
    val resetId: String,
    val otp: String,
    val newPassword: String
)

data class ResetPasswordResponse(
    val status: String
)

data class LoanProductItem(
    val id: String,
    val name: String,
    val annualRate: Double,
    val maxAmount: Double,
    val minTermMonths: Int,
    val maxTermMonths: Int
)

data class LoanApplicationRequest(
    val customerId: Int,
    val loanProductId: String,
    val requestedAmount: Double,
    val termMonths: Int,
    val purpose: String,
    val monthlyIncome: Double,
    val occupation: String
)

data class LoanApplicationItem(
    val id: Int,
    val customerId: Int,
    val loanProductId: String,
    val requestedAmount: Double,
    val termMonths: Int,
    val purpose: String,
    val monthlyIncome: Double,
    val occupation: String,
    val status: String,
    val createdAt: String,
    val submittedAt: String,
    val reviewedAt: String?
)

data class InterestSummaryItem(
    val year: Int,
    val accountId: Int,
    val customerId: Int,
    val customerName: String,
    val grossInterest: Double,
    val withholdingTax: Double,
    val netInterest: Double,
    val status: String
)

data class InvestmentRequest(
    val customerId: Int,
    val investmentType: String,
    val amount: Double,
    val expectedReturn: Double? = null,
    val maturityDate: String? = null,
    val status: String? = null
)

data class InvestmentItem(
    val id: Int,
    val customerId: Int,
    val customerName: String,
    val investmentType: String,
    val amount: Double,
    val expectedReturn: Double?,
    val maturityDate: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
