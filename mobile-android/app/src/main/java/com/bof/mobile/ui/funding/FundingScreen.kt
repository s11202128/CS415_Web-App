package com.bof.mobile.ui.funding

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel

private enum class FundingTab {
    INVESTMENT,
    LOAN
}

@Composable
fun FundingScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeTab by remember { mutableStateOf(FundingTab.INVESTMENT) }

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadLoanProducts()
        viewModel.loadLoanApplications()
        viewModel.loadInvestments(customerId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(
                title = "Funding",
                subtitle = "Manage your investments and loans in one place.",
                onBack = onBack,
                enabled = canGoBack
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            Text("Funding Dashboard", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Switch between Investment and Loan tabs.",
                                color = Color.White.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                FundingTabCard(
                                    icon = "📈",
                                    title = "Investment",
                                    selected = activeTab == FundingTab.INVESTMENT,
                                    onClick = { activeTab = FundingTab.INVESTMENT },
                                    modifier = Modifier.weight(1f)
                                )
                                FundingTabCard(
                                    icon = "🏦",
                                    title = "Loan",
                                    selected = activeTab == FundingTab.LOAN,
                                    onClick = { activeTab = FundingTab.LOAN },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        if (!uiState.errorMessage.isNullOrBlank()) {
                            FundingBanner(
                                text = uiState.errorMessage ?: "",
                                isError = true,
                                onAction = viewModel::clearMessages,
                                actionLabel = "Clear"
                            )
                        }

                        if (!uiState.successMessage.isNullOrBlank()) {
                            FundingBanner(text = uiState.successMessage ?: "", isError = false)
                        }

                        when (activeTab) {
                            FundingTab.INVESTMENT -> InvestmentTab(
                                uiState = uiState,
                                onInvestmentTypeChanged = viewModel::onInvestmentTypeChanged,
                                onInvestmentAmountChanged = viewModel::onInvestmentAmountChanged,
                                onInvestmentExpectedReturnChanged = viewModel::onInvestmentExpectedReturnChanged,
                                onInvestmentMaturityDateChanged = viewModel::onInvestmentMaturityDateChanged,
                                onCreateInvestment = viewModel::createInvestment,
                                onLoadInvestments = { viewModel.loadInvestments(customerId) }
                            )
                            FundingTab.LOAN -> LoanTab(
                                uiState = uiState,
                                onLoanProductIdChanged = viewModel::onLoanProductIdChanged,
                                onLoanRequestedAmountChanged = viewModel::onLoanRequestedAmountChanged,
                                onLoanTermMonthsChanged = viewModel::onLoanTermMonthsChanged,
                                onLoanPurposeChanged = viewModel::onLoanPurposeChanged,
                                onLoanMonthlyIncomeChanged = viewModel::onLoanMonthlyIncomeChanged,
                                onLoanOccupationChanged = viewModel::onLoanOccupationChanged,
                                onLoadLoanProducts = viewModel::loadLoanProducts,
                                onSubmitLoan = viewModel::submitLoanApplication,
                                onLoadLoanApplications = viewModel::loadLoanApplications
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentTab(
    uiState: com.bof.mobile.viewmodel.FeatureUiState,
    onInvestmentTypeChanged: (String) -> Unit,
    onInvestmentAmountChanged: (String) -> Unit,
    onInvestmentExpectedReturnChanged: (String) -> Unit,
    onInvestmentMaturityDateChanged: (String) -> Unit,
    onCreateInvestment: () -> Unit,
    onLoadInvestments: () -> Unit
) {
    Text("Investment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    OutlinedTextField(
        value = uiState.investmentType,
        onValueChange = onInvestmentTypeChanged,
        label = { Text("Investment type") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentAmount,
        onValueChange = onInvestmentAmountChanged,
        label = { Text("Amount") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentExpectedReturn,
        onValueChange = onInvestmentExpectedReturnChanged,
        label = { Text("Expected return % (optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentMaturityDate,
        onValueChange = onInvestmentMaturityDateChanged,
        label = { Text("Maturity date (YYYY-MM-DD, optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onCreateInvestment, modifier = Modifier.fillMaxWidth()) {
        Text("Create investment")
    }
    OutlinedButton(onClick = onLoadInvestments, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh investments")
    }

    uiState.investments.take(6).forEach { investment ->
        Text(
            "#${investment.id} ${investment.investmentType} FJD ${"%.2f".format(investment.amount)} ${investment.status}"
        )
    }
}

@Composable
private fun LoanTab(
    uiState: com.bof.mobile.viewmodel.FeatureUiState,
    onLoanProductIdChanged: (String) -> Unit,
    onLoanRequestedAmountChanged: (String) -> Unit,
    onLoanTermMonthsChanged: (String) -> Unit,
    onLoanPurposeChanged: (String) -> Unit,
    onLoanMonthlyIncomeChanged: (String) -> Unit,
    onLoanOccupationChanged: (String) -> Unit,
    onLoadLoanProducts: () -> Unit,
    onSubmitLoan: () -> Unit,
    onLoadLoanApplications: () -> Unit
) {
    Text("Loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    OutlinedButton(onClick = onLoadLoanProducts, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh loan products")
    }

    uiState.loanProducts.take(3).forEach { product ->
        Text("${product.id} ${product.name} ${(product.annualRate * 100).toInt()}%")
    }

    OutlinedTextField(
        value = uiState.loanProductId,
        onValueChange = onLoanProductIdChanged,
        label = { Text("Loan product id") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanRequestedAmount,
        onValueChange = onLoanRequestedAmountChanged,
        label = { Text("Requested amount") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanTermMonths,
        onValueChange = onLoanTermMonthsChanged,
        label = { Text("Term months") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanPurpose,
        onValueChange = onLoanPurposeChanged,
        label = { Text("Purpose") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanMonthlyIncome,
        onValueChange = onLoanMonthlyIncomeChanged,
        label = { Text("Monthly income") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanOccupation,
        onValueChange = onLoanOccupationChanged,
        label = { Text("Occupation") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onSubmitLoan, modifier = Modifier.fillMaxWidth()) {
        Text("Submit loan application")
    }
    OutlinedButton(onClick = onLoadLoanApplications, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh loan applications")
    }

    uiState.loanApplications.take(6).forEach { application ->
        Text("Loan #${application.id} ${application.status} FJD ${"%.2f".format(application.requestedAmount)}")
    }
}

@Composable
private fun FundingTabCard(
    icon: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large
) {
    Card(
        modifier = modifier.height(92.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(
                text = if (selected) "Open" else "Tap to open",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun FundingBanner(
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
