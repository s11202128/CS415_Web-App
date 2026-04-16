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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bof.mobile.R
import com.bof.mobile.model.NotificationItem
import com.bof.mobile.model.ProfileResponse
import com.bof.mobile.viewmodel.DashboardViewModel
import com.bof.mobile.viewmodel.FeatureViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    featureViewModel: FeatureViewModel,
    customerId: Int,
    onLogout: () -> Unit,
    onNavigateToCreateAccount: () -> Unit = {},
    onNavigateToTransfers: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToFeatures: () -> Unit = {},
    onNavigateToDeposit: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToFunding: () -> Unit = {},
    onNavigateToBillPayment: () -> Unit = {},
    onNavigateToStatement: () -> Unit = {},
    onNavigateToReport: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val featureUiState by featureViewModel.uiState.collectAsState()
    var showBalance by remember { mutableStateOf(true) }
    var showNotifications by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var changePinMessage by remember { mutableStateOf<String?>(null) }
    var showMenuDrawer by remember { mutableStateOf(false) }
    var menuMessage by remember { mutableStateOf<String?>(null) }

    // Self-healing trigger: if dashboard wasn't loaded upstream, kick one load from here.
    LaunchedEffect(customerId, uiState.hasLoadedOnce, uiState.isLoading) {
        if (!uiState.hasLoadedOnce && !uiState.isLoading) {
            viewModel.loadDashboard(customerId.takeIf { it > 0 })
        }
    }

    LaunchedEffect(customerId) {
        featureViewModel.loadNotifications(customerId)
        featureViewModel.loadProfile(customerId)
    }

    LaunchedEffect(showNotifications, customerId) {
        if (showNotifications) {
            featureViewModel.loadNotifications(customerId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading && uiState.data == null) {
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
        if (data == null && uiState.hasLoadedOnce) {
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

        if (data == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Box
        }

        val totalBalance = data.accounts.sumOf { it.balance }
        val firstAccount = data.accounts.firstOrNull()
        val accountNumberText = firstAccount?.accountNumber ?: "No account number"

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

            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Refreshing dashboard...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                HeaderSection(
                    name = data.customer.fullName,
                    profile = featureUiState.profile,
                    notificationCount = featureUiState.notifications.size,
                    onOpenMenu = { showMenuDrawer = true },
                    onOpenNotifications = { showNotifications = true },
                    onLogout = onLogout
                )
            }

            item {
                BalanceCard(
                    balance = totalBalance,
                    showBalance = showBalance,
                    onToggleVisibility = { showBalance = !showBalance },
                    maskedNumber = accountNumberText
                )
            }

            item {
                ActionButtonsSection(
                    onSendMoney = onNavigateToTransfers,
                    onLoanFeature = onNavigateToFeatures,
                    onFunding = onNavigateToFunding,
                    onBillPayment = onNavigateToBillPayment,
                    onAccounts = onNavigateToAccounts,
                    onStatements = onNavigateToStatement,
                    onTransactionHistory = onNavigateToActivity
                )
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        DashboardFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            onStatement = onNavigateToStatement,
            onReport = onNavigateToReport,
            onActivity = onNavigateToActivity
        )

        if (showNotifications) {
            NotificationsPanel(
                notifications = featureUiState.notifications,
                onRefresh = { featureViewModel.loadNotifications(customerId) },
                onDismiss = { showNotifications = false }
            )
        }

        if (showProfile && featureUiState.profile != null) {
            ProfilePanel(
                profile = featureUiState.profile!!,
                onDismiss = { showProfile = false }
            )
        }

        if (showChangePin) {
            ChangePinPanel(
                oldPassword = oldPassword,
                newPassword = newPassword,
                confirmNewPassword = confirmNewPassword,
                message = changePinMessage,
                onOldPasswordChanged = {
                    oldPassword = it
                    changePinMessage = null
                },
                onNewPasswordChanged = {
                    newPassword = it
                    changePinMessage = null
                },
                onConfirmNewPasswordChanged = {
                    confirmNewPassword = it
                    changePinMessage = null
                },
                onSubmit = {
                    when {
                        oldPassword.isBlank() -> changePinMessage = "Enter your old password"
                        newPassword.isBlank() -> changePinMessage = "Enter your new password"
                        confirmNewPassword.isBlank() -> changePinMessage = "Confirm your new password"
                        newPassword != confirmNewPassword -> changePinMessage = "New passwords do not match"
                        else -> changePinMessage = "PIN updated successfully"
                    }
                },
                onDismiss = {
                    showChangePin = false
                    oldPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    changePinMessage = null
                }
            )
        }

        menuMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { menuMessage = null },
                title = { Text("Find Us") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { menuMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showMenuDrawer) {
            DashboardMenuDrawer(
                accountName = data.customer.fullName,
                accountNumber = accountNumberText,
                lastLogin = featureUiState.profile?.lastLoginAt ?: "Not available",
                onDismiss = { showMenuDrawer = false },
                onMenuItemSelected = { label ->
                    showMenuDrawer = false
                    when (label) {
                        "Home" -> Unit
                        "My Contact Details" -> {
                            featureViewModel.loadProfile(customerId)
                            showProfile = true
                        }
                        "Change My PIN" -> {
                            showChangePin = true
                        }
                        "Find Us" -> {
                            menuMessage = "BRED Bank Fiji: Head Office in Suva, plus branches and ATMs across Fiji."
                        }
                        "Notification Setting" -> {
                            featureViewModel.loadNotifications(customerId)
                            showNotifications = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HeaderSection(
    name: String,
    profile: ProfileResponse?,
    notificationCount: Int,
    onOpenMenu: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallIconActionButton(symbol = "☰", onClick = onOpenMenu)
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
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout"
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Logout", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private data class DashboardMenuItem(
    val label: String,
    val icon: ImageVector,
    val backgroundColor: Color
)

@Composable
private fun DashboardMenuDrawer(
    accountName: String,
    accountNumber: String,
    lastLogin: String,
    onDismiss: () -> Unit,
    onMenuItemSelected: (String) -> Unit
) {
    val menuItems = listOf(
        DashboardMenuItem("Home", Icons.Filled.Timeline, Color(0xFFE0F2FE)),
        DashboardMenuItem("My Contact Details", Icons.Filled.Description, Color(0xFFECFCCB)),
        DashboardMenuItem("Change My PIN", Icons.Filled.Security, Color(0xFFFEE2E2)),
        DashboardMenuItem("Find Us", Icons.Filled.CreditCard, Color(0xFFF3E8FF)),
        DashboardMenuItem("Notification Setting", Icons.Filled.Settings, Color(0xFFFFF7ED))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.app_image),
                        contentDescription = "Bank of Fiji logo",
                        modifier = Modifier.size(40.dp)
                    )
                    Column {
                        Text("Bank of Fiji", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Digital Banking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Account Name: $accountName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Account Number: $accountNumber", style = MaterialTheme.typography.bodySmall)
                        Text("Last Login: $lastLogin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                menuItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMenuItemSelected(item.label) },
                        colors = CardDefaults.cardColors(containerColor = item.backgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = item.icon, contentDescription = item.label)
                            Text(text = item.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
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
private fun RowScope.ProfileStatusChip(label: String, verified: Boolean? = null, value: String? = null) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(28.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = when {
                verified == true -> MaterialTheme.colorScheme.tertiaryContainer
                verified == false -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = when {
                    verified != null -> if (verified) "✓ $label" else "○ $label"
                    value != null -> "$label: $value"
                    else -> label
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    verified == true -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
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
private fun ChangePinPanel(
    oldPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    message: String?,
    onOldPasswordChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Change My PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = onOldPasswordChanged,
                    label = { Text("Enter old password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChanged,
                    label = { Text("Enter new password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = onConfirmNewPasswordChanged,
                    label = { Text("Re-enter new password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                if (!message.isNullOrBlank()) {
                    val isSuccess = message.contains("success", ignoreCase = true)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                    Button(onClick = onSubmit, modifier = Modifier.weight(1f)) {
                        Text("Update PIN")
                    }
                }
            }
        }
    }
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
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
private fun ProfilePanel(
    profile: ProfileResponse,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👤", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                "Profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                profile.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ProfileStatusChip(label = "Email verified", verified = profile.emailVerified)
                            ProfileStatusChip(label = "Identity verified", verified = profile.identityVerified)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ProfileStatusChip(label = "Failed logins", value = profile.failedLoginAttempts.toString())
                            ProfileStatusChip(label = "Registration", value = profile.registrationStatus)
                        }

                        if (!profile.lastLoginAt.isNullOrBlank()) {
                            Text(
                                text = "Last login: ${profile.lastLoginAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Name: ${profile.fullName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Mobile: ${profile.mobile}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!profile.lockedUntil.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(
                                    text = "Account locked until ${profile.lockedUntil}",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
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
    onSendMoney: () -> Unit,
    onLoanFeature: () -> Unit,
    onFunding: () -> Unit,
    onBillPayment: () -> Unit,
    onAccounts: () -> Unit,
    onStatements: () -> Unit,
    onTransactionHistory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButtonCard("🏛", "Accounts", onAccounts, Modifier.weight(1f))
            ActionButtonCard("↗", "Transfer Money", onSendMoney, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButtonCard("🧾", "Bill Payment", onBillPayment, Modifier.weight(1f))
            ActionButtonCard("📄", "Apply Loan", onLoanFeature, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButtonCard("📑", "Statements", onStatements, Modifier.weight(1f))
            ActionButtonCard("📜", "Transaction History", onTransactionHistory, Modifier.weight(1f))
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
private fun DashboardFooter(
    modifier: Modifier = Modifier,
    onStatement: () -> Unit,
    onReport: () -> Unit,
    onActivity: () -> Unit
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
            BottomNavItem(icon = Icons.Filled.Description, label = "Statement", active = false, onClick = onStatement)
            BottomNavItem(symbol = "📊", label = "Report", active = false, onClick = onReport)
            BottomNavItem(icon = Icons.Filled.Timeline, label = "Activity", active = false, onClick = onActivity)
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(symbol: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, label: String, active: Boolean, onClick: () -> Unit) {
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
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = label)
            } else {
                Text(symbol.orEmpty())
            }
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
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
