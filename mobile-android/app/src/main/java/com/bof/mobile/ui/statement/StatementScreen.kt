package com.bof.mobile.ui.statement

import android.app.DatePickerDialog
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatementScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.initializeStatementDateDefaults()
        viewModel.loadBankStatement()
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
                title = "Bank Statement",
                subtitle = "View all your account transactions and download PDF.",
                onBack = onBack,
                enabled = canGoBack
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF1976D2), Color(0xFF5E35B1))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Customer Statement", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Pick your from and to dates to generate your statement.",
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                StatementBanner(
                    text = uiState.errorMessage ?: "",
                    isError = true,
                    onAction = viewModel::clearMessages,
                    actionLabel = "Clear"
                )
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                StatementBanner(text = uiState.successMessage ?: "", isError = false)
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
                    Text("Date Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = uiState.statementFromDate,
                        onValueChange = viewModel::onStatementFromDateChanged,
                        readOnly = false,
                        enabled = !uiState.isLoading,
                        label = { Text("From Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isLoading) {
                                val defaultDate = runCatching { LocalDate.parse(uiState.statementFromDate, dateFormatter) }
                                    .getOrDefault(LocalDate.now().minusMonths(1))
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                        viewModel.onStatementFromDateChanged(selected.format(dateFormatter))
                                    },
                                    defaultDate.year,
                                    defaultDate.monthValue - 1,
                                    defaultDate.dayOfMonth
                                ).show()
                            }
                    )

                    OutlinedTextField(
                        value = uiState.statementToDate,
                        onValueChange = viewModel::onStatementToDateChanged,
                        readOnly = false,
                        enabled = !uiState.isLoading,
                        label = { Text("To Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isLoading) {
                                val defaultDate = runCatching { LocalDate.parse(uiState.statementToDate, dateFormatter) }
                                    .getOrDefault(LocalDate.now())
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                        viewModel.onStatementToDateChanged(selected.format(dateFormatter))
                                    },
                                    defaultDate.year,
                                    defaultDate.monthValue - 1,
                                    defaultDate.dayOfMonth
                                ).show()
                            }
                    )

                    Button(
                        onClick = viewModel::loadBankStatement,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isLoading) "Generating..." else "Generate Statement")
                    }

                    OutlinedButton(
                        enabled = !uiState.isLoading,
                        onClick = {
                            viewModel.downloadBankStatementPdf { bytes, fileName ->
                                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                                val output = File(downloadDir, fileName)
                                output.writeBytes(bytes)
                                Toast.makeText(
                                    context,
                                    "PDF saved: ${output.absolutePath}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isLoading) "Please wait..." else "Download PDF")
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
                    Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (uiState.statementTransactions.isEmpty()) {
                        Text("No transactions found for the selected period", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.statementTransactions.forEach { row ->
                            Text(
                                text = "${row.date.take(10)}  ${row.transactionType.uppercase()}  FJD ${"%.2f".format(row.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${row.description} | Bal: FJD ${"%.2f".format(row.balance)} | Acct: ${row.accountNumber}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementBanner(
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
