package com.bof.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.NotificationItem
import com.bof.mobile.viewmodel.DashboardViewModel
import com.bof.mobile.viewmodel.FeatureViewModel

private enum class DashboardTxFilter {
    ALL,
    INCOME,
    EXPENSE
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    featureViewModel: FeatureViewModel,
    customerId: Int,
    onLogout: () -> Unit,
    onNavigateToTransfers: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToFeatures: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val featureUiState by featureViewModel.uiState.collectAsState()
    var showBalance by remember { mutableStateOf(true) }
    var showNotifications by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var txFilter by remember { mutableStateOf(DashboardTxFilter.ALL) }

    LaunchedEffect(customerId) {
        viewModel.loadDashboard(customerId)
        featureViewModel.loadNotifications(customerId)
    }

    LaunchedEffect(showNotifications, customerId) {
        if (showNotifications) {
            featureViewModel.loadNotifications(customerId)
        }
    }

    LaunchedEffect(showSettings, customerId) {
        if (showSettings) {
            featureViewModel.loadProfile(customerId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Box
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LogoutButton(onLogout = onLogout)
                }
                DashboardMessageBanner(
                    text = uiState.errorMessage ?: "",
                    isError = true,
                    onDismiss = { viewModel.loadDashboard(customerId) },
                    actionLabel = "Retry"
                )
            }
            return@Box
        }

        val data = uiState.data
        if (data == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LogoutButton(onLogout = onLogout)
                }
                DashboardMessageBanner(text = "No dashboard data available", isError = true)
            }
            return@Box
        }

        val totalBalance = data.accounts.sumOf { it.balance }
        val firstAccount = data.accounts.firstOrNull()
        val cardSuffix = firstAccount?.accountNumber?.takeLast(4).orEmpty()
        val filteredTransactions = data.recentTransactions.filter { tx ->
            when (txFilter) {
                DashboardTxFilter.ALL -> true
                DashboardTxFilter.INCOME -> isIncomeTransaction(tx.type, tx.amount)
                DashboardTxFilter.EXPENSE -> !isIncomeTransaction(tx.type, tx.amount)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        top = 0.dp,
                        end = 16.dp,
                        bottom = 96.dp
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                HeaderSection(
                    name = data.customer.fullName,
                    notificationCount = featureUiState.notifications.size,
                    onOpenNotifications = { showNotifications = true },
                    onOpenProfile = { showSettings = true },
                    onLogout = onLogout
                )
            }

            item {
                BalanceCard(
                    balance = totalBalance,
                    showBalance = showBalance,
                    onToggleVisibility = { showBalance = !showBalance },
                    maskedNumber = if (cardSuffix.isBlank()) "**** **** **** 0000" else "**** **** **** $cardSuffix"
                )
            }

            item {
                ActionButtonsSection(
                    onAddMoney = onNavigateToFeatures,
                    onSendMoney = onNavigateToTransfers,
                    onDeposit = onNavigateToFeatures,
                    onWithdraw = onNavigateToFeatures
                )
            }

            item {
                TransactionHeader(onSeeAll = onNavigateToFeatures)
            }

            item {
                TransactionFilterRow(selected = txFilter, onSelect = { txFilter = it })
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = "No transactions for this filter",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTransactions.size) { idx ->
                    TransactionItemRow(
                        title = filteredTransactions[idx].description.ifBlank { filteredTransactions[idx].type },
                        subtitle = filteredTransactions[idx].type,
                        amount = filteredTransactions[idx].amount,
                        isIncome = isIncomeTransaction(filteredTransactions[idx].type, filteredTransactions[idx].amount)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        DashboardFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            onWallet = onNavigateToAccounts,
            onReport = onNavigateToFeatures,
            onAccount = onNavigateToFeatures
        )

        if (showNotifications) {
            NotificationsPanel(
                notifications = featureUiState.notifications,
                onRefresh = { featureViewModel.loadNotifications(customerId) },
                onDismiss = { showNotifications = false }
            )
        }

        if (showSettings) {
            SettingsPanel(
                uiState = featureUiState,
                onFullNameChanged = featureViewModel::onFullNameChanged,
                onMobileChanged = featureViewModel::onMobileChanged,
                onNationalIdChanged = featureViewModel::onNationalIdChanged,
                onResidencyStatusChanged = featureViewModel::onResidencyStatusChanged,
                onTinChanged = featureViewModel::onTinChanged,
                onUpdateProfile = featureViewModel::updateProfile,
                onSendPasswordReset = featureViewModel::forgotPassword,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun FeaturesTabSection(
    onNavigateToTransfers: () -> Unit,
    onNavigateToFeatures: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text("Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureButton("Transfer", onClick = onNavigateToTransfers)
            FeatureButton("Pay Bills", onClick = onNavigateToFeatures)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureButton("Statements", onClick = onNavigateToFeatures)
            FeatureButton("Profile", onClick = onNavigateToFeatures)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureButton("Notifications", onClick = onNavigateToFeatures)
            FeatureButton("Loans", onClick = onNavigateToFeatures)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureButton("Investments", onClick = onNavigateToFeatures)
            FeatureButton("Interest", onClick = onNavigateToFeatures)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            FeatureButton("Password Reset", onClick = onNavigateToFeatures)
            Spacer(modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
        }
    }
}

@Composable
private fun HeaderSection(
    name: String,
    notificationCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👤", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(
                    text = "Hi, ${name.ifBlank { "User" }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallIconActionButton(symbol = "🔔", badgeCount = notificationCount, onClick = onOpenNotifications)
            SmallIconActionButton(symbol = "⚙", onClick = onOpenProfile)
            SmallIconActionButton(symbol = "⎋", onClick = onLogout)
        }
    }
}

@Composable
private fun SmallIconActionButton(symbol: String, badgeCount: Int = 0, onClick: () -> Unit) {
    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.TopEnd) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(symbol)
        }

        if (badgeCount > 0) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 1.dp, end = 1.dp)
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationsPanel(
    notifications: List<NotificationItem>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Latest alerts and delivery updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh notifications")
                }

                if (notifications.isEmpty()) {
                    Text(
                        text = "No notifications available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { item ->
                            NotificationRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    uiState: com.bof.mobile.viewmodel.FeatureUiState,
    onFullNameChanged: (String) -> Unit,
    onMobileChanged: (String) -> Unit,
    onNationalIdChanged: (String) -> Unit,
    onResidencyStatusChanged: (String) -> Unit,
    onTinChanged: (String) -> Unit,
    onUpdateProfile: () -> Unit,
    onSendPasswordReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Update your profile details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (!uiState.successMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(
                            text = uiState.successMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                uiState.profile?.let { profile ->
                    SettingsSectionTitle("Security")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SettingsStatChip(label = "Email verified", value = if (profile.emailVerified) "Yes" else "No")
                        SettingsStatChip(label = "Identity verified", value = if (profile.identityVerified) "Yes" else "No")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SettingsStatChip(label = "Failed logins", value = profile.failedLoginAttempts.toString())
                        SettingsStatChip(label = "Registration", value = profile.registrationStatus)
                    }
                    if (!profile.lastLoginAt.isNullOrBlank()) {
                        Text(
                            text = "Last login: ${profile.lastLoginAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!profile.lockedUntil.isNullOrBlank()) {
                        Text(
                            text = "Account locked until ${profile.lockedUntil}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedButton(
                        onClick = onSendPasswordReset,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = profile.email.isNotBlank()
                    ) {
                        Text("Send password reset code")
                    }
                }

                SettingsSectionTitle("Profile")

                SettingsField(value = uiState.fullName, label = "Full name", onValueChange = onFullNameChanged)
                SettingsField(value = uiState.mobile, label = "Mobile", onValueChange = onMobileChanged)
                SettingsField(value = uiState.nationalId, label = "National ID", onValueChange = onNationalIdChanged)
                SettingsField(value = uiState.residencyStatus, label = "Residency status", onValueChange = onResidencyStatusChanged)
                SettingsField(value = uiState.tin, label = "TIN", onValueChange = onTinChanged)

                Button(onClick = onUpdateProfile, modifier = Modifier.fillMaxWidth()) {
                    Text("Update profile")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun RowScope.SettingsStatChip(label: String, value: String) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SettingsField(value: String, label: String, onValueChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NotificationRow(item: NotificationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.notificationType, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(item.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${item.deliveryStatus} • ${item.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    showBalance: Boolean,
    onToggleVisibility: () -> Unit,
    maskedNumber: String
) {
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
                        listOf(Color(0xFF1976D2), Color(0xFF5E35B1))
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Balance", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onToggleVisibility,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (showBalance) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (showBalance) "$${"%,.2f".format(balance)}" else "$••••••",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(maskedNumber, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.94f))
                    Text("VISA", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onAddMoney: () -> Unit,
    onSendMoney: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButtonCard("➕", "Add Money", onAddMoney, Modifier.weight(1f))
            ActionButtonCard("↗", "Send Money", onSendMoney, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButtonCard("🏦", "Deposit", onDeposit, Modifier.weight(1f))
            ActionButtonCard("💸", "Withdraw", onWithdraw, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionButtonCard(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(1.8f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TransactionHeader(onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Transaction History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Button(
            onClick = onSeeAll,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            elevation = null
        ) {
            Text("See all", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TransactionFilterRow(selected: DashboardTxFilter, onSelect: (DashboardTxFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == DashboardTxFilter.ALL,
            onClick = { onSelect(DashboardTxFilter.ALL) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = selected == DashboardTxFilter.INCOME,
            onClick = { onSelect(DashboardTxFilter.INCOME) },
            label = { Text("Income") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        FilterChip(
            selected = selected == DashboardTxFilter.EXPENSE,
            onClick = { onSelect(DashboardTxFilter.EXPENSE) },
            label = { Text("Expense") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun TransactionItemRow(title: String, subtitle: String, amount: Double, isIncome: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = MaterialTheme.shapes.large,
                    color = if (isIncome) Color(0xFFD9F8E6) else Color(0xFFFFE2E2)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (isIncome) "⬇" else "⬆")
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = if (isIncome) "+$${"%.2f".format(amount)}" else "-$${"%.2f".format(kotlin.math.abs(amount))}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isIncome) Color(0xFF1B8F47) else Color(0xFFC73737),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DashboardFooter(
    modifier: Modifier = Modifier,
    onWallet: () -> Unit,
    onReport: () -> Unit,
    onAccount: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(symbol = "🏠", label = "Home", active = true, onClick = {})
            BottomNavItem(symbol = "👛", label = "Wallet", active = false, onClick = onWallet)
            BottomNavItem(symbol = "📊", label = "Report", active = false, onClick = onReport)
            BottomNavItem(symbol = "👤", label = "Account", active = false, onClick = onAccount)
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(symbol: String, label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(symbol)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun isIncomeTransaction(type: String, amount: Double): Boolean {
    val normalized = type.lowercase()
    if (normalized.contains("deposit") || normalized.contains("credit") || normalized.contains("income")) return true
    if (normalized.contains("withdraw") || normalized.contains("debit") || normalized.contains("expense") || normalized.contains("payment")) return false
    return amount >= 0
}

@Composable
private fun RowScope.FeatureButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(4.dp)
    ) {
        Text("Logout", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DashboardMessageBanner(
    text: String,
    isError: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onDismiss != null) {
                Button(onClick = onDismiss, modifier = Modifier.padding(start = 8.dp)) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
