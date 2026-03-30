package com.bof.mobile.data.remote

import com.bof.mobile.model.AccountDetailsResponse
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
import com.bof.mobile.model.BillHistoryItem
import com.bof.mobile.model.BillPaymentRequest
import com.bof.mobile.model.BillerItem
import com.bof.mobile.model.DashboardResponse
import com.bof.mobile.model.ForgotPasswordRequest
import com.bof.mobile.model.ForgotPasswordResponse
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
import com.bof.mobile.model.ResetPasswordRequest
import com.bof.mobile.model.ResetPasswordResponse
import com.bof.mobile.model.ScheduledBillItem
import com.bof.mobile.model.StatementRequestItem
import com.bof.mobile.model.StatementRequestPayload
import com.bof.mobile.model.StatementRowItem
import com.bof.mobile.model.TransactionItem
import com.bof.mobile.model.UpdateProfileRequest
import com.bof.mobile.model.ValidateDestinationRequest
import com.bof.mobile.model.ValidateDestinationResponse
import com.bof.mobile.model.VerifyTransferRequest
import com.bof.mobile.model.VerifyTransferResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.Path

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("dashboard")
    suspend fun getDashboard(@Query("customerId") customerId: Int): DashboardResponse

    @GET("accounts")
    suspend fun getAccounts(): List<AccountItem>

    @GET("accounts/{id}/details")
    suspend fun getAccountDetails(@Path("id") accountId: Int, @Query("limit") limit: Int = 20): AccountDetailsResponse

    @GET("transactions")
    suspend fun getTransactions(@Query("accountId") accountId: Int): List<TransactionItem>

    @GET("transactions")
    suspend fun getTransactionsPaginated(
        @Query("accountId") accountId: Int,
        @Query("paginated") paginated: Boolean = true,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("type") type: String? = null
    ): PaginatedTransactionsResponse

    @POST("transfers/validate-destination")
    suspend fun validateDestination(@Body request: ValidateDestinationRequest): ValidateDestinationResponse

    @POST("transfers/initiate")
    suspend fun initiateTransfer(@Body request: InitiateTransferRequest): InitiateTransferResponse

    @POST("otp/verify")
    suspend fun verifyTransfer(@Body request: VerifyTransferRequest): VerifyTransferResponse

    @GET("recipients/search")
    suspend fun searchRecipients(@Query("q") query: String): List<RecipientItem>

    @GET("billers")
    suspend fun getBillers(): List<BillerItem>

    @POST("bills/manual")
    suspend fun payBillManual(@Body request: BillPaymentRequest): BillHistoryItem

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

    @GET("notifications/history")
    suspend fun getNotificationsHistory(
        @Query("limit") limit: Int = 200,
        @Query("customerId") customerId: Int? = null
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
    suspend fun getInvestments(@Query("customerId") customerId: Int? = null): List<InvestmentItem>

    @POST("investments")
    suspend fun createInvestment(@Body request: InvestmentRequest): InvestmentItem

    @GET("admin/customers")
    suspend fun getAdminCustomers(@Query("q") query: String? = null): List<ProfileResponse>

    @PATCH("admin/customers/{id}")
    suspend fun updateAdminCustomer(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): ProfileResponse

    @POST("admin/create-account")
    suspend fun createAdminAccount(@Body body: AdminCreateAccountRequest): AccountItem

    @PATCH("admin/accounts/{id}")
    suspend fun updateAdminAccount(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): AccountItem

    @POST("admin/accounts/{id}/freeze")
    suspend fun freezeAdminAccount(@Path("id") id: Int): AccountItem

    @POST("admin/deposits")
    suspend fun createAdminDeposit(@Body body: AdminCreateDepositRequest): AdminCreateDepositResponse

    @PATCH("admin/loan-applications/{id}")
    suspend fun updateAdminLoanApplication(@Path("id") id: Int, @Body body: AdminUpdateLoanRequest): LoanApplicationItem

    @GET("admin/transactions")
    suspend fun getAdminTransactions(@Query("accountNumber") accountNumber: String? = null): List<AdminTransactionItem>

    @POST("admin/transactions/{id}/reverse")
    suspend fun reverseAdminTransaction(@Path("id") id: Int): Map<String, Any?>

    @GET("admin/login-logs")
    suspend fun getAdminLoginLogs(@Query("limit") limit: Int = 200): List<AdminLoginLogItem>

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
    suspend fun getAdminNotificationLogs(@Query("limit") limit: Int = 200): List<AdminNotificationLogItem>

    @POST("admin/test-sms")
    suspend fun sendAdminTestSms(@Body body: AdminTestSmsRequest): AdminTestSmsResponse

    @GET("admin/otp-attempts")
    suspend fun getAdminOtpAttempts(@Query("limit") limit: Int = 200): List<AdminOtpAttemptItem>

    @GET("admin/investments")
    suspend fun getAdminInvestments(
        @Query("customerId") customerId: Int? = null,
        @Query("status") status: String? = null
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
}
