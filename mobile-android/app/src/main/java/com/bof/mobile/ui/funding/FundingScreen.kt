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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.bof.mobile.viewmodel.FeatureUiState
import com.bof.mobile.viewmodel.FeatureViewModel

private enum class FundingTab {
    INVESTMENT,
    LOAN
}

private val investmentTypes = listOf("Fixed Deposit", "Mutual Fund", "Retirement Plan")
private val loanTypes = listOf("Personal Loan", "Home Loan", "Car Loan")

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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(
                title = "Funding Services",
                subtitle = "Submit investment and loan requests securely.",
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
                                "Choose a service and submit your request.",
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
                                onInvestmentNotesChanged = viewModel::onInvestmentExpectedReturnChanged,
                                onInvestmentMaturityDateChanged = viewModel::onInvestmentMaturityDateChanged,
                                onSubmitInvestment = viewModel::submitFundingInvestment
                            )
                            FundingTab.LOAN -> LoanTab(
                                uiState = uiState,
                                onLoanTypeChanged = viewModel::onLoanProductIdChanged,
                                onLoanRequestedAmountChanged = viewModel::onLoanRequestedAmountChanged,
                                onRepaymentPeriodChanged = viewModel::onLoanTermMonthsChanged,
                                onLoanPurposeChanged = viewModel::onLoanPurposeChanged,
                                onLoanDetailsChanged = viewModel::onLoanOccupationChanged,
                                onSubmitLoan = viewModel::submitFundingLoan
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
    uiState: FeatureUiState,
    onInvestmentTypeChanged: (String) -> Unit,
    onInvestmentAmountChanged: (String) -> Unit,
    onInvestmentNotesChanged: (String) -> Unit,
    onInvestmentMaturityDateChanged: (String) -> Unit,
    onSubmitInvestment: () -> Unit
) {
    Text("Investment Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    FundingTypeDropdown(
        label = "Type",
        value = uiState.investmentType,
        options = investmentTypes,
        onSelected = onInvestmentTypeChanged,
        placeholder = "Select investment type"
    )
    OutlinedTextField(
        value = uiState.investmentAmount,
        onValueChange = onInvestmentAmountChanged,
        label = { Text("Amount") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentExpectedReturn,
        onValueChange = onInvestmentNotesChanged,
        label = { Text("Notes (optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.investmentMaturityDate,
        onValueChange = onInvestmentMaturityDateChanged,
        label = { Text("Duration (months)") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onSubmitInvestment, modifier = Modifier.fillMaxWidth()) {
        Text("Submit investment request")
    }
}

@Composable
private fun LoanTab(
    uiState: FeatureUiState,
    onLoanTypeChanged: (String) -> Unit,
    onLoanRequestedAmountChanged: (String) -> Unit,
    onRepaymentPeriodChanged: (String) -> Unit,
    onLoanPurposeChanged: (String) -> Unit,
    onLoanDetailsChanged: (String) -> Unit,
    onSubmitLoan: () -> Unit,
) {
    Text("Loan Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

    FundingTypeDropdown(
        label = "Type",
        value = uiState.loanProductId,
        options = loanTypes,
        onSelected = onLoanTypeChanged,
        placeholder = "Select loan type"
    )
    OutlinedTextField(
        value = uiState.loanRequestedAmount,
        onValueChange = onLoanRequestedAmountChanged,
        label = { Text("Amount") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanTermMonths,
        onValueChange = onRepaymentPeriodChanged,
        label = { Text("Repayment period (months)") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanPurpose,
        onValueChange = onLoanPurposeChanged,
        label = { Text("Purpose") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.loanOccupation,
        onValueChange = onLoanDetailsChanged,
        label = { Text("Details (optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = onSubmitLoan, modifier = Modifier.fillMaxWidth()) {
        Text("Submit loan request")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FundingTypeDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
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
