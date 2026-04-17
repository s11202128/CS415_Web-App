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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
) { /* ...existing code... */ }

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
) { /* ...existing code... */ }

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
) { /* ...existing code... */ }

/**
 * Masks an account number, showing only the last 4 digits.
 *
 * @param accountNumber The full account number
 * @return The masked account number
 */
private fun maskAccountNumber(accountNumber: String): String { /* ...existing code... */ }

/**
 * Formats the account updated time from a raw string.
 *
 * @param raw The raw date string
 * @return The formatted date string
 */
private fun formatAccountUpdatedTime(raw: String?): String { /* ...existing code... */ }

/**
 * Formats the balance updated time from an epoch value.
 *
 * @param epochMs The epoch time in milliseconds
 * @return The formatted date string
 */
private fun formatBalanceUpdatedTime(epochMs: Long?): String { /* ...existing code... */ }

/**
 * Formats a double value as a money string.
 *
 * @param value The value to format
 * @return The formatted money string
 */
private fun formatMoney(value: Double): String { /* ...existing code... */ }

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
) { /* ...existing code... */ }
