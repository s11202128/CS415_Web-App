package com.bof.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bof.mobile.data.local.LoginSuggestionsStore
import com.bof.mobile.data.repository.AuthRepository
import com.bof.mobile.model.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val registrationAccountType: String = "Simple Access",
    val token: String? = null,
    val userId: Int? = null,
    val customerId: Int? = null,
    val isAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val loginSuggestions: List<String> = emptyList(),
    val registrationSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val loginSuggestionsStore: LoginSuggestionsStore? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        val saved = loginSuggestionsStore?.getSuggestions().orEmpty()
        if (saved.isNotEmpty()) {
            _uiState.update { it.copy(loginSuggestions = saved) }
        }
    }

    fun onFullNameChanged(value: String) = _uiState.update { it.copy(fullName = value) }
    fun onMobileChanged(value: String) = _uiState.update { it.copy(mobile = value) }
    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value) }
    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value) }
    fun onConfirmPasswordChanged(value: String) = _uiState.update { it.copy(confirmPassword = value) }
    fun onRegistrationAccountTypeChanged(value: String) = _uiState.update { it.copy(registrationAccountType = value) }
    fun applyLoginSuggestion(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    fun clearMessages() = _uiState.update { it.copy(errorMessage = null, registrationSuccessMessage = null) }

    fun login() {
        val current = _uiState.value
        val identifier = current.email.trim()
        if (identifier.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email or mobile and password are required") }
            return
        }
        if (!isEmailOrMobile(identifier)) {
            _uiState.update { it.copy(errorMessage = "Enter a valid email address or mobile number") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.login(identifier, current.password)) {
                is ApiResult.Success -> {
                    val suggestions = saveSuggestion(current.email)
                    val resolvedCustomerId = if (result.data.isAdmin) {
                        result.data.customerId
                    } else {
                        result.data.customerId ?: result.data.userId
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            token = result.data.token,
                            userId = result.data.userId,
                            customerId = resolvedCustomerId,
                            fullName = result.data.fullName,
                            email = result.data.email,
                            mobile = result.data.mobile ?: it.mobile,
                            isAdmin = result.data.isAdmin,
                            loginSuggestions = suggestions,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun register() {
        val current = _uiState.value
        if (current.fullName.isBlank() || current.mobile.isBlank() || current.email.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All registration fields are required") }
            return
        }
        if (!isEmail(current.email.trim())) {
            _uiState.update { it.copy(errorMessage = "Enter a valid email address") }
            return
        }
        if (!isMobile(current.mobile.trim())) {
            _uiState.update { it.copy(errorMessage = "Enter a valid mobile number") }
            return
        }
        if (current.password != current.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, registrationSuccessMessage = null) }

            when (
                val result = authRepository.register(
                    fullName = current.fullName,
                    mobile = current.mobile,
                    email = current.email,
                    accountType = current.registrationAccountType,
                    password = current.password,
                    confirmPassword = current.confirmPassword
                )
            ) {
                is ApiResult.Success -> {
                    val suggestions = saveSuggestion(current.email)
                    _uiState.update {
                        it.copy(
                            fullName = "",
                            mobile = "",
                            email = "",
                            password = "",
                            confirmPassword = "",
                            registrationAccountType = "Simple Access",
                            isLoading = false,
                            loginSuggestions = suggestions,
                            registrationSuccessMessage = "Application Submitted!\nYour account is under review.",
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                fullName = "",
                mobile = "",
                email = "",
                token = null,
                userId = null,
                customerId = null,
                isAdmin = false,
                isLoggedIn = false,
                password = "",
                confirmPassword = "",
                errorMessage = null
            )
        }
    }

    private fun isEmailOrMobile(value: String): Boolean = isEmail(value) || isMobile(value)

    private fun isEmail(value: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        return emailRegex.matches(value)
    }

    private fun isMobile(value: String): Boolean {
        val mobileRegex = Regex("^[+]?[0-9]{7,15}$")
        return mobileRegex.matches(value)
    }

    private fun saveSuggestion(identifier: String): List<String> {
        return loginSuggestionsStore?.saveIdentifier(identifier).orEmpty()
    }
}
