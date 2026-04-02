package com.bof.mobile.ui.withdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.DashboardAccount
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun WithdrawScreen(
    featureViewModel: FeatureViewModel,
    accountsList: List<DashboardAccount>,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by featureViewModel.uiState.collectAsState()
    var expandedAccounts by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        featureViewModel.clearMessages()
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(
                title = "Withdraw Money",
                subtitle = "Remove funds from your account",
                onBack = onBack,
                enabled = canGoBack
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                MessageBanner(
                    text = uiState.errorMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    OutlinedButton(onClick = { featureViewModel.clearMessages() }) {
                        Text("Dismiss")
                    }
                }
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                MessageBanner(
                    text = uiState.successMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    OutlinedButton(onClick = { featureViewModel.clearMessages() }) {
                        Text("OK")
                    }
                }
            }

            // Withdraw Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!uiState.showWithdrawOtpField) {
                        // Initial Withdraw Form
                        Text(
                            "Select Account",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box {
                            OutlinedTextField(
                                value = accountsList.find { it.id.toString() == uiState.withdrawAccountId }?.accountNumber
                                    ?: "Select an account",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                label = { Text("Account") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.AttachMoney,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = expandedAccounts,
                                onDismissRequest = { expandedAccounts = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                accountsList.forEach { account ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${account.accountNumber} (${account.accountType}) - FJD ${String.format("%.2f", account.balance)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = {
                                            featureViewModel.onWithdrawAccountIdChanged(account.id.toString())
                                            expandedAccounts = false
                                        }
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { expandedAccounts = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {}
                        }

                        // Amount Input
                        Text(
                            "Amount (FJD)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.withdrawAmount,
                            onValueChange = { featureViewModel.onWithdrawAmountChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Amount") },
                            placeholder = { Text("Enter amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        // Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Withdrawal Information",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "✓ Withdrawals are processed immediately\n✓ No fees apply\n✓ High-value withdrawals (> FJD 1000) require OTP verification",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Withdraw Button
                        Button(
                            onClick = { featureViewModel.withdraw() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !uiState.isLoading && uiState.withdrawAccountId.isNotEmpty() && uiState.withdrawAmount.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                            } else {
                                Text("Proceed", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // OTP Verification Form
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.height(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        "OTP Verification Required",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "An OTP has been sent to your registered phone",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // OTP Input
                        Text(
                            "Enter OTP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.withdrawOtp,
                            onValueChange = { featureViewModel.onWithdrawOtpChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enter 6-digit OTP") },
                            placeholder = { Text("000000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Verify Button
                        Button(
                            onClick = { featureViewModel.verifyWithdrawalOtp() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !uiState.isLoading && uiState.withdrawOtp.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.height(24.dp))
                            } else {
                                Text("Verify & Complete", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Cancel Button
                        OutlinedButton(
                            onClick = {
                                featureViewModel.onWithdrawOtpChanged("")
                                featureViewModel.clearMessages()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading
                        ) {
                            Text("Cancel Withdrawal")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageBanner(
    text: String,
    containerColor: Color,
    textColor: Color,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            if (content != null) {
                content()
            }
        }
    }
}
