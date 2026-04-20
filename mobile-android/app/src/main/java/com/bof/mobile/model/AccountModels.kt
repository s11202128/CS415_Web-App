package com.bof.mobile.model

data class AccountItem(
    val id: Int,
    val accountNumber: String,
    val accountPin: String? = null,
    val customerId: Int,
    val accountHolder: String,
    val tin: String? = null,
    val type: String,
    val balance: Double,
    val requestedOpeningBalance: Double? = null,
    val approvedOpeningBalance: Double? = null,
    val approvedByAdminId: Int? = null,
    val approvedAt: String? = null,
    val rejectionReason: String? = null,
    val maintenanceFee: Double,
    val currency: String,
    val status: String,
    val createdAt: String
)

data class AccountDetailsResponse(
    val account: AccountItem,
    val customer: AccountOwner,
    val transactions: List<TransactionItem>
)

data class AccountOwner(
    val id: Int,
    val fullName: String
)

data class TransactionItem(
    val id: Int,
    val accountId: Int?,
    val accountNumber: String,
    val kind: String,
    val amount: Double,
    val description: String,
    val status: String,
    val suspicious: Boolean,
    val createdAt: String
)

data class PaginatedTransactionsResponse(
    val items: List<TransactionItem>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int
)

data class DebugAccountsResponse(
    val matchedAccountCount: Int,
    val matchedAccounts: List<DebugAccountItem>
)

data class DebugAccountItem(
    val id: Int,
    val customerId: Int,
    val accountNumber: String,
    val accountHolder: String,
    val accountType: String,
    val status: String,
    val balance: Double,
    val createdAt: String? = null
)

data class RecipientItem(
    val accountId: Int,
    val accountNumber: String,
    val accountHolder: String,
    val customerId: Int
)

data class BillerItem(
    val code: String,
    val name: String
)

data class CreateAccountRequest(
    val type: String,
    val openingBalance: Double,
    val customerId: Int? = null,
    val customerName: String? = null
)
