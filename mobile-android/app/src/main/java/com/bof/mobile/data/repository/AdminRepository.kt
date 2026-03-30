package com.bof.mobile.data.repository

import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.AdminCreateAccountRequest
import com.bof.mobile.model.AdminCreateDepositRequest
import com.bof.mobile.model.AdminCreateDepositResponse
import com.bof.mobile.model.AdminDashboardReport
import com.bof.mobile.model.AdminLoginLogItem
import com.bof.mobile.model.AdminNotificationLogItem
import com.bof.mobile.model.AdminOtpAttemptItem
import com.bof.mobile.model.AdminStatementUpdateRequest
import com.bof.mobile.model.AdminTestSmsRequest
import com.bof.mobile.model.AdminTestSmsResponse
import com.bof.mobile.model.AdminTransactionItem
import com.bof.mobile.model.AdminTransferLimitResponse
import com.bof.mobile.model.AdminUpdateLoanRequest
import com.bof.mobile.model.AdminUpdateTransferLimitRequest
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.InterestSummaryItem
import com.bof.mobile.model.InvestmentItem
import com.bof.mobile.model.InvestmentRequest
import com.bof.mobile.model.LoanApplicationItem
import com.bof.mobile.model.ProfileResponse
import com.bof.mobile.model.StatementRequestItem
import retrofit2.HttpException
import java.io.IOException

class AdminRepository(private val apiService: ApiService) {

    private fun extractErrorMessage(e: HttpException): String {
        val raw = e.response()?.errorBody()?.string()?.trim()
        if (!raw.isNullOrBlank()) {
            val marker = "\"error\":\""
            val start = raw.indexOf(marker)
            if (start >= 0) {
                val messageStart = start + marker.length
                val end = raw.indexOf('"', messageStart)
                if (end > messageStart) {
                    return raw.substring(messageStart, end)
                }
            }
            return raw
        }
        return e.message() ?: "Request failed"
    }

    suspend fun getAdminDashboardReport(): ApiResult<AdminDashboardReport> = safeCall { apiService.getAdminDashboardReport() }

    suspend fun getAdminCustomers(query: String? = null): ApiResult<List<ProfileResponse>> = safeCall {
        apiService.getAdminCustomers(query)
    }

    suspend fun updateAdminCustomer(id: Int, body: Map<String, Any?>): ApiResult<ProfileResponse> = safeCall {
        apiService.updateAdminCustomer(id, body)
    }

    suspend fun getAccounts(): ApiResult<List<AccountItem>> = safeCall { apiService.getAccounts() }

    suspend fun createAdminAccount(body: AdminCreateAccountRequest): ApiResult<AccountItem> = safeCall {
        apiService.createAdminAccount(body)
    }

    suspend fun updateAdminAccount(id: Int, body: Map<String, Any?>): ApiResult<AccountItem> = safeCall {
        apiService.updateAdminAccount(id, body)
    }

    suspend fun freezeAdminAccount(id: Int): ApiResult<AccountItem> = safeCall {
        apiService.freezeAdminAccount(id)
    }

    suspend fun createAdminDeposit(body: AdminCreateDepositRequest): ApiResult<AdminCreateDepositResponse> = safeCall {
        apiService.createAdminDeposit(body)
    }

    suspend fun getLoanApplications(): ApiResult<List<LoanApplicationItem>> = safeCall { apiService.getLoanApplications() }

    suspend fun updateLoanApplication(id: Int, status: String): ApiResult<LoanApplicationItem> = safeCall {
        apiService.updateAdminLoanApplication(id, AdminUpdateLoanRequest(status = status))
    }

    suspend fun getAdminTransactions(accountNumber: String? = null): ApiResult<List<AdminTransactionItem>> = safeCall {
        apiService.getAdminTransactions(accountNumber)
    }

    suspend fun reverseTransaction(id: Int): ApiResult<String> = safeCall {
        val response = apiService.reverseAdminTransaction(id)
        (response["status"] ?: "Transaction reversed").toString()
    }

    suspend fun getAdminLoginLogs(limit: Int = 200): ApiResult<List<AdminLoginLogItem>> = safeCall {
        apiService.getAdminLoginLogs(limit)
    }

    suspend fun getAdminTransfers(): ApiResult<List<AdminTransactionItem>> = safeCall { apiService.getAdminTransfers() }

    suspend fun getTransferLimit(): ApiResult<AdminTransferLimitResponse> = safeCall { apiService.getAdminTransferLimit() }

    suspend fun updateTransferLimit(value: Double): ApiResult<AdminTransferLimitResponse> = safeCall {
        apiService.updateAdminTransferLimit(AdminUpdateTransferLimitRequest(highValueTransferLimit = value))
    }

    suspend fun getAdminStatementRequests(): ApiResult<List<StatementRequestItem>> = safeCall { apiService.getAdminStatementRequests() }

    suspend fun updateAdminStatementRequest(id: Int, status: String, note: String?): ApiResult<StatementRequestItem> = safeCall {
        apiService.updateAdminStatementRequest(id, AdminStatementUpdateRequest(status = status, adminNote = note))
    }

    suspend fun getAdminNotificationLogs(limit: Int = 200): ApiResult<List<AdminNotificationLogItem>> = safeCall {
        apiService.getAdminNotificationLogs(limit)
    }

    suspend fun sendAdminTestSms(mobile: String, message: String): ApiResult<AdminTestSmsResponse> = safeCall {
        apiService.sendAdminTestSms(AdminTestSmsRequest(mobile = mobile, message = message))
    }

    suspend fun getAdminOtpAttempts(limit: Int = 200): ApiResult<List<AdminOtpAttemptItem>> = safeCall {
        apiService.getAdminOtpAttempts(limit)
    }

    suspend fun getAdminInvestments(customerId: Int? = null, status: String? = null): ApiResult<List<InvestmentItem>> = safeCall {
        apiService.getAdminInvestments(customerId = customerId, status = status)
    }

    suspend fun createAdminInvestment(request: InvestmentRequest): ApiResult<InvestmentItem> = safeCall {
        apiService.createAdminInvestment(request)
    }

    suspend fun updateAdminInvestment(id: Int, body: Map<String, Any?>): ApiResult<InvestmentItem> = safeCall {
        apiService.updateAdminInvestment(id, body)
    }

    suspend fun getInterestRate(): ApiResult<Double> = safeCall {
        apiService.getInterestRateConfig()["reserveBankMinSavingsInterestRate"] ?: 0.0
    }

    suspend fun updateInterestRate(rate: Double): ApiResult<Double> = safeCall {
        apiService.updateInterestRateConfig(mapOf("reserveBankMinSavingsInterestRate" to rate))["reserveBankMinSavingsInterestRate"] ?: rate
    }

    suspend fun generateSummaries(year: Int): ApiResult<List<InterestSummaryItem>> = safeCall {
        apiService.generateInterestSummaries(mapOf("year" to year))
    }

    suspend fun getSummaries(year: Int): ApiResult<List<InterestSummaryItem>> = safeCall {
        apiService.getInterestSummaries(year)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (e: HttpException) {
            ApiResult.Error(message = extractErrorMessage(e), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
