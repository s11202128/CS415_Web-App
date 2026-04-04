package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.FeatureRepository
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.BankStatementResponse
import com.bof.mobile.model.BankStatementTransaction
import com.bof.mobile.model.BillHistoryItem
import com.bof.mobile.model.BillPaymentRequest
import com.bof.mobile.model.ActivityLogItem
import com.bof.mobile.model.InterestSummaryItem
import com.bof.mobile.model.InvestmentRequest
import com.bof.mobile.model.InvestmentItem
import com.bof.mobile.model.LoanApplicationItem
import com.bof.mobile.model.LoanApplicationRequest
import com.bof.mobile.model.LoanProductItem
import com.bof.mobile.model.NotificationItem
import com.bof.mobile.model.ProfileResponse
import com.bof.mobile.model.AccountOverviewReport
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.ReportPoint
import com.bof.mobile.model.ResetPasswordRequest
import com.bof.mobile.model.ScheduledBillItem
import com.bof.mobile.model.StatementRequestItem
import com.bof.mobile.model.StatementRequestPayload
import com.bof.mobile.model.StatementRowItem
import com.bof.mobile.model.UpdateProfileRequest
import com.bof.mobile.model.VerifyTransferOtpRequest
import com.bof.mobile.model.VerifyWithdrawalRequest
import com.bof.mobile.model.WithdrawRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeatureUiState(
    val customerId: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    val profile: ProfileResponse? = null,
    val customerAccounts: List<AccountItem> = emptyList(),
    val fullName: String = "",
    val mobile: String = "",
    val nationalId: String = "",
    val residencyStatus: String = "resident",
    val tin: String = "",

    val billAccountId: String = "",
    val billPayee: String = "",
    val billAmount: String = "",
    val scheduledDate: String = "",
    val scheduledBills: List<ScheduledBillItem> = emptyList(),
    val billHistory: List<BillHistoryItem> = emptyList(),

    val statementAccountId: String = "",
    val statementAccountNumber: String = "",
    val statementFromDate: String = "",
    val statementToDate: String = "",
    val statementFilter: String = StatementFilter.MONTHS.name,
    val statementSelectedYear: Int = LocalDate.now().year,
    val statementSelectedMonth: Int = LocalDate.now().monthValue,
    val statementSelectedDay: Int = LocalDate.now().dayOfMonth,
    val statementBankName: String = "",
    val statementCustomerName: String = "",
    val statementTransactions: List<BankStatementTransaction> = emptyList(),
    val statementRequests: List<StatementRequestItem> = emptyList(),
    val statementRows: List<StatementRowItem> = emptyList(),
    val reportOverview: AccountOverviewReport? = null,
    val reportPoints: List<ReportPoint> = emptyList(),
    val activityLogs: List<ActivityLogItem> = emptyList(),
    val activityFilterType: String = "",
    val activityFromDate: String = "",
    val activityToDate: String = "",

    val notifications: List<NotificationItem> = emptyList(),

    val resetEmail: String = "",
    val resetId: String = "",
    val resetOtp: String = "",
    val newPassword: String = "",

    val loanProductId: String = "",
    val loanRequestedAmount: String = "",
    val loanTermMonths: String = "",
    val loanPurpose: String = "",
    val loanMonthlyIncome: String = "",
    val loanOccupation: String = "",
    val loanProducts: List<LoanProductItem> = emptyList(),
    val loanApplications: List<LoanApplicationItem> = emptyList(),

    val selectedYear: String = LocalDate.now().year.toString(),
    val interestSummaries: List<InterestSummaryItem> = emptyList(),

    val investmentType: String = "",
    val investmentAmount: String = "",
    val investmentExpectedReturn: String = "",
    val investmentMaturityDate: String = "",
    val investments: List<InvestmentItem> = emptyList(),

    val depositFromAccountId: String = "",
    val depositDestinationAccountId: String = "",
    val depositAmount: String = "",
    val depositNote: String = "",
    val depositOtp: String = "",
    val depositTransferId: String? = null,
    val showDepositOtpField: Boolean = false,
    val withdrawAccountId: String = "",
    val withdrawAmount: String = "",
    val withdrawNote: String = "",
    val withdrawOtp: String = "",
    val showWithdrawOtpField: Boolean = false,
    val withdrawalId: String? = null
)

enum class StatementFilter {
    YEARS,
    MONTHS,
    WEEKS,
    DAYS
}

class FeatureViewModel(private val featureRepository: FeatureRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState

    fun initialize(customerId: Int) {
        if (_uiState.value.customerId == customerId) return
        _uiState.update { it.copy(customerId = customerId, resetEmail = it.resetEmail.ifBlank { it.profile?.email ?: "" }) }
        loadInitialData(customerId)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun onFullNameChanged(value: String) = _uiState.update { it.copy(fullName = value) }
    fun onMobileChanged(value: String) = _uiState.update { it.copy(mobile = value) }
    fun onNationalIdChanged(value: String) = _uiState.update { it.copy(nationalId = value) }
    fun onResidencyStatusChanged(value: String) = _uiState.update { it.copy(residencyStatus = value) }
    fun onTinChanged(value: String) = _uiState.update { it.copy(tin = value) }

    fun onBillAccountIdChanged(value: String) = _uiState.update { it.copy(billAccountId = value) }
    fun onBillPayeeChanged(value: String) = _uiState.update { it.copy(billPayee = value) }
    fun onBillAmountChanged(value: String) = _uiState.update { it.copy(billAmount = value) }
    fun onScheduledDateChanged(value: String) = _uiState.update { it.copy(scheduledDate = value) }

    fun onStatementAccountIdChanged(value: String) = _uiState.update { it.copy(statementAccountId = value) }
    fun onStatementAccountNumberChanged(value: String) = _uiState.update { it.copy(statementAccountNumber = value) }
    fun onStatementFromDateChanged(value: String) = _uiState.update { it.copy(statementFromDate = value) }
    fun onStatementToDateChanged(value: String) = _uiState.update { it.copy(statementToDate = value) }
    fun onStatementFilterChanged(value: StatementFilter) {
        _uiState.update { current ->
            val normalizedDay = normalizeDayForMonth(
                year = current.statementSelectedYear,
                month = current.statementSelectedMonth,
                day = current.statementSelectedDay
            )
            val next = current.copy(
                statementFilter = value.name,
                statementSelectedDay = normalizedDay
            )
            val (fromDate, toDate) = resolveStatementRange(value, next)
            next.copy(statementFromDate = fromDate, statementToDate = toDate)
        }
    }

    fun onStatementYearChanged(value: Int) {
        _uiState.update { current ->
            val normalizedDay = normalizeDayForMonth(
                year = value,
                month = current.statementSelectedMonth,
                day = current.statementSelectedDay
            )
            val filter = runCatching { StatementFilter.valueOf(current.statementFilter) }.getOrDefault(StatementFilter.MONTHS)
            val next = current.copy(
                statementSelectedYear = value,
                statementSelectedDay = normalizedDay
            )
            val (fromDate, toDate) = resolveStatementRange(filter, next)
            next.copy(statementFromDate = fromDate, statementToDate = toDate)
        }
    }

    fun onStatementMonthChanged(value: Int) {
        _uiState.update { current ->
            val normalizedDay = normalizeDayForMonth(
                year = current.statementSelectedYear,
                month = value,
                day = current.statementSelectedDay
            )
            val filter = runCatching { StatementFilter.valueOf(current.statementFilter) }.getOrDefault(StatementFilter.MONTHS)
            val next = current.copy(
                statementSelectedMonth = value,
                statementSelectedDay = normalizedDay
            )
            val (fromDate, toDate) = resolveStatementRange(filter, next)
            next.copy(statementFromDate = fromDate, statementToDate = toDate)
        }
    }

    fun onStatementDayChanged(value: Int) {
        _uiState.update { current ->
            val normalizedDay = normalizeDayForMonth(
                year = current.statementSelectedYear,
                month = current.statementSelectedMonth,
                day = value
            )
            val filter = runCatching { StatementFilter.valueOf(current.statementFilter) }.getOrDefault(StatementFilter.MONTHS)
            val next = current.copy(statementSelectedDay = normalizedDay)
            val (fromDate, toDate) = resolveStatementRange(filter, next)
            next.copy(statementFromDate = fromDate, statementToDate = toDate)
        }
    }

    fun onResetEmailChanged(value: String) = _uiState.update { it.copy(resetEmail = value) }
    fun onActivityFilterTypeChanged(value: String) = _uiState.update { it.copy(activityFilterType = value) }
    fun onActivityFromDateChanged(value: String) = _uiState.update { it.copy(activityFromDate = value) }
    fun onActivityToDateChanged(value: String) = _uiState.update { it.copy(activityToDate = value) }
    fun onResetIdChanged(value: String) = _uiState.update { it.copy(resetId = value) }
    fun onResetOtpChanged(value: String) = _uiState.update { it.copy(resetOtp = value) }
    fun onNewPasswordChanged(value: String) = _uiState.update { it.copy(newPassword = value) }

    fun onLoanProductIdChanged(value: String) = _uiState.update { it.copy(loanProductId = value) }
    fun onLoanRequestedAmountChanged(value: String) = _uiState.update { it.copy(loanRequestedAmount = value) }
    fun onLoanTermMonthsChanged(value: String) = _uiState.update { it.copy(loanTermMonths = value) }
    fun onLoanPurposeChanged(value: String) = _uiState.update { it.copy(loanPurpose = value) }
    fun onLoanMonthlyIncomeChanged(value: String) = _uiState.update { it.copy(loanMonthlyIncome = value) }
    fun onLoanOccupationChanged(value: String) = _uiState.update { it.copy(loanOccupation = value) }

    fun onSelectedYearChanged(value: String) = _uiState.update { it.copy(selectedYear = value) }
    fun onInvestmentTypeChanged(value: String) = _uiState.update { it.copy(investmentType = value) }
    fun onInvestmentAmountChanged(value: String) = _uiState.update { it.copy(investmentAmount = value) }
    fun onInvestmentExpectedReturnChanged(value: String) = _uiState.update { it.copy(investmentExpectedReturn = value) }
    fun onInvestmentMaturityDateChanged(value: String) = _uiState.update { it.copy(investmentMaturityDate = value) }

    fun onDepositFromAccountIdChanged(value: String) = _uiState.update { it.copy(depositFromAccountId = value) }
    fun onDepositDestinationAccountIdChanged(value: String) = _uiState.update { it.copy(depositDestinationAccountId = value) }
    fun onDepositAmountChanged(value: String) = _uiState.update { it.copy(depositAmount = value) }
    fun onDepositNoteChanged(value: String) = _uiState.update { it.copy(depositNote = value) }
    fun onDepositOtpChanged(value: String) = _uiState.update { it.copy(depositOtp = value) }
    fun onWithdrawAccountIdChanged(value: String) = _uiState.update { it.copy(withdrawAccountId = value) }
    fun onWithdrawAmountChanged(value: String) = _uiState.update { it.copy(withdrawAmount = value) }
    fun onWithdrawNoteChanged(value: String) = _uiState.update { it.copy(withdrawNote = value) }
    fun onWithdrawOtpChanged(value: String) = _uiState.update { it.copy(withdrawOtp = value) }

    fun loadInitialData(customerId: Int) {
        loadProfile(customerId)
        loadCustomerAccounts()
        loadScheduledBills()
        loadBillHistory()
        loadStatementRequests()
        loadLoanProducts()
        loadLoanApplications()
        loadInterestSummaries()
        loadInvestments()
    }

    fun initializeStatementDateDefaults() {
        val state = _uiState.value
        if (state.statementFromDate.isNotBlank() && state.statementToDate.isNotBlank()) {
            return
        }

        val (defaultFrom, defaultTo) = defaultStatementRange()
        _uiState.update {
            it.copy(
                statementFromDate = defaultFrom,
                statementToDate = defaultTo
            )
        }
    }

    fun loadBankStatement() {
        val state = _uiState.value
        if (state.statementFromDate.isBlank() || state.statementToDate.isBlank()) {
            val (fromDate, toDate) = defaultStatementRange()
            _uiState.update { it.copy(statementFromDate = fromDate, statementToDate = toDate) }
        }

        val fromDate = _uiState.value.statementFromDate
        val toDate = _uiState.value.statementToDate

        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.getBankStatement(fromDate, toDate)) {
                is ApiResult.Success -> updateStatementFromResponse(result.data)
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadCustomerAccounts() {
        viewModelScope.launch {
            when (val result = featureRepository.getAccounts()) {
                is ApiResult.Success -> _uiState.update { it.copy(customerAccounts = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadReport() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.getReport()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            reportOverview = result.data.accountOverview,
                            reportPoints = result.data.points,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadActivityLogs() {
        val state = _uiState.value
        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.getActivityLogs(
                    fromDate = state.activityFromDate.ifBlank { null },
                    toDate = state.activityToDate.ifBlank { null },
                    activityType = state.activityFilterType.ifBlank { null }
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            activityLogs = result.data,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun logClientActivity(activityType: String, description: String, status: String = "success") {
        viewModelScope.launch {
            featureRepository.createActivityLog(
                activityType = activityType,
                description = description,
                status = status
            )
        }
    }

    fun downloadBankStatementPdf(onSuccess: (ByteArray, String) -> Unit) {
        val state = _uiState.value
        val fromDate = state.statementFromDate
        val toDate = state.statementToDate
        if (fromDate.isBlank() || toDate.isBlank()) {
            return setError("Select a filter first")
        }

        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.downloadBankStatementPdf(fromDate, toDate)) {
                is ApiResult.Success -> {
                    val fileName = "bank-statement-${LocalDate.now()}.pdf"
                    onSuccess(result.data, fileName)
                    _uiState.update { it.copy(successMessage = "PDF generated successfully", errorMessage = null) }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadProfile(customerId: Int? = _uiState.value.customerId) {
        val resolvedCustomerId = customerId ?: return
        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.getProfile(resolvedCustomerId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            profile = result.data,
                            fullName = result.data.fullName,
                            mobile = result.data.mobile,
                            nationalId = result.data.nationalId,
                            residencyStatus = result.data.residencyStatus,
                            tin = result.data.tin,
                            resetEmail = result.data.email,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun updateProfile() {
        val state = _uiState.value
        val customerId = state.customerId ?: return setError("Customer not loaded")

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.updateProfile(
                    UpdateProfileRequest(
                        customerId = customerId,
                        fullName = state.fullName,
                        mobile = state.mobile,
                        nationalId = state.nationalId,
                        residencyStatus = state.residencyStatus,
                        tin = state.tin
                    )
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(profile = result.data, successMessage = "Profile updated", errorMessage = null) }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun payBillManual() {
        val state = _uiState.value
        val accountId = state.billAccountId.toIntOrNull()
        val amount = state.billAmount.toDoubleOrNull()
        if (accountId == null || amount == null || state.billPayee.isBlank()) {
            return setError("Bill payment needs accountId, payee and amount")
        }

        payBillManual(
            BillPaymentRequest(
                accountId = accountId,
                payee = state.billPayee,
                amount = amount,
                billType = null,
                paymentMethod = null,
                note = null
            )
        )
    }

    fun payBillManual(request: BillPaymentRequest) {
        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.payBillManual(request.copy(scheduledDate = null))) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = "Bill paid successfully",
                            billHistory = listOf(result.data) + it.billHistory,
                            errorMessage = null
                        )
                    }
                }

                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun scheduleBill() {
        val state = _uiState.value
        val accountId = state.billAccountId.toIntOrNull()
        val amount = state.billAmount.toDoubleOrNull()
        if (accountId == null || amount == null || state.billPayee.isBlank() || state.scheduledDate.isBlank()) {
            return setError("Scheduled bill needs accountId, payee, amount and date")
        }

        scheduleBill(
            BillPaymentRequest(
                accountId = accountId,
                payee = state.billPayee,
                amount = amount,
                scheduledDate = state.scheduledDate
            )
        )
    }

    fun scheduleBill(request: BillPaymentRequest) {
        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.scheduleBill(request)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = "Bill scheduled",
                            scheduledBills = listOf(result.data) + it.scheduledBills,
                            errorMessage = null
                        )
                    }
                }

                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadScheduledBills() {
        viewModelScope.launch {
            when (val result = featureRepository.getScheduledBills()) {
                is ApiResult.Success -> _uiState.update { it.copy(scheduledBills = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadBillHistory() {
        viewModelScope.launch {
            when (val result = featureRepository.getBillHistory()) {
                is ApiResult.Success -> _uiState.update { it.copy(billHistory = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun runScheduledBill(id: Int) {
        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.runScheduledBill(id)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(successMessage = result.data, errorMessage = null) }
                    loadScheduledBills()
                    loadBillHistory()
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun createStatementRequest() {
        val state = _uiState.value
        val customer = state.profile ?: return setError("Load profile before creating statement request")
        val accountId = state.statementAccountId.toIntOrNull()
        if (accountId == null || state.statementFromDate.isBlank() || state.statementToDate.isBlank() || state.statementAccountNumber.isBlank()) {
            return setError("Statement request needs accountId, account number, from date and to date")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.createStatementRequest(
                    StatementRequestPayload(
                        accountId = accountId,
                        accountNumber = state.statementAccountNumber,
                        fullName = customer.fullName,
                        accountHolder = customer.fullName,
                        fromDate = state.statementFromDate,
                        toDate = state.statementToDate
                    )
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = "Statement request submitted",
                            statementRequests = listOf(result.data) + it.statementRequests,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadStatementRequests() {
        viewModelScope.launch {
            when (val result = featureRepository.getStatementRequests()) {
                is ApiResult.Success -> _uiState.update { it.copy(statementRequests = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadStatementByRequest(requestId: Int) {
        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.getStatementByRequest(requestId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statementRows = result.data,
                            successMessage = "Statement loaded",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    private fun updateStatementFromResponse(response: BankStatementResponse) {
        _uiState.update {
            it.copy(
                statementBankName = response.bankName,
                statementCustomerName = response.customerName,
                statementAccountNumber = response.accountNumber,
                statementFromDate = response.dateRange.fromDate.take(10),
                statementToDate = response.dateRange.toDate.take(10),
                statementTransactions = response.transactions,
                successMessage = "Statement loaded",
                errorMessage = null
            )
        }
    }

    private fun resolveStatementRange(filter: StatementFilter, state: FeatureUiState): Pair<String, String> {
        val year = state.statementSelectedYear
        val month = state.statementSelectedMonth
        val day = normalizeDayForMonth(year, month, state.statementSelectedDay)

        val (fromDate, toDate) = when (filter) {
            StatementFilter.DAYS -> {
                val selected = LocalDate.of(year, month, day)
                Pair(selected, selected)
            }
            StatementFilter.WEEKS -> {
                val selected = LocalDate.of(year, month, day)
                val from = selected.minusDays(selected.dayOfWeek.value.toLong() - 1L)
                val to = from.plusDays(6)
                Pair(from, to)
            }
            StatementFilter.MONTHS -> {
                val from = LocalDate.of(year, month, 1)
                val to = from.withDayOfMonth(from.lengthOfMonth())
                Pair(from, to)
            }
            StatementFilter.YEARS -> {
                val from = LocalDate.of(year, 1, 1)
                val to = LocalDate.of(year, 12, 31)
                Pair(from, to)
            }
        }

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return Pair(fromDate.format(formatter), toDate.format(formatter))
    }

    private fun normalizeDayForMonth(year: Int, month: Int, day: Int): Int {
        val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
        return day.coerceIn(1, maxDay)
    }

    private fun defaultStatementRange(): Pair<String, String> {
        val toDate = LocalDate.now()
        val fromDate = toDate.minusMonths(1)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return Pair(fromDate.format(formatter), toDate.format(formatter))
    }

    fun loadNotifications(customerId: Int? = _uiState.value.customerId) {
        val resolvedCustomerId = customerId ?: return
        viewModelScope.launch {
            when (val result = featureRepository.getNotifications(resolvedCustomerId)) {
                is ApiResult.Success -> _uiState.update { it.copy(notifications = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun forgotPassword() {
        val email = _uiState.value.resetEmail
        if (email.isBlank()) {
            return setError("Email is required")
        }

        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.forgotPassword(email)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            resetId = result.data.resetId,
                            successMessage = result.data.message,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.resetEmail.isBlank() || state.resetId.isBlank() || state.resetOtp.isBlank() || state.newPassword.isBlank()) {
            return setError("Email, resetId, OTP and new password are required")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.resetPassword(
                    ResetPasswordRequest(
                        email = state.resetEmail,
                        resetId = state.resetId,
                        otp = state.resetOtp,
                        newPassword = state.newPassword
                    )
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(successMessage = result.data.status, errorMessage = null)
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadLoanProducts() {
        viewModelScope.launch {
            when (val result = featureRepository.getLoanProducts()) {
                is ApiResult.Success -> _uiState.update { it.copy(loanProducts = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun submitLoanApplication() {
        val state = _uiState.value
        val customerId = state.customerId ?: return setError("Customer not loaded")
        val amount = state.loanRequestedAmount.toDoubleOrNull()
        val termMonths = state.loanTermMonths.toIntOrNull()
        val income = state.loanMonthlyIncome.toDoubleOrNull() ?: 0.0

        if (state.loanProductId.isBlank() || amount == null || termMonths == null || state.loanPurpose.isBlank()) {
            return setError("Loan application needs product, amount, term and purpose")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.submitLoanApplication(
                    LoanApplicationRequest(
                        customerId = customerId,
                        loanProductId = state.loanProductId,
                        requestedAmount = amount,
                        termMonths = termMonths,
                        purpose = state.loanPurpose,
                        monthlyIncome = income,
                        occupation = state.loanOccupation.ifBlank { "unknown" }
                    )
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            loanApplications = listOf(result.data) + it.loanApplications,
                            successMessage = "Loan application submitted",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadLoanApplications() {
        viewModelScope.launch {
            when (val result = featureRepository.getLoanApplications()) {
                is ApiResult.Success -> {
                    val customerId = _uiState.value.customerId
                    val filtered = if (customerId == null) result.data else result.data.filter { it.customerId == customerId }
                    _uiState.update { it.copy(loanApplications = filtered, errorMessage = null) }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadInterestSummaries() {
        val year = _uiState.value.selectedYear.toIntOrNull() ?: LocalDate.now().year
        viewModelScope.launch {
            when (val result = featureRepository.getInterestSummaries(year)) {
                is ApiResult.Success -> {
                    val customerId = _uiState.value.customerId
                    val filtered = if (customerId == null) result.data else result.data.filter { it.customerId == customerId }
                    _uiState.update { it.copy(interestSummaries = filtered, errorMessage = null) }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadInvestments(customerId: Int? = _uiState.value.customerId) {
        val resolvedCustomerId = customerId ?: return
        viewModelScope.launch {
            when (val result = featureRepository.getInvestments(resolvedCustomerId)) {
                is ApiResult.Success -> _uiState.update { it.copy(investments = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun createInvestment() {
        val state = _uiState.value
        val customerId = state.customerId ?: return setError("Customer not loaded")
        val amount = state.investmentAmount.toDoubleOrNull()
        val expectedReturn = state.investmentExpectedReturn.toDoubleOrNull()

        if (state.investmentType.isBlank() || amount == null || amount <= 0) {
            return setError("Investment needs type and positive amount")
        }
        if (state.investmentExpectedReturn.isNotBlank() && expectedReturn == null) {
            return setError("Expected return must be a number")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.createInvestment(
                    InvestmentRequest(
                        customerId = customerId,
                        investmentType = state.investmentType,
                        amount = amount,
                        expectedReturn = expectedReturn,
                        maturityDate = state.investmentMaturityDate.ifBlank { null }
                    )
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            investments = listOf(result.data) + it.investments,
                            successMessage = "Investment created",
                            errorMessage = null,
                            investmentAmount = "",
                            investmentExpectedReturn = "",
                            investmentMaturityDate = ""
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun submitFundingInvestment() {
        val state = _uiState.value
        val amount = state.investmentAmount.toDoubleOrNull()
        val durationMonths = state.investmentMaturityDate.toIntOrNull()
        val investmentType = state.investmentType.trim()
        val notes = state.investmentExpectedReturn.trim().ifBlank { null }

        if (investmentType.isBlank()) {
            return setError("Investment type is required")
        }
        if (amount == null || amount <= 0) {
            return setError("Investment amount must be greater than 0")
        }
        if (durationMonths == null || durationMonths <= 0) {
            return setError("Duration (months) must be greater than 0")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.submitFundingInvestment(
                    amount = amount,
                    investmentType = investmentType,
                    durationMonths = durationMonths,
                    notes = notes
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = "Investment request submitted (${result.data.status})",
                            errorMessage = null,
                            investmentAmount = "",
                            investmentExpectedReturn = "",
                            investmentMaturityDate = ""
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun submitFundingLoan() {
        val state = _uiState.value
        val loanAmount = state.loanRequestedAmount.toDoubleOrNull()
        val repaymentPeriod = state.loanTermMonths.toIntOrNull()
        val loanType = state.loanProductId.trim()
        val purpose = state.loanPurpose.trim()
        val details = state.loanOccupation.trim().ifBlank { null }

        if (loanType.isBlank()) {
            return setError("Loan type is required")
        }
        if (loanAmount == null || loanAmount <= 0) {
            return setError("Loan amount must be greater than 0")
        }
        if (repaymentPeriod == null || repaymentPeriod <= 0) {
            return setError("Repayment period must be greater than 0")
        }
        if (purpose.isBlank()) {
            return setError("Loan purpose is required")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.submitFundingLoan(
                    loanAmount = loanAmount,
                    loanType = loanType,
                    repaymentPeriodMonths = repaymentPeriod,
                    purpose = purpose,
                    details = details
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = "Loan request submitted (${result.data.status})",
                            errorMessage = null,
                            loanRequestedAmount = "",
                            loanTermMonths = "",
                            loanPurpose = "",
                            loanOccupation = ""
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun deposit() {
        val state = _uiState.value
        val fromAccountId = state.depositFromAccountId.toIntOrNull()
        val destinationAccountId = state.depositDestinationAccountId.toIntOrNull()
        val amount = state.depositAmount.toDoubleOrNull()

        if (fromAccountId == null || fromAccountId <= 0) {
            return setError("Please select a valid from account")
        }

        if (destinationAccountId == null || destinationAccountId <= 0) {
            return setError("Please select a valid destination account")
        }

        if (fromAccountId == destinationAccountId) {
            return setError("From and destination accounts must be different")
        }

        if (amount == null || amount <= 0) {
            return setError("Amount must be greater than 0")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.depositBetweenAccounts(
                    fromAccountId = fromAccountId,
                    destinationAccountId = destinationAccountId,
                    amount = amount,
                    note = state.depositNote.ifBlank { null }
                )
            ) {
                is ApiResult.Success -> {
                    if (result.data.requiresOtp) {
                        _uiState.update {
                            it.copy(
                                depositTransferId = result.data.transferId,
                                showDepositOtpField = true,
                                successMessage = result.data.message.ifBlank { "OTP verification required" },
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                depositAmount = "",
                                depositNote = "",
                                depositOtp = "",
                                depositTransferId = null,
                                showDepositOtpField = false,
                                successMessage = "Deposit successful - FJD ${String.format("%.2f", amount)}",
                                errorMessage = null
                            )
                        }
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun verifyDepositOtp() {
        val state = _uiState.value
        val transferId = state.depositTransferId
        if (transferId.isNullOrBlank()) {
            return setError("Deposit transfer ID not found")
        }
        if (state.depositOtp.isBlank()) {
            return setError("OTP is required")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.verifyTransferOtp(
                    VerifyTransferOtpRequest(transferId = transferId, otp = state.depositOtp.trim())
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            depositAmount = "",
                            depositNote = "",
                            depositOtp = "",
                            depositTransferId = null,
                            showDepositOtpField = false,
                            successMessage = "Deposit successful${result.data.amount?.let { amt -> " - FJD ${String.format("%.2f", amt)}" } ?: ""}",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun withdraw() {
        val state = _uiState.value
        val accountId = state.withdrawAccountId.toIntOrNull()
        val amount = state.withdrawAmount.toDoubleOrNull()

        if (accountId == null || accountId <= 0) {
            return setError("Please select a valid account")
        }

        if (amount == null || amount <= 0) {
            return setError("Amount must be greater than 0")
        }

        viewModelScope.launch {
            setLoading(true)
            when (val result = featureRepository.withdraw(WithdrawRequest(accountId, amount, state.withdrawNote.ifBlank { null }))) {
                is ApiResult.Success -> {
                    if (result.data.requiresOtp) {
                        // OTP required, show OTP field
                        _uiState.update {
                            it.copy(
                                showWithdrawOtpField = true,
                                withdrawalId = result.data.withdrawalId,
                                successMessage = "OTP verification required",
                                errorMessage = null
                            )
                        }
                    } else {
                        // Direct withdrawal succeeded
                        _uiState.update {
                            it.copy(
                                withdrawAmount = "",
                                withdrawAccountId = "",
                                withdrawNote = "",
                                withdrawOtp = "",
                                showWithdrawOtpField = false,
                                withdrawalId = null,
                                successMessage = "Withdrawal successful - FJD ${String.format("%.2f", amount)}",
                                errorMessage = null
                            )
                        }
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun verifyWithdrawalOtp() {
        val state = _uiState.value
        val withdrawalId = state.withdrawalId

        if (withdrawalId.isNullOrBlank()) {
            return setError("Withdrawal ID not found")
        }

        if (state.withdrawOtp.isBlank()) {
            return setError("OTP is required")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.verifyWithdrawal(
                    VerifyWithdrawalRequest(withdrawalId, state.withdrawOtp)
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            withdrawAmount = "",
                            withdrawAccountId = "",
                            withdrawNote = "",
                            withdrawOtp = "",
                            showWithdrawOtpField = false,
                            withdrawalId = null,
                            successMessage = "Withdrawal verified and completed successfully",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message, successMessage = null) }
    }
}
