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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.AccountItem
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.AccountsViewModel

@Composable
fun AccountsScreen(viewModel: AccountsViewModel, canGoBack: Boolean, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

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

            if (uiState.isLoadingAccounts) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                AccountsMessageBanner(
                    text = uiState.errorMessage ?: "",
                    isError = true,
                    onDismiss = { viewModel.loadAccounts() },
                    actionLabel = "Retry"
                )
            }

            if (uiState.accounts.isEmpty()) {
                AccountsMessageBanner(text = "No accounts found", isError = true)
                return@Column
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
                                onClick = { viewModel.selectAccount(account.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("View details")
                            }
                        }
                    }
                }
            }

            // Accounts list with horizontal scroll and sticky header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(scrollState)
                        .padding(12.dp)
                ) {
                    // Sticky header row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(vertical = 8.dp),
                    ) {
                        Text("Account #", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
                        Text("Type", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                        Text("Holder", modifier = Modifier.width(180.dp), fontWeight = FontWeight.Bold)
                        Text("Balance", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
                        Text("Status", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                        Text("Actions", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                    }
                    // Data rows
                    uiState.accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .background(if (account.id % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(account.accountNumber, modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.type, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.accountHolder, modifier = Modifier.width(180.dp), style = MaterialTheme.typography.bodyMedium)
                            Text("FJD ${"%.2f".format(account.balance)}", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(account.status, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.width(100.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.selectAccount(account.id) },
                                    modifier = Modifier.height(36.dp)
                                ) { Text("View") }
                            }
                        }
                    }
                }
            }

            // Selected account details
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
                        OutlinedButton(onClick = viewModel::loadPreviousPage, enabled = uiState.page > 1) {
                            Text("Previous")
                        }
                        Text("Page ${uiState.page} / ${uiState.totalPages}", modifier = Modifier.align(Alignment.CenterVertically))
                        OutlinedButton(onClick = viewModel::loadNextPage, enabled = uiState.page < uiState.totalPages) {
                            Text("Next")
                        }
                    }
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
private fun AccountItem(account: AccountItem, onSelect: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${account.accountNumber} (${account.type})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Balance: FJD ${"%.2f".format(account.balance)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onSelect) {
                Text("View", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
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
