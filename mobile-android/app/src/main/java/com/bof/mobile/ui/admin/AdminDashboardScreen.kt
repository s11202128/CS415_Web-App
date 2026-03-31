package com.bof.mobile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.viewmodel.AdminMenuGroup
import com.bof.mobile.viewmodel.AdminTab
import com.bof.mobile.viewmodel.AdminUiState
import com.bof.mobile.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAllAdminData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFF5EC),
                        Color(0xFFFFFDF7),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Organized by sections and feature tabs for faster operations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                AdminMessageBanner(
                    text = uiState.errorMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    OutlinedButton(onClick = viewModel::clearMessages) { Text("Clear") }
                }
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                AdminMessageBanner(
                    text = uiState.successMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Section: ${menuTitle(uiState.activeMenu)}")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    AdminMenuGroup.values().forEach { group ->
                        DropdownMenuItem(
                            text = { Text(menuTitle(group)) },
                            onClick = {
                                viewModel.setActiveMenu(group)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminTab.values().filter { it.group == uiState.activeMenu }.forEach { tab ->
                    val isActive = tab == uiState.activeTab
                    if (isActive) {
                        Button(onClick = { viewModel.setActiveTab(tab) }) { Text(tab.label) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setActiveTab(tab) }) { Text(tab.label) }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminTabContent(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

private fun menuTitle(group: AdminMenuGroup): String {
    return when (group) {
        AdminMenuGroup.OVERVIEW -> "Overview"
        AdminMenuGroup.OPERATIONS -> "Operations"
        AdminMenuGroup.MONITORING -> "Monitoring"
        AdminMenuGroup.CONFIG -> "Config"
        AdminMenuGroup.REPORTING -> "Reporting"
    }
}

@Composable
private fun AdminTabContent(uiState: AdminUiState, viewModel: AdminViewModel) {
    when (uiState.activeTab) {
        AdminTab.OVERVIEW -> OverviewTab(uiState = uiState, viewModel = viewModel)
        AdminTab.CUSTOMERS -> CustomersTab(uiState = uiState, viewModel = viewModel)
        AdminTab.ACCOUNTS -> AccountsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.DEPOSITS -> DepositsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.INVESTMENTS -> InvestmentsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.LOANS -> LoansTab(uiState = uiState, viewModel = viewModel)
        AdminTab.TRANSACTIONS -> TransactionsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.TRANSFER_HISTORY -> TransferHistoryTab(uiState = uiState, viewModel = viewModel)
        AdminTab.TRANSFER_LIMIT -> TransferLimitTab(uiState = uiState, viewModel = viewModel)
        AdminTab.LOGIN_LOGS -> LoginLogsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.NOTIFICATION_LOGS -> NotificationLogsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.OTP_ATTEMPTS -> OtpAttemptsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.VERIFICATION -> VerificationTab(uiState = uiState, viewModel = viewModel)
        AdminTab.TEST_SMS -> TestSmsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.INTEREST_RATE -> InterestRateTab(uiState = uiState, viewModel = viewModel)
        AdminTab.INTEREST_SUMMARIES -> InterestSummariesTab(uiState = uiState, viewModel = viewModel)
        AdminTab.REPORTS -> ReportsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.STATEMENTS -> StatementsTab(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun VerificationTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Account Verification Monitoring", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        uiState.customers.forEach { customer ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("#${customer.id} ${customer.fullName} (${customer.status})", fontWeight = FontWeight.Bold)
                    Text("Email: ${customer.email}")
                    Text("Mobile: ${customer.mobile}")
                    Text("Email Verified: ${customer.emailVerified}")
                    Text("ID Verified: ${customer.identityVerified}")
                    Text("KYC Status: ${customer.registrationStatus}")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.updateCustomer(customer.id, mapOf("emailVerified" to true)) },
                            enabled = !customer.emailVerified,
                            modifier = Modifier.weight(1f)
                        ) { Text("Mark Email Verified") }
                        OutlinedButton(
                            onClick = { viewModel.updateCustomer(customer.id, mapOf("emailVerified" to false)) },
                            enabled = customer.emailVerified,
                            modifier = Modifier.weight(1f)
                        ) { Text("Unverify Email") }
                        OutlinedButton(
                            onClick = { viewModel.updateCustomer(customer.id, mapOf("identityVerified" to true)) },
                            enabled = !customer.identityVerified,
                            modifier = Modifier.weight(1f)
                        ) { Text("Verify ID") }
                        OutlinedButton(
                            onClick = { viewModel.updateCustomer(customer.id, mapOf("identityVerified" to false)) },
                            enabled = customer.identityVerified,
                            modifier = Modifier.weight(1f)
                        ) { Text("Unverify ID") }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Overview", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadOverview, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh report")
    }

    val report = uiState.report
    if (report != null) {
        Text("Customers: ${report.metrics.totalCustomers}")
        Text("Accounts: ${report.metrics.totalAccounts}")
        Text("Total deposits: FJD ${"%.2f".format(report.metrics.totalDeposits)}")
        Text("Pending loans: ${report.metrics.pendingLoans}")
        Text("Frozen accounts: ${report.metrics.frozenAccounts}")
        Text("Today's transactions: ${report.metrics.todaysTransactions}")

        Spacer(modifier = Modifier.height(8.dp))
        Text("Recent transactions", style = MaterialTheme.typography.titleMedium)
        report.recentTransactions.take(6).forEach {
            Text("#${it.id} ${it.kind} FJD ${"%.2f".format(it.amount)} ${it.status}")
        }
    }
}

@Composable
private fun CustomersTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Customers", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = uiState.customerSearchQuery,
        onValueChange = viewModel::onCustomerSearchChanged,
        label = { Text("Search by name/email/mobile") },
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::loadCustomers, modifier = Modifier.weight(1f)) { Text("Search") }
        OutlinedButton(
            onClick = { viewModel.onCustomerSearchChanged(""); viewModel.loadCustomers() },
            modifier = Modifier.weight(1f)
        ) { Text("Clear") }
    }

    // Table container with horizontal scroll and sticky header
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState)
            .padding(bottom = 8.dp)
    ) {
        // Sticky header row
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(vertical = 8.dp),
        ) {
            Text("ID", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
            Text("Name", modifier = Modifier.width(180.dp), fontWeight = FontWeight.Bold)
            Text("Status", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
            Text("Email", modifier = Modifier.width(200.dp), fontWeight = FontWeight.Bold)
            Text("Mobile", modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold)
            Text("Actions", modifier = Modifier.width(320.dp), fontWeight = FontWeight.Bold)
        }
        // Data rows
        uiState.customers.take(12).forEach { customer ->
            Row(
                modifier = Modifier
                    .background(if (customer.id % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#${customer.id}", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                Text(customer.fullName, modifier = Modifier.width(180.dp), style = MaterialTheme.typography.bodyMedium)
                Text(customer.status, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium)
                Text(customer.email, modifier = Modifier.width(200.dp), style = MaterialTheme.typography.bodyMedium)
                Text(customer.mobile, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.width(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.updateCustomer(customer.id, mapOf("status" to "active")) },
                        modifier = Modifier.height(36.dp),
                        enabled = customer.status != "active"
                    ) { Text("Activate") }
                    OutlinedButton(
                        onClick = { viewModel.updateCustomer(customer.id, mapOf("status" to "blocked")) },
                        modifier = Modifier.height(36.dp),
                        enabled = customer.status != "blocked"
                    ) { Text("Block") }
                    OutlinedButton(
                        onClick = { viewModel.updateCustomer(customer.id, mapOf("identityVerified" to true)) },
                        modifier = Modifier.height(36.dp),
                        enabled = !customer.identityVerified
                    ) { Text("Verify ID") }
                    OutlinedButton(
                        onClick = { viewModel.updateCustomer(customer.id, mapOf("registrationStatus" to "approved")) },
                        modifier = Modifier.height(36.dp),
                        enabled = customer.registrationStatus != "approved"
                    ) { Text("Approve KYC") }
                }
            }
        }
    }
}

@Composable
private fun AccountsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Accounts", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadAccounts, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh accounts")
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Create account", style = MaterialTheme.typography.titleMedium)
    Text(
        "Customer must already exist. Use exact existing customer name from the Customers tab.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = uiState.createAccountCustomerName,
        onValueChange = viewModel::onCreateAccountCustomerNameChanged,
        label = { Text("Existing customer name") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.createAccountType,
        onValueChange = viewModel::onCreateAccountTypeChanged,
        label = { Text("Type") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.createAccountOpeningBalance,
        onValueChange = viewModel::onCreateAccountOpeningBalanceChanged,
        label = { Text("Opening balance") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.createAccountNumber,
        onValueChange = viewModel::onCreateAccountNumberChanged,
        label = { Text("Custom account number (optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = viewModel::createAccount, modifier = Modifier.fillMaxWidth()) {
        Text("Create account")
    }

    uiState.accounts.take(10).forEach { account ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("#${account.id} ${account.accountNumber} ${account.type} ${account.status}")
        Text("${account.accountHolder} | FJD ${"%.2f".format(account.balance)}")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.updateAccount(account.id, mapOf("status" to "active")) },
                modifier = Modifier.weight(1f)
            ) { Text("Activate") }
            OutlinedButton(
                onClick = { viewModel.freezeAccount(account.id) },
                modifier = Modifier.weight(1f)
            ) { Text("Freeze") }
        }
    }
}

@Composable
private fun DepositsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Deposits", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = uiState.depositAccountId,
        onValueChange = viewModel::onDepositAccountIdChanged,
        label = { Text("Account ID") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.depositAmount,
        onValueChange = viewModel::onDepositAmountChanged,
        label = { Text("Amount") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.depositDescription,
        onValueChange = viewModel::onDepositDescriptionChanged,
        label = { Text("Description") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = viewModel::createDeposit, modifier = Modifier.fillMaxWidth()) {
        Text("Submit deposit")
    }
}

@Composable
private fun InvestmentsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Investments", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = uiState.investmentCustomerId,
        onValueChange = viewModel::onInvestmentCustomerIdChanged,
        label = { Text("Customer ID") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentStatusFilter,
        onValueChange = viewModel::onInvestmentStatusFilterChanged,
        label = { Text("Status filter (optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::loadInvestments, modifier = Modifier.weight(1f)) { Text("Load") }
        OutlinedButton(onClick = {
            viewModel.onInvestmentCustomerIdChanged("")
            viewModel.onInvestmentStatusFilterChanged("")
            viewModel.loadInvestments()
        }, modifier = Modifier.weight(1f)) { Text("Clear") }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Create investment", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = uiState.investmentType,
        onValueChange = viewModel::onInvestmentTypeChanged,
        label = { Text("Investment type") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentAmount,
        onValueChange = viewModel::onInvestmentAmountChanged,
        label = { Text("Amount") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentExpectedReturn,
        onValueChange = viewModel::onInvestmentExpectedReturnChanged,
        label = { Text("Expected return (optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentMaturityDate,
        onValueChange = viewModel::onInvestmentMaturityDateChanged,
        label = { Text("Maturity date YYYY-MM-DD (optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = viewModel::createInvestment, modifier = Modifier.fillMaxWidth()) {
        Text("Create investment")
    }

    uiState.investments.take(12).forEach { item ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("#${item.id} ${item.customerName} ${item.investmentType}")
        Text("FJD ${"%.2f".format(item.amount)} | ${item.status}")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.updateInvestmentStatus(item.id, "active") },
                modifier = Modifier.weight(1f)
            ) { Text("Set Active") }
            OutlinedButton(
                onClick = { viewModel.updateInvestmentStatus(item.id, "matured") },
                modifier = Modifier.weight(1f)
            ) { Text("Set Matured") }
        }
    }
}

@Composable
private fun LoansTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Loan Applications", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadLoans, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh loans")
    }

    uiState.loanApplications.take(12).forEach { loan ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("#${loan.id} ${loan.loanProductId} ${loan.status}")
        Text("Customer ${loan.customerId} | FJD ${"%.2f".format(loan.requestedAmount)}")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.updateLoanStatus(loan.id, "approved") },
                modifier = Modifier.weight(1f)
            ) { Text("Approve") }
            OutlinedButton(
                onClick = { viewModel.updateLoanStatus(loan.id, "rejected") },
                modifier = Modifier.weight(1f)
            ) { Text("Reject") }
        }
    }
}

@Composable
private fun TransactionsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Admin Transactions", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = uiState.selectedAccountNumberForTransactions,
        onValueChange = viewModel::onSelectedAccountNumberForTransactionsChanged,
        label = { Text("Account number filter") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = viewModel::loadAdminTransactions, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh transactions")
    }

    uiState.transactions.take(10).forEach { tx ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("#${tx.id} ${tx.accountNumber} ${tx.kind}")
        Text("FJD ${"%.2f".format(tx.amount)} | ${tx.status}")
        if (tx.status.equals("completed", ignoreCase = true)) {
            OutlinedButton(onClick = { viewModel.reverseTransaction(tx.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Reverse #${tx.id}")
            }
        }
    }
}

@Composable
private fun TransferHistoryTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Transfer History", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadAdminTransferHistory, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh transfers")
    }

    uiState.transferHistory.take(12).forEach { tx ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("#${tx.id} ${tx.accountNumber}")
        Text("${tx.kind} | FJD ${"%.2f".format(tx.amount)} | ${tx.status}")
    }
}

@Composable
private fun TransferLimitTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Transfer Limit", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadTransferLimit, modifier = Modifier.fillMaxWidth()) {
        Text("Load current limit")
    }
    OutlinedTextField(
        value = uiState.transferLimit,
        onValueChange = viewModel::onTransferLimitChanged,
        label = { Text("High-value transfer limit") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = viewModel::updateTransferLimit, modifier = Modifier.fillMaxWidth()) {
        Text("Update transfer limit")
    }
}

@Composable
private fun LoginLogsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Login Logs", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadLoginLogs, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh login logs")
    }

    uiState.loginLogs.take(12).forEach { log ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("${log.email} | success=${log.success}")
        Text(log.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotificationLogsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Notification Logs", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadNotificationLogs, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh notification logs")
    }

    uiState.notificationLogs.take(12).forEach { item ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("${item.phoneNumber} | ${item.deliveryStatus}")
        Text(item.notificationType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OtpAttemptsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("OTP Attempts", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadOtpAttempts, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh OTP attempts")
    }

    uiState.otpAttempts.take(12).forEach { otp ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("${otp.referenceCode} | verified=${otp.verified}")
        Text("Attempts ${otp.attempts}/${otp.maxAttempts}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TestSmsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Test SMS", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = uiState.testSmsMobile,
        onValueChange = viewModel::onTestSmsMobileChanged,
        label = { Text("Mobile") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.testSmsMessage,
        onValueChange = viewModel::onTestSmsMessageChanged,
        label = { Text("Message") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = viewModel::sendTestSms, modifier = Modifier.fillMaxWidth()) {
        Text("Send test SMS")
    }
}

@Composable
private fun InterestRateTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Interest Rate", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadComplianceData, modifier = Modifier.fillMaxWidth()) {
        Text("Load config")
    }
    OutlinedTextField(
        value = uiState.interestRate,
        onValueChange = viewModel::onInterestRateChanged,
        label = { Text("Reserve bank min savings interest rate") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = viewModel::updateInterestRate, modifier = Modifier.fillMaxWidth()) {
        Text("Update interest rate")
    }
}

@Composable
private fun InterestSummariesTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Interest Summaries", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = uiState.summaryYear,
        onValueChange = viewModel::onSummaryYearChanged,
        label = { Text("Year") },
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::generateSummaries, modifier = Modifier.weight(1f)) {
            Text("Generate")
        }
        OutlinedButton(onClick = viewModel::loadSummaries, modifier = Modifier.weight(1f)) {
            Text("Load")
        }
    }

    uiState.summaries.take(20).forEach { summary ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("${summary.customerName} | account ${summary.accountId}")
        Text(
            "Gross FJD ${"%.2f".format(summary.grossInterest)} | Net FJD ${"%.2f".format(summary.netInterest)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ReportsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Reports", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadOverview, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh report")
    }

    val report = uiState.report
    if (report != null) {
        Text("Transactions by day", style = MaterialTheme.typography.titleMedium)
        report.transactionsByDay.forEach { row ->
            Text("${row.day} count ${row.count} total FJD ${"%.2f".format(row.totalAmount)}")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Account type breakdown", style = MaterialTheme.typography.titleMedium)
        report.accountTypeBreakdown.forEach { row ->
            Text("${row.label}: ${row.value}")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Loan status breakdown", style = MaterialTheme.typography.titleMedium)
        report.loanStatusBreakdown.forEach { row ->
            Text("${row.label}: ${row.value}")
        }
    }
}

@Composable
private fun StatementsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Text("Statement Requests", style = MaterialTheme.typography.titleLarge)
    OutlinedButton(onClick = viewModel::loadStatements, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh requests")
    }

    uiState.statementRequests.take(12).forEach { request ->
        Spacer(modifier = Modifier.height(8.dp))
        Text("#${request.id} ${request.accountNumber} ${request.status}")
        Text("${request.fullName} ${request.fromDate} to ${request.toDate}")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.updateStatementRequest(request.id, "approved") },
                modifier = Modifier.weight(1f)
            ) { Text("Approve") }
            OutlinedButton(
                onClick = { viewModel.updateStatementRequest(request.id, "rejected") },
                modifier = Modifier.weight(1f)
            ) { Text("Reject") }
        }
    }
}

@Composable
private fun AdminMessageBanner(
    text: String,
    containerColor: Color,
    textColor: Color,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}
