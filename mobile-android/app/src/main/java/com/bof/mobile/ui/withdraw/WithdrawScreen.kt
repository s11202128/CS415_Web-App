package com.bof.mobile.ui.withdraw

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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.OutlinedButton
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
fun WithdrawScreen(
    featureViewModel: FeatureViewModel,
    accountsList: List<DashboardAccount>,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onWithdrawCompleted: () -> Unit = {}
) {
    val uiState by featureViewModel.uiState.collectAsState()
    var accountMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(accountsList, uiState.withdrawAccountId) {
        if (uiState.withdrawAccountId.isBlank() && accountsList.isNotEmpty()) {
            featureViewModel.onWithdrawAccountIdChanged(accountsList.first().id.toString())
        }
    }

    LaunchedEffect(Unit) {
        featureViewModel.clearMessages()
    }

    LaunchedEffect(uiState.successMessage, uiState.showWithdrawOtpField) {
        val success = uiState.successMessage ?: ""
        if (!uiState.showWithdrawOtpField && (
                success.startsWith("Withdrawal successful") ||
                success.startsWith("Withdrawal verified and completed successfully")
            )
        ) {
            onWithdrawCompleted()
        }
    }

    val selectedAccount = accountsList.firstOrNull { it.id.toString() == uiState.withdrawAccountId }
    val effectiveAccount = selectedAccount ?: accountsList.firstOrNull()
    val currentBalance = effectiveAccount?.balance ?: 0.0
    val enteredAmount = uiState.withdrawAmount.toDoubleOrNull() ?: 0.0
    val amountValid = enteredAmount > 0.0
    val withinBalance = enteredAmount <= currentBalance
    val hasValidAccount = effectiveAccount != null
    val remainingBalance = if (hasValidAccount) currentBalance - enteredAmount else 0.0

    LaunchedEffect(effectiveAccount?.id, uiState.withdrawAccountId) {
        if (uiState.withdrawAccountId.isBlank() && effectiveAccount != null) {
            featureViewModel.onWithdrawAccountIdChanged(effectiveAccount.id.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(
                title = "Withdraw Money",
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

            SectionLabel("From Account")

            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = accountsList.isNotEmpty()) { accountMenuExpanded = true },
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = effectiveAccount?.accountType ?: "Select an account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = effectiveAccount?.accountNumber?.let { "•••• ${it.takeLast(4)}" } ?: "Tap to choose from your accounts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current balance: ${formatFjd(currentBalance)}",
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
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false }
                ) {
                    accountsList.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                androidx.compose.foundation.layout.Column {
                                    Text(account.accountType, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "•••• ${account.accountNumber.takeLast(4)} · ${formatFjd(account.balance)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                featureViewModel.onWithdrawAccountIdChanged(account.id.toString())
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }

            SectionLabel("Withdrawal Amount")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                OutlinedTextField(
                    value = uiState.withdrawAmount,
                    onValueChange = { featureViewModel.onWithdrawAmountChanged(sanitizeCurrencyInput(it)) },
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

            Text(
                text = "Available Balance: ${formatFjd(currentBalance)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            SectionLabel("Note (optional)")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                OutlinedTextField(
                    value = uiState.withdrawNote,
                    onValueChange = featureViewModel::onWithdrawNoteChanged,
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
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Verification (if needed)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "OTP verification is required for withdrawals over FJD 1,000. A code will be sent to your registered mobile number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.large
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryRow("Withdrawal Amount", formatFjd(enteredAmount))
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("Remaining Balance", formatFjd(remainingBalance))
                }
            }

            Button(
                onClick = {
                    if (enteredAmount > 1000 && !uiState.showWithdrawOtpField) {
                        featureViewModel.withdraw()
                    } else if (uiState.showWithdrawOtpField) {
                        featureViewModel.verifyWithdrawalOtp()
                    } else {
                        featureViewModel.withdraw()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !uiState.isLoading && hasValidAccount && amountValid && withinBalance,
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
                        text = if (uiState.showWithdrawOtpField) "Verify & Complete" else "Continue",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!withinBalance && amountValid) {
                Text(
                    text = "Withdrawal amount exceeds available balance.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (uiState.showWithdrawOtpField) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "OTP verification required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "A code has been sent to your registered mobile number. Enter it below to complete the withdrawal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = uiState.withdrawOtp,
                            onValueChange = { featureViewModel.onWithdrawOtpChanged(sanitizeCurrencyInput(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedButton(
                            onClick = {
                                featureViewModel.onWithdrawOtpChanged("")
                                featureViewModel.clearMessages()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading
                        ) {
                            Text("Cancel OTP")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
