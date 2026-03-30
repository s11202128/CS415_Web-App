package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.FeatureRepository
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.BillHistoryItem
import com.bof.mobile.model.BillPaymentRequest
import com.bof.mobile.model.InterestSummaryItem
import com.bof.mobile.model.InvestmentRequest
import com.bof.mobile.model.InvestmentItem
import com.bof.mobile.model.LoanApplicationItem
import com.bof.mobile.model.LoanApplicationRequest
import com.bof.mobile.model.LoanProductItem
import com.bof.mobile.model.NotificationItem
import com.bof.mobile.model.ProfileResponse
import com.bof.mobile.model.ResetPasswordRequest
import com.bof.mobile.model.ScheduledBillItem
import com.bof.mobile.model.StatementRequestItem
import com.bof.mobile.model.StatementRequestPayload
import com.bof.mobile.model.StatementRowItem
import com.bof.mobile.model.UpdateProfileRequest
import java.time.LocalDate
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
    val statementRequests: List<StatementRequestItem> = emptyList(),
    val statementRows: List<StatementRowItem> = emptyList(),

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
    val investments: List<InvestmentItem> = emptyList()
)

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

    fun onResetEmailChanged(value: String) = _uiState.update { it.copy(resetEmail = value) }
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

    fun loadInitialData(customerId: Int) {
        loadProfile(customerId)
        loadScheduledBills()
        loadBillHistory()
        loadStatementRequests()
        loadNotifications(customerId)
        loadLoanProducts()
        loadLoanApplications()
        loadInterestSummaries()
        loadInvestments()
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

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.payBillManual(
                    BillPaymentRequest(accountId = accountId, payee = state.billPayee, amount = amount)
                )
            ) {
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

        viewModelScope.launch {
            setLoading(true)
            when (
                val result = featureRepository.scheduleBill(
                    BillPaymentRequest(
                        accountId = accountId,
                        payee = state.billPayee,
                        amount = amount,
                        scheduledDate = state.scheduledDate
                    )
                )
            ) {
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

    private fun setLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message, successMessage = null) }
    }
}
