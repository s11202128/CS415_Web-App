package com.bof.mobile.ui.accounts

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.AccountItem
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.AccountsViewModel

private enum class AccountsTab {
    OVERVIEW,
    REQUEST
}

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onNavigateToCreateAccount: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(AccountsTab.OVERVIEW) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            ScreenHeader(
                title = "Accounts",
                subtitle = "Manage your accounts",
                onBack = onBack,
                enabled = canGoBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoadingAccounts && uiState.accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (uiState.isLoadingAccounts) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refreshing accounts...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                AccountsMessageBanner(
                    text = uiState.errorMessage ?: "",
                    isError = true,
                    onDismiss = { viewModel.loadAccounts() },
                    actionLabel = "Retry"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == AccountsTab.OVERVIEW,
                    onClick = { selectedTab = AccountsTab.OVERVIEW },
                    label = { Text("Account Overview") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                FilterChip(
                    selected = selectedTab == AccountsTab.REQUEST,
                    onClick = { selectedTab = AccountsTab.REQUEST },
                    label = { Text("Request New Account") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.loadAccounts() }) {
                Text("Refresh accounts")
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == AccountsTab.OVERVIEW) {
                OverviewTab(accounts = uiState.accounts)
                return@Column
            }

            CreateTab(
                uiState = uiState,
                onOpenCreateAccount = onNavigateToCreateAccount,
                onSelectAccount = viewModel::selectAccount,
                onLoadPreviousPage = viewModel::loadPreviousPage,
                onLoadNextPage = viewModel::loadNextPage
            )
        }
    }
}

@Composable
private fun OverviewTab(accounts: List<AccountItem>) {
    val allAccounts = accounts.sortedByDescending { it.createdAt }
    val totalBalance = allAccounts.sumOf { it.balance }
    val simpleCount = allAccounts.count { it.type.equals("Simple Access", ignoreCase = true) }
    val savingsCount = allAccounts.count { it.type.equals("Savings", ignoreCase = true) }
    val currentCount = allAccounts.count { it.type.equals("Current", ignoreCase = true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Account Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Total Accounts: ${allAccounts.size}", style = MaterialTheme.typography.bodyMedium)
            Text("Simple Access: $simpleCount", style = MaterialTheme.typography.bodyMedium)
            Text("Savings: $savingsCount", style = MaterialTheme.typography.bodyMedium)
            Text("Current: $currentCount", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Total Balance: FJD ${"%.2f".format(totalBalance)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Account Summaries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (allAccounts.isEmpty()) {
                Text(
                    text = "No accounts found yet. If you just submitted an account request, tap Refresh accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val pendingCount = allAccounts.count { it.status.equals("pending_approval", ignoreCase = true) }
                if (pendingCount > 0) {
                    Text(
                        text = "$pendingCount account request(s) pending admin approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "Showing all created accounts for this logged-in customer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(vertical = 8.dp)
                    ) {
                        Text("ID", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold)
                        Text("Account Number", modifier = Modifier.width(170.dp), fontWeight = FontWeight.Bold)
                        Text("Account Holder", modifier = Modifier.width(170.dp), fontWeight = FontWeight.Bold)
                        Text("PIN", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                        Text("Type", modifier = Modifier.width(140.dp), fontWeight = FontWeight.Bold)
                        Text("Status", modifier = Modifier.width(130.dp), fontWeight = FontWeight.Bold)
                        Text("Balance", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
                        Text("Charge", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
                    }

                    allAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(vertical = 8.dp)
                        ) {
                            Text(account.id.toString(), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.accountNumber, modifier = Modifier.width(170.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.accountHolder, modifier = Modifier.width(170.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.accountPin ?: "----", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.type, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.status, modifier = Modifier.width(130.dp), style = MaterialTheme.typography.bodyMedium)
                            Text("FJD ${"%.2f".format(account.balance)}", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                            Text("FJD ${"%.2f".format(account.maintenanceFee)}", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTab(
    uiState: com.bof.mobile.viewmodel.AccountsUiState,
    onOpenCreateAccount: () -> Unit,
    onSelectAccount: (Int) -> Unit,
    onLoadPreviousPage: () -> Unit,
    onLoadNextPage: () -> Unit
) {
    Button(
        onClick = onOpenCreateAccount,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text("Open Request Account Form")
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (uiState.accounts.isEmpty()) {
        AccountsMessageBanner(text = "No accounts found", isError = true)
        return
    }

    Text("My Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uiState.accounts.forEach { account ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = account.type.ifBlank { "Account" },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "FJD ${"%.2f".format(account.balance)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = maskAccountNumber(account.accountNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSelectAccount(account.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("View details")
                    }
                }
            }
        }
    }

    val details = uiState.selectedAccountDetails
    if (details != null) {
        Text("Selected Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Holder: ${details.customer.fullName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Status: ${details.account.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(8.dp))
                Text("Recent Transactions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                details.transactions.take(5).forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(tx.kind.uppercase(), style = MaterialTheme.typography.bodySmall)
                            Text(tx.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("FJD ${"%.2f".format(tx.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.selectedAccountId != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onLoadPreviousPage, enabled = uiState.page > 1) {
                    Text("Previous")
                }
                Text("Page ${uiState.page} / ${uiState.totalPages}", modifier = Modifier.align(Alignment.CenterVertically))
                OutlinedButton(onClick = onLoadNextPage, enabled = uiState.page < uiState.totalPages) {
                    Text("Next")
                }
            }
        }
    }
}

private fun maskAccountNumber(accountNumber: String): String {
    val suffix = accountNumber.takeLast(4)
    return if (suffix.isBlank()) "...." else ".... $suffix"
}

@Composable
private fun AccountsMessageBanner(
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
