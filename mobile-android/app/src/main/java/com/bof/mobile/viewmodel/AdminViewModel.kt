package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.AdminRepository
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.AdminDashboardReport
import com.bof.mobile.model.AdminLoginLogItem
import com.bof.mobile.model.AdminNotificationLogItem
import com.bof.mobile.model.AdminOtpAttemptItem
import com.bof.mobile.model.AdminTransactionItem
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.InterestSummaryItem
import com.bof.mobile.model.InvestmentItem
import com.bof.mobile.model.InvestmentRequest
import com.bof.mobile.model.LoanApplicationItem
import com.bof.mobile.model.ProfileResponse
import com.bof.mobile.model.StatementRequestItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminMenuGroup {
    OVERVIEW,
    OPERATIONS,
    MONITORING,
    CONFIG,
    REPORTING
}

enum class AdminTab(val group: AdminMenuGroup, val label: String) {
    OVERVIEW(AdminMenuGroup.OVERVIEW, "Overview"),

    CUSTOMERS(AdminMenuGroup.OPERATIONS, "Customers"),
    REGISTRATION_REQUESTS(AdminMenuGroup.OPERATIONS, "Registration Requests"),
    ACCOUNTS(AdminMenuGroup.OPERATIONS, "Accounts"),
    DEPOSITS(AdminMenuGroup.OPERATIONS, "Deposits"),
    INVESTMENTS(AdminMenuGroup.OPERATIONS, "Investments"),
    LOAN_REQUESTS(AdminMenuGroup.OPERATIONS, "Loan Requests"),

    TRANSACTIONS(AdminMenuGroup.MONITORING, "Transactions"),
    TRANSFER_HISTORY(AdminMenuGroup.MONITORING, "Transfers"),
    TRANSFER_LIMIT(AdminMenuGroup.MONITORING, "Transfer Limit"),
    LOGIN_LOGS(AdminMenuGroup.MONITORING, "Login Logs"),
    NOTIFICATION_LOGS(AdminMenuGroup.MONITORING, "Notification Logs"),
    OTP_ATTEMPTS(AdminMenuGroup.MONITORING, "OTP Attempts"),
    VERIFICATION(AdminMenuGroup.MONITORING, "Verification"),

    TEST_SMS(AdminMenuGroup.CONFIG, "Test SMS"),
    INTEREST_RATE(AdminMenuGroup.CONFIG, "Interest Rate"),
    INTEREST_SUMMARIES(AdminMenuGroup.CONFIG, "Interest Summaries"),

    REPORTS(AdminMenuGroup.REPORTING, "Reports"),
    STATEMENTS(AdminMenuGroup.REPORTING, "Statements")
}

data class AdminUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    val activeMenu: AdminMenuGroup = AdminMenuGroup.OVERVIEW,
    val activeTab: AdminTab = AdminTab.OVERVIEW,

    val report: AdminDashboardReport? = null,
    val customers: List<ProfileResponse> = emptyList(),
    val accounts: List<AccountItem> = emptyList(),
    val loanApplications: List<LoanApplicationItem> = emptyList(),
    val transactions: List<AdminTransactionItem> = emptyList(),
    val transferHistory: List<AdminTransactionItem> = emptyList(),
    val loginLogs: List<AdminLoginLogItem> = emptyList(),
    val notificationLogs: List<AdminNotificationLogItem> = emptyList(),
    val otpAttempts: List<AdminOtpAttemptItem> = emptyList(),
    val statementRequests: List<StatementRequestItem> = emptyList(),
    val investments: List<InvestmentItem> = emptyList(),

    val accountSearchQuery: String = "",
    val customerSearchQuery: String = "",
    val selectedAccountNumberForTransactions: String = "",

    val createAccountCustomerId: String = "",
    val createAccountCustomerName: String = "",
    val createAccountName: String = "",
    val createAccountPassword: String = "",
    val createAccountType: String = "Simple Access",
    val createAccountPin: String = "Will be auto-generated",
    val createAccountNumber: String = "Will be auto-generated",
    val createAccountOpeningBalance: String = "0",

    val depositAccountId: String = "",
    val depositAmount: String = "",
    val depositDescription: String = "Admin deposit",

    val investmentCustomerId: String = "",
    val investmentType: String = "",
    val investmentAmount: String = "",
    val investmentExpectedReturn: String = "",
    val investmentMaturityDate: String = "",
    val investmentStatusFilter: String = "",

    val transferLimit: String = "10000",

    val interestRate: String = "0.0325",
    val summaryYear: String = "2026",
    val summaries: List<InterestSummaryItem> = emptyList(),

    val testSmsMobile: String = "",
    val testSmsMessage: String = "Test SMS from Bank of Fiji"
    ,

    // Persist selected tab in Accounts section
    val accountSectionTab: String = "Create Account"
)

class AdminViewModel(val repository: AdminRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    fun setAccountSectionTab(tab: String) {
        _uiState.update { it.copy(accountSectionTab = tab) }
    }

    fun setActiveMenu(menu: AdminMenuGroup) {
        _uiState.update { state ->
            val currentTab = state.activeTab
            val targetTab = if (currentTab.group == menu) {
                currentTab
            } else {
                AdminTab.values().first { it.group == menu }
            }
            state.copy(activeMenu = menu, activeTab = targetTab)
        }
    }

    fun setActiveTab(tab: AdminTab) {
        _uiState.update { it.copy(activeMenu = tab.group, activeTab = tab) }
        if (tab == AdminTab.INVESTMENTS) {
            loadInvestments()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun onCustomerSearchChanged(value: String) = _uiState.update { it.copy(customerSearchQuery = value) }
    fun onAccountSearchChanged(value: String) = _uiState.update { it.copy(accountSearchQuery = value) }
    fun onSelectedAccountNumberForTransactionsChanged(value: String) = _uiState.update { it.copy(selectedAccountNumberForTransactions = value) }

    fun onCreateAccountCustomerIdChanged(value: String) = _uiState.update { it.copy(createAccountCustomerId = value) }
    fun onCreateAccountCustomerNameChanged(value: String) = _uiState.update { it.copy(createAccountCustomerName = value) }
    fun onCreateAccountNameChanged(value: String) = _uiState.update { it.copy(createAccountName = value) }
    fun onCreateAccountPasswordChanged(value: String) = _uiState.update { it.copy(createAccountPassword = value) }
    fun onCreateAccountTypeChanged(value: String) = _uiState.update { it.copy(createAccountType = value) }
    fun onCreateAccountOpeningBalanceChanged(value: String) = _uiState.update { it.copy(createAccountOpeningBalance = value) }
    fun onCreateAccountNumberChanged(value: String) = _uiState.update { it.copy(createAccountNumber = value) }

    fun onDepositAccountIdChanged(value: String) = _uiState.update { it.copy(depositAccountId = value) }
    fun onDepositAmountChanged(value: String) = _uiState.update { it.copy(depositAmount = value) }
    fun onDepositDescriptionChanged(value: String) = _uiState.update { it.copy(depositDescription = value) }

    fun onInvestmentCustomerIdChanged(value: String) = _uiState.update { it.copy(investmentCustomerId = value) }
    fun onInvestmentTypeChanged(value: String) = _uiState.update { it.copy(investmentType = value) }
    fun onInvestmentAmountChanged(value: String) = _uiState.update { it.copy(investmentAmount = value) }
    fun onInvestmentExpectedReturnChanged(value: String) = _uiState.update { it.copy(investmentExpectedReturn = value) }
    fun onInvestmentMaturityDateChanged(value: String) = _uiState.update { it.copy(investmentMaturityDate = value) }
    fun onInvestmentStatusFilterChanged(value: String) = _uiState.update { it.copy(investmentStatusFilter = value) }

    fun onTransferLimitChanged(value: String) = _uiState.update { it.copy(transferLimit = value) }

    fun onInterestRateChanged(value: String) = _uiState.update { it.copy(interestRate = value) }
    fun onSummaryYearChanged(value: String) = _uiState.update { it.copy(summaryYear = value) }

    fun onTestSmsMobileChanged(value: String) = _uiState.update { it.copy(testSmsMobile = value) }
    fun onTestSmsMessageChanged(value: String) = _uiState.update { it.copy(testSmsMessage = value) }

    fun loadAllAdminData() {
        loadOverview()
        loadCustomers()
        loadAccounts()
        loadInvestments()
        loadLoans()
        loadMonitoringData()
        loadStatements()
        loadComplianceData()
    }

    fun loadOverview() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.getAdminDashboardReport()) {
                is ApiResult.Success -> _uiState.update { it.copy(report = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.getAdminCustomers(_uiState.value.customerSearchQuery.ifBlank { null })) {
                is ApiResult.Success -> _uiState.update { it.copy(customers = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun updateCustomer(id: Int, body: Map<String, Any?>) {
        viewModelScope.launch {
            when (val result = repository.updateAdminCustomer(id, body)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            customers = state.customers.map { if (it.id == id) result.data else it },
                            successMessage = "Customer updated",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadAccounts() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.getAccounts()) {
                is ApiResult.Success -> _uiState.update { it.copy(accounts = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun createAccount() {
        val state = _uiState.value
        val customerId = state.createAccountCustomerId.toIntOrNull()
        val accountName = state.createAccountName.trim()
        
        if ((customerId == null || customerId <= 0) && state.createAccountCustomerName.isBlank()) {
            return setError("Customer ID or name is required")
        }
        
        if (accountName.isBlank()) {
            return setError("Account name is required")
        }

        viewModelScope.launch {
            setLoading(true)
            val result = repository.createAdminAccount(
                com.bof.mobile.model.AdminCreateAccountRequest(
                    customerId = customerId,
                    customerName = state.createAccountCustomerName.ifBlank { null },
                    type = state.createAccountType,
                    openingBalance = state.createAccountOpeningBalance.toDoubleOrNull() ?: 0.0,
                    accountNumber = if (state.createAccountNumber == "Will be auto-generated") null else state.createAccountNumber.ifBlank { null }
                )
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            accounts = listOf(result.data) + it.accounts,
                            createAccountName = "",
                            createAccountPassword = "",
                            createAccountPin = "Will be auto-generated",
                            createAccountNumber = "Will be auto-generated",
                            createAccountCustomerId = "",
                            createAccountCustomerName = "",
                            createAccountOpeningBalance = "0",
                            successMessage = "Account created successfully. PIN: ${result.data.accountPin}, Account#: ${result.data.accountNumber}",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun updateAccount(id: Int, body: Map<String, Any?>) {
        viewModelScope.launch {
            when (val result = repository.updateAdminAccount(id, body)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            accounts = state.accounts.map { if (it.id == id) result.data else it },
                            successMessage = "Account updated",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun freezeAccount(id: Int) {
        viewModelScope.launch {
            when (val result = repository.freezeAdminAccount(id)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            accounts = state.accounts.map { if (it.id == id) result.data else it },
                            successMessage = "Account frozen",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun approveAccountRequest(id: Int, approvedOpeningBalance: Double) {
        viewModelScope.launch {
            when (val result = repository.approveAdminAccount(id, approvedOpeningBalance)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            accounts = state.accounts.map { if (it.id == id) result.data else it },
                            successMessage = "Account approved",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun rejectAccountRequest(id: Int, rejectionReason: String) {
        if (rejectionReason.isBlank()) {
            return setError("Rejection reason is required")
        }
        viewModelScope.launch {
            when (val result = repository.rejectAdminAccount(id, rejectionReason)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            accounts = state.accounts.map { if (it.id == id) result.data else it },
                            successMessage = "Account rejected",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun createDeposit() {
        val state = _uiState.value
        val accountId = state.depositAccountId.toIntOrNull()
        val amount = state.depositAmount.toDoubleOrNull()
        if (accountId == null || amount == null || amount <= 0) {
            return setError("Valid account ID and amount are required")
        }

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = repository.createAdminDeposit(
                    com.bof.mobile.model.AdminCreateDepositRequest(
                        accountId = accountId,
                        amount = amount,
                        description = state.depositDescription.ifBlank { "Admin deposit" }
                    )
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            accounts = it.accounts.map { account ->
                                if (account.id == result.data.account.id) result.data.account else account
                            },
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

    fun loadInvestments() {
        viewModelScope.launch {
            when (val result = repository.getAdminInvestments(customerId = null, status = null)) {
                is ApiResult.Success -> _uiState.update { it.copy(investments = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun createInvestment() {
        val state = _uiState.value
        val customerId = state.investmentCustomerId.toIntOrNull()
        val amount = state.investmentAmount.toDoubleOrNull()
        val expectedReturn = state.investmentExpectedReturn.toDoubleOrNull()

        if (customerId == null || state.investmentType.isBlank() || amount == null || amount <= 0) {
            return setError("Customer ID, investment type and positive amount are required")
        }
        if (state.investmentExpectedReturn.isNotBlank() && expectedReturn == null) {
            return setError("Expected return must be a number")
        }

        viewModelScope.launch {
            when (
                val result = repository.createAdminInvestment(
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
        }
    }

    fun updateInvestmentStatus(id: Int, status: String) {
        if (status.isBlank()) {
            return setError("Investment status is required")
        }
        viewModelScope.launch {
            when (val result = repository.updateAdminInvestment(id, mapOf("status" to status))) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            investments = state.investments.map { if (it.id == id) result.data else it },
                            successMessage = "Investment updated",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadLoans() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.getLoanApplications()) {
                is ApiResult.Success -> _uiState.update { it.copy(loanApplications = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun updateLoanStatus(id: Int, status: String) {
        viewModelScope.launch {
            when (val result = repository.updateLoanApplication(id, status)) {
                is ApiResult.Success -> {
                    val successText = when {
                        status.equals("approved", ignoreCase = true) -> "Loan request approved successfully"
                        status.equals("rejected", ignoreCase = true) -> "Loan request rejected successfully"
                        else -> "Loan updated successfully"
                    }
                    _uiState.update { state ->
                        state.copy(
                            loanApplications = state.loanApplications.map { if (it.id == id) result.data else it },
                            successMessage = successText,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadMonitoringData() {
        loadAdminTransactions()
        loadAdminTransferHistory()
        loadTransferLimit()
        loadLoginLogs()
        loadNotificationLogs()
        loadOtpAttempts()
    }

    fun loadAdminTransactions() {
        viewModelScope.launch {
            when (val result = repository.getAdminTransactions(_uiState.value.selectedAccountNumberForTransactions.ifBlank { null })) {
                is ApiResult.Success -> _uiState.update { it.copy(transactions = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun reverseTransaction(id: Int) {
        viewModelScope.launch {
            when (val result = repository.reverseTransaction(id)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(successMessage = result.data, errorMessage = null) }
                    loadAdminTransactions()
                    loadOverview()
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadAdminTransferHistory() {
        viewModelScope.launch {
            when (val result = repository.getAdminTransfers()) {
                is ApiResult.Success -> _uiState.update { it.copy(transferHistory = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadTransferLimit() {
        viewModelScope.launch {
            when (val result = repository.getTransferLimit()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        transferLimit = result.data.highValueTransferLimit.toString(),
                        errorMessage = null
                    )
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun updateTransferLimit() {
        val value = _uiState.value.transferLimit.toDoubleOrNull() ?: return setError("Invalid transfer limit")
        viewModelScope.launch {
            when (val result = repository.updateTransferLimit(value)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            transferLimit = result.data.highValueTransferLimit.toString(),
                            successMessage = "Transfer limit updated",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun applyMaintenanceFees() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.applyMaintenanceFees()) {
                is ApiResult.Success -> {
                    val count = result.data.count
                    _uiState.update {
                        it.copy(
                            successMessage = "Maintenance fees applied to $count account" + if (count == 1) "" else "s",
                            errorMessage = null
                        )
                    }
                    loadAccounts()
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadLoginLogs() {
        viewModelScope.launch {
            when (val result = repository.getAdminLoginLogs()) {
                is ApiResult.Success -> _uiState.update { it.copy(loginLogs = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadNotificationLogs() {
        viewModelScope.launch {
            when (val result = repository.getAdminNotificationLogs()) {
                is ApiResult.Success -> _uiState.update { it.copy(notificationLogs = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun sendTestSms() {
        val state = _uiState.value
        if (state.testSmsMobile.isBlank()) {
            return setError("Mobile is required")
        }
        viewModelScope.launch {
            when (val result = repository.sendAdminTestSms(state.testSmsMobile, state.testSmsMessage)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = "SMS sent: ${result.data.status}",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    val message = if (result.message.contains("Twilio is not fully configured", ignoreCase = true)) {
                        "SMS provider is not configured on backend (Twilio). Set TWILIO_* environment values to enable SMS."
                    } else {
                        result.message
                    }
                    setError(message)
                }
            }
        }
    }

    fun loadOtpAttempts() {
        viewModelScope.launch {
            when (val result = repository.getAdminOtpAttempts()) {
                is ApiResult.Success -> _uiState.update { it.copy(otpAttempts = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadStatements() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.getAdminStatementRequests()) {
                is ApiResult.Success -> _uiState.update { it.copy(statementRequests = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun updateStatementRequest(id: Int, status: String) {
        viewModelScope.launch {
            when (val result = repository.updateAdminStatementRequest(id, status, null)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            statementRequests = state.statementRequests.map { if (it.id == id) result.data else it },
                            successMessage = "Statement request updated",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun loadComplianceData() {
        viewModelScope.launch {
            when (val rateResult = repository.getInterestRate()) {
                is ApiResult.Success -> _uiState.update { it.copy(interestRate = rateResult.data.toString(), errorMessage = null) }
                is ApiResult.Error -> setError(rateResult.message)
            }
            loadSummaries()
        }
    }

    fun updateInterestRate() {
        val rate = _uiState.value.interestRate.toDoubleOrNull() ?: return setError("Invalid interest rate")
        viewModelScope.launch {
            when (val result = repository.updateInterestRate(rate)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            interestRate = result.data.toString(),
                            successMessage = "Interest rate updated",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    fun applyMonthlyInterest() {
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.applyMonthlyInterest()) {
                is ApiResult.Success -> {
                    val response = result.data
                    _uiState.update {
                        it.copy(
                            successMessage = "Applied monthly interest to ${response.count} account" +
                                if (response.count == 1) "" else "s" +
                                " (Net FJD ${"%.2f".format(response.totals.netInterest)})",
                            errorMessage = null
                        )
                    }
                    loadAccounts()
                    loadSummaries()
                }
                is ApiResult.Error -> {
                    val message = if (
                        result.message.contains("Cannot POST /api/year-end/apply-monthly-interest", ignoreCase = true) ||
                        result.message.contains("404", ignoreCase = true)
                    ) {
                        "Backend is running an older build. Restart the backend server, then try Apply monthly savings interest again."
                    } else {
                        result.message
                    }
                    setError(message)
                }
            }
            setLoading(false)
        }
    }

    fun generateSummaries() {
        val year = _uiState.value.summaryYear.toIntOrNull() ?: return setError("Invalid year")
        viewModelScope.launch {
            setLoading(true)
            when (val result = repository.generateSummaries(year)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(summaries = result.data, successMessage = "Summaries generated", errorMessage = null) }
                }
                is ApiResult.Error -> setError(result.message)
            }
            setLoading(false)
        }
    }

    fun loadSummaries() {
        val year = _uiState.value.summaryYear.toIntOrNull() ?: return
        viewModelScope.launch {
            when (val result = repository.getSummaries(year)) {
                is ApiResult.Success -> _uiState.update { it.copy(summaries = result.data, errorMessage = null) }
                is ApiResult.Error -> setError(result.message)
            }
        }
    }

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message, successMessage = null) }
    }
}
