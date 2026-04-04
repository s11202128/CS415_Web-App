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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.clickable
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.AdminMenuGroup
import com.bof.mobile.viewmodel.AdminTab
import com.bof.mobile.viewmodel.AdminUiState
import com.bof.mobile.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel, canGoBack: Boolean, onBack: () -> Unit, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAllAdminData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(
                title = "Admin Dashboard",
                subtitle = "Organized by sections and feature tabs for faster operations.",
                onBack = onBack,
                enabled = canGoBack
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onLogout) { Text("Logout") }
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
    AdminSectionSurface(
        title = "Account Verification Monitoring",
        subtitle = "Review verification flags and update customer compliance status."
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("Name", 180.dp),
                    AdminTableColumn("Email", 220.dp),
                    AdminTableColumn("Mobile", 140.dp),
                    AdminTableColumn("Email Verified", 120.dp),
                    AdminTableColumn("ID Verified", 110.dp),
                    AdminTableColumn("KYC Status", 130.dp),
                    AdminTableColumn("Actions", 320.dp)
                )
            )

            if (uiState.customers.isEmpty()) {
                AdminEmptyTableRow("No customers available for verification review.")
            } else {
                uiState.customers.forEachIndexed { index, customer ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${customer.id}", 70.dp)
                        AdminTableCell(customer.fullName, 180.dp, bold = true)
                        AdminTableCell(customer.email, 220.dp)
                        AdminTableCell(customer.mobile, 140.dp)
                        AdminStatusChip(if (customer.emailVerified) "verified" else "pending", 120.dp)
                        AdminStatusChip(if (customer.identityVerified) "verified" else "pending", 110.dp)
                        AdminStatusChip(customer.registrationStatus, 130.dp)
                        Row(
                            modifier = Modifier.width(320.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateCustomer(customer.id, mapOf("emailVerified" to true)) },
                                enabled = !customer.emailVerified,
                                modifier = Modifier.height(36.dp)
                            ) { Text("Mark Email Verified") }
                            OutlinedButton(
                                onClick = { viewModel.updateCustomer(customer.id, mapOf("emailVerified" to false)) },
                                enabled = customer.emailVerified,
                                modifier = Modifier.height(36.dp)
                            ) { Text("Unverify Email") }
                            OutlinedButton(
                                onClick = { viewModel.updateCustomer(customer.id, mapOf("identityVerified" to true)) },
                                enabled = !customer.identityVerified,
                                modifier = Modifier.height(36.dp)
                            ) { Text("Verify ID") }
                            OutlinedButton(
                                onClick = { viewModel.updateCustomer(customer.id, mapOf("identityVerified" to false)) },
                                enabled = customer.identityVerified,
                                modifier = Modifier.height(36.dp)
                            ) { Text("Unverify ID") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF1976D2),
                                Color(0xFF5E35B1)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Admin Overview",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monitor approvals, balances, and banking activity from one place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )

                    val totalAccounts = uiState.accounts.size
                    val pendingAccounts = uiState.accounts.count { it.status.equals("pending_approval", ignoreCase = true) }
                    val activeAccounts = uiState.accounts.count { it.status.equals("active", ignoreCase = true) }
                    val totalBalance = uiState.accounts.sumOf { it.balance }

                    AdminMetricCard(label = "Accounts", value = totalAccounts.toString())
                    AdminMetricCard(label = "Pending approvals", value = pendingAccounts.toString())
                    AdminMetricCard(label = "Active accounts", value = activeAccounts.toString())
                    AdminMetricCard(label = "Total Balance", value = "FJD ${"%.2f".format(totalBalance)}")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Banking Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = viewModel::loadOverview) {
                        Text("Refresh report")
                    }
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
                } else {
                    Text(
                        text = "No dashboard report is loaded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomersTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Customers",
        subtitle = "Search and update customer verification states."
    ) {
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

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 80.dp),
                    AdminTableColumn("Name", 180.dp),
                    AdminTableColumn("Status", 110.dp),
                    AdminTableColumn("Email", 200.dp),
                    AdminTableColumn("Mobile", 140.dp),
                    AdminTableColumn("Actions", 320.dp)
                )
            )
            if (uiState.customers.isEmpty()) {
                AdminEmptyTableRow("No customers available.")
            } else {
                uiState.customers.take(12).forEachIndexed { index, customer ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${customer.id}", 80.dp)
                        AdminTableCell(customer.fullName, 180.dp, bold = true)
                        AdminStatusChip(customer.status, 110.dp)
                        AdminTableCell(customer.email, 200.dp)
                        AdminTableCell(customer.mobile, 140.dp)
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
    }
}

@Composable
private fun AccountsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Accounts", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = viewModel::loadAccounts) {
                Text("Refresh accounts")
            }
        }

        Text(
            "Openings and approvals are managed in Overview. This tab is for account-level operations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Create account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Customer must already exist. Use Customer ID from the Customers tab.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        var createAccountTypeExpanded by remember { mutableStateOf(false) }
        var showCreateAccountPassword by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = uiState.createAccountCustomerId,
            onValueChange = viewModel::onCreateAccountCustomerIdChanged,
            label = { Text("Existing customer ID") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.createAccountCustomerName,
            onValueChange = viewModel::onCreateAccountCustomerNameChanged,
            label = { Text("Existing customer name (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Account Name", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.createAccountName,
            onValueChange = viewModel::onCreateAccountNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Account name") },
            placeholder = { Text("e.g. Admin Operating Account") },
            singleLine = true
        )

        Text("Type of Account", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedTextField(
                value = uiState.createAccountType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select account type") },
                trailingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.clickable { createAccountTypeExpanded = true }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { createAccountTypeExpanded = true }
            )

            DropdownMenu(
                expanded = createAccountTypeExpanded,
                onDismissRequest = { createAccountTypeExpanded = false }
            ) {
                listOf("Simple Access", "Savings", "Current").forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            viewModel.onCreateAccountTypeChanged(type)
                            createAccountTypeExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.createAccountPassword,
            onValueChange = viewModel::onCreateAccountPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (showCreateAccountPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showCreateAccountPassword = !showCreateAccountPassword }) {
                    Icon(
                        imageVector = if (showCreateAccountPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        OutlinedTextField(
            value = uiState.createAccountPin,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PIN Number") },
            readOnly = true,
            enabled = false
        )

        OutlinedTextField(
            value = uiState.createAccountNumber,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Account Number") },
            readOnly = true,
            enabled = false
        )

        if (!uiState.successMessage.isNullOrBlank()) {
            Text(
                text = uiState.successMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(onClick = viewModel::createAccount, modifier = Modifier.fillMaxWidth()) {
            Text("Create account")
        }

        Text("Account Summaries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        val summaryScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(summaryScrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("Account Number", 170.dp),
                    AdminTableColumn("Account Holder", 150.dp),
                    AdminTableColumn("PIN", 80.dp),
                    AdminTableColumn("Type", 140.dp),
                    AdminTableColumn("Status", 130.dp),
                    AdminTableColumn("Balance", 120.dp),
                    AdminTableColumn("Requested", 120.dp),
                    AdminTableColumn("Approved", 120.dp),
                    AdminTableColumn("Charge", 120.dp),
                    AdminTableColumn("Approved At", 180.dp),
                    AdminTableColumn("Rejection Reason", 220.dp),
                    AdminTableColumn("Actions", 260.dp)
                )
            )

            if (uiState.accounts.isEmpty()) {
                AdminEmptyTableRow("No account summaries available yet.")
            } else {
                uiState.accounts.forEachIndexed { index, account ->
                    var approveAmount by remember(account.id, account.requestedOpeningBalance) {
                        mutableStateOf(
                            "%.2f".format(
                                account.requestedOpeningBalance ?: account.balance
                            )
                        )
                    }
                    var rejectionReason by remember(account.id) { mutableStateOf(account.rejectionReason ?: "") }
                    AdminTableDataRow(index) {
                        AdminTableCell(account.id.toString(), 70.dp)
                        AdminTableCell(account.accountNumber, 170.dp, bold = true)
                        AdminTableCell(account.accountHolder, 150.dp)
                        AdminTableCell(account.accountPin ?: "----", 80.dp)
                        AdminTableCell(account.type, 140.dp)
                        AdminStatusChip(account.status, 130.dp)
                        AdminTableCell("FJD ${"%.2f".format(account.balance)}", 120.dp)
                        AdminTableCell(
                            account.requestedOpeningBalance?.let { "FJD ${"%.2f".format(it)}" } ?: "-",
                            120.dp
                        )
                        AdminTableCell(
                            account.approvedOpeningBalance?.let { "FJD ${"%.2f".format(it)}" } ?: "-",
                            120.dp
                        )
                        AdminTableCell("FJD ${"%.2f".format(account.maintenanceFee)}", 120.dp)
                        AdminTableCell(account.approvedAt ?: "-", 180.dp)
                        AdminTableCell(account.rejectionReason ?: "-", 220.dp)

                        Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (account.status == "pending_approval") {
                                OutlinedTextField(
                                    value = approveAmount,
                                    onValueChange = { approveAmount = it },
                                    label = { Text("Approve amount") },
                                    modifier = Modifier.width(250.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val parsed = approveAmount.toDoubleOrNull()
                                            if (parsed != null && parsed >= 0) {
                                                viewModel.approveAccountRequest(account.id, parsed)
                                            }
                                        }
                                    ) { Text("Approve") }
                                    OutlinedButton(
                                        onClick = {
                                            val reason = rejectionReason.ifBlank { "Rejected by admin" }
                                            viewModel.rejectAccountRequest(account.id, reason)
                                        }
                                    ) { Text("Reject") }
                                }
                                OutlinedTextField(
                                    value = rejectionReason,
                                    onValueChange = { rejectionReason = it },
                                    label = { Text("Rejection reason") },
                                    modifier = Modifier.width(250.dp)
                                )
                            } else {
                                Text("Managed", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.updateAccount(account.id, mapOf("status" to "active")) }
                                    ) { Text("Activate") }
                                    OutlinedButton(onClick = { viewModel.freezeAccount(account.id) }) { Text("Freeze") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DepositsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Deposit Approvals",
        subtitle = "Approve or reject pending opening deposit requests.",
        actionContent = { OutlinedButton(onClick = viewModel::loadAccounts) { Text("Refresh deposits") } }
    ) {
        val pendingDepositRequests = uiState.accounts
            .filter {
                it.status.equals("pending_approval", ignoreCase = true) ||
                    (it.requestedOpeningBalance != null && !it.status.equals("rejected", ignoreCase = true))
            }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("Account Number", 170.dp),
                    AdminTableColumn("Account Holder", 170.dp),
                    AdminTableColumn("Requested Deposit", 150.dp),
                    AdminTableColumn("Status", 130.dp),
                    AdminTableColumn("Actions", 280.dp)
                )
            )

            if (pendingDepositRequests.isEmpty()) {
                AdminEmptyTableRow("No pending deposit requests available.")
            } else {
                pendingDepositRequests.forEachIndexed { index, account ->
                    var approveAmount by remember(account.id, account.requestedOpeningBalance) {
                        mutableStateOf("%.2f".format(account.requestedOpeningBalance ?: account.balance))
                    }
                    var rejectionReason by remember(account.id) { mutableStateOf(account.rejectionReason ?: "") }

                    AdminTableDataRow(index) {
                        AdminTableCell(account.id.toString(), 70.dp)
                        AdminTableCell(account.accountNumber, 170.dp, bold = true)
                        AdminTableCell(account.accountHolder, 170.dp)
                        AdminTableCell(
                            account.requestedOpeningBalance?.let { "FJD ${"%.2f".format(it)}" } ?: "-",
                            150.dp
                        )
                        AdminStatusChip(account.status, 130.dp)
                        Column(
                            modifier = Modifier.width(280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = approveAmount,
                                onValueChange = { approveAmount = it },
                                label = { Text("Approved amount") },
                                modifier = Modifier.width(270.dp)
                            )
                            OutlinedTextField(
                                value = rejectionReason,
                                onValueChange = { rejectionReason = it },
                                label = { Text("Rejection reason") },
                                modifier = Modifier.width(270.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val parsed = approveAmount.toDoubleOrNull()
                                        if (parsed != null && parsed >= 0) {
                                            viewModel.approveAccountRequest(account.id, parsed)
                                        }
                                    },
                                    enabled = !account.status.equals("approved", ignoreCase = true)
                                ) { Text("Approve") }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.rejectAccountRequest(
                                            account.id,
                                            rejectionReason.ifBlank { "Rejected by admin" }
                                        )
                                    },
                                    enabled = !account.status.equals("rejected", ignoreCase = true)
                                ) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Investment Approvals",
        subtitle = "Review investment requests and approve or reject them.",
        actionContent = { OutlinedButton(onClick = viewModel::loadInvestments) { Text("Refresh investments") } }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("Customer", 180.dp),
                    AdminTableColumn("Type", 150.dp),
                    AdminTableColumn("Amount", 130.dp),
                    AdminTableColumn("Expected Return", 150.dp),
                    AdminTableColumn("Status", 120.dp),
                    AdminTableColumn("Actions", 220.dp)
                )
            )

            if (uiState.investments.isEmpty()) {
                AdminEmptyTableRow("No investment requests available.")
            } else {
                uiState.investments.take(30).forEachIndexed { index, item ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${item.id}", 70.dp)
                        AdminTableCell(item.customerName, 180.dp, bold = true)
                        AdminTableCell(item.investmentType, 150.dp)
                        AdminTableCell("FJD ${"%.2f".format(item.amount)}", 130.dp)
                        AdminTableCell(
                            item.expectedReturn?.let { "${"%.2f".format(it)}%" } ?: "-",
                            150.dp
                        )
                        AdminStatusChip(item.status, 120.dp)
                        Row(
                            modifier = Modifier.width(220.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateInvestmentStatus(item.id, "approved") },
                                enabled = !item.status.equals("approved", ignoreCase = true)
                            ) { Text("Approve") }
                            OutlinedButton(
                                onClick = { viewModel.updateInvestmentStatus(item.id, "rejected") },
                                enabled = !item.status.equals("rejected", ignoreCase = true)
                            ) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoansTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Loan Applications",
        subtitle = "Review pending loans with a compact table layout.",
        actionContent = { OutlinedButton(onClick = viewModel::loadLoans) { Text("Refresh loans") } }
    ) {

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 80.dp),
                    AdminTableColumn("Customer", 110.dp),
                    AdminTableColumn("Loan Type", 150.dp),
                    AdminTableColumn("Amount", 130.dp),
                    AdminTableColumn("Term", 110.dp),
                    AdminTableColumn("Status", 120.dp),
                    AdminTableColumn("Submitted", 170.dp),
                    AdminTableColumn("Actions", 220.dp)
                )
            )

            if (uiState.loanApplications.isEmpty()) {
                AdminEmptyTableRow("No loan applications available.")
            } else {
                uiState.loanApplications.take(20).forEachIndexed { index, loan ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${loan.id}", 80.dp)
                        AdminTableCell(loan.customerId.toString(), 110.dp)
                        AdminTableCell(loan.loanProductId, 150.dp)
                        AdminTableCell("FJD ${"%.2f".format(loan.requestedAmount)}", 130.dp)
                        AdminTableCell("${loan.termMonths} months", 110.dp)
                        AdminStatusChip(loan.status, 120.dp)
                        AdminTableCell(loan.createdAt.take(16), 170.dp)
                        Row(
                            modifier = Modifier.width(220.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateLoanStatus(loan.id, "approved") },
                                enabled = !loan.status.equals("approved", ignoreCase = true)
                            ) { Text("Approve") }
                            OutlinedButton(
                                onClick = { viewModel.updateLoanStatus(loan.id, "rejected") },
                                enabled = !loan.status.equals("rejected", ignoreCase = true)
                            ) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Admin Transactions",
        subtitle = "Track transaction history and reverse completed entries when necessary."
    ) {
        OutlinedTextField(
            value = uiState.selectedAccountNumberForTransactions,
            onValueChange = viewModel::onSelectedAccountNumberForTransactionsChanged,
            label = { Text("Account number filter") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = viewModel::loadAdminTransactions, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh transactions")
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 80.dp),
                    AdminTableColumn("Account", 170.dp),
                    AdminTableColumn("Type", 120.dp),
                    AdminTableColumn("Amount", 130.dp),
                    AdminTableColumn("Status", 120.dp),
                    AdminTableColumn("Date", 170.dp),
                    AdminTableColumn("Actions", 150.dp)
                )
            )

            if (uiState.transactions.isEmpty()) {
                AdminEmptyTableRow("No transactions available.")
            } else {
                uiState.transactions.take(30).forEachIndexed { index, tx ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${tx.id}", 80.dp)
                        AdminTableCell(tx.accountNumber, 170.dp, bold = true)
                        AdminTableCell(tx.kind, 120.dp)
                        AdminTableCell("FJD ${"%.2f".format(tx.amount)}", 130.dp)
                        AdminStatusChip(tx.status, 120.dp)
                        AdminTableCell(tx.createdAt.take(16), 170.dp)
                        Box(modifier = Modifier.width(150.dp)) {
                            if (tx.status.equals("completed", ignoreCase = true)) {
                                OutlinedButton(onClick = { viewModel.reverseTransaction(tx.id) }) {
                                    Text("Reverse")
                                }
                            }
                        }
                    }
                }
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
    AdminSectionSurface(
        title = "Login Logs",
        subtitle = "Audit authentication activity across customer and admin accounts.",
        actionContent = { OutlinedButton(onClick = viewModel::loadLoginLogs) { Text("Refresh login logs") } }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("Type", 110.dp),
                    AdminTableColumn("Email", 220.dp),
                    AdminTableColumn("Success", 100.dp),
                    AdminTableColumn("Failure Reason", 220.dp),
                    AdminTableColumn("IP Address", 140.dp),
                    AdminTableColumn("Created At", 180.dp)
                )
            )

            if (uiState.loginLogs.isEmpty()) {
                AdminEmptyTableRow("No login logs available.")
            } else {
                uiState.loginLogs.take(20).forEachIndexed { index, log ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${log.id}", 70.dp)
                        AdminTableCell(log.userType, 110.dp, bold = true)
                        AdminTableCell(log.email, 220.dp)
                        AdminStatusChip(if (log.success) "success" else "failed", 100.dp)
                        AdminTableCell(log.failureReason ?: "-", 220.dp)
                        AdminTableCell(log.ipAddress ?: "-", 140.dp)
                        AdminTableCell(log.createdAt.take(16), 180.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLogsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Notification Logs",
        subtitle = "Track delivery status for SMS and notification events.",
        actionContent = { OutlinedButton(onClick = viewModel::loadNotificationLogs) { Text("Refresh notification logs") } }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("User ID", 100.dp),
                    AdminTableColumn("Phone", 150.dp),
                    AdminTableColumn("Type", 160.dp),
                    AdminTableColumn("Status", 120.dp),
                    AdminTableColumn("Provider Msg ID", 180.dp),
                    AdminTableColumn("Timestamp", 180.dp)
                )
            )

            if (uiState.notificationLogs.isEmpty()) {
                AdminEmptyTableRow("No notification logs available.")
            } else {
                uiState.notificationLogs.take(20).forEachIndexed { index, item ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${item.id}", 70.dp)
                        AdminTableCell(item.userId.toString(), 100.dp)
                        AdminTableCell(item.phoneNumber, 150.dp, bold = true)
                        AdminTableCell(item.notificationType, 160.dp)
                        AdminStatusChip(item.deliveryStatus, 120.dp)
                        AdminTableCell(item.providerMessageId ?: "-", 180.dp)
                        AdminTableCell(item.timestamp.take(16), 180.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpAttemptsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "OTP Attempts",
        subtitle = "Monitor OTP verification attempts and expiry windows.",
        actionContent = { OutlinedButton(onClick = viewModel::loadOtpAttempts) { Text("Refresh OTP attempts") } }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("Reference", 170.dp),
                    AdminTableColumn("Customer ID", 110.dp),
                    AdminTableColumn("Transaction", 150.dp),
                    AdminTableColumn("Attempts", 100.dp),
                    AdminTableColumn("Verified", 100.dp),
                    AdminTableColumn("Expires At", 180.dp),
                    AdminTableColumn("Created At", 180.dp)
                )
            )

            if (uiState.otpAttempts.isEmpty()) {
                AdminEmptyTableRow("No OTP attempts available.")
            } else {
                uiState.otpAttempts.take(20).forEachIndexed { index, otp ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${otp.id}", 70.dp)
                        AdminTableCell(otp.referenceCode, 170.dp, bold = true)
                        AdminTableCell(otp.customerId.toString(), 110.dp)
                        AdminTableCell(otp.transactionType, 150.dp)
                        AdminTableCell("${otp.attempts}/${otp.maxAttempts}", 100.dp)
                        AdminStatusChip(if (otp.verified) "verified" else "pending", 100.dp)
                        AdminTableCell(otp.expiresAt.take(16), 180.dp)
                        AdminTableCell(otp.createdAt.take(16), 180.dp)
                    }
                }
            }
        }
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
    AdminSectionSurface(
        title = "Interest Summaries",
        subtitle = "Generate and review annual interest summary rows for the configured year.",
        actionContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::generateSummaries) {
                    Text("Generate")
                }
                OutlinedButton(onClick = viewModel::loadSummaries) {
                    Text("Load")
                }
            }
        }
    ) {
        OutlinedTextField(
            value = uiState.summaryYear,
            onValueChange = viewModel::onSummaryYearChanged,
            label = { Text("Year") },
            modifier = Modifier.fillMaxWidth()
        )

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("Year", 90.dp),
                    AdminTableColumn("Account", 110.dp),
                    AdminTableColumn("Customer", 200.dp),
                    AdminTableColumn("Gross", 120.dp),
                    AdminTableColumn("Withholding Tax", 150.dp),
                    AdminTableColumn("Net", 120.dp),
                    AdminTableColumn("Status", 120.dp)
                )
            )

            if (uiState.summaries.isEmpty()) {
                AdminEmptyTableRow("No interest summaries available.")
            } else {
                uiState.summaries.take(20).forEachIndexed { index, summary ->
                    AdminTableDataRow(index) {
                        AdminTableCell(summary.year.toString(), 90.dp)
                        AdminTableCell(summary.accountId.toString(), 110.dp, bold = true)
                        AdminTableCell(summary.customerName, 200.dp)
                        AdminTableCell("FJD ${"%.2f".format(summary.grossInterest)}", 120.dp)
                        AdminTableCell("FJD ${"%.2f".format(summary.withholdingTax)}", 150.dp)
                        AdminTableCell("FJD ${"%.2f".format(summary.netInterest)}", 120.dp)
                        AdminStatusChip(summary.status, 120.dp)
                    }
                }
            }
        }
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("Day", 150.dp),
                    AdminTableColumn("Count", 100.dp),
                    AdminTableColumn("Total Amount", 150.dp)
                )
            )

            if (report.transactionsByDay.isEmpty()) {
                AdminEmptyTableRow("No transaction-by-day data available.")
            } else {
                report.transactionsByDay.forEachIndexed { index, row ->
                    AdminTableDataRow(index) {
                        AdminTableCell(row.day, 150.dp, bold = true)
                        AdminTableCell(row.count.toString(), 100.dp)
                        AdminTableCell("FJD ${"%.2f".format(row.totalAmount)}", 150.dp)
                    }
                }
            }
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
    AdminSectionSurface(
        title = "Statement Requests",
        subtitle = "Approve or reject pending statements from one table.",
        actionContent = { OutlinedButton(onClick = viewModel::loadStatements) { Text("Refresh requests") } }
    ) {

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .then(adminTableFrame())
        ) {
            AdminTableHeaderRow(
                listOf(
                    AdminTableColumn("ID", 70.dp),
                    AdminTableColumn("Name", 170.dp),
                    AdminTableColumn("Account", 150.dp),
                    AdminTableColumn("From", 120.dp),
                    AdminTableColumn("To", 120.dp),
                    AdminTableColumn("Status", 110.dp),
                    AdminTableColumn("Actions", 220.dp)
                )
            )

            if (uiState.statementRequests.isEmpty()) {
                AdminEmptyTableRow("No statement requests available.")
            } else {
                uiState.statementRequests.take(30).forEachIndexed { index, request ->
                    AdminTableDataRow(index) {
                        AdminTableCell("#${request.id}", 70.dp)
                        AdminTableCell(request.fullName, 170.dp, bold = true)
                        AdminTableCell(request.accountNumber, 150.dp)
                        AdminTableCell(request.fromDate.take(10), 120.dp)
                        AdminTableCell(request.toDate.take(10), 120.dp)
                        AdminStatusChip(request.status, 110.dp)
                        Row(
                            modifier = Modifier.width(220.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.updateStatementRequest(request.id, "approved") },
                                enabled = !request.status.equals("approved", ignoreCase = true)
                            ) { Text("Approve") }
                            OutlinedButton(
                                onClick = { viewModel.updateStatementRequest(request.id, "rejected") },
                                enabled = !request.status.equals("rejected", ignoreCase = true)
                            ) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
}

private data class AdminTableColumn(val title: String, val width: androidx.compose.ui.unit.Dp)

@Composable
private fun AdminSectionSurface(
    title: String,
    subtitle: String? = null,
    actionContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                actionContent?.invoke()
            }

            content()
        }
    }
}

@Composable
private fun adminTableFrame(): Modifier {
    return Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
        .padding(bottom = 8.dp)
}

@Composable
private fun AdminTableHeaderRow(columns: List<AdminTableColumn>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            .padding(vertical = 10.dp)
    ) {
        columns.forEach { column ->
            Text(
                text = column.title,
                modifier = Modifier.width(column.width).padding(horizontal = 10.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AdminTableDataRow(index: Int, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (index % 2 == 0) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
                }
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() }
    )
}

@Composable
private fun AdminTableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    bold: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2
    )
}

@Composable
private fun AdminStatusChip(status: String, width: androidx.compose.ui.unit.Dp) {
    val normalized = status.lowercase()
    val containerColor = when {
        normalized.contains("approved") || normalized.contains("active") || normalized.contains("completed") -> {
            MaterialTheme.colorScheme.tertiaryContainer
        }
        normalized.contains("pending") || normalized.contains("processing") -> {
            MaterialTheme.colorScheme.secondaryContainer
        }
        normalized.contains("reject") || normalized.contains("block") || normalized.contains("fail") -> {
            MaterialTheme.colorScheme.errorContainer
        }
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        normalized.contains("approved") || normalized.contains("active") || normalized.contains("completed") -> {
            MaterialTheme.colorScheme.onTertiaryContainer
        }
        normalized.contains("pending") || normalized.contains("processing") -> {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
        normalized.contains("reject") || normalized.contains("block") || normalized.contains("fail") -> {
            MaterialTheme.colorScheme.onErrorContainer
        }
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun AdminEmptyTableRow(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
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

@Composable
private fun AdminMetricCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.88f))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
