package com.bof.mobile.ui.billpayment
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bof.mobile.model.BillHistoryItem
import com.bof.mobile.model.BillPaymentRequest
import com.bof.mobile.model.ScheduledBillItem
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Calendar

private val CardCorner = RoundedCornerShape(24.dp)
private val SectionHeaderColors = Brush.horizontalGradient(
    listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
)
private const val DefaultBillType = "Utilities"
private const val DefaultPaymentMethod = "Account Balance"
private const val MinBillAmount = 0.01
private const val MaxBillAmount = 10000.0
private const val DigicelBiller = "Digicel Fiji"
private const val EnergyFijiBiller = "Energy Fiji"
private const val EnergyFijiService = "Electricity"
private const val HousingAuthorityBiller = "Housing Authority"
private const val HousingAuthorityService = "Housing Services"
private const val TelecomFijiBiller = "Telecom Fiji"
private const val VodafoneFijiBiller = "Vodafone"
private const val WaterAuthorityBiller = "Water Authority"
private const val WaterAuthorityService = "Water Service"
private const val RequiredAccountNumberLength = 10
private const val ScheduleMode = "schedule"
private const val RecurringMode = "recurring"

@Composable
fun BillPaymentScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val billers = remember {
        listOf(
            DigicelBiller,
            "Energy Fiji",
            "Housing Authority",
            "Telecom Fiji",
            "Vodafone",
            "Water Authority"
        )
    }
    val digicelServices = remember {
        listOf("Sky Pacific Payment", "Digicel Phone Service")
    }
    val telecomServices = remember {
        listOf("Phone Landline", "Internet Services")
    }
    val vodafoneServices = remember {
        listOf("Internet Services", "Mobile Service")
    }
    val recurrenceOptions = remember { listOf("Daily", "Weekly", "Monthly") }

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedBiller by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDigicelService by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTelecomService by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVodafoneService by rememberSaveable { mutableStateOf<String?>(null) }
    var billAmount by rememberSaveable { mutableStateOf("") }
    var serviceAccountNumber by rememberSaveable { mutableStateOf("") }
    var receiptDate by rememberSaveable { mutableStateOf("") }
    var selectedMode by rememberSaveable { mutableStateOf<String?>(null) }
    var recurrenceOption by rememberSaveable { mutableStateOf("Monthly") }
    var scheduledDate by rememberSaveable { mutableStateOf("") }
    var scheduledHour by rememberSaveable { mutableStateOf("12") }
    var scheduledMinute by rememberSaveable { mutableStateOf("00") }
    var scheduledMeridiem by rememberSaveable { mutableStateOf("AM") }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var showPaymentSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var paymentSuccessMessage by rememberSaveable { mutableStateOf("Bill paid successfully") }

    val clearBillForm = {
        selectedBiller = null
        selectedDigicelService = null
        selectedTelecomService = null
        selectedVodafoneService = null
        billAmount = ""
        serviceAccountNumber = ""
        receiptDate = ""
        selectedMode = null
        recurrenceOption = "Monthly"
        scheduledDate = ""
        scheduledHour = "12"
        scheduledMinute = "00"
        scheduledMeridiem = "AM"
        formError = null
    }

    val filteredBillers = remember(searchQuery, billers) {
        val normalizedQuery = searchQuery.trim()
        if (normalizedQuery.isBlank()) {
            billers
        } else {
            billers.filter { it.contains(normalizedQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadAccounts()
        viewModel.loadBillHistory()
        viewModel.loadScheduledBills()
    }

    LaunchedEffect(uiState.successMessage) {
        val success = uiState.successMessage.orEmpty()
        if (
            success.startsWith("Bill paid successfully") ||
            success.startsWith("Bill scheduled successfully") ||
            success.startsWith("Recurring bill scheduled successfully")
        ) {
            paymentSuccessMessage = success
            showPaymentSuccessDialog = true
            viewModel.clearMessages()
        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader(
                title = "Bill Payment",
                subtitle = "Choose your biller or review your bill payment history.",
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
                    text = { Text("Choose Your Biller") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bill Payment History") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("SCHEDULE BILLS") }
                )
            }

            when (selectedTab) {
                0 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedBiller == null) {
                        BillSectionCard(
                            title = "Choose Biller",
                            subtitle = "Search or tap a biller to open its payment options."
                        ) {
                            BillTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = "Search Biller",
                                placeholder = "Search billers",
                                enabled = !uiState.isLoading,
                                keyboardType = KeyboardType.Text
                            )

                            if (filteredBillers.isEmpty()) {
                                Text(
                                    text = "No billers match your search.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredBillers.forEach { biller ->
                                        BillerListItem(
                                            name = biller,
                                            subtitle = "",
                                            isSelected = false,
                                            onClick = {
                                                selectedBiller = biller
                                                if (biller == DigicelBiller) {
                                                    selectedDigicelService = null
                                                }
                                                if (biller == TelecomFijiBiller) {
                                                    selectedTelecomService = null
                                                }
                                                if (biller == VodafoneFijiBiller) {
                                                    selectedVodafoneService = null
                                                }
                                                billAmount = ""
                                                serviceAccountNumber = ""
                                                receiptDate = ""
                                                selectedMode = null
                                                recurrenceOption = "Monthly"
                                                scheduledDate = ""
                                                scheduledHour = "12"
                                                scheduledMinute = "00"
                                                scheduledMeridiem = "AM"
                                                formError = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val openedBiller = selectedBiller!!
                        BillSectionCard(
                            title = openedBiller,
                            subtitle = if (openedBiller == DigicelBiller) {
                            "Choose a Digicel service to open its payment section."
                        } else {
                            "Enter the amount and account number for this biller."
                        }
                        ) {
                            Button(
                                onClick = {
                                    selectedBiller = null
                                    selectedDigicelService = null
                                    selectedTelecomService = null
                                    selectedVodafoneService = null
                                    selectedMode = null
                                    recurrenceOption = "Monthly"
                                    scheduledDate = ""
                                    scheduledHour = "12"
                                    scheduledMinute = "00"
                                    scheduledMeridiem = "AM"
                                    formError = null
                                },
                                enabled = !uiState.isLoading,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Back To Billers")
                            }

                            if (openedBiller == DigicelBiller && selectedDigicelService == null) {
                                Text(
                                    text = "Digicel Services",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    digicelServices.forEach { service ->
                                        BillerListItem(
                                            name = service,
                                            subtitle = "",
                                            isSelected = service == selectedDigicelService,
                                            onClick = {
                                                selectedDigicelService = service
                                                selectedTelecomService = null
                                                selectedVodafoneService = null
                                                billAmount = ""
                                                serviceAccountNumber = ""
                                                receiptDate = ""
                                                selectedMode = null
                                                recurrenceOption = "Monthly"
                                                scheduledDate = ""
                                                scheduledHour = "12"
                                                scheduledMinute = "00"
                                                scheduledMeridiem = "AM"
                                                formError = null
                                            }
                                        )
                                    }
                                }
                                if (selectedDigicelService == null) {
                                    Text(
                                        text = "Select a Digicel service to continue to payment.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (openedBiller == DigicelBiller && !selectedDigicelService.isNullOrBlank()) {
                                Text(
                                    text = "Selected Service: ${selectedDigicelService.orEmpty()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = {
                                        selectedDigicelService = null
                                        selectedTelecomService = null
                                        selectedVodafoneService = null
                                        billAmount = ""
                                        serviceAccountNumber = ""
                                        receiptDate = ""
                                        selectedMode = null
                                        recurrenceOption = "Monthly"
                                        scheduledDate = ""
                                        scheduledHour = "12"
                                        scheduledMinute = "00"
                                        scheduledMeridiem = "AM"
                                        formError = null
                                    },
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Change Service")
                                }
                            }

                            if (openedBiller == TelecomFijiBiller && selectedTelecomService == null) {
                                Text(
                                    text = "Telecom Services",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    telecomServices.forEach { service ->
                                        BillerListItem(
                                            name = service,
                                            subtitle = "",
                                            isSelected = service == selectedTelecomService,
                                            onClick = {
                                                selectedTelecomService = service
                                                selectedDigicelService = null
                                                selectedVodafoneService = null
                                                billAmount = ""
                                                serviceAccountNumber = ""
                                                receiptDate = ""
                                                selectedMode = null
                                                recurrenceOption = "Monthly"
                                                scheduledDate = ""
                                                scheduledHour = "12"
                                                scheduledMinute = "00"
                                                scheduledMeridiem = "AM"
                                                formError = null
                                            }
                                        )
                                    }
                                }
                                Text(
                                    text = "Select a Telecom Fiji service to continue to payment.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (openedBiller == TelecomFijiBiller && !selectedTelecomService.isNullOrBlank()) {
                                Text(
                                    text = "Selected Service: ${selectedTelecomService.orEmpty()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = {
                                        selectedTelecomService = null
                                        selectedDigicelService = null
                                        selectedVodafoneService = null
                                        billAmount = ""
                                        serviceAccountNumber = ""
                                        receiptDate = ""
                                        selectedMode = null
                                        recurrenceOption = "Monthly"
                                        scheduledDate = ""
                                        scheduledHour = "12"
                                        scheduledMinute = "00"
                                        scheduledMeridiem = "AM"
                                        formError = null
                                    },
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Change Service")
                                }
                            }

                            if (openedBiller == VodafoneFijiBiller && selectedVodafoneService == null) {
                                Text(
                                    text = "Vodafone Fiji Services",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    vodafoneServices.forEach { service ->
                                        BillerListItem(
                                            name = service,
                                            subtitle = "",
                                            isSelected = service == selectedVodafoneService,
                                            onClick = {
                                                selectedVodafoneService = service
                                                selectedDigicelService = null
                                                selectedTelecomService = null
                                                billAmount = ""
                                                serviceAccountNumber = ""
                                                receiptDate = ""
                                                selectedMode = null
                                                recurrenceOption = "Monthly"
                                                scheduledDate = ""
                                                scheduledHour = "12"
                                                scheduledMinute = "00"
                                                scheduledMeridiem = "AM"
                                                formError = null
                                            }
                                        )
                                    }
                                }
                                Text(
                                    text = "Select a Vodafone Fiji service to continue to payment.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (openedBiller == VodafoneFijiBiller && !selectedVodafoneService.isNullOrBlank()) {
                                Text(
                                    text = "Selected Service: ${selectedVodafoneService.orEmpty()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = {
                                        selectedVodafoneService = null
                                        selectedDigicelService = null
                                        selectedTelecomService = null
                                        billAmount = ""
                                        serviceAccountNumber = ""
                                        receiptDate = ""
                                        selectedMode = null
                                        recurrenceOption = "Monthly"
                                        scheduledDate = ""
                                        scheduledHour = "12"
                                        scheduledMinute = "00"
                                        scheduledMeridiem = "AM"
                                        formError = null
                                    },
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Change Service")
                                }
                            }

                            if (openedBiller == EnergyFijiBiller) {
                                Text(
                                    text = "Service: $EnergyFijiService",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (openedBiller == HousingAuthorityBiller) {
                                Text(
                                    text = "Service: $HousingAuthorityService",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (openedBiller == WaterAuthorityBiller) {
                                Text(
                                    text = "Service: $WaterAuthorityService",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val showPaymentSection = when (openedBiller) {
                                DigicelBiller -> !selectedDigicelService.isNullOrBlank()
                                TelecomFijiBiller -> !selectedTelecomService.isNullOrBlank()
                                VodafoneFijiBiller -> !selectedVodafoneService.isNullOrBlank()
                                else -> true
                            }
                            if (showPaymentSection) {
                                val payeeLabel = resolvedPayee(
                                    openedBiller,
                                    selectedDigicelService,
                                    selectedTelecomService,
                                    selectedVodafoneService
                                )

                                BillTextField(
                                    value = billAmount,
                                    onValueChange = {
                                        billAmount = it
                                        formError = null
                                    },
                                    label = "Amount",
                                    placeholder = "0.01 - 10000.00",
                                    enabled = !uiState.isLoading,
                                    keyboardType = KeyboardType.Decimal
                                )
                                BillTextField(
                                    value = serviceAccountNumber,
                                    onValueChange = {
                                        serviceAccountNumber =
                                            it.filter { char -> char.isDigit() }.take(RequiredAccountNumberLength)
                                        formError = null
                                    },
                                    label = "Account Number",
                                    placeholder = "Enter 10-digit account number",
                                    enabled = !uiState.isLoading,
                                    keyboardType = KeyboardType.Number
                                )
                                Text(
                                    text = "Account number must be exactly 10 digits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                ScheduleDatePickerField(
                                    value = receiptDate,
                                    enabled = !uiState.isLoading,
                                    labelText = "Invoice Date",
                                    emptyButtonText = "Pick invoice date",
                                    onDateSelected = {
                                        receiptDate = it
                                        formError = null
                                    }
                                )

                                formError?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                }

                                ScheduleModeToggle(
                                    selectedMode = selectedMode,
                                    onScheduleSelected = {
                                        selectedMode = if (selectedMode == ScheduleMode) null else ScheduleMode
                                        formError = null
                                    },
                                    onRecurringSelected = {
                                        selectedMode = if (selectedMode == RecurringMode) null else RecurringMode
                                        formError = null
                                    }
                                )

                                if (selectedMode == RecurringMode) {
                                    RecurrenceDropdown(
                                        value = recurrenceOption,
                                        options = recurrenceOptions,
                                        enabled = !uiState.isLoading,
                                        onValueSelected = {
                                            recurrenceOption = it
                                            formError = null
                                        }
                                    )
                                }

                                if (selectedMode == ScheduleMode || selectedMode == RecurringMode) {
                                    ScheduleDatePickerField(
                                        value = scheduledDate,
                                        enabled = !uiState.isLoading,
                                        onDateSelected = {
                                            scheduledDate = it
                                            formError = null
                                        }
                                    )

                                    TimeDropdownPicker(
                                        hour = scheduledHour,
                                        minute = scheduledMinute,
                                        meridiem = scheduledMeridiem,
                                        enabled = !uiState.isLoading,
                                        onHourSelected = {
                                            scheduledHour = it
                                            formError = null
                                        },
                                        onMinuteSelected = {
                                            scheduledMinute = it
                                            formError = null
                                        },
                                        onMeridiemSelected = {
                                            scheduledMeridiem = it
                                            formError = null
                                        }
                                    )
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val validationError = validateBillRequest(
                                                amount = billAmount,
                                                accountNumber = serviceAccountNumber,
                                                receiptDate = receiptDate
                                            )
                                            val selectedAccount = uiState.selectedAccount ?: uiState.accounts.firstOrNull()
                                            if (validationError != null) {
                                                formError = validationError
                                                return@Button
                                            }
                                            if (selectedAccount == null) {
                                                formError = "No bank account available for bill payment"
                                                return@Button
                                            }

                                            val amountValue = billAmount.trim().toDoubleOrNull()
                                            if (amountValue == null) {
                                                formError = "Enter a valid amount"
                                                return@Button
                                            }

                                            val scheduleModeSelected = selectedMode == ScheduleMode || selectedMode == RecurringMode
                                            if (!scheduleModeSelected) {
                                                viewModel.payBillManual(
                                                    BillPaymentRequest(
                                                        accountId = selectedAccount.id,
                                                        accountNumber = selectedAccount.accountNumber,
                                                        payee = payeeLabel,
                                                        amount = amountValue,
                                                        billType = DefaultBillType,
                                                        paymentMethod = DefaultPaymentMethod,
                                                        note = buildBillNote(serviceAccountNumber, receiptDate)
                                                    )
                                                )
                                                return@Button
                                            }

                                            val scheduledDateTime = buildScheduledDateTime(
                                                dateInput = scheduledDate,
                                                hourInput = scheduledHour,
                                                minuteInput = scheduledMinute,
                                                meridiemInput = scheduledMeridiem
                                            )
                                            if (scheduledDateTime == null) {
                                                formError = "Select valid schedule date and time"
                                                return@Button
                                            }
                                            if (scheduledDateTime.isBefore(LocalDateTime.now())) {
                                                formError = "Schedule date/time must be in the future"
                                                return@Button
                                            }

                                            viewModel.scheduleBill(
                                                BillPaymentRequest(
                                                    accountId = selectedAccount.id,
                                                    accountNumber = selectedAccount.accountNumber,
                                                    payee = payeeLabel,
                                                    amount = amountValue,
                                                    scheduledDate = scheduledDateTime.toString(),
                                                    billType = DefaultBillType,
                                                    paymentMethod = DefaultPaymentMethod,
                                                    note = buildBillNote(serviceAccountNumber, receiptDate),
                                                    repeat = if (selectedMode == RecurringMode) recurrenceOption else "One-time",
                                                    paymentDate = scheduledDateTime.toString()
                                                )
                                            )
                                        } catch (exception: Exception) {
                                            formError = "Unable to process bill request. Please check your details and try again."
                                        }
                                    },
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    val buttonLabel = when (selectedMode) {
                                        ScheduleMode -> "Schedule Bill"
                                        RecurringMode -> "Set Recurring Bill"
                                        else -> "Pay Now"
                                    }
                                    Text(buttonLabel)
                                }
                            }
                        }
                    }
                }

                1 -> BillHistoryTab(
                    historyItems = uiState.billHistory,
                    isLoading = uiState.isLoading
                )

                2 -> ScheduledBillsTab(
                    scheduledItems = uiState.scheduledBills,
                    isLoading = uiState.isLoading
                )
            }
        }

        if (showPaymentSuccessDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Successful") },
                text = { Text(paymentSuccessMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPaymentSuccessDialog = false
                            clearBillForm()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private fun resolvedPayee(
    selectedBiller: String,
    selectedDigicelService: String?,
    selectedTelecomService: String?,
    selectedVodafoneService: String?
): String {
    return if (selectedBiller == DigicelBiller) {
        "$selectedBiller - ${selectedDigicelService.orEmpty()}"
    } else if (selectedBiller == EnergyFijiBiller) {
        "$selectedBiller - $EnergyFijiService"
    } else if (selectedBiller == HousingAuthorityBiller) {
        "$selectedBiller - $HousingAuthorityService"
    } else if (selectedBiller == TelecomFijiBiller) {
        "$selectedBiller - ${selectedTelecomService.orEmpty()}"
    } else if (selectedBiller == VodafoneFijiBiller) {
        "$selectedBiller - ${selectedVodafoneService.orEmpty()}"
    } else if (selectedBiller == WaterAuthorityBiller) {
        "$selectedBiller - $WaterAuthorityService"
    } else {
        selectedBiller
    }
}

private fun buildBillNote(
    serviceAccountNumber: String,
    receiptDate: String
): String {
    val accountPrefix = "Service Account: ${serviceAccountNumber.trim()}"
    val receiptPrefix = "Invoice Date: ${receiptDate.trim()}"
    return if (receiptDate.trim().isBlank()) accountPrefix else "$accountPrefix | $receiptPrefix"
}

@Composable
private fun BillHistoryTab(
    historyItems: List<BillHistoryItem>,
    isLoading: Boolean
) {
    BillSectionCard(
        title = "Bill Payment History",
        subtitle = "Review your previous bill payments here."
    ) {
        if (historyItems.isEmpty()) {
            Text(
                text = if (isLoading) "Loading payment history..." else "No bill payments found yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@BillSectionCard
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(historyItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.payee,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Amount: FJD ${"%.2f".format(item.amount)}")
                        Text("Status: ${item.status}")
                        Text("Created: ${item.createdAt.take(10)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledBillsTab(
    scheduledItems: List<ScheduledBillItem>,
    isLoading: Boolean
) {
    BillSectionCard(
        title = "Scheduled Bills",
        subtitle = "Review your pending scheduled bill payments."
    ) {
        if (scheduledItems.isEmpty()) {
            Text(
                text = if (isLoading) "Loading scheduled bills..." else "No scheduled bills found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@BillSectionCard
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(scheduledItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.payee,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Amount: FJD ${"%.2f".format(item.amount)}")
                        Text("Scheduled: ${item.scheduledDate}")
                        Text("Status: ${item.status}")
                        Text("Created: ${item.createdAt.take(10)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun BillerListItem(
    name: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                },
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsForBiller(name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSelected || subtitle.isNotBlank()) {
                    Text(
                        text = if (isSelected) "Currently selected" else subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(99.dp)
                    )
            )
        }
    }
}

private fun initialsForBiller(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "BL"
    }
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

private fun validateBillRequest(
    amount: String,
    accountNumber: String,
    receiptDate: String
): String? {
    if (accountNumber.trim().isBlank()) return "Account Number is required"
    val normalizedAccount = accountNumber.trim()
    if (normalizedAccount.length != RequiredAccountNumberLength || !normalizedAccount.all { it.isDigit() }) {
        return "Account number must be exactly 10 digits"
    }
    if (receiptDate.trim().isBlank()) {
        return "Invoice Date is required"
    }
    if (runCatching { LocalDate.parse(receiptDate.trim()) }.isFailure) {
        return "Invoice Date is invalid"
    }
    val parsedAmount = amount.toDoubleOrNull()
    if (parsedAmount == null || parsedAmount < MinBillAmount || parsedAmount > MaxBillAmount) {
        return "Amount must be between 0.01 and 10000"
    }
    return null
}

private fun buildScheduledDateTime(
    dateInput: String,
    hourInput: String,
    minuteInput: String,
    meridiemInput: String
): LocalDateTime? {
    val date = runCatching { LocalDate.parse(dateInput.trim()) }.getOrNull() ?: return null
    val hour12 = hourInput.toIntOrNull() ?: return null
    val minute = minuteInput.toIntOrNull() ?: return null
    if (hour12 !in 1..12 || minute !in 0..59) return null
    val normalizedMeridiem = meridiemInput.trim().uppercase()
    val hour24 = when (normalizedMeridiem) {
        "AM" -> if (hour12 == 12) 0 else hour12
        "PM" -> if (hour12 == 12) 12 else hour12 + 12
        else -> return null
    }
    val time = LocalTime.of(hour24, minute)
    return LocalDateTime.of(date, time)
}

@Composable
private fun ScheduleModeToggle(
    selectedMode: String?,
    onScheduleSelected: () -> Unit,
    onRecurringSelected: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = selectedMode == ScheduleMode,
                onCheckedChange = { onScheduleSelected() }
            )
            Text("Schedule")
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = selectedMode == RecurringMode,
                onCheckedChange = { onRecurringSelected() }
            )
            Text("Recurring")
        }
    }
}

@Composable
private fun ScheduleDatePickerField(
    value: String,
    enabled: Boolean,
    labelText: String = "Schedule Date",
    emptyButtonText: String = "Pick date",
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = {
                val picker = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onDateSelected(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                picker.show()
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(if (value.isBlank()) emptyButtonText else value)
        }
    }
}

@Composable
private fun TimeDropdownPicker(
    hour: String,
    minute: String,
    meridiem: String,
    enabled: Boolean,
    onHourSelected: (String) -> Unit,
    onMinuteSelected: (String) -> Unit,
    onMeridiemSelected: (String) -> Unit
) {
    val hourOptions = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val minuteOptions = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val meridiemOptions = remember { listOf("AM", "PM") }

    Text(
        text = "Schedule Time",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimePartDropdown(
            value = hour,
            label = "Hour",
            options = hourOptions,
            enabled = enabled,
            onValueSelected = onHourSelected,
            modifier = Modifier.weight(1f)
        )
        TimePartDropdown(
            value = minute,
            label = "Minute",
            options = minuteOptions,
            enabled = enabled,
            onValueSelected = onMinuteSelected,
            modifier = Modifier.weight(1f)
        )
        TimePartDropdown(
            value = meridiem,
            label = "Unit",
            options = meridiemOptions,
            enabled = enabled,
            onValueSelected = onMeridiemSelected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimePartDropdown(
    value: String,
    label: String,
    options: List<String>,
    enabled: Boolean,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(value)
            }
            DropdownMenu(
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
}

@Composable
private fun RecurrenceDropdown(
    value: String,
    options: List<String>,
    enabled: Boolean,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Recurrence",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(value.ifBlank { "Choose recurrence" })
            }
            DropdownMenu(
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
                Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}
