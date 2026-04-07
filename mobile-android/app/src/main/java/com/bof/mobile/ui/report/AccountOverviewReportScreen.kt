package com.bof.mobile.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import kotlin.math.abs

@Composable
fun AccountOverviewReportScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadReport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "User Report",
            subtitle = "Transaction report and insights.",
            onBack = onBack,
            enabled = canGoBack
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
                    OutlinedButton(onClick = { viewModel.loadReport() }) {
                        Text("Retry")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { viewModel.loadReport() }) {
                        Text("Refresh report")
                    }
                }

                uiState.reportOverview?.let { overview ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Customer: ${overview.customerName}", style = MaterialTheme.typography.bodySmall)
                            Text("Account: ${overview.accountNumber}", style = MaterialTheme.typography.bodySmall)
                            Text("Current Balance: FJD ${"%.2f".format(overview.currentBalance)}", style = MaterialTheme.typography.bodySmall)
                            Text("Total Transactions: ${overview.totalTransactions}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (uiState.reportPoints.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transaction data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Transactions by period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val tableScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(tableScroll)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                .padding(vertical = 6.dp)
                        ) {
                            ReportTableHeader("Period", 110.dp)
                            ReportTableHeader("Credit", 120.dp)
                            ReportTableHeader("Debit", 120.dp)
                            ReportTableHeader("Net", 120.dp)
                        }

                        uiState.reportPoints.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
                                    )
                                    .padding(vertical = 6.dp)
                            ) {
                                ReportTableCell(row.period, 110.dp, true)
                                ReportTableCell("FJD ${"%.2f".format(row.credit)}", 120.dp)
                                ReportTableCell("FJD ${"%.2f".format(row.debit)}", 120.dp)
                                ReportTableCell("FJD ${"%.2f".format(row.total)}", 120.dp)
                            }
                        }
                    }

                    VerticalBarGraphCard(
                        title = "Credit Amount by Period",
                        description = "Shows incoming transaction value per period.",
                        points = uiState.reportPoints.map { point ->
                            GraphPoint(
                                label = point.period.takeLast(5),
                                value = point.credit,
                                valueText = "FJD ${formatCompactNumber(point.credit)}"
                            )
                        },
                        accentColor = Color(0xFF1565C0)
                    )

                    VerticalBarGraphCard(
                        title = "Net Movement by Period",
                        description = "Shows net movement after credits and debits for each period.",
                        points = uiState.reportPoints.map { point ->
                            GraphPoint(
                                label = point.period.takeLast(5),
                                value = abs(point.total),
                                valueText = "FJD ${formatCompactNumber(point.total)}"
                            )
                        },
                        accentColor = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportTableHeader(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
private fun ReportTableCell(
    text: String,
    width: Dp,
    bold: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2
    )
}

private data class GraphPoint(
    val label: String,
    val value: Double,
    val valueText: String
)

@Composable
private fun VerticalBarGraphCard(
    title: String,
    description: String,
    points: List<GraphPoint>,
    accentColor: Color
) {
    if (points.isEmpty()) return

    val maxValue = points.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { point ->
                    val ratio = (point.value / maxValue).toFloat().coerceIn(0.03f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = point.valueText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height((140f * ratio).dp)
                                .background(accentColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        )
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactNumber(value: Double): String {
    val absValue = abs(value)
    return when {
        absValue >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000.0)
        absValue >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
        absValue >= 1_000 -> "%.1fK".format(value / 1_000.0)
        else -> "%.2f".format(value)
    }
}
