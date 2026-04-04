package com.bof.mobile.model

data class BillPaymentRequest(
    val accountId: Int,
    val accountNumber: String? = null,
    val payee: String,
    val amount: Double,
    val scheduledDate: String? = null,
    val billType: String? = null,
    val paymentMethod: String? = null,
    val note: String? = null,
    val repeat: String? = null,
    val paymentDate: String? = null
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

data class BankStatementRequest(
    val fromDate: String,
    val toDate: String
)

data class BankStatementDateRange(
    val fromDate: String,
    val toDate: String
)

data class BankStatementTransaction(
    val id: Int,
    val user_id: Int,
    val date: String,
    val description: String,
    val amount: Double,
    val balance: Double,
    val transactionType: String,
    val accountNumber: String
)

data class BankStatementResponse(
    val bankName: String,
    val customerName: String,
    val accountNumber: String,
    val dateRange: BankStatementDateRange,
    val transactions: List<BankStatementTransaction>
)

data class AccountOverviewReport(
    val customerName: String,
    val accountNumber: String,
    val currentBalance: Double,
    val totalTransactions: Int
)

data class ReportPoint(
    val period: String,
    val credit: Double,
    val debit: Double,
    val total: Double
)

data class ReportResponse(
    val accountOverview: AccountOverviewReport,
    val points: List<ReportPoint>
)

data class ActivityLogRequest(
    val activityType: String,
    val description: String,
    val status: String = "success"
)

data class ActivityLogItem(
    val id: Int,
    val user_id: Int,
    val activity_type: String,
    val description: String,
    val timestamp: String,
    val status: String
)

data class FundingInvestmentRequest(
    val amount: Double,
    val investmentType: String,
    val durationMonths: Int,
    val notes: String? = null
)

data class FundingInvestmentResponse(
    val id: Int,
    val customerId: Int,
    val investmentType: String,
    val amount: Double,
    val durationMonths: Int,
    val notes: String?,
    val status: String,
    val createdAt: String
)

data class FundingLoanRequest(
    val loanAmount: Double,
    val loanType: String,
    val repaymentPeriodMonths: Int,
    val purpose: String,
    val details: String? = null
)

data class FundingLoanResponse(
    val id: Int,
    val customerId: Int,
    val loanType: String,
    val loanAmount: Double,
    val repaymentPeriodMonths: Int,
    val purpose: String,
    val details: String?,
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
    val tin: String,
    val email: String? = null
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
