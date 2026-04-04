package com.bof.mobile.model

data class AdminDashboardReport(
    val generatedAt: String,
    val metrics: AdminMetrics,
    val transactionsByDay: List<TransactionsByDayItem>,
    val accountTypeBreakdown: List<BreakdownItem>,
    val loanStatusBreakdown: List<BreakdownItem>,
    val recentTransactions: List<AdminTransactionItem>
)

data class AdminMetrics(
    val totalCustomers: Int,
    val totalAccounts: Int,
    val totalDeposits: Double,
    val pendingLoans: Int,
    val frozenAccounts: Int,
    val todaysTransactions: Int
)

data class TransactionsByDayItem(
    val day: String,
    val count: Int,
    val totalAmount: Double
)

data class BreakdownItem(
    val label: String,
    val value: Int
)

data class AdminTransferLimitResponse(
    val highValueTransferLimit: Double
)

data class AdminUpdateTransferLimitRequest(
    val highValueTransferLimit: Double
)

data class AdminCreateAccountRequest(
    val customerId: Int? = null,
    val customerName: String? = null,
    val type: String,
    val openingBalance: Double,
    val accountNumber: String? = null
)

data class AdminCreateDepositRequest(
    val accountId: Int,
    val amount: Double,
    val description: String
)

data class AdminCreateDepositResponse(
    val message: String,
    val account: AccountItem
)

data class AdminUpdateLoanRequest(
    val status: String
)

data class AdminStatementUpdateRequest(
    val status: String,
    val adminNote: String? = null
)

data class AdminTestSmsRequest(
    val mobile: String,
    val message: String
)

data class AdminNotificationLogItem(
    val id: Int,
    val userId: Int,
    val phoneNumber: String,
    val message: String,
    val notificationType: String,
    val deliveryStatus: String,
    val providerMessageId: String?,
    val timestamp: String
)

data class AdminTestSmsResponse(
    val status: String,
    val provider: String?,
    val providerMessageId: String?,
    val mobile: String,
    val message: String
)

data class AdminLoginLogItem(
    val id: Int,
    val userType: String,
    val userId: Int?,
    val email: String,
    val success: Boolean,
    val failureReason: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: String
)

data class AdminOtpAttemptItem(
    val id: Int,
    val referenceCode: String,
    val customerId: Int,
    val transactionType: String,
    val attempts: Int,
    val maxAttempts: Int,
    val verified: Boolean,
    val expiresAt: String,
    val createdAt: String,
    val updatedAt: String
)

data class AdminTransactionItem(
    val id: Int,
    val accountId: Int,
    val accountNumber: String,
    val kind: String,
    val amount: Double,
    val description: String,
    val status: String,
    val suspicious: Boolean,
    val createdAt: String
)
