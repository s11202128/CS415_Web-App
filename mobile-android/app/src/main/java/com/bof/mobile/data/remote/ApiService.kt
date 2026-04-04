package com.bof.mobile.data.remote

import com.bof.mobile.model.AccountDetailsResponse
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.CreateAccountRequest
import com.bof.mobile.model.DebugAccountsResponse
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
import com.bof.mobile.model.BillHistoryItem
import com.bof.mobile.model.ActivityLogItem
import com.bof.mobile.model.ActivityLogRequest
import com.bof.mobile.model.BillPaymentRequest
import com.bof.mobile.model.BankStatementRequest
import com.bof.mobile.model.BankStatementResponse
import com.bof.mobile.model.BillerItem
import com.bof.mobile.model.DashboardResponse
import com.bof.mobile.model.DepositRequest
import com.bof.mobile.model.DepositResponse
import com.bof.mobile.model.ForgotPasswordRequest
import com.bof.mobile.model.ForgotPasswordResponse
import com.bof.mobile.model.FundingInvestmentRequest
import com.bof.mobile.model.FundingInvestmentResponse
import com.bof.mobile.model.FundingLoanRequest
import com.bof.mobile.model.FundingLoanResponse
import com.bof.mobile.model.InitiateTransferRequest
import com.bof.mobile.model.InitiateTransferResponse
import com.bof.mobile.model.InterestSummaryItem
import com.bof.mobile.model.InvestmentItem
import com.bof.mobile.model.InvestmentRequest
import com.bof.mobile.model.LoginRequest
import com.bof.mobile.model.LoginResponse
import com.bof.mobile.model.LoanApplicationItem
import com.bof.mobile.model.LoanApplicationRequest
import com.bof.mobile.model.LoanProductItem
import com.bof.mobile.model.NotificationItem
import com.bof.mobile.model.PaginatedTransactionsResponse
import com.bof.mobile.model.ProfileResponse
import com.bof.mobile.model.RecipientItem
import com.bof.mobile.model.RegisterRequest
import com.bof.mobile.model.RegisterResponse
import com.bof.mobile.model.ReportResponse
import com.bof.mobile.model.ResetPasswordRequest
import com.bof.mobile.model.ResetPasswordResponse
import com.bof.mobile.model.ScheduledBillItem
import com.bof.mobile.model.StatementRequestItem
import com.bof.mobile.model.StatementRequestPayload
import com.bof.mobile.model.StatementRowItem
import com.bof.mobile.model.TransferMoneyRequest
import com.bof.mobile.model.TransferMoneyResponse
import com.bof.mobile.model.TransactionItem
import com.bof.mobile.model.UpdateProfileRequest
import com.bof.mobile.model.ValidateDestinationRequest
import com.bof.mobile.model.ValidateDestinationResponse
import com.bof.mobile.model.VerifyTransferRequest
import com.bof.mobile.model.VerifyTransferResponse
import com.bof.mobile.model.VerifyWithdrawalRequest
import com.bof.mobile.model.VerifyTransferOtpRequest
import com.bof.mobile.model.WithdrawRequest
import com.bof.mobile.model.WithdrawResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.Streaming
import okhttp3.ResponseBody
import retrofit2.Response

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("dashboard")
    suspend fun getDashboard(@Query("customerId") customerId: Int? = null): DashboardResponse

    @GET("accounts")
    suspend fun getAccounts(): List<AccountItem>

    @GET("debug/my-accounts")
    suspend fun getDebugMyAccounts(): DebugAccountsResponse

    @POST("accounts")
    suspend fun createAccount(@Body request: CreateAccountRequest): AccountItem

    @POST("accounts/request")
    suspend fun requestAccount(@Body request: CreateAccountRequest): AccountItem

    @GET("accounts/{id}/details")
    suspend fun getAccountDetails(@Path("id") accountId: Int, @Query("limit") limit: Int): AccountDetailsResponse

    @GET("transactions")
    suspend fun getTransactions(@Query("accountId") accountId: Int): List<TransactionItem>

    @GET("report")
    suspend fun getReport(): ReportResponse

    @GET("activity")
    suspend fun getActivityLogs(
        @Query("fromDate") fromDate: String?,
        @Query("toDate") toDate: String?,
        @Query("activityType") activityType: String?,
        @Query("limit") limit: Int = 200
    ): List<ActivityLogItem>

    @POST("activity")
    suspend fun createActivityLog(@Body request: ActivityLogRequest): ActivityLogItem

    @GET("transactions")
    suspend fun getTransactionsPaginated(
        @Query("accountId") accountId: Int,
        @Query("paginated") paginated: Boolean,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
        @Query("type") type: String?
    ): PaginatedTransactionsResponse

    @POST("transfers/validate-destination")
    suspend fun validateDestination(@Body request: ValidateDestinationRequest): ValidateDestinationResponse

    @POST("transfers/initiate")
    suspend fun initiateTransfer(@Body request: InitiateTransferRequest): InitiateTransferResponse

    @POST("transactions/transfer")
    suspend fun transfer(@Body request: TransferMoneyRequest): TransferMoneyResponse

    @POST("otp/verify")
    suspend fun verifyTransfer(@Body request: VerifyTransferRequest): VerifyTransferResponse

    @POST("transactions/transfer/verify")
    suspend fun verifyTransferOtp(@Body request: VerifyTransferOtpRequest): TransferMoneyResponse

    @GET("recipients/search")
    suspend fun searchRecipients(@Query("q") query: String): List<RecipientItem>

    @GET("billers")
    suspend fun getBillers(): List<BillerItem>

    @POST("bill-payment")
    suspend fun payBill(@Body request: BillPaymentRequest): BillHistoryItem

    @POST("pay-bill")
    suspend fun payBillManual(@Body request: BillPaymentRequest): BillHistoryItem = payBill(request)

    @POST("bills/scheduled")
    suspend fun scheduleBill(@Body request: BillPaymentRequest): ScheduledBillItem

    @GET("bills/scheduled")
    suspend fun getScheduledBills(): List<ScheduledBillItem>

    @GET("bills/history")
    suspend fun getBillHistory(): List<BillHistoryItem>

    @POST("bills/scheduled/{id}/run")
    suspend fun runScheduledBill(@Path("id") id: Int): Map<String, Any?>

    @POST("statements/request")
    suspend fun createStatementRequest(@Body request: StatementRequestPayload): StatementRequestItem

    @GET("statements/requests")
    suspend fun getStatementRequests(): List<StatementRequestItem>

    @GET("statements/request/{requestId}")
    suspend fun getStatementByRequest(@Path("requestId") requestId: Int): List<StatementRowItem>

    @POST("statement")
    suspend fun getBankStatement(@Body request: BankStatementRequest): BankStatementResponse

    @Streaming
    @POST("statement/download")
    suspend fun downloadBankStatement(@Body request: BankStatementRequest): Response<ResponseBody>

    @GET("notifications/history")
    suspend fun getNotificationsHistory(
        @Query("limit") limit: Int,
        @Query("customerId") customerId: Int?
    ): List<NotificationItem>

    @GET("profile/{customerId}")
    suspend fun getProfile(@Path("customerId") customerId: Int): ProfileResponse

    @PUT("update-profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ProfileResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ForgotPasswordResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ResetPasswordResponse

    @GET("loan-products")
    suspend fun getLoanProducts(): List<LoanProductItem>

    @POST("loan-applications")
    suspend fun submitLoanApplication(@Body request: LoanApplicationRequest): LoanApplicationItem

    @GET("loan-applications")
    suspend fun getLoanApplications(): List<LoanApplicationItem>

    @GET("year-end/interest-summaries")
    suspend fun getInterestSummaries(@Query("year") year: Int): List<InterestSummaryItem>

    @GET("investments")
    suspend fun getInvestments(@Query("customerId") customerId: Int?): List<InvestmentItem>

    @POST("investments")
    suspend fun createInvestment(@Body request: InvestmentRequest): InvestmentItem

    @POST("investment")
    suspend fun submitFundingInvestment(@Body request: FundingInvestmentRequest): FundingInvestmentResponse

    @POST("loan")
    suspend fun submitFundingLoan(@Body request: FundingLoanRequest): FundingLoanResponse

    @GET("admin/customers")
    suspend fun getAdminCustomers(@Query("q") query: String?): List<ProfileResponse>

    @PATCH("admin/customers/{id}")
    suspend fun updateAdminCustomer(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): ProfileResponse

    @POST("admin/create-account")
    suspend fun createAdminAccount(@Body body: AdminCreateAccountRequest): AccountItem

    @PATCH("admin/accounts/{id}")
    suspend fun updateAdminAccount(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): AccountItem

    @PATCH("admin/accounts/{id}/approve")
    suspend fun approveAdminAccount(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): AccountItem

    @PATCH("admin/accounts/{id}/reject")
    suspend fun rejectAdminAccount(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): AccountItem

    @POST("admin/accounts/{id}/freeze")
    suspend fun freezeAdminAccount(@Path("id") id: Int): AccountItem

    @POST("admin/deposits")
    suspend fun createAdminDeposit(@Body body: AdminCreateDepositRequest): AdminCreateDepositResponse

    @PATCH("admin/loan-applications/{id}")
    suspend fun updateAdminLoanApplication(@Path("id") id: Int, @Body body: AdminUpdateLoanRequest): LoanApplicationItem

    @GET("admin/transactions")
    suspend fun getAdminTransactions(@Query("accountNumber") accountNumber: String?): List<AdminTransactionItem>

    @POST("admin/transactions/{id}/reverse")
    suspend fun reverseAdminTransaction(@Path("id") id: Int): Map<String, Any?>

    @GET("admin/login-logs")
    suspend fun getAdminLoginLogs(@Query("limit") limit: Int): List<AdminLoginLogItem>

    @GET("admin/transfers")
    suspend fun getAdminTransfers(): List<AdminTransactionItem>

    @GET("admin/transfer-limit")
    suspend fun getAdminTransferLimit(): AdminTransferLimitResponse

    @PUT("admin/transfer-limit")
    suspend fun updateAdminTransferLimit(@Body body: AdminUpdateTransferLimitRequest): AdminTransferLimitResponse

    @GET("admin/dashboard-report")
    suspend fun getAdminDashboardReport(): AdminDashboardReport

    @GET("admin/statement-requests")
    suspend fun getAdminStatementRequests(): List<StatementRequestItem>

    @PATCH("admin/statement-requests/{id}")
    suspend fun updateAdminStatementRequest(@Path("id") id: Int, @Body body: AdminStatementUpdateRequest): StatementRequestItem

    @GET("admin/notifications/logs")
    suspend fun getAdminNotificationLogs(@Query("limit") limit: Int): List<AdminNotificationLogItem>

    @POST("admin/test-sms")
    suspend fun sendAdminTestSms(@Body body: AdminTestSmsRequest): AdminTestSmsResponse

    @GET("admin/otp-attempts")
    suspend fun getAdminOtpAttempts(@Query("limit") limit: Int): List<AdminOtpAttemptItem>

    @GET("admin/investments")
    suspend fun getAdminInvestments(
        @Query("customerId") customerId: Int?,
        @Query("status") status: String?
    ): List<InvestmentItem>

    @POST("admin/investments")
    suspend fun createAdminInvestment(@Body request: InvestmentRequest): InvestmentItem

    @PATCH("admin/investments/{id}")
    suspend fun updateAdminInvestment(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): InvestmentItem

    @GET("config/interest-rate")
    suspend fun getInterestRateConfig(): Map<String, Double>

    @PUT("config/interest-rate")
    suspend fun updateInterestRateConfig(@Body body: Map<String, @JvmSuppressWildcards Any>): Map<String, Double>

    @POST("year-end/interest-summaries")
    suspend fun generateInterestSummaries(@Body body: Map<String, @JvmSuppressWildcards Any>): List<InterestSummaryItem>

    @POST("transactions/deposit")
    suspend fun deposit(@Body request: DepositRequest): DepositResponse

    @POST("transactions/withdraw")
    suspend fun withdraw(@Body request: WithdrawRequest): WithdrawResponse

    @POST("transactions/verify-withdrawal")
    suspend fun verifyWithdrawal(@Body request: VerifyWithdrawalRequest): WithdrawResponse
}
