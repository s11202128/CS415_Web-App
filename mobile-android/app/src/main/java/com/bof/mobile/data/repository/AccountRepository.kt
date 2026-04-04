package com.bof.mobile.data.repository

import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.AccountDetailsResponse
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.CreateAccountRequest
import com.bof.mobile.model.PaginatedTransactionsResponse
import com.bof.mobile.model.TransactionItem
import com.bof.mobile.model.UpdateProfileRequest
import retrofit2.HttpException
import java.io.IOException

class AccountRepository(private val apiService: ApiService) {

    suspend fun syncProfileData(
        customerId: Int,
        fullName: String,
        mobile: String,
        email: String
    ): ApiResult<Unit> {
        return try {
            apiService.updateProfile(
                UpdateProfileRequest(
                    customerId = customerId,
                    fullName = fullName,
                    mobile = mobile,
                    nationalId = "",
                    residencyStatus = "resident",
                    tin = "",
                    email = email
                )
            )
            ApiResult.Success(Unit)
        } catch (e: HttpException) {
            ApiResult.Error(message = parseHttpError(e, "Failed to sync profile"), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun createAccount(request: CreateAccountRequest): ApiResult<AccountItem> {
        return try {
            ApiResult.Success(apiService.requestAccount(request))
        } catch (e: HttpException) {
            ApiResult.Error(message = parseHttpError(e, "Failed to create account"), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun getAccounts(): ApiResult<List<AccountItem>> {
        return try {
            val accounts = apiService.getAccounts()
            if (accounts.isNotEmpty()) {
                return ApiResult.Success(accounts)
            }

            // Fallback for legacy identity fragmentation: use server-side matched accounts.
            val debug = apiService.getDebugMyAccounts()
            if (debug.matchedAccountCount > 0) {
                val mapped = debug.matchedAccounts.map { row ->
                    AccountItem(
                        id = row.id,
                        accountNumber = row.accountNumber,
                        accountPin = null,
                        customerId = row.customerId,
                        accountHolder = row.accountHolder,
                        type = row.accountType,
                        balance = row.balance,
                        requestedOpeningBalance = null,
                        approvedOpeningBalance = null,
                        approvedByAdminId = null,
                        approvedAt = null,
                        rejectionReason = null,
                        maintenanceFee = if (row.accountType.equals("Simple Access", ignoreCase = true)) 2.5 else 0.0,
                        currency = "FJD",
                        status = row.status,
                        createdAt = row.createdAt ?: ""
                    )
                }
                return ApiResult.Success(mapped)
            }

            ApiResult.Success(accounts)
        } catch (e: HttpException) {
            ApiResult.Error(message = parseHttpError(e, "Failed to load accounts"), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun getAccountDetails(accountId: Int): ApiResult<AccountDetailsResponse> {
        return try {
            ApiResult.Success(apiService.getAccountDetails(accountId, 20))
        } catch (e: HttpException) {
            ApiResult.Error(message = parseHttpError(e, "Failed to load account details"), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun getTransactions(
        accountId: Int,
        page: Int = 1,
        pageSize: Int = 20,
        typeFilter: String? = null
    ): ApiResult<PaginatedTransactionsResponse> {
        return try {
            ApiResult.Success(
                apiService.getTransactionsPaginated(
                    accountId = accountId,
                    paginated = true,
                    page = page,
                    pageSize = pageSize,
                    type = typeFilter
                )
            )
        } catch (e: HttpException) {
            try {
                val fallbackItems: List<TransactionItem> = apiService.getTransactions(accountId)
                val filtered = if (typeFilter.isNullOrBlank()) {
                    fallbackItems
                } else {
                    fallbackItems.filter { it.kind.equals(typeFilter, ignoreCase = true) }
                }
                val start = ((page - 1) * pageSize).coerceAtLeast(0)
                val pageItems = if (start >= filtered.size) emptyList() else filtered.drop(start).take(pageSize)
                val totalPages = if (filtered.isEmpty()) 0 else ((filtered.size + pageSize - 1) / pageSize)
                ApiResult.Success(
                    PaginatedTransactionsResponse(
                        items = pageItems,
                        page = page,
                        pageSize = pageSize,
                        total = filtered.size,
                        totalPages = totalPages
                    )
                )
            } catch (inner: Exception) {
                ApiResult.Error(message = parseHttpError(e, "Failed to load transactions"), code = e.code())
            }
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
