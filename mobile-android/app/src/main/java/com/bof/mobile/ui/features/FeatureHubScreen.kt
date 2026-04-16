package com.bof.mobile.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel

@Composable
fun FeatureHubScreen(viewModel: FeatureViewModel, customerId: Int, canGoBack: Boolean, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadLoanProducts()
        viewModel.loadLoanApplications()
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
                title = "More Features",
                subtitle = "Apply for loans and manage account services in one place.",
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
                    OutlinedButton(onClick = viewModel::clearMessages) { Text("Clear") }
                }
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                MessageBanner(
                    text = uiState.successMessage ?: "",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            FeaturePanel("Loan Application") {
                var productMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedTextField(
                        value = uiState.loanProductId,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loan product") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { productMenuExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { productMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = productMenuExpanded,
                        onDismissRequest = { productMenuExpanded = false }
                    ) {
                        uiState.loanProducts.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(product.name)
                                        Text(
                                            "Rate ${"%.2f".format(product.annualRate)}% | ${product.minTermMonths}-${product.maxTermMonths} months",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.onLoanProductIdChanged(product.id)
                                    productMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.loanRequestedAmount,
                    onValueChange = viewModel::onLoanRequestedAmountChanged,
                    label = { Text("Requested amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.loanTermMonths,
                    onValueChange = viewModel::onLoanTermMonthsChanged,
                    label = { Text("Term (months)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.loanPurpose,
                    onValueChange = viewModel::onLoanPurposeChanged,
                    label = { Text("Purpose") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.loanMonthlyIncome,
                    onValueChange = viewModel::onLoanMonthlyIncomeChanged,
                    label = { Text("Monthly income") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.loanOccupation,
                    onValueChange = viewModel::onLoanOccupationChanged,
                    label = { Text("Occupation") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::submitLoanApplication, modifier = Modifier.weight(1f)) {
                        Text("Submit Loan")
                    }
                    OutlinedButton(onClick = viewModel::loadLoanApplications, modifier = Modifier.weight(1f)) {
                        Text("Refresh")
                    }
                }

                uiState.loanApplications.take(3).forEach { loan ->
                    val productName = uiState.loanProducts.firstOrNull { it.id == loan.loanProductId }?.name
                        ?: loan.loanProductId
                    Text(
                        "$productName • FJD ${"%.2f".format(loan.requestedAmount)} • ${loan.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeaturePanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                content()
            }
        )
    }
}

@Composable
private fun MessageBanner(
    text: String,
    containerColor: Color,
    textColor: Color,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}
