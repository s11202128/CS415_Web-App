package com.bof.mobile.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.BankStatementTransaction
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TransactionDescriptionParts(
    val provider: String,
    val service: String
)

@Composable
fun ActivityLogScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions = uiState.statementTransactions.sortedByDescending { it.date }

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadCustomerAccounts()
        viewModel.initializeStatementDateDefaults()
    }

    LaunchedEffect(
        uiState.customerAccountsLoaded,
        uiState.statementAccountId,
        uiState.statementFromDate,
        uiState.statementToDate
    ) {
        if (
            uiState.customerAccountsLoaded &&
            uiState.statementAccountId.isNotBlank() &&
            uiState.statementFromDate.isNotBlank() &&
            uiState.statementToDate.isNotBlank()
        ) {
            viewModel.loadBankStatement()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Transaction History",
            subtitle = "Your account transactions with separate credit and debit columns.",
            onBack = onBack,
            enabled = canGoBack
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (!uiState.isLoading && transactions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No transactions found for the selected period",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(vertical = 10.dp)
                ) {
                    Text("Date", modifier = Modifier.weight(1.5f).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Description", modifier = Modifier.weight(1.9f).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Credit", modifier = Modifier.weight(1.2f).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.End)
                    Text("Debit", modifier = Modifier.weight(1.2f).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.End)
                    Text("Balance", modifier = Modifier.weight(1.3f).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.End)
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    itemsIndexed(items = transactions, key = { _, item -> item.id }) { index, item ->
                        val direction = resolveTransactionDirection(item)
                        val isCredit = direction == TransactionDirection.CREDIT
                        val isDebit = direction == TransactionDirection.DEBIT
                        val creditAmount = if (isCredit) item.amount else 0.0
                        val debitAmount = if (isDebit) item.amount else 0.0
                        val descriptionParts = parseTransactionDescription(item.description)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatTransactionDate(item.date),
                                modifier = Modifier.weight(1.5f).padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.weight(1.9f).padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    descriptionParts.provider,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    descriptionParts.service,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatAmount(creditAmount),
                                modifier = Modifier.weight(1.2f).padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCredit) CreditAmountColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )
                            Text(
                                formatAmount(debitAmount),
                                modifier = Modifier.weight(1.2f).padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDebit) DebitAmountColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )
                            Text(
                                formatBalanceAmount(item.balance),
                                modifier = Modifier.weight(1.3f).padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class TransactionDirection {
    CREDIT,
    DEBIT
}

private val CreditAmountColor = Color(0xFF2E7D32)
private val DebitAmountColor = Color(0xFFC62828)

private fun resolveTransactionDirection(item: BankStatementTransaction): TransactionDirection {
    val normalized = "${item.transactionType} ${item.description}".lowercase()
    return when {
        normalized.contains("debit") ||
            normalized.contains("withdraw") ||
            normalized.contains("transfer out") ||
            normalized.contains("transfer_out") ||
            normalized.contains("bill") ||
            normalized.contains("payment") ||
            normalized.contains("fee") -> TransactionDirection.DEBIT
        normalized.contains("credit") ||
            normalized.contains("deposit") ||
            normalized.contains("refund") ||
            normalized.contains("interest") ||
            normalized.contains("transfer in") ||
            normalized.contains("transfer_in") ||
            normalized.contains("incoming") -> TransactionDirection.CREDIT
        item.amount < 0 -> TransactionDirection.DEBIT
        else -> TransactionDirection.CREDIT
    }
}

private fun formatAmount(value: Double): String {
    return "$${"%.2f".format(value)}"
}

private fun formatBalanceAmount(value: Double): String {
    val prefix = if (value < 0) "-" else ""
    return "$prefix${formatAmount(kotlin.math.abs(value))}"
}

private fun formatTransactionDate(rawDate: String): String {
    val fijiZone = ZoneId.of("Pacific/Fiji")
    val outputFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.ENGLISH)

    val parsed = runCatching {
        Instant.parse(rawDate).atZone(fijiZone).format(outputFormatter)
    }.recoverCatching {
        OffsetDateTime.parse(rawDate).atZoneSameInstant(fijiZone).format(outputFormatter)
    }.recoverCatching {
        LocalDateTime.parse(rawDate).atZone(fijiZone).format(outputFormatter)
    }.getOrNull()

    return parsed ?: rawDate
}

private fun parseTransactionDescription(raw: String): TransactionDescriptionParts {
    val normalized = raw.trim()
    if (normalized.isBlank()) {
        return TransactionDescriptionParts(provider = "UNKNOWN", service = "Service")
    }

    val pieces = normalized.split(" - ", limit = 2)
    val leftPart = pieces.getOrNull(0).orEmpty().trim()
    val rightPart = pieces.getOrNull(1).orEmpty().trim()

    val providerSource = if (leftPart.contains(" to ", ignoreCase = true)) {
        leftPart.substringAfter(" to ", "").trim()
    } else {
        leftPart
    }

    val provider = when {
        providerSource.contains("energy fiji", ignoreCase = true) -> "EFL"
        providerSource.contains("digicel", ignoreCase = true) -> "DIGICEL"
        providerSource.contains("water authority", ignoreCase = true) -> "WAF"
        providerSource.isNotBlank() -> providerSource.uppercase(Locale.ENGLISH)
        else -> "UNKNOWN"
    }

    val service = when {
        rightPart.isNotBlank() -> rightPart
        normalized.contains("electric", ignoreCase = true) || normalized.contains("energy", ignoreCase = true) -> "Electricity"
        normalized.contains("water", ignoreCase = true) -> "Water"
        normalized.contains("sky pacific", ignoreCase = true) -> "Sky Pacific"
        normalized.contains("phone", ignoreCase = true) || normalized.contains("mobile", ignoreCase = true) -> "Phone Service"
        else -> "Service"
    }

    return TransactionDescriptionParts(provider = provider, service = service)
}
