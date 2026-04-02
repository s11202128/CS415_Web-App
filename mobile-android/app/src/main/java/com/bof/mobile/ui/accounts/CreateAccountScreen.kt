package com.bof.mobile.ui.accounts

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.CreateAccountRequest
import com.bof.mobile.ui.components.ScreenHeader
import kotlinx.coroutines.launch

private enum class SignupAccountType(val label: String, val subLabel: String) {
    SIMPLE_ACCESS("Simple Access", "$2.50/month"),
    SAVINGS("Savings", "No monthly fee")
}

@Composable
fun CreateAccountScreen(
    accountRepository: AccountRepository,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onAccountCreated: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+679") }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf(SignupAccountType.SIMPLE_ACCESS) }
    var accountPassword by remember { mutableStateOf("") }
    var showAccountPassword by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var submitErrorMessage by remember { mutableStateOf<String?>(null) }

    var nameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    var accountPasswordTouched by remember { mutableStateOf(false) }
    var termsTouched by remember { mutableStateOf(false) }

    val trimmedName = fullName.trim()
    val trimmedEmail = email.trim()
    val trimmedPhone = phoneNumber.trim()

    val nameValid = trimmedName.isNotEmpty()
    val emailValid = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(trimmedEmail)
    val phoneValid = trimmedPhone.isNotEmpty()
    val passwordHasLength = accountPassword.length >= 8
    val passwordHasUpper = accountPassword.any { it.isUpperCase() }
    val passwordHasNumber = accountPassword.any { it.isDigit() }
    val accountPasswordValid = passwordHasLength && passwordHasUpper && passwordHasNumber
    val termsValid = agreedToTerms

    val formValid = nameValid && emailValid && phoneValid && accountPasswordValid && termsValid

    val nameError = if (nameTouched && !nameValid) "Full name is required" else null
    val emailError = if (emailTouched && !emailValid) "Enter a valid email address" else null
    val phoneError = if (phoneTouched && !phoneValid) "Phone number is required" else null
    val passwordError = if (accountPasswordTouched && !accountPasswordValid) "Account password does not meet all requirements" else null
    val termsError = if (termsTouched && !termsValid) "You must agree before continuing" else null

    LaunchedEffect(fullName, email, phoneNumber, accountPassword, agreedToTerms) {
        successMessage = null
        submitErrorMessage = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenHeader(
                title = "Create Account",
                subtitle = "Set up your new banking account",
                onBack = onBack,
                enabled = canGoBack
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            nameTouched = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your full name") },
                        singleLine = true,
                        isError = nameError != null
                    )
                    if (nameError != null) {
                        InlineError(nameError)
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailTouched = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Address") },
                        placeholder = { Text("Enter your email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailError != null
                    )
                    if (emailError != null) {
                        InlineError(emailError)
                    }

                    Text(
                        text = "Phone Number",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(0.38f)) {
                            OutlinedTextField(
                                value = countryCode,
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { countryMenuExpanded = true },
                                readOnly = true,
                                singleLine = true,
                                label = { Text("Code")
                                }
                            )
                            DropdownMenu(
                                expanded = countryMenuExpanded,
                                onDismissRequest = { countryMenuExpanded = false }
                            ) {
                                listOf("+679", "+61", "+64", "+1").forEach { code ->
                                    DropdownMenuItem(
                                        text = { Text(code) },
                                        onClick = {
                                            countryCode = code
                                            countryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it.filter { ch -> ch.isDigit() }
                                phoneTouched = true
                            },
                            modifier = Modifier.weight(0.62f),
                            label = { Text("Phone") },
                            placeholder = { Text("Enter your phone number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneError != null
                        )
                    }
                    if (phoneError != null) {
                        InlineError(phoneError)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Account Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    SignupAccountType.entries.forEach { type ->
                        val selected = selectedAccountType == type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedAccountType = type },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(type.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(type.subLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Savings accounts earn interest. Simple Access accounts have a $2.50/month maintenance fee.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Account Password",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = accountPassword,
                        onValueChange = {
                            accountPassword = it
                            accountPasswordTouched = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Account password") },
                        placeholder = { Text("Enter a password") },
                        singleLine = true,
                        visualTransformation = if (showAccountPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showAccountPassword = !showAccountPassword }) {
                                Icon(
                                    imageVector = if (showAccountPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showAccountPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        isError = passwordError != null
                    )
                    if (passwordError != null) {
                        InlineError(passwordError)
                    }

                    PasswordRule(label = "At least 8 characters", valid = passwordHasLength)
                    PasswordRule(label = "One uppercase letter", valid = passwordHasUpper)
                    PasswordRule(label = "One number (0-9)", valid = passwordHasNumber)

                    TermsRow(
                        checked = agreedToTerms,
                        onCheckedChange = {
                            agreedToTerms = it
                            termsTouched = true
                        }
                    )
                    if (termsError != null) {
                        InlineError(termsError)
                    }

                    if (successMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = successMessage ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (submitErrorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = submitErrorMessage ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            nameTouched = true
                            emailTouched = true
                            phoneTouched = true
                            accountPasswordTouched = true
                            termsTouched = true

                            if (!formValid || isSubmitting) return@Button

                            submitErrorMessage = null
                            isSubmitting = true
                            scope.launch {
                                when (
                                    val syncResult = accountRepository.syncProfileData(
                                        customerId = customerId,
                                        fullName = trimmedName,
                                        mobile = "$countryCode$trimmedPhone",
                                        email = trimmedEmail
                                    )
                                ) {
                                    is ApiResult.Success -> Unit
                                    is ApiResult.Error -> {
                                        submitErrorMessage = syncResult.message
                                        isSubmitting = false
                                        return@launch
                                    }
                                }

                                val request = CreateAccountRequest(
                                    type = selectedAccountType.label,
                                    openingBalance = 0.0,
                                    customerId = customerId,
                                    customerName = trimmedName
                                )

                                when (val result = accountRepository.createAccount(request)) {
                                    is ApiResult.Success -> {
                                        successMessage = "Request submitted. Account ${result.data.accountNumber} is pending admin approval. PIN: ${result.data.accountPin ?: "----"}."
                                        onAccountCreated()
                                    }
                                    is ApiResult.Error -> {
                                        successMessage = null
                                        submitErrorMessage = result.message
                                    }
                                }
                                isSubmitting = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = formValid && !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Create Account", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

@Composable
private fun PasswordRule(label: String, valid: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (valid) "•" else "•",
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) Color(0xFF198754) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (valid) Color(0xFF198754) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TermsRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(top = 2.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append("I agree to the ")
                    pushStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append("Terms and Conditions")
                    pop()
                    append(" and ")
                    pushStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append("Privacy Policy")
                    pop()
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {}) { Text("Terms and Conditions") }
                TextButton(onClick = {}) { Text("Privacy Policy") }
            }
        }
    }
}

@Composable
private fun InlineError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 2.dp)
    )
}
