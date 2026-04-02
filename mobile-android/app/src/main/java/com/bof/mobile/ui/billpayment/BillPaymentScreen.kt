package com.bof.mobile.ui.billpayment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel

@Composable
fun BillPaymentScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadScheduledBills()
        viewModel.loadBillHistory()
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
                title = "Bill Payment",
                subtitle = "Pay now, schedule bills, and review payment history.",
                onBack = onBack,
                enabled = canGoBack
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                BillMessageBanner(
                    text = uiState.errorMessage ?: "",
                    isError = true,
                    onAction = viewModel::clearMessages,
                    actionLabel = "Clear"
                )
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                BillMessageBanner(
                    text = uiState.successMessage ?: "",
                    isError = false
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Bill Payment Form", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = uiState.billAccountId,
                        onValueChange = viewModel::onBillAccountIdChanged,
                        label = { Text("Account ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.billPayee,
                        onValueChange = viewModel::onBillPayeeChanged,
                        label = { Text("Payee") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.billAmount,
                        onValueChange = viewModel::onBillAmountChanged,
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.scheduledDate,
                        onValueChange = viewModel::onScheduledDateChanged,
                        label = { Text("Scheduled date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(onClick = viewModel::payBillManual, modifier = Modifier.fillMaxWidth()) {
                        Text("Pay bill now")
                    }
                    OutlinedButton(onClick = viewModel::scheduleBill, modifier = Modifier.fillMaxWidth()) {
                        Text("Schedule bill")
                    }
                    OutlinedButton(onClick = viewModel::loadScheduledBills, modifier = Modifier.fillMaxWidth()) {
                        Text("Refresh scheduled")
                    }
                    OutlinedButton(onClick = viewModel::loadBillHistory, modifier = Modifier.fillMaxWidth()) {
                        Text("Refresh bill history")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Scheduled Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (uiState.scheduledBills.isEmpty()) {
                        Text("No scheduled bills", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.scheduledBills.take(6).forEach { bill ->
                            OutlinedButton(
                                onClick = { viewModel.runScheduledBill(bill.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Run #${bill.id} ${bill.payee} FJD ${"%.2f".format(bill.amount)}")
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Bill History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (uiState.billHistory.isEmpty()) {
                        Text("No bill payments yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.billHistory.take(8).forEach { bill ->
                            Text("${bill.status.uppercase()} ${bill.payee} FJD ${"%.2f".format(bill.amount)}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillMessageBanner(
    text: String,
    isError: Boolean,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (onAction != null && !actionLabel.isNullOrBlank()) {
                OutlinedButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
