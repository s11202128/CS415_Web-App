package com.bof.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bof.mobile.data.remote.NetworkModule
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.data.repository.AdminRepository
import com.bof.mobile.data.repository.AuthRepository
import com.bof.mobile.data.repository.DashboardRepository
import com.bof.mobile.data.repository.FeatureRepository
import com.bof.mobile.data.repository.TransferRepository
import com.bof.mobile.model.DashboardAccount
import com.bof.mobile.ui.accounts.AccountsScreen
import com.bof.mobile.ui.accounts.CreateAccountScreen
import com.bof.mobile.ui.admin.AdminDashboardScreen
import com.bof.mobile.ui.auth.LoginScreen
import com.bof.mobile.ui.auth.RegisterScreen
import com.bof.mobile.ui.billpayment.BillPaymentScreen
import com.bof.mobile.ui.dashboard.DashboardScreen
import com.bof.mobile.ui.deposit.DepositScreen
import com.bof.mobile.ui.features.FeatureHubScreen
import com.bof.mobile.ui.transfers.TransferScreen
import com.bof.mobile.ui.withdraw.WithdrawScreen
import com.bof.mobile.viewmodel.AccountsViewModel
import com.bof.mobile.viewmodel.AdminViewModel
import com.bof.mobile.viewmodel.AuthViewModel
import com.bof.mobile.viewmodel.DashboardViewModel
import com.bof.mobile.viewmodel.FeatureViewModel
import com.bof.mobile.viewmodel.TransferViewModel

private enum class MainTab {
    DASHBOARD,
    CREATE_ACCOUNT,
    ACCOUNTS,
    TRANSFERS,
    FEATURES,
    DEPOSIT,
    WITHDRAW,
    BILL_PAYMENT
}

@Composable
fun AppRoot() {
    val authViewModel = remember { AuthViewModel(AuthRepository(NetworkModule.createApiService { null })) }
    val authState by authViewModel.uiState.collectAsState()
    
    // Create apiService with current token - recreate when token changes
    val apiService = remember(authState.token) { 
        NetworkModule.createApiService { authState.token } 
    }

    // Recreate ViewModels when apiService changes (token changes)
    val accountRepository = remember(apiService) { AccountRepository(apiService) }
    val dashboardViewModel = remember(apiService) { DashboardViewModel(DashboardRepository(apiService)) }
    val accountsViewModel = remember(apiService) { AccountsViewModel(accountRepository) }
    val transferViewModel = remember(apiService) { TransferViewModel(TransferRepository(apiService)) }
    val featureViewModel = remember(apiService) { FeatureViewModel(FeatureRepository(apiService)) }
    val adminViewModel = remember(apiService) { AdminViewModel(AdminRepository(apiService)) }

    var showRegister by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    val navigationHistory = remember { mutableStateListOf<MainTab>() }

    fun navigateTo(tab: MainTab) {
        if (activeTab != tab) {
            navigationHistory.add(activeTab)
            activeTab = tab
        }
    }

    fun goBack() {
        if (navigationHistory.isNotEmpty()) {
            activeTab = navigationHistory.removeAt(navigationHistory.lastIndex)
        }
    }

    fun logout() {
        authViewModel.logout()
        showRegister = false
        activeTab = MainTab.DASHBOARD
        navigationHistory.clear()
    }

    if (!authState.isLoggedIn) {
        if (showRegister) {
            RegisterScreen(
                viewModel = authViewModel,
                onBackToLogin = { showRegister = false }
            )
        } else {
            LoginScreen(
                viewModel = authViewModel,
                onOpenRegister = { showRegister = true }
            )
        }
        return
    }

    val customerId = authState.customerId ?: authState.userId ?: 0
    val accountsState by accountsViewModel.uiState.collectAsState()
    val accountsFromRoute = accountsState.accounts.map {
        DashboardAccount(
            id = it.id,
            accountNumber = it.accountNumber,
            accountHolder = it.accountHolder,
            accountType = it.type,
            balance = it.balance,
            status = it.status
        )
    }
    val moneyAccounts = accountsFromRoute

    if (authState.isAdmin) {
        AdminDashboardScreen(
            viewModel = adminViewModel,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onLogout = { logout() }
        )
        return
    }

    when (activeTab) {
        MainTab.DASHBOARD -> DashboardScreen(
            viewModel = dashboardViewModel,
            featureViewModel = featureViewModel,
            customerId = customerId,
            onLogout = { logout() },
            onNavigateToTransfers = { navigateTo(MainTab.TRANSFERS) },
            onNavigateToCreateAccount = { navigateTo(MainTab.ACCOUNTS) },
            onNavigateToAccounts = { navigateTo(MainTab.ACCOUNTS) },
            onNavigateToFeatures = { navigateTo(MainTab.FEATURES) },
            onNavigateToDeposit = { navigateTo(MainTab.DEPOSIT) },
            onNavigateToWithdraw = { navigateTo(MainTab.WITHDRAW) },
            onNavigateToFunding = { navigateTo(MainTab.FEATURES) },
            onNavigateToBillPayment = { navigateTo(MainTab.BILL_PAYMENT) }
        )
        MainTab.CREATE_ACCOUNT -> CreateAccountScreen(
            accountRepository = accountRepository,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onAccountCreated = { createdAccount ->
                accountsViewModel.upsertAccount(createdAccount)
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
                navigateTo(MainTab.ACCOUNTS)
            }
        )
        MainTab.ACCOUNTS -> AccountsScreen(
            viewModel = accountsViewModel,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onNavigateToCreateAccount = { navigateTo(MainTab.CREATE_ACCOUNT) }
        )
        MainTab.TRANSFERS -> TransferScreen(
            viewModel = transferViewModel,
            accountsList = moneyAccounts,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onTransferCompleted = {
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
            }
        )
        MainTab.FEATURES -> FeatureHubScreen(
            viewModel = featureViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() }
        )
        MainTab.DEPOSIT -> DepositScreen(
            featureViewModel = featureViewModel,
            accountsList = moneyAccounts,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onDepositCompleted = {
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
            }
        )
        MainTab.WITHDRAW -> WithdrawScreen(
            featureViewModel = featureViewModel,
            accountsList = moneyAccounts,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onWithdrawCompleted = {
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
            }
        )
        MainTab.BILL_PAYMENT -> BillPaymentScreen(
            viewModel = featureViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() }
        )
    }

    LaunchedEffect(customerId, authState.isLoggedIn, authState.isAdmin) {
        if (authState.isLoggedIn && !authState.isAdmin) {
            dashboardViewModel.loadDashboard(customerId.takeIf { it > 0 })
            accountsViewModel.loadAccounts()
        }
    }

    LaunchedEffect(activeTab, customerId) {
        if (activeTab == MainTab.ACCOUNTS && authState.isLoggedIn && !authState.isAdmin) {
            accountsViewModel.loadAccounts()
        }
        if ((activeTab == MainTab.TRANSFERS || activeTab == MainTab.DEPOSIT || activeTab == MainTab.WITHDRAW) && accountsState.accounts.isEmpty() && authState.isLoggedIn && !authState.isAdmin) {
            accountsViewModel.loadAccounts()
        }
    }
}
