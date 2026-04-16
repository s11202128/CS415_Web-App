package com.bof.mobile.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bof.mobile.model.AccountItem
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.CreateAccountViewModel

private val REQUESTABLE_ACCOUNT_TYPES = listOf("Simple Access", "Savings", "Current")

@Composable
fun CreateAccountScreen(
    viewModel: CreateAccountViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onAccountCreated: (AccountItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var accountTypeExpanded by remember { mutableStateOf(false) }
    var lastHandledAccountId by remember { mutableStateOf<Int?>(null) }
    var showSubmittedDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.createdAccount?.id) {
        val createdAccount = uiState.createdAccount ?: return@LaunchedEffect
        if (lastHandledAccountId != createdAccount.id) {
            lastHandledAccountId = createdAccount.id
            onAccountCreated(createdAccount)
            showSubmittedDialog = true
        }
    }

    val accountNameValid = uiState.accountName.trim().isNotBlank()
    val passwordValid = uiState.accountPassword.length >= 8
    val formValid = accountNameValid && passwordValid
    val accountNameError = if (uiState.accountNameTouched && !accountNameValid) "Account name is required" else null
    val passwordError = if (uiState.passwordTouched && !passwordValid) "Password must be at least 8 characters" else null

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
                title = "Request New Account",
                subtitle = "Submit an account request for admin approval",
                onBack = onBack,
                enabled = canGoBack
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.accountName,
                        onValueChange = viewModel::onAccountNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. Peter Main Account") },
                        singleLine = true,
                        isError = accountNameError != null
                    )
                    if (accountNameError != null) {
                        Text(accountNameError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    Text("Type of Account", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedTextField(
                            value = uiState.accountType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select account type") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { accountTypeExpanded = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountTypeExpanded = true }
                        )

                        DropdownMenu(
                            expanded = accountTypeExpanded,
                            onDismissRequest = { accountTypeExpanded = false }
                        ) {
                            REQUESTABLE_ACCOUNT_TYPES.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        viewModel.onAccountTypeChanged(type)
                                        accountTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.accountPassword,
                        onValueChange = viewModel::onPasswordChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (uiState.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisibility) {
                                Icon(
                                    imageVector = if (uiState.showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        isError = passwordError != null
                    )
                    if (passwordError != null) {
                        Text(passwordError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedTextField(
                        value = uiState.generatedPin,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("PIN Number") },
                        readOnly = true,
                        enabled = false
                    )

                    OutlinedTextField(
                        value = uiState.generatedAccountNumber,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Account Number") },
                        readOnly = true,
                        enabled = false
                    )

                    if (!uiState.successMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.successMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = { viewModel.submit(customerId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = formValid && !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Request Account", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showSubmittedDialog) {
            AlertDialog(
                onDismissRequest = { showSubmittedDialog = false },
                confirmButton = {
                    Button(onClick = { showSubmittedDialog = false }) {
                        Text("OK")
                    }
                },
                title = {
                    Text("Application Submitted!")
                },
                text = {
                    Text("Your account is under review.")
                }
            )
        }
    }
}
