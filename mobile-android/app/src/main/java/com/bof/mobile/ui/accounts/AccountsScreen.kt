/**
 * Provides UI screens and components for managing and viewing user bank accounts.
 * Includes account overview, account creation, and transaction details.
 *
 * @author s11202128
 * @version 1.0
 */
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tabs for the Accounts screen.
 * @author s11202128
 * @version 1.0
 */
private enum class AccountsTab {
    /** Overview of accounts. */
    OVERVIEW,
    /** Request a new account. */
    REQUEST
}

/**
 * Main screen for managing user accounts.
 *
 * @param viewModel The ViewModel for account state and actions
 * @param canGoBack Whether the back button is enabled
 * @param onBack Callback for back navigation
 * @param onNavigateToCreateAccount Callback to navigate to account creation
 */
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
                OverviewTab(
                    uiState = uiState,
                    accounts = uiState.accounts,
                    onLoadPreviousPage = viewModel::loadPreviousPage,
                    onLoadNextPage = viewModel::loadNextPage
                )
            } else {
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
}

/**
 * Displays an overview of the user's accounts.
 *
 * @param uiState The current UI state
 * @param accounts List of user accounts
 * @param onLoadPreviousPage Callback to load previous page
 * @param onLoadNextPage Callback to load next page
 */
@Composable
private fun OverviewTab(
    uiState: com.bof.mobile.viewmodel.AccountsUiState,
    accounts: List<AccountItem>,
    onLoadPreviousPage: () -> Unit,
    onLoadNextPage: () -> Unit
) {
    val allAccounts = accounts.sortedByDescending { it.createdAt }
    val selectedAccount = allAccounts.firstOrNull { it.id == uiState.selectedAccountId } ?: allAccounts.firstOrNull()
    val accountName = selectedAccount?.accountHolder
        ?: uiState.selectedAccountDetails?.customer?.fullName
        ?: "N/A"
    val accountType = selectedAccount?.type ?: uiState.selectedAccountDetails?.account?.type ?: "N/A"
    val accountNumber = selectedAccount?.accountNumber ?: uiState.selectedAccountDetails?.account?.accountNumber ?: ""
    val accountStatus = selectedAccount?.status ?: uiState.selectedAccountDetails?.account?.status ?: "N/A"
    val currentBalance = selectedAccount?.balance ?: uiState.selectedAccountDetails?.account?.balance
    val updatedAtText = formatBalanceUpdatedTime(uiState.lastUpdatedAtEpochMs)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Account Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (selectedAccount == null) {
                Text(
                    text = "No account selected yet. Tap Refresh accounts to load your latest account overview.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (accountNumber.isBlank()) "N/A" else accountNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "FJD ${formatMoney(currentBalance ?: 0.0)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Updated at: $updatedAtText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Account Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(accountType, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(accountStatus, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (uiState.selectedAccountDetails != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AccountInlineDetails(
                        uiState = uiState,
                        onLoadPreviousPage = onLoadPreviousPage,
                        onLoadNextPage = onLoadNextPage
                    )
                }
            }
        }
    }
}

/**
 * Displays the account creation tab and a list of accounts.
 *
 * @param uiState The current UI state
 * @param onOpenCreateAccount Callback to open the account creation form
 * @param onSelectAccount Callback when an account is selected
 * @param onLoadPreviousPage Callback to load previous page
 * @param onLoadNextPage Callback to load next page
 */
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        onClick = { onSelectAccount(account.id) },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("View details")
                    }

                    if (uiState.selectedAccountId == account.id && uiState.selectedAccountDetails != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AccountInlineDetails(
                            uiState = uiState,
                            onLoadPreviousPage = onLoadPreviousPage,
                            onLoadNextPage = onLoadNextPage
                        )
                    }
                }
            }
        }
    }

}

/**
 * Shows inline details for a selected account, including recent transactions.
 *
 * @param uiState The current UI state
 * @param onLoadPreviousPage Callback to load previous page
 * @param onLoadNextPage Callback to load next page
 */
@Composable
private fun AccountInlineDetails(
    uiState: com.bof.mobile.viewmodel.AccountsUiState,
    onLoadPreviousPage: () -> Unit,
    onLoadNextPage: () -> Unit
) {
    val details = uiState.selectedAccountDetails
    if (details != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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

/**
 * Masks an account number, showing only the last 4 digits.
 *
 * @param accountNumber The full account number
 * @return The masked account number
 */
private fun maskAccountNumber(accountNumber: String): String {
    val suffix = accountNumber.takeLast(4)
    return if (suffix.isBlank()) "...." else ".... $suffix"
}

/**
 * Formats the account updated time from a raw string.
 *
 * @param raw The raw date string
 * @return The formatted date string
 */
private fun formatAccountUpdatedTime(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return "N/A"

    val outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    val zone = ZoneId.systemDefault()

    val parsed = runCatching {
        Instant.parse(value).atZone(zone).format(outputFormatter)
    }.recoverCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(zone).format(outputFormatter)
    }.recoverCatching {
        LocalDateTime.parse(value).atZone(zone).format(outputFormatter)
    }.getOrNull()

    return parsed ?: value
}

/**
 * Formats the balance updated time from an epoch value.
 *
 * @param epochMs The epoch time in milliseconds
 * @return The formatted date string
 */
private fun formatBalanceUpdatedTime(epochMs: Long?): String {
    val value = epochMs ?: return "N/A"
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH)
    return Instant.ofEpochMilli(value)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

/**
 * Formats a double value as a money string.
 *
 * @param value The value to format
 * @return The formatted money string
 */
private fun formatMoney(value: Double): String {
    return "%,.2f".format(Locale.ENGLISH, value)
}

/**
 * Displays a message banner for account-related messages.
 *
 * @param text The message text
 * @param isError Whether the message is an error
 * @param onDismiss Optional callback for dismissing the banner
 * @param actionLabel Optional label for the action button
 */
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
