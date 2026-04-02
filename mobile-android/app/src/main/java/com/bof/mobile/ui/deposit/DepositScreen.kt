package com.bof.mobile.ui.deposit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.DashboardAccount
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun DepositScreen(
    featureViewModel: FeatureViewModel,
    accountsList: List<DashboardAccount>,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onDepositCompleted: () -> Unit = {}
) {
    val uiState by featureViewModel.uiState.collectAsState()
    var accountMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(accountsList, uiState.depositAccountId) {
        if (uiState.depositAccountId.isBlank() && accountsList.isNotEmpty()) {
            featureViewModel.onDepositAccountIdChanged(accountsList.first().id.toString())
        }
    }

    LaunchedEffect(Unit) {
        featureViewModel.clearMessages()
    }

    LaunchedEffect(uiState.successMessage) {
        if ((uiState.successMessage ?: "").startsWith("Deposit successful")) {
            onDepositCompleted()
        }
    }

    val selectedAccount = accountsList.firstOrNull { it.id.toString() == uiState.depositAccountId }
    val effectiveAccount = selectedAccount ?: accountsList.firstOrNull()
    val accountIdValid = (uiState.depositAccountId.toIntOrNull()?.let { it > 0 } == true) || effectiveAccount != null
    val enteredAmount = uiState.depositAmount.toDoubleOrNull() ?: 0.0
    val amountValid = enteredAmount > 0.0
    val summaryAmount = if (amountValid) formatFjd(enteredAmount) else "FJD 0.00"

    LaunchedEffect(effectiveAccount?.id, uiState.depositAccountId) {
        if (uiState.depositAccountId.isBlank() && effectiveAccount != null) {
            featureViewModel.onDepositAccountIdChanged(effectiveAccount.id.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(
                title = "Deposit Money",
                onBack = onBack,
                enabled = canGoBack
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                MessageBanner(
                    text = uiState.errorMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionLabel = "Dismiss",
                    onAction = featureViewModel::clearMessages
                )
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                MessageBanner(
                    text = uiState.successMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    actionLabel = "OK",
                    onAction = featureViewModel::clearMessages
                )
            }

            AccountSelectorCard(
                selectedAccount = effectiveAccount,
                expanded = accountMenuExpanded,
                onExpandedChange = { accountMenuExpanded = it },
                onAccountSelected = {
                    featureViewModel.onDepositAccountIdChanged(it.id.toString())
                    accountMenuExpanded = false
                },
                accountsList = accountsList
            )

            if (accountsList.isEmpty()) {
                MessageBanner(
                    text = "No account found for this customer. Create an account first to deposit.",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionLabel = "OK",
                    onAction = featureViewModel::clearMessages
                )
            }

            SectionLabel(text = "Deposit Amount")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                OutlinedTextField(
                    value = uiState.depositAmount,
                    onValueChange = { featureViewModel.onDepositAmountChanged(sanitizeCurrencyInput(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Enter amount") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            SectionLabel(text = "Note (optional)")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                OutlinedTextField(
                    value = uiState.depositNote,
                    onValueChange = featureViewModel::onDepositNoteChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Add a note") },
                    minLines = 3,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow(label = "Deposit Amount", value = summaryAmount)
                    if (!uiState.depositNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SummaryRow(label = "Note", value = uiState.depositNote)
                    }
                }
            }

            Button(
                onClick = { featureViewModel.deposit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !uiState.isLoading && accountIdValid && amountValid,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AccountSelectorCard(
    selectedAccount: DashboardAccount?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAccountSelected: (DashboardAccount) -> Unit,
    accountsList: List<DashboardAccount>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = "From Account")

        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = accountsList.isNotEmpty()) { onExpandedChange(true) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedAccount?.accountType ?: "Select an account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedAccount?.accountNumber?.let { "•••• ${it.takeLast(4)}" } ?: "Tap to choose from your accounts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedAccount?.let { "Current balance: ${formatFjd(it.balance)}" } ?: "Current balance: account not selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                accountsList.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(account.accountType, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "•••• ${account.accountNumber.takeLast(4)} · ${formatFjd(account.balance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { onAccountSelected(account) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MessageBanner(
    text: String,
    containerColor: Color,
    textColor: Color,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private fun sanitizeCurrencyInput(input: String): String {
    val builder = StringBuilder()
    var dotSeen = false
    input.forEach { char ->
        when {
            char.isDigit() -> builder.append(char)
            char == '.' && !dotSeen -> {
                builder.append(char)
                dotSeen = true
            }
        }
    }
    return builder.toString()
}

private fun formatFjd(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.UK).apply {
        currency = Currency.getInstance("FJD")
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    return formatter.format(amount)
}
