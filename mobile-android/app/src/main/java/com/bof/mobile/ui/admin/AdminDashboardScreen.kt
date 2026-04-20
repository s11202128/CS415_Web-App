package com.bof.mobile.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.AdminNotificationLogItem
import com.bof.mobile.viewmodel.AdminTab
import com.bof.mobile.viewmodel.AdminUiState
import com.bof.mobile.viewmodel.AdminViewModel

private enum class AdminBottomNavItem(
    val label: String,
    val icon: ImageVector,
    val tabs: List<AdminTab>
) {
    DASHBOARD(
        label = "Dashboard",
        icon = Icons.Filled.Dashboard,
        tabs = listOf(AdminTab.OVERVIEW, AdminTab.LOAN_REQUESTS, AdminTab.CUSTOMERS, AdminTab.REPORTS, AdminTab.STATEMENTS)
    ),
    ACCOUNTS(
        label = "Accounts",
        icon = Icons.Filled.People,
        tabs = listOf(AdminTab.REGISTRATION_REQUESTS, AdminTab.ACCOUNTS)
    ),
    TRANSACTIONS(
        label = "Transactions",
        icon = Icons.Filled.Payments,
        tabs = listOf(AdminTab.TRANSACTIONS, AdminTab.TRANSFER_HISTORY, AdminTab.INVESTMENTS, AdminTab.DEPOSITS)
    ),
    ACTIVITY(
        label = "Activity",
        icon = Icons.Filled.Tune,
        tabs = listOf(AdminTab.LOGIN_LOGS, AdminTab.NOTIFICATION_LOGS, AdminTab.OTP_ATTEMPTS, AdminTab.VERIFICATION)
    ),
    APPROVALS(
        label = "Interest",
        icon = Icons.AutoMirrored.Filled.FactCheck,
        tabs = listOf(AdminTab.TRANSFER_LIMIT, AdminTab.INTEREST_RATE, AdminTab.INTEREST_SUMMARIES)
    )
}

private fun resolveBottomNav(activeTab: AdminTab): AdminBottomNavItem {
    return AdminBottomNavItem.entries.firstOrNull { activeTab in it.tabs } ?: AdminBottomNavItem.DASHBOARD
}

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel, canGoBack: Boolean, onBack: () -> Unit, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val activeBottomNav = resolveBottomNav(uiState.activeTab)
    var showNotificationPage by remember { mutableStateOf(false) }
    var showProfilePage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAllAdminData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .padding(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminTopHeader(
                canGoBack = canGoBack,
                onBack = onBack,
                onLogout = onLogout,
                onNotificationsClick = {
                    showProfilePage = false
                    showNotificationPage = true
                    viewModel.loadNotificationLogs()
                },
                onProfileClick = {
                    showNotificationPage = false
                    showProfilePage = true
                }
            )

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

            if (!showNotificationPage && !showProfilePage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    activeBottomNav.tabs.forEach { tab ->
                        val isActive = tab == uiState.activeTab
                        if (isActive) {
                            Button(onClick = { viewModel.setActiveTab(tab) }) { Text(tab.label) }
                        } else {
                            OutlinedButton(onClick = { viewModel.setActiveTab(tab) }) { Text(tab.label) }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showProfilePage) {
                        AdminProfilePage(
                            uiState = uiState,
                            onBackToDashboard = { showProfilePage = false }
                        )
                    } else if (showNotificationPage) {
                        NotificationCenterPage(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBackToDashboard = { showNotificationPage = false }
                        )
                    } else {
                        AdminTabContent(uiState = uiState, viewModel = viewModel)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminBottomNavItem.entries.forEach { item ->
                    val selected = item == activeBottomNav
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showNotificationPage = false
                                showProfilePage = false
                                viewModel.setActiveTab(item.tabs.first())
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTopHeader(
    canGoBack: Boolean,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canGoBack) {
                    TextButton(onClick = onBack) { Text("Back") }
                }
                Column {
                    Text(
                        "Administrator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Secure Portal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onProfileClick) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AdminProfilePage(uiState: AdminUiState, onBackToDashboard: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBackToDashboard) {
                Icon(imageVector = Icons.Filled.Dashboard, contentDescription = "Back")
            }
            Text("Admin Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("AD", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Column {
                        Text("System Administrator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Role: Super Admin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        AdminSectionSurface(title = "Security Settings", subtitle = "Manage your administrative access.") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Change Admin Password") }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Configure 2FA") }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("View Access Logs") }
            }
        }
    }
}

@Composable
private fun NotificationCenterPage(uiState: AdminUiState, viewModel: AdminViewModel, onBackToDashboard: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBackToDashboard) {
                Icon(imageVector = Icons.Filled.Dashboard, contentDescription = "Back")
            }
            Text("Notification Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        AdminSectionSurface(title = "System Alerts", subtitle = "Recent automated system notifications.") {
            if (uiState.notificationLogs.isEmpty()) {
                Text("No recent alerts found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                uiState.notificationLogs.take(10).forEach { log ->
                    NotificationItem(log)
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(item: AdminNotificationLogItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.notificationType, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(item.timestamp.take(16), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("To: ${item.phoneNumber}", style = MaterialTheme.typography.bodySmall)
        Text("Status: ${item.deliveryStatus}", style = MaterialTheme.typography.bodySmall, color = if (item.deliveryStatus.contains("success", true)) Color(0xFF2E7D32) else Color(0xFFC62828))
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun AdminTabContent(uiState: AdminUiState, viewModel: AdminViewModel) {
    when (uiState.activeTab) {
        AdminTab.OVERVIEW -> OverviewTab(uiState = uiState, viewModel = viewModel)
        AdminTab.CUSTOMERS -> CustomersTab(uiState = uiState, viewModel = viewModel)
        AdminTab.REGISTRATION_REQUESTS -> RegistrationRequestsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.ACCOUNTS -> AccountsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.DEPOSITS -> DepositsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.INVESTMENTS -> InvestmentsTab(uiState = uiState, viewModel = viewModel)
        AdminTab.LOAN_REQUESTS -> LoanRequestsTab(uiState = uiState, viewModel = viewModel)
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
private fun RegistrationRequestsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    val pendingRegistrations = uiState.customers
        .filter { it.registrationStatus.equals("pending", ignoreCase = true) }
        .sortedBy { it.id }

    AdminSectionSurface(
        title = "Registration Requests",
        subtitle = "Approve or decline each request directly from the name list.",
        actionContent = {
            OutlinedButton(onClick = viewModel::loadCustomers) { Text("Refresh") }
        }
    ) {
        if (pendingRegistrations.isEmpty()) {
            Text("No new registration submissions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingRegistrations.forEach { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.updateCustomer(customer.id, mapOf("registrationStatus" to "approved")) }
                                ) { Text("Confirm") }
                                OutlinedButton(
                                    onClick = { viewModel.updateCustomer(customer.id, mapOf("registrationStatus" to "rejected")) }
                                ) { Text("Reject") }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(customer.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(customer.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Phone: ${customer.mobile}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Identity Verifications",
        subtitle = "Audit KYC documents and verification status for all users.",
        actionContent = { OutlinedButton(onClick = viewModel::loadCustomers) { Text("Reload") } }
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
                    AdminTableColumn("KYC Status", 130.dp),
                    AdminTableColumn("Actions", 150.dp)
                )
            )

            if (uiState.customers.isEmpty()) {
                AdminEmptyTableRow("No customers available for verification.")
            } else {
                uiState.customers.forEachIndexed { index, customer ->
                    AdminTableDataRow(index) {
                        AdminTableCell(customer.id.toString(), 70.dp)
                        AdminTableCell(customer.fullName, 180.dp, bold = true)
                        AdminTableCell(customer.email, 220.dp)
                        AdminStatusChip(customer.registrationStatus, 130.dp)
                        Box(modifier = Modifier.width(150.dp)) {
                            OutlinedButton(onClick = { /* View documents logic */ }) {
                                Text("Verify Docs")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    ),
                    shape = MaterialTheme.shapes.large
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("System Liquidity", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                        Text(
                            "FJD ${"%,.2f".format(uiState.accounts.sumOf { it.balance })}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = Color.White.copy(alpha = 0.25f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdminMetricCard("Total Customers", uiState.customers.size.toString())
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminMetricCard("Active Accounts", uiState.accounts.size.toString())
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardQuickAction(
                modifier = Modifier.weight(1f),
                title = "Approve Users",
                icon = Icons.AutoMirrored.Filled.FactCheck,
                count = uiState.customers.count { it.registrationStatus == "pending" },
                onClick = { viewModel.setActiveTab(AdminTab.REGISTRATION_REQUESTS) }
            )
            DashboardQuickAction(
                modifier = Modifier.weight(1f),
                title = "Loan Requests",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                count = uiState.loanApplications.count { it.status == "pending" },
                onClick = { viewModel.setActiveTab(AdminTab.LOAN_REQUESTS) }
            )
        }

        AdminSectionSurface(
            title = "Recent Activity",
            subtitle = "Overview of the latest 5 transactions across the bank.",
            actionContent = {
                TextButton(onClick = { viewModel.setActiveTab(AdminTab.TRANSACTIONS) }) { Text("View All") }
            }
        ) {
            if (uiState.transactions.isEmpty()) {
                Text("No recent transactions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.transactions.take(5).forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    color = if (tx.kind.lowercase().contains("credit")) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (tx.kind.lowercase().contains("credit")) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (tx.kind.lowercase().contains("credit")) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Column {
                                    Text(tx.accountNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(tx.kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                "FJD ${"%.2f".format(tx.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.kind.lowercase().contains("credit")) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardQuickAction(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (count > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            } else {
                Text("Up to date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CustomersTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Customer Directory",
        subtitle = "Manage all registered bank customers and their profile status.",
        actionContent = { OutlinedButton(onClick = viewModel::loadCustomers) { Text("Refresh") } }
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
                    AdminTableColumn("Full Name", 180.dp),
                    AdminTableColumn("Email Address", 240.dp),
                    AdminTableColumn("Phone", 150.dp),
                    AdminTableColumn("Verified", 110.dp),
                    AdminTableColumn("Joined", 170.dp),
                    AdminTableColumn("Actions", 140.dp)
                )
            )

            if (uiState.customers.isEmpty()) {
                AdminEmptyTableRow("No registered customers found.")
            } else {
                uiState.customers.forEachIndexed { index, customer ->
                    AdminTableDataRow(index) {
                        AdminTableCell(customer.id.toString(), 70.dp)
                        AdminTableCell(customer.fullName, 180.dp, bold = true)
                        AdminTableCell(customer.email, 240.dp)
                        AdminTableCell(customer.mobile, 150.dp)
                        AdminStatusChip(customer.registrationStatus, 110.dp)
                        AdminTableCell(customer.lastLoginAt?.take(16) ?: "-", 170.dp)
                        Box(modifier = Modifier.width(140.dp)) {
                            TextButton(onClick = { /* TODO */ }) { Text("Manage") }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Create Account", "Account Summaries", "Deposit", "Create Deposit").forEach { label ->
                val selected = uiState.accountSectionTab == label
                if (selected) {
                    Button(onClick = { viewModel.setAccountSectionTab(label) }) { Text(label) }
                } else {
                    OutlinedButton(onClick = { viewModel.setAccountSectionTab(label) }) { Text(label) }
                }
            }
        }

        if (uiState.accountSectionTab == "Create Account") {
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
                            imageVector = Icons.Filled.KeyboardArrowDown,
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
        }

        if (uiState.accountSectionTab == "Deposit") {
            com.bof.mobile.ui.deposit.DepositScreen(
                viewModel = com.bof.mobile.viewmodel.DepositViewModel(
                    com.bof.mobile.data.repository.AccountRepository(viewModel.repository.apiService),
                    com.bof.mobile.data.repository.FeatureRepository(viewModel.repository.apiService)
                ),
                canGoBack = false,
                onBack = {},
                onDepositCompleted = { viewModel.loadAccounts() }
            )
        }
        if (uiState.accountSectionTab == "Create Deposit") {
            CreateDepositTab(
                accounts = uiState.accounts,
                onDeposit = { accountId, amount, note ->
                    // Use a simple ViewModel-less approach for now, or you can wire to AdminViewModel
                    val depositViewModel = com.bof.mobile.viewmodel.DepositViewModel(
                        com.bof.mobile.data.repository.AccountRepository(viewModel.repository.apiService),
                        com.bof.mobile.data.repository.FeatureRepository(viewModel.repository.apiService)
                    )
                    depositViewModel.onDepositAccountIdChanged(accountId)
                    depositViewModel.onDepositAmountChanged(amount)
                    depositViewModel.onDepositNoteChanged(note)
                    depositViewModel.deposit()
                }
            )
        }

        if (uiState.accountSectionTab == "Account Summaries") {
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
}

@Composable
private fun CreateDepositTab(
    accounts: List<com.bof.mobile.model.AccountItem>,
    onDeposit: (String, String, String) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Create Deposit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = selectedAccountId,
            onValueChange = { selectedAccountId = it },
            label = { Text("Account ID") },
            placeholder = { Text("Enter or select account ID") },
            modifier = Modifier.fillMaxWidth()
        )
        // Optionally, you can add a dropdown for account selection
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            placeholder = { Text("Enter deposit amount") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optional)") },
            placeholder = { Text("Add a note") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (selectedAccountId.isNotBlank() && amount.isNotBlank()) {
                    onDeposit(selectedAccountId, amount, note)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Deposit to Account")
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
private fun LoanRequestsTab(uiState: AdminUiState, viewModel: AdminViewModel) {
    AdminSectionSurface(
        title = "Loan Requests",
        subtitle = "Review each request details and decide quickly.",
        actionContent = { OutlinedButton(onClick = viewModel::loadLoans) { Text("Refresh requests") } }
    ) {
        if (uiState.loanApplications.isEmpty()) {
            Text("No loan applications available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val sortedLoanRequests = uiState.loanApplications
                .sortedByDescending { it.submittedAt.ifBlank { it.createdAt } }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sortedLoanRequests.take(20).forEach { loan ->
                    val customerName = uiState.customers
                        .firstOrNull { it.id == loan.customerId }
                        ?.fullName
                        ?: "Customer #${loan.customerId}"
                    val linkedAccountNumber = uiState.accounts
                        .firstOrNull { it.customerId == loan.customerId }
                        ?.accountNumber
                        ?: "-"
                    val requestedOn = loan.submittedAt
                        .ifBlank { loan.createdAt }
                        .take(16)
                        .ifBlank { "-" }
                    val reviewedOn = loan.reviewedAt
                        ?.take(16)
                        ?: "-"
                    val safeLoanType = loan.loanProductId.ifBlank { "-" }
                    val safePurpose = loan.purpose.ifBlank { "-" }
                    val safeOccupation = loan.occupation.ifBlank { "-" }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Request #${loan.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                AdminStatusChip(loan.status, 110.dp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Customer: $customerName", style = MaterialTheme.typography.bodyMedium)
                                    Text("Account #: $linkedAccountNumber", style = MaterialTheme.typography.bodyMedium)
                                    Text("Loan Type: $safeLoanType", style = MaterialTheme.typography.bodyMedium)
                                    Text("Amount: FJD ${"%.2f".format(loan.requestedAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Term: ${loan.termMonths} months", style = MaterialTheme.typography.bodyMedium)
                                    Text("Purpose: $safePurpose", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Monthly Income: FJD ${"%.2f".format(loan.monthlyIncome)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Occupation: $safeOccupation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Requested On: $requestedOn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Reviewed On: $reviewedOn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Button(
                                        modifier = Modifier.width(112.dp),
                                        onClick = { viewModel.updateLoanStatus(loan.id, "approved") },
                                        enabled = !loan.status.equals("approved", ignoreCase = true)
                                    ) {
                                        Text("Accept")
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.width(112.dp),
                                        onClick = { viewModel.updateLoanStatus(loan.id, "rejected") },
                                        enabled = !loan.status.equals("rejected", ignoreCase = true)
                                    ) {
                                        Text("Reject")
                                    }
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
    Text(
        "Interest posting creates transaction records (interest credit + withholding tax) for Savings accounts.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
    Button(onClick = viewModel::applyMonthlyInterest, modifier = Modifier.fillMaxWidth()) {
        Text("Apply monthly savings interest")
    }
    OutlinedButton(onClick = viewModel::applyMaintenanceFees, modifier = Modifier.fillMaxWidth()) {
        Text("Apply monthly maintenance fees")
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
        VerticalBarGraphCard(
            title = "Daily Transaction Count",
            description = "Shows how many transactions were processed each day over the last 7 days. Use this to spot operational peaks and slow days.",
            points = report.transactionsByDay.map { row ->
                GraphPoint(
                    label = row.day.takeLast(5),
                    value = row.count.toDouble(),
                    valueText = formatCompactNumber(row.count.toDouble())
                )
            },
            accentColor = Color(0xFF1565C0)
        )

        Spacer(modifier = Modifier.height(8.dp))
        VerticalBarGraphCard(
            title = "Average Transaction Amount by Day",
            description = "Shows the average amount per transaction for each day (total amount divided by count). This highlights value intensity, not just activity volume.",
            points = report.transactionsByDay.map { row ->
                val average = if (row.count > 0) row.totalAmount / row.count else 0.0
                GraphPoint(
                    label = row.day.takeLast(5),
                    value = average,
                    valueText = "FJD ${formatCompactNumber(average)}"
                )
            },
            accentColor = Color(0xFF2E7D32)
        )

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

private data class GraphPoint(
    val label: String,
    val value: Double,
    val valueText: String
)

@Composable
private fun VerticalBarGraphCard(
    title: String,
    description: String,
    points: List<GraphPoint>,
    accentColor: Color
) {
    if (points.isEmpty()) {
        return
    }

    val maxValue = points.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
    val peakValue = points.maxOfOrNull { it.value } ?: 0.0
    val peakPoints = points.filter { it.value == peakValue }
    val peakSummary = peakPoints.joinToString(", ") { it.label }
    val yMidValue = maxValue / 2.0

    AdminSectionSurface(title = title, subtitle = description) {
        Text(
            text = "Peak day: $peakSummary (${peakPoints.firstOrNull()?.valueText ?: "0"})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatCompactNumber(maxValue), style = MaterialTheme.typography.labelSmall)
                Text(formatCompactNumber(yMidValue), style = MaterialTheme.typography.labelSmall)
                Text("0", style = MaterialTheme.typography.labelSmall)
            }

            points.forEach { point ->
                val ratio = (point.value / maxValue).toFloat().coerceIn(0f, 1f)
                val isPeak = point.value == peakValue
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = point.valueText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .height(120.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((120f * ratio).dp)
                                .background(
                                    if (isPeak) accentColor else accentColor.copy(alpha = 0.7f),
                                    MaterialTheme.shapes.small
                                )
                        )
                    }
                    Text(
                        text = if (isPeak) "${point.label}*" else point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPeak) accentColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun formatCompactNumber(value: Double): String {
    val absolute = kotlin.math.abs(value)
    return when {
        absolute >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
        absolute >= 1_000 -> "%.1fK".format(value / 1_000.0)
        absolute >= 100 -> "%.0f".format(value)
        absolute >= 10 -> "%.1f".format(value)
        else -> "%.2f".format(value)
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

private fun compactTableWidth(width: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    val reduced = width * 0.82f
    return if (reduced < 72.dp) 72.dp else reduced
}

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
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    .padding(bottom = 4.dp)
}

@Composable
private fun AdminTableHeaderRow(columns: List<AdminTableColumn>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            .padding(vertical = 6.dp)
    ) {
        columns.forEach { column ->
            Text(
                text = column.title,
                modifier = Modifier.width(compactTableWidth(column.width)).padding(horizontal = 6.dp),
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
            .padding(vertical = 6.dp),
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
        modifier = Modifier.width(compactTableWidth(width)).padding(horizontal = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2
    )
}

@Composable
private fun AdminStatusChip(status: String?, width: androidx.compose.ui.unit.Dp) {
    val resolvedStatus = status?.trim().takeUnless { it.isNullOrBlank() } ?: "unknown"
    val normalized = resolvedStatus.lowercase()
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
            .width(compactTableWidth(width))
            .padding(horizontal = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = resolvedStatus,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
            .padding(vertical = 8.dp)
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
