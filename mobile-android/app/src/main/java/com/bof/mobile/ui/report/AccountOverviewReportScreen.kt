package com.bof.mobile.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import kotlin.math.max

private enum class ReportChartMode {
    CREDIT_DEBIT,
    NET_TOTAL
}

@Composable
fun AccountOverviewReportScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var chartMode by remember { mutableStateOf(ReportChartMode.CREDIT_DEBIT) }

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
            title = "Account Overview & Report",
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
                Text("Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    ChartModeToggle(
                        selectedMode = chartMode,
                        onModeChange = { chartMode = it }
                    )

                    if (chartMode == ReportChartMode.CREDIT_DEBIT) {
                        Text(
                            "Monthly credits vs debits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ReportBarChart(
                            valuesA = uiState.reportPoints.map { it.credit },
                            valuesB = uiState.reportPoints.map { it.debit },
                            colorA = Color(0xFF1B8F47),
                            colorB = Color(0xFFC73737)
                        )
                    } else {
                        Text(
                            "Monthly net total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ReportSingleSeriesChart(
                            values = uiState.reportPoints.map { it.total }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartModeToggle(
    selectedMode: ReportChartMode,
    onModeChange: (ReportChartMode) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedMode == ReportChartMode.CREDIT_DEBIT,
            onClick = { onModeChange(ReportChartMode.CREDIT_DEBIT) },
            label = { Text("Credits vs Debits") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == ReportChartMode.NET_TOTAL,
            onClick = { onModeChange(ReportChartMode.NET_TOTAL) },
            label = { Text("Net Total") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReportBarChart(
    valuesA: List<Double>,
    valuesB: List<Double>,
    colorA: Color,
    colorB: Color
) {
    val maxValue = max(
        valuesA.maxOrNull() ?: 0.0,
        valuesB.maxOrNull() ?: 0.0
    ).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        val barGroups = max(valuesA.size, valuesB.size)
        if (barGroups == 0) {
            return@Canvas
        }

        val groupWidth = size.width / barGroups
        val barWidth = groupWidth * 0.32f

        repeat(barGroups) { idx ->
            val credit = (valuesA.getOrNull(idx) ?: 0.0).toFloat()
            val debit = (valuesB.getOrNull(idx) ?: 0.0).toFloat()

            val creditHeight = (credit / maxValue.toFloat()) * size.height
            val debitHeight = (debit / maxValue.toFloat()) * size.height

            val xBase = idx * groupWidth
            val creditLeft = xBase + groupWidth * 0.14f
            val debitLeft = xBase + groupWidth * 0.54f

            drawRoundRect(
                color = colorA,
                topLeft = Offset(creditLeft, size.height - creditHeight),
                size = Size(barWidth, creditHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            drawRoundRect(
                color = colorB,
                topLeft = Offset(debitLeft, size.height - debitHeight),
                size = Size(barWidth, debitHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
private fun ReportSingleSeriesChart(values: List<Double>) {
    val positiveMax = values.maxOrNull() ?: 0.0
    val negativeMin = values.minOrNull() ?: 0.0
    val absMax = max(kotlin.math.abs(positiveMax), kotlin.math.abs(negativeMin)).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (values.isEmpty()) return@Canvas

        val zeroY = size.height / 2f
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(0f, zeroY),
            end = Offset(size.width, zeroY),
            strokeWidth = 2f
        )

        val groupWidth = size.width / values.size
        val barWidth = groupWidth * 0.5f

        values.forEachIndexed { idx, raw ->
            val value = raw.toFloat()
            val scaled = (kotlin.math.abs(value) / absMax.toFloat()) * (size.height * 0.42f)
            val left = idx * groupWidth + (groupWidth - barWidth) / 2f
            val top = if (value >= 0f) zeroY - scaled else zeroY
            val height = scaled

            drawRoundRect(
                color = if (value >= 0f) Color(0xFF0F766E) else Color(0xFFB45309),
                topLeft = Offset(left, top),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}
