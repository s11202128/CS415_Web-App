package com.bof.mobile.ui.transfers

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.bof.mobile.model.TransferMode
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.TransferViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    accountsList: List<DashboardAccount>,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onTransferCompleted: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var fromAccountMenuExpanded by remember { mutableStateOf(false) }
    var destinationMenuExpanded by remember { mutableStateOf(false) }
    var handledSuccessMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accountsList, uiState.fromAccountId) {
        if (uiState.fromAccountId.isBlank() && accountsList.isNotEmpty()) {
            viewModel.onFromAccountIdChanged(accountsList.first().id.toString())
        }
    }

    LaunchedEffect(accountsList, uiState.internalDestinationAccountId, uiState.fromAccountId, uiState.transferMode) {
        if (uiState.transferMode == TransferMode.INTERNAL && uiState.internalDestinationAccountId.isBlank()) {
            val preferredDestination = accountsList.firstOrNull { it.id.toString() != uiState.fromAccountId }
            preferredDestination?.let { viewModel.onInternalDestinationAccountIdChanged(it.id.toString()) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.clearMessages()
    }

    LaunchedEffect(uiState.successMessage, uiState.requiresOtp) {
        val successMessage = uiState.successMessage
        if (successMessage.isNullOrBlank()) {
            handledSuccessMessage = null
        } else if (!uiState.requiresOtp && handledSuccessMessage != successMessage) {
            handledSuccessMessage = successMessage
            onTransferCompleted()
        }
    }

    val selectedFromAccount = accountsList.firstOrNull { it.id.toString() == uiState.fromAccountId }
    val effectiveFromAccount = selectedFromAccount ?: accountsList.firstOrNull()
    val selectedDestinationAccount = accountsList.firstOrNull { it.id.toString() == uiState.internalDestinationAccountId }
    val enteredAmount = uiState.amount.toDoubleOrNull() ?: 0.0
    val transferReady = viewModel.validateTransferReady()
    val amountWithinLimit = enteredAmount in 0.01..uiState.dailyLimit
    val amountShown = if (enteredAmount > 0.0) formatFjd(enteredAmount) else "FJD 0.00"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
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
                title = "Transfer Money",
                subtitle = "Move money between your accounts or to someone else",
                onBack = onBack,
                enabled = canGoBack
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Transfer type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = uiState.transferMode == TransferMode.INTERNAL,
                            onClick = { viewModel.onTransferModeChanged(TransferMode.INTERNAL) },
                            label = { Text("My Accounts") },
                            leadingIcon = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                        FilterChip(
                            selected = uiState.transferMode == TransferMode.EXTERNAL,
                            onClick = { viewModel.onTransferModeChanged(TransferMode.EXTERNAL) },
                            label = { Text("Other Accounts") },
                            leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                    Text(
                        text = if (uiState.transferMode == TransferMode.INTERNAL) {
                            "Send money between your own accounts instantly."
                        } else {
                            "Send money to another person using their bank details."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                MessageBanner(
                    text = uiState.errorMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionLabel = "Dismiss",
                    onAction = viewModel::clearMessages
                )
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                MessageBanner(
                    text = uiState.successMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    actionLabel = "OK",
                    onAction = viewModel::clearMessages
                )
            }

            AccountSelectorCard(
                title = "From account",
                selectedAccount = effectiveFromAccount,
                expanded = fromAccountMenuExpanded,
                enabled = accountsList.isNotEmpty(),
                onExpandedChange = { fromAccountMenuExpanded = it },
                onAccountSelected = {
                    viewModel.onFromAccountIdChanged(it.id.toString())
                    fromAccountMenuExpanded = false
                },
                accountsList = accountsList,
                excludeAccountId = null
            )

            if (uiState.transferMode == TransferMode.INTERNAL) {
                AccountSelectorCard(
                    title = "Destination account",
                    selectedAccount = selectedDestinationAccount,
                    expanded = destinationMenuExpanded,
                    enabled = accountsList.count { it.id.toString() != uiState.fromAccountId } > 0,
                    onExpandedChange = { destinationMenuExpanded = it },
                    onAccountSelected = {
                        viewModel.onInternalDestinationAccountIdChanged(it.id.toString())
                        destinationMenuExpanded = false
                    },
                    accountsList = accountsList,
                    excludeAccountId = uiState.fromAccountId
                )
            } else {
                SectionLabel(text = "External beneficiary")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.recipientName,
                            onValueChange = viewModel::onRecipientNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Recipient name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.bankName,
                            onValueChange = viewModel::onBankNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Bank name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.externalAccountNumber,
                            onValueChange = viewModel::onExternalAccountNumberChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Account number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            SectionLabel(text = "Amount")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.onAmountChanged(sanitizeCurrencyInput(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    label = { Text("Transfer amount") },
                    leadingIcon = { Text("FJD", fontWeight = FontWeight.SemiBold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            SectionLabel(text = "Note (optional)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    label = { Text("Payment note") },
                    minLines = 2,
                    maxLines = 4
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SummaryRow("Source account", effectiveFromAccount?.accountNumber?.let { "•••• ${it.takeLast(4)}" } ?: "Not selected")
                    SummaryRow(
                        "Destination",
                        if (uiState.transferMode == TransferMode.INTERNAL) {
                            selectedDestinationAccount?.accountNumber?.let { "•••• ${it.takeLast(4)}" } ?: "Select account"
                        } else {
                            uiState.recipientName.ifBlank { "External beneficiary" }
                        }
                    )
                    SummaryRow("Amount", amountShown)
                    SummaryRow("Daily limit", formatFjd(uiState.dailyLimit))
                    if (!uiState.note.isBlank()) {
                        SummaryRow("Note", uiState.note)
                    }
                    if (enteredAmount > 1000.0) {
                        SummaryRow("OTP", "Required above FJD 1,000")
                    }
                }
            }

            if (uiState.requiresOtp) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Verify transfer OTP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Enter the code sent to your registered mobile number to complete this transfer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = uiState.otp,
                            onValueChange = { viewModel.onOtpChanged(sanitizeCurrencyInput(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (uiState.requiresOtp) {
                        viewModel.verifyOtp()
                    } else {
                        viewModel.submitTransfer()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !uiState.isLoading && transferReady && amountWithinLimit,
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
                    Text(
                        text = if (uiState.requiresOtp) "Verify & Send" else "Continue",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState.transferMode == TransferMode.INTERNAL && accountsList.count { it.id.toString() != uiState.fromAccountId } == 0) {
                Text(
                    text = "Add another account to transfer internally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!amountWithinLimit && enteredAmount > 0.0) {
                Text(
                    text = "Transfer amount exceeds the daily limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AccountSelectorCard(
    title: String,
    selectedAccount: DashboardAccount?,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAccountSelected: (DashboardAccount) -> Unit,
    accountsList: List<DashboardAccount>,
    excludeAccountId: String?
) {
    val options = accountsList.filter { it.id.toString() != excludeAccountId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(text = title)
        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onExpandedChange(true) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedAccount?.accountType ?: "Select account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedAccount?.let { "•••• ${it.accountNumber.takeLast(4)}" } ?: "Tap to choose",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedAccount?.let { formatFjd(it.balance) } ?: "No account selected",
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
                options.forEach { account ->
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
            Spacer(modifier = Modifier.size(12.dp))
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
