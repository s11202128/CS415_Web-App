package com.bof.mobile.ui.features

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel

@Composable
fun FeatureHubScreen(viewModel: FeatureViewModel, customerId: Int, canGoBack: Boolean, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
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
                subtitle = "Manage profile, payments, statements, loans, and investments in one place.",
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

            FeaturePanel("Statements") {
                OutlinedTextField(
                    value = uiState.statementAccountId,
                    onValueChange = viewModel::onStatementAccountIdChanged,
                    label = { Text("Account ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.statementAccountNumber,
                    onValueChange = viewModel::onStatementAccountNumberChanged,
                    label = { Text("Account Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.statementFromDate,
                    onValueChange = viewModel::onStatementFromDateChanged,
                    label = { Text("From date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.statementToDate,
                    onValueChange = viewModel::onStatementToDateChanged,
                    label = { Text("To date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = viewModel::createStatementRequest, modifier = Modifier.fillMaxWidth()) { Text("Submit statement request") }
                OutlinedButton(onClick = viewModel::loadStatementRequests, modifier = Modifier.fillMaxWidth()) { Text("Refresh requests") }

                uiState.statementRequests.take(3).forEach { request ->
                    OutlinedButton(
                        onClick = { viewModel.loadStatementByRequest(request.id) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = request.status.equals("approved", ignoreCase = true)
                    ) {
                        Text("Request #${request.id} ${request.status}")
                    }
                }
                // Statement rows table with horizontal scroll and sticky header
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(scrollState)
                        .padding(bottom = 8.dp)
                ) {
                    // Sticky header row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(vertical = 8.dp),
                    ) {
                        Text("Type", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                        Text("Amount", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
                        Text("Description", modifier = Modifier.width(220.dp), fontWeight = FontWeight.Bold)
                    }
                    // Data rows
                    uiState.statementRows.take(5).forEach { row ->
                        val kind = row.kind ?: row.type ?: "tx"
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(kind.uppercase(), modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                            Text("FJD ${"%.2f".format(row.amount)}", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(row.description, modifier = Modifier.width(220.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            FeaturePanel("Loans") {
                OutlinedButton(onClick = viewModel::loadLoanProducts, modifier = Modifier.fillMaxWidth()) { Text("Refresh loan products") }
                uiState.loanProducts.take(3).forEach { p ->
                    Text("${p.id} ${p.name} ${(p.annualRate * 100).toInt()}%")
                }
                OutlinedTextField(
                    value = uiState.loanProductId,
                    onValueChange = viewModel::onLoanProductIdChanged,
                    label = { Text("Loan product id") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.loanRequestedAmount,
                    onValueChange = viewModel::onLoanRequestedAmountChanged,
                    label = { Text("Requested amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.loanTermMonths,
                    onValueChange = viewModel::onLoanTermMonthsChanged,
                    label = { Text("Term months") },
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
                Button(onClick = viewModel::submitLoanApplication, modifier = Modifier.fillMaxWidth()) {
                    Text("Submit loan application")
                }
                OutlinedButton(onClick = viewModel::loadLoanApplications, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh loan applications")
                }
                uiState.loanApplications.take(3).forEach { item ->
                    Text("Loan #${item.id} ${item.status} FJD ${"%.2f".format(item.requestedAmount)}")
                }
            }

            FeaturePanel("Interest Summaries") {
                OutlinedTextField(
                    value = uiState.selectedYear,
                    onValueChange = viewModel::onSelectedYearChanged,
                    label = { Text("Year") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = viewModel::loadInterestSummaries, modifier = Modifier.fillMaxWidth()) {
                    Text("Load interest summaries")
                }
                uiState.interestSummaries.take(3).forEach { row ->
                    Text("${row.year} Net ${"%.2f".format(row.netInterest)} (${row.status})")
                }
            }

            FeaturePanel("Investments") {
                OutlinedTextField(
                    value = uiState.investmentType,
                    onValueChange = viewModel::onInvestmentTypeChanged,
                    label = { Text("Investment type") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.investmentAmount,
                    onValueChange = viewModel::onInvestmentAmountChanged,
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.investmentExpectedReturn,
                    onValueChange = viewModel::onInvestmentExpectedReturnChanged,
                    label = { Text("Expected return % (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.investmentMaturityDate,
                    onValueChange = viewModel::onInvestmentMaturityDateChanged,
                    label = { Text("Maturity date (YYYY-MM-DD, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = viewModel::createInvestment, modifier = Modifier.fillMaxWidth()) {
                    Text("Create investment")
                }
                OutlinedButton(onClick = { viewModel.loadInvestments(customerId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh investments")
                }
                uiState.investments.take(5).forEach { investment ->
                    Text(
                        "#${investment.id} ${investment.investmentType} FJD ${"%.2f".format(investment.amount)} ${investment.status}"
                    )
                }
            }

            FeaturePanel("Password Reset") {
                OutlinedTextField(
                    value = uiState.resetEmail,
                    onValueChange = viewModel::onResetEmailChanged,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = viewModel::forgotPassword, modifier = Modifier.fillMaxWidth()) {
                    Text("Send reset code")
                }
                OutlinedTextField(
                    value = uiState.resetId,
                    onValueChange = viewModel::onResetIdChanged,
                    label = { Text("Reset ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.resetOtp,
                    onValueChange = viewModel::onResetOtpChanged,
                    label = { Text("OTP") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChanged,
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = viewModel::resetPassword, modifier = Modifier.fillMaxWidth()) {
                    Text("Complete password reset")
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
