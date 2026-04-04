package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.model.ApiResult
import com.bof.mobile.model.AccountItem
import com.bof.mobile.model.CreateAccountRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateAccountUiState(
    val accountName: String = "",
    val accountType: String = REQUESTABLE_ACCOUNT_TYPES.first(),
    val accountPassword: String = "",
    val showPassword: Boolean = false,
    val accountNameTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val generatedPin: String = "Will be auto-generated",
    val generatedAccountNumber: String = "Will be auto-generated",
    val createdAccount: AccountItem? = null
)

private val REQUESTABLE_ACCOUNT_TYPES = listOf("Simple Access", "Savings")

class CreateAccountViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState

    fun onAccountNameChanged(value: String) {
        _uiState.update {
            it.copy(
                accountName = value,
                accountNameTouched = true,
                successMessage = null,
                errorMessage = null,
                createdAccount = null
            )
        }
    }

    fun onAccountTypeChanged(value: String) {
        _uiState.update {
            it.copy(
                accountType = value,
                successMessage = null,
                errorMessage = null,
                createdAccount = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                accountPassword = value,
                passwordTouched = true,
                successMessage = null,
                errorMessage = null,
                createdAccount = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(showPassword = !it.showPassword) }
    }

    fun submit(customerId: Int) {
        val state = _uiState.value
        val trimmedAccountName = state.accountName.trim()
        val accountNameValid = trimmedAccountName.isNotBlank()
        val passwordValid = state.accountPassword.length >= 8

        _uiState.update {
            it.copy(
                accountNameTouched = true,
                passwordTouched = true
            )
        }

        if (!accountNameValid) {
            _uiState.update { it.copy(errorMessage = "Account name is required", isSubmitting = false) }
            return
        }

        if (!passwordValid) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters", isSubmitting = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            when (
                val result = accountRepository.createAccount(
                    CreateAccountRequest(
                        type = state.accountType,
                        openingBalance = 0.0,
                        customerId = customerId.takeIf { it > 0 },
                        customerName = trimmedAccountName
                    )
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            successMessage = "Account request submitted. Status: ${result.data.status}.",
                            errorMessage = null,
                            generatedPin = result.data.accountPin ?: "Generated",
                                generatedAccountNumber = result.data.accountNumber,
                                createdAccount = result.data
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}