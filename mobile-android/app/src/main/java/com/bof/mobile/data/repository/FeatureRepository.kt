package com.bof.mobile.data.repository

import com.bof.mobile.data.remote.ApiService
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.BillHistoryItem
import com.bof.mobile.model.BillPaymentRequest
import com.bof.mobile.model.ActivityLogItem
import com.bof.mobile.model.ActivityLogRequest
import com.bof.mobile.model.BankStatementRequest
import com.bof.mobile.model.BankStatementResponse
import com.bof.mobile.model.DepositRequest
import com.bof.mobile.model.DepositResponse
import com.bof.mobile.model.ForgotPasswordRequest
import com.bof.mobile.model.ForgotPasswordResponse
import com.bof.mobile.model.FundingInvestmentRequest
import com.bof.mobile.model.FundingInvestmentResponse
import com.bof.mobile.model.FundingLoanRequest
import com.bof.mobile.model.FundingLoanResponse
import com.bof.mobile.model.InterestSummaryItem
import com.bof.mobile.model.InvestmentItem
import com.bof.mobile.model.InvestmentRequest
import com.bof.mobile.model.LoanApplicationItem
import com.bof.mobile.model.LoanApplicationRequest
import com.bof.mobile.model.LoanProductItem
import com.bof.mobile.model.NotificationItem
import com.bof.mobile.model.ProfileResponse
import com.bof.mobile.model.ReportResponse
import com.bof.mobile.model.ResetPasswordRequest
import com.bof.mobile.model.ResetPasswordResponse
import com.bof.mobile.model.ScheduledBillItem
import com.bof.mobile.model.StatementRequestItem
import com.bof.mobile.model.StatementRequestPayload
import com.bof.mobile.model.StatementRowItem
import com.bof.mobile.model.TransferMoneyRequest
import com.bof.mobile.model.TransferMoneyResponse
import com.bof.mobile.model.UpdateProfileRequest
import com.bof.mobile.model.VerifyTransferOtpRequest
import com.bof.mobile.model.VerifyWithdrawalRequest
import com.bof.mobile.model.WithdrawRequest
import com.bof.mobile.model.WithdrawResponse
import retrofit2.HttpException
import java.io.IOException

class FeatureRepository(private val apiService: ApiService) {

    suspend fun getProfile(customerId: Int): ApiResult<ProfileResponse> = safeCall { apiService.getProfile(customerId) }

    suspend fun getAccounts(): ApiResult<List<AccountItem>> = safeCall { apiService.getAccounts() }

    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<ProfileResponse> = safeCall {
        apiService.updateProfile(request)
    }

    suspend fun payBill(request: BillPaymentRequest): ApiResult<BillHistoryItem> = safeCall {
        apiService.payBill(request)
    }

    suspend fun payBillManual(request: BillPaymentRequest): ApiResult<BillHistoryItem> = safeCall {
        apiService.payBillManual(request)
    }

    suspend fun scheduleBill(request: BillPaymentRequest): ApiResult<ScheduledBillItem> = safeCall {
        apiService.scheduleBill(request)
    }

    suspend fun getScheduledBills(): ApiResult<List<ScheduledBillItem>> = safeCall {
        apiService.getScheduledBills()
    }

    suspend fun getBillHistory(): ApiResult<List<BillHistoryItem>> = safeCall {
        apiService.getBillHistory()
    }

    suspend fun runScheduledBill(id: Int): ApiResult<String> = safeCall {
        val result = apiService.runScheduledBill(id)
        (result["status"] ?: "scheduled payment executed").toString()
    }

    suspend fun createStatementRequest(payload: StatementRequestPayload): ApiResult<StatementRequestItem> = safeCall {
        apiService.createStatementRequest(payload)
    }

    suspend fun getStatementRequests(): ApiResult<List<StatementRequestItem>> = safeCall {
        apiService.getStatementRequests()
    }

    suspend fun getStatementByRequest(requestId: Int): ApiResult<List<StatementRowItem>> = safeCall {
        apiService.getStatementByRequest(requestId)
    }

    suspend fun getBankStatement(fromDate: String, toDate: String): ApiResult<BankStatementResponse> = safeCall {
        apiService.getBankStatement(BankStatementRequest(fromDate = fromDate, toDate = toDate))
    }

    suspend fun downloadBankStatementPdf(fromDate: String, toDate: String): ApiResult<ByteArray> {
        return try {
            val bytes = apiService.downloadBankStatement(BankStatementRequest(fromDate = fromDate, toDate = toDate)).bytes()
            ApiResult.Success(bytes)
        } catch (e: HttpException) {
            ApiResult.Error(message = parseHttpError(e, "Download failed"), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }

    suspend fun getNotifications(customerId: Int): ApiResult<List<NotificationItem>> = safeCall {
        apiService.getNotificationsHistory(limit = 200, customerId = customerId)
    }

    suspend fun getReport(): ApiResult<ReportResponse> = safeCall {
        apiService.getReport()
    }

    suspend fun getActivityLogs(fromDate: String?, toDate: String?, activityType: String?): ApiResult<List<ActivityLogItem>> = safeCall {
        apiService.getActivityLogs(fromDate = fromDate, toDate = toDate, activityType = activityType, limit = 500)
    }

    suspend fun createActivityLog(activityType: String, description: String, status: String = "success"): ApiResult<ActivityLogItem> = safeCall {
        apiService.createActivityLog(ActivityLogRequest(activityType = activityType, description = description, status = status))
    }

    suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResponse> = safeCall {
        apiService.forgotPassword(ForgotPasswordRequest(email = email))
    }

    suspend fun resetPassword(request: ResetPasswordRequest): ApiResult<ResetPasswordResponse> = safeCall {
        apiService.resetPassword(request)
    }

    suspend fun getLoanProducts(): ApiResult<List<LoanProductItem>> = safeCall {
        apiService.getLoanProducts()
    }

    suspend fun submitLoanApplication(request: LoanApplicationRequest): ApiResult<LoanApplicationItem> = safeCall {
        apiService.submitLoanApplication(request)
    }

    suspend fun getLoanApplications(): ApiResult<List<LoanApplicationItem>> = safeCall {
        apiService.getLoanApplications()
    }

    suspend fun getInterestSummaries(year: Int): ApiResult<List<InterestSummaryItem>> = safeCall {
        apiService.getInterestSummaries(year)
    }

    suspend fun getInvestments(customerId: Int): ApiResult<List<InvestmentItem>> = safeCall {
        apiService.getInvestments(customerId = customerId)
    }

    suspend fun createInvestment(request: InvestmentRequest): ApiResult<InvestmentItem> = safeCall {
        apiService.createInvestment(request)
    }

    suspend fun submitFundingInvestment(
        amount: Double,
        investmentType: String,
        durationMonths: Int,
        notes: String?
    ): ApiResult<FundingInvestmentResponse> = safeCall {
        apiService.submitFundingInvestment(
            FundingInvestmentRequest(
                amount = amount,
                investmentType = investmentType,
                durationMonths = durationMonths,
                notes = notes
            )
        )
    }

    suspend fun submitFundingLoan(
        loanAmount: Double,
        loanType: String,
        repaymentPeriodMonths: Int,
        purpose: String,
        details: String?
    ): ApiResult<FundingLoanResponse> = safeCall {
        apiService.submitFundingLoan(
            FundingLoanRequest(
                loanAmount = loanAmount,
                loanType = loanType,
                repaymentPeriodMonths = repaymentPeriodMonths,
                purpose = purpose,
                details = details
            )
        )
    }

    suspend fun deposit(request: DepositRequest): ApiResult<DepositResponse> = safeCall {
        apiService.deposit(request)
    }

    suspend fun depositBetweenAccounts(
        fromAccountId: Int,
        destinationAccountId: Int,
        amount: Double,
        note: String?
    ): ApiResult<TransferMoneyResponse> = safeCall {
        apiService.transfer(
            TransferMoneyRequest(
                fromAccount = fromAccountId,
                transferType = "internal",
                toAccount = destinationAccountId,
                amount = amount,
                note = note
            )
        )
    }

    suspend fun withdraw(request: WithdrawRequest): ApiResult<WithdrawResponse> = safeCall {
        apiService.withdraw(request)
    }

    suspend fun verifyWithdrawal(request: VerifyWithdrawalRequest): ApiResult<WithdrawResponse> = safeCall {
        apiService.verifyWithdrawal(request)
    }

    suspend fun verifyTransferOtp(request: VerifyTransferOtpRequest): ApiResult<TransferMoneyResponse> = safeCall {
        apiService.verifyTransferOtp(request)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (e: HttpException) {
            ApiResult.Error(message = parseHttpError(e, "Request failed"), code = e.code())
        } catch (e: IOException) {
            ApiResult.Error(message = "Network unavailable. Please try again.")
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Unexpected error")
        }
    }
}
