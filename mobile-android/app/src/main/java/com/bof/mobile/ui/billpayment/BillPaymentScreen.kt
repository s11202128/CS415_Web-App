package com.bof.mobile.ui.billpayment

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.BillPaymentRequest
import com.bof.mobile.model.AccountItem
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CardCorner = RoundedCornerShape(24.dp)
private val SectionHeaderColors = Brush.horizontalGradient(
    listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
)

@Composable
fun BillPaymentScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    var manualPayeeName by rememberSaveable { mutableStateOf("") }
    var manualBillAmount by rememberSaveable { mutableStateOf("") }
    var manualBillType by rememberSaveable { mutableStateOf("") }
    var manualPaymentMethod by rememberSaveable { mutableStateOf("") }
    var manualNote by rememberSaveable { mutableStateOf("") }
    var manualError by rememberSaveable { mutableStateOf<String?>(null) }

    var schedulePayeeName by rememberSaveable { mutableStateOf("") }
    var scheduleBillAmount by rememberSaveable { mutableStateOf("") }
    var scheduleBillType by rememberSaveable { mutableStateOf("") }
    var schedulePaymentMethod by rememberSaveable { mutableStateOf("") }
    var schedulePaymentDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).format(dateFormatter)) }
    var scheduleRepeat by rememberSaveable { mutableStateOf("") }
    var scheduleNote by rememberSaveable { mutableStateOf("") }
    var scheduleError by rememberSaveable { mutableStateOf<String?>(null) }

    val billTypes = remember { listOf("Utilities", "Internet", "Rent", "Insurance", "Subscription", "Other") }
    val paymentMethods = remember { listOf("Account Balance", "Debit Card", "Credit Card", "Bank Transfer") }
    val repeatOptions = remember { listOf("One-time", "Daily", "Weekly", "Monthly") }

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadAccounts()
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
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(
                title = "Bill Payment",
                subtitle = "Use the two form panels below to pay now or schedule later.",
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

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Manual Bill Payment") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Schedule Bill Payment") }
                )
            }

            when (selectedTab) {
                0 -> ManualBillPage(
                    payeeName = manualPayeeName,
                    billAmount = manualBillAmount,
                    billType = manualBillType,
                    paymentMethod = manualPaymentMethod,
                    note = manualNote,
                    billTypes = billTypes,
                    paymentMethods = paymentMethods,
                    accounts = uiState.accounts,
                    selectedAccount = uiState.selectedAccount,
                    isLoading = uiState.isLoading,
                    errorText = manualError,
                    onAccountSelected = viewModel::selectAccount,
                    onPayeeNameChange = {
                        manualPayeeName = it
                        manualError = null
                    },
                    onBillAmountChange = {
                        manualBillAmount = it
                        manualError = null
                    },
                    onBillTypeChange = {
                        manualBillType = it
                        manualError = null
                    },
                    onPaymentMethodChange = {
                        manualPaymentMethod = it
                        manualError = null
                    },
                    onNoteChange = {
                        manualNote = it
                        manualError = null
                    },
                    onSubmit = {
                        val validationError = validateBillForm(
                            payeeName = manualPayeeName,
                            accountNumber = uiState.selectedAccount?.accountNumber.orEmpty(),
                            billAmount = manualBillAmount,
                            billType = manualBillType,
                            paymentMethod = manualPaymentMethod
                        )
                        if (validationError != null) {
                            manualError = validationError
                            return@ManualBillPage
                        }
                        val selectedAccount = uiState.selectedAccount
                        if (selectedAccount == null) {
                                        manualError = "Select a valid account number"
                                        return@ManualBillPage
                                    }
                        viewModel.payBillManual(
                            BillPaymentRequest(
                                accountId = selectedAccount.id,
                                accountNumber = selectedAccount.accountNumber,
                                payee = manualPayeeName.trim(),
                                amount = manualBillAmount.trim().toDouble(),
                                billType = manualBillType,
                                paymentMethod = manualPaymentMethod,
                                note = manualNote.ifBlank { null }
                            )
                        )
                    }
                )

                1 -> ScheduleBillPage(
                    payeeName = schedulePayeeName,
                    billAmount = scheduleBillAmount,
                    billType = scheduleBillType,
                    paymentMethod = schedulePaymentMethod,
                    paymentDate = schedulePaymentDate,
                    repeat = scheduleRepeat,
                    note = scheduleNote,
                    billTypes = billTypes,
                    paymentMethods = paymentMethods,
                    repeatOptions = repeatOptions,
                    accounts = uiState.accounts,
                    selectedAccount = uiState.selectedAccount,
                    isLoading = uiState.isLoading,
                    errorText = scheduleError,
                    onAccountSelected = viewModel::selectAccount,
                    onPayeeNameChange = {
                        schedulePayeeName = it
                        scheduleError = null
                    },
                    onBillAmountChange = {
                        scheduleBillAmount = it
                        scheduleError = null
                    },
                    onBillTypeChange = {
                        scheduleBillType = it
                        scheduleError = null
                    },
                    onPaymentMethodChange = {
                        schedulePaymentMethod = it
                        scheduleError = null
                    },
                    onPaymentDateChange = {
                        schedulePaymentDate = it
                        scheduleError = null
                    },
                    onRepeatChange = {
                        scheduleRepeat = it
                        scheduleError = null
                    },
                    onNoteChange = {
                        scheduleNote = it
                        scheduleError = null
                    },
                    onDatePickerRequest = {
                        showDatePicker(
                            context = context,
                            currentValue = schedulePaymentDate,
                            formatter = dateFormatter,
                            onDateSelected = { selectedDate ->
                                schedulePaymentDate = selectedDate
                                scheduleError = null
                            }
                        )
                    },
                    onSubmit = {
                        val validationError = validateScheduledBillForm(
                            payeeName = schedulePayeeName,
                            accountNumber = uiState.selectedAccount?.accountNumber.orEmpty(),
                            billAmount = scheduleBillAmount,
                            billType = scheduleBillType,
                            paymentMethod = schedulePaymentMethod,
                            paymentDate = schedulePaymentDate,
                            repeat = scheduleRepeat
                        )
                        if (validationError != null) {
                            scheduleError = validationError
                            return@ScheduleBillPage
                        }
                        val selectedAccount = uiState.selectedAccount
                        if (selectedAccount == null) {
                                        scheduleError = "Select a valid account number"
                                        return@ScheduleBillPage
                                    }
                        viewModel.scheduleBill(
                            BillPaymentRequest(
                                accountId = selectedAccount.id,
                                accountNumber = selectedAccount.accountNumber,
                                payee = schedulePayeeName.trim(),
                                amount = scheduleBillAmount.trim().toDouble(),
                                scheduledDate = schedulePaymentDate.trim(),
                                billType = scheduleBillType,
                                paymentMethod = schedulePaymentMethod,
                                note = scheduleNote.ifBlank { null },
                                repeat = scheduleRepeat,
                                paymentDate = schedulePaymentDate.trim()
                            )
                        )
                    },
                    scheduledBills = uiState.scheduledBills,
                    onRunScheduledBill = viewModel::runScheduledBill,
                    onRefreshScheduled = viewModel::loadScheduledBills,
                    onRefreshHistory = viewModel::loadBillHistory
                )
            }
        }
    }
}

@Composable
private fun ManualBillPage(
    payeeName: String,
    billAmount: String,
    billType: String,
    paymentMethod: String,
    note: String,
    billTypes: List<String>,
    paymentMethods: List<String>,
    accounts: List<AccountItem>,
    selectedAccount: AccountItem?,
    isLoading: Boolean,
    errorText: String?,
    onAccountSelected: (AccountItem) -> Unit,
    onPayeeNameChange: (String) -> Unit,
    onBillAmountChange: (String) -> Unit,
    onBillTypeChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    BillSectionCard(
        title = "Manual Bill Payment",
        subtitle = "Open the manual payment form and submit an immediate bill payment."
    ) {
        ManualBillPaymentForm(
            payeeName = payeeName,
            billAmount = billAmount,
            billType = billType,
            paymentMethod = paymentMethod,
            note = note,
            billTypes = billTypes,
            paymentMethods = paymentMethods,
            accounts = accounts,
            selectedAccount = selectedAccount,
            isLoading = isLoading,
            onAccountSelected = onAccountSelected,
            onPayeeNameChange = onPayeeNameChange,
            onBillAmountChange = onBillAmountChange,
            onBillTypeChange = onBillTypeChange,
            onPaymentMethodChange = onPaymentMethodChange,
            onNoteChange = onNoteChange,
            errorText = errorText,
            onSubmit = onSubmit
        )
    }
}

@Composable
private fun ScheduleBillPage(
    payeeName: String,
    billAmount: String,
    billType: String,
    paymentMethod: String,
    paymentDate: String,
    repeat: String,
    note: String,
    billTypes: List<String>,
    paymentMethods: List<String>,
    repeatOptions: List<String>,
    accounts: List<AccountItem>,
    selectedAccount: AccountItem?,
    isLoading: Boolean,
    errorText: String?,
    onAccountSelected: (AccountItem) -> Unit,
    onPayeeNameChange: (String) -> Unit,
    onBillAmountChange: (String) -> Unit,
    onBillTypeChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onPaymentDateChange: (String) -> Unit,
    onRepeatChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDatePickerRequest: () -> Unit,
    onSubmit: () -> Unit,
    scheduledBills: List<com.bof.mobile.model.ScheduledBillItem>,
    onRunScheduledBill: (Int) -> Unit,
    onRefreshScheduled: () -> Unit,
    onRefreshHistory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BillSectionCard(
            title = "Schedule Bill Payment",
            subtitle = "Open the schedule payment form and set a future payment date."
        ) {
            ScheduledBillPaymentForm(
                payeeName = payeeName,
                billAmount = billAmount,
                billType = billType,
                paymentMethod = paymentMethod,
                paymentDate = paymentDate,
                repeat = repeat,
                note = note,
                billTypes = billTypes,
                paymentMethods = paymentMethods,
                repeatOptions = repeatOptions,
                accounts = accounts,
                selectedAccount = selectedAccount,
                isLoading = isLoading,
                onAccountSelected = onAccountSelected,
                onPayeeNameChange = onPayeeNameChange,
                onBillAmountChange = onBillAmountChange,
                onBillTypeChange = onBillTypeChange,
                onPaymentMethodChange = onPaymentMethodChange,
                onPaymentDateChange = onPaymentDateChange,
                onRepeatChange = onRepeatChange,
                onNoteChange = onNoteChange,
                onDatePickerRequest = onDatePickerRequest,
                errorText = errorText,
                onSubmit = onSubmit
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onRefreshScheduled, enabled = !isLoading, modifier = Modifier.weight(1f)) {
                Text("Refresh scheduled")
            }
            OutlinedButton(onClick = onRefreshHistory, enabled = !isLoading, modifier = Modifier.weight(1f)) {
                Text("Refresh history")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = CardCorner
        ) {
            Column(
                modifier = Modifier.padding(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SectionHeaderColors)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Scheduled Bills",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "View upcoming scheduled payments and run a pending bill manually if needed.",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScheduledBillsTable(
                        bills = scheduledBills,
                        onRunScheduledBill = onRunScheduledBill
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledBillsTable(
    bills: List<com.bof.mobile.model.ScheduledBillItem>,
    onRunScheduledBill: (Int) -> Unit
) {
    if (bills.isEmpty()) {
        Text("No scheduled bills", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TableHeaderCell("ID", 48.dp)
            TableHeaderCell("Account", 120.dp)
            TableHeaderCell("Payee", 130.dp)
            TableHeaderCell("Amount", 90.dp)
            TableHeaderCell("Date", 110.dp)
            TableHeaderCell("Status", 90.dp)
            TableHeaderCell("Action", 90.dp)
        }

        bills.take(8).forEachIndexed { index, bill ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableBodyCell(bill.id.toString(), 48.dp)
                TableBodyCell(bill.accountId.toString(), 120.dp)
                TableBodyCell(bill.payee, 130.dp, bold = true)
                TableBodyCell("FJD ${"%.2f".format(bill.amount)}", 90.dp)
                TableBodyCell(bill.scheduledDate.take(10), 110.dp)
                TableBodyCell(bill.status, 90.dp)
                Button(
                    onClick = { onRunScheduledBill(bill.id) },
                    modifier = Modifier.width(90.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Run")
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
private fun TableBodyCell(text: String, width: androidx.compose.ui.unit.Dp, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2
    )
}

@Composable
private fun BillSectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = CardCorner
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SectionHeaderColors)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ManualBillPaymentForm(
    payeeName: String,
    billAmount: String,
    billType: String,
    paymentMethod: String,
    note: String,
    billTypes: List<String>,
    paymentMethods: List<String>,
    accounts: List<AccountItem>,
    selectedAccount: AccountItem?,
    isLoading: Boolean,
    onAccountSelected: (AccountItem) -> Unit,
    onPayeeNameChange: (String) -> Unit,
    onBillAmountChange: (String) -> Unit,
    onBillTypeChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    errorText: String?,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BillTextField(
            value = payeeName,
            onValueChange = onPayeeNameChange,
            label = "Payee Name",
            placeholder = "Enter payee name",
            enabled = !isLoading,
            keyboardType = KeyboardType.Text
        )
        BillAccountDropdown(
            selectedAccount = selectedAccount,
            label = "Account Number",
            placeholder = if (accounts.isEmpty()) "No accounts available" else "Choose your account number",
            accounts = accounts,
            enabled = !isLoading && accounts.isNotEmpty(),
            onAccountSelected = onAccountSelected
        )
        BillTextField(
            value = billAmount,
            onValueChange = onBillAmountChange,
            label = "Bill Amount",
            placeholder = "0.00",
            enabled = !isLoading,
            keyboardType = KeyboardType.Decimal
        )
        BillDropdownField(
            value = billType,
            onValueSelected = onBillTypeChange,
            label = "Select Bill Type",
            placeholder = "Choose bill type",
            options = billTypes,
            enabled = !isLoading
        )
        BillDropdownField(
            value = paymentMethod,
            onValueSelected = onPaymentMethodChange,
            label = "Payment Method",
            placeholder = "Choose payment method",
            options = paymentMethods,
            enabled = !isLoading
        )
        BillTextField(
            value = note,
            onValueChange = onNoteChange,
            label = "Note (optional)",
            placeholder = "Add a note",
            enabled = !isLoading,
            keyboardType = KeyboardType.Text,
            minLines = 3
        )

        errorText?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
        ) {
            Text(if (isLoading) "Submitting..." else "Pay Bill")
        }
    }
}

@Composable
private fun ScheduledBillPaymentForm(
    payeeName: String,
    billAmount: String,
    billType: String,
    paymentMethod: String,
    paymentDate: String,
    repeat: String,
    note: String,
    billTypes: List<String>,
    paymentMethods: List<String>,
    repeatOptions: List<String>,
    accounts: List<AccountItem>,
    selectedAccount: AccountItem?,
    isLoading: Boolean,
    onAccountSelected: (AccountItem) -> Unit,
    onPayeeNameChange: (String) -> Unit,
    onBillAmountChange: (String) -> Unit,
    onBillTypeChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onPaymentDateChange: (String) -> Unit,
    onRepeatChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDatePickerRequest: () -> Unit,
    errorText: String?,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BillTextField(
            value = payeeName,
            onValueChange = onPayeeNameChange,
            label = "Payee Name",
            placeholder = "Enter payee name",
            enabled = !isLoading,
            keyboardType = KeyboardType.Text
        )
        BillAccountDropdown(
            selectedAccount = selectedAccount,
            label = "Account Number",
            placeholder = if (accounts.isEmpty()) "No accounts available" else "Choose your account number",
            accounts = accounts,
            enabled = !isLoading && accounts.isNotEmpty(),
            onAccountSelected = onAccountSelected
        )
        BillTextField(
            value = billAmount,
            onValueChange = onBillAmountChange,
            label = "Bill Amount",
            placeholder = "0.00",
            enabled = !isLoading,
            keyboardType = KeyboardType.Decimal
        )
        BillDropdownField(
            value = billType,
            onValueSelected = onBillTypeChange,
            label = "Select Bill Type",
            placeholder = "Choose bill type",
            options = billTypes,
            enabled = !isLoading
        )
        BillDropdownField(
            value = paymentMethod,
            onValueSelected = onPaymentMethodChange,
            label = "Payment Method",
            placeholder = "Choose payment method",
            options = paymentMethods,
            enabled = !isLoading
        )
        BillDateField(
            value = paymentDate,
            label = "Payment Date",
            placeholder = "Select a date",
            enabled = !isLoading,
            onValueChange = onPaymentDateChange,
            onClick = onDatePickerRequest
        )
        BillDropdownField(
            value = repeat,
            onValueSelected = onRepeatChange,
            label = "Repeat",
            placeholder = "Select repeat frequency",
            options = repeatOptions,
            enabled = !isLoading
        )
        BillTextField(
            value = note,
            onValueChange = onNoteChange,
            label = "Note (optional)",
            placeholder = "Add a note",
            enabled = !isLoading,
            keyboardType = KeyboardType.Text,
            minLines = 3
        )

        errorText?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
        ) {
            Text(if (isLoading) "Submitting..." else "Schedule Payment")
        }
    }
}

@Composable
private fun BillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillAccountDropdown(
    selectedAccount: AccountItem?,
    label: String,
    placeholder: String,
    accounts: List<AccountItem>,
    enabled: Boolean,
    onAccountSelected: (AccountItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = selectedAccount?.let { "${it.accountNumber} • ${it.type}" }.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text("${account.accountNumber} • ${account.type}") },
                    onClick = {
                        onAccountSelected(account)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillDropdownField(
    value: String,
    onValueSelected: (String) -> Unit,
    label: String,
    placeholder: String,
    options: List<String>,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BillDateField(
    value: String,
    label: String,
    placeholder: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

private fun validateBillForm(
    payeeName: String,
    accountNumber: String,
    billAmount: String,
    billType: String,
    paymentMethod: String
): String? {
    if (payeeName.isBlank()) return "Payee Name is required"
    if (accountNumber.isBlank()) return "Account Number is required"
    val amount = billAmount.toDoubleOrNull()
    if (amount == null || amount <= 0.0) return "Bill Amount must be a valid positive number"
    if (billType.isBlank()) return "Select Bill Type is required"
    if (paymentMethod.isBlank()) return "Payment Method is required"
    return null
}

private fun validateScheduledBillForm(
    payeeName: String,
    accountNumber: String,
    billAmount: String,
    billType: String,
    paymentMethod: String,
    paymentDate: String,
    repeat: String
): String? {
    validateBillForm(payeeName, accountNumber, billAmount, billType, paymentMethod)?.let { return it }
    if (paymentDate.isBlank()) return "Payment Date is required"
    if (runCatching { LocalDate.parse(paymentDate, DateTimeFormatter.ISO_LOCAL_DATE) }.isFailure) {
        return "Payment Date must be a valid date"
    }
    if (repeat.isBlank()) return "Repeat is required"
    return null
}

private fun showDatePicker(
    context: android.content.Context,
    currentValue: String,
    formatter: DateTimeFormatter,
    onDateSelected: (String) -> Unit
) {
    val defaultDate = runCatching { LocalDate.parse(currentValue, formatter) }
        .getOrDefault(LocalDate.now())
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).format(formatter))
        },
        defaultDate.year,
        defaultDate.monthValue - 1,
        defaultDate.dayOfMonth
    ).show()
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
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
