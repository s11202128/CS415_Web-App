package com.bof.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bof.mobile.data.remote.NetworkModule
import com.bof.mobile.data.repository.AccountRepository
import com.bof.mobile.data.repository.AdminRepository
import com.bof.mobile.data.repository.AuthRepository
import com.bof.mobile.data.repository.DashboardRepository
import com.bof.mobile.data.repository.FeatureRepository
import com.bof.mobile.data.repository.TransferRepository
import com.bof.mobile.ui.accounts.AccountsScreen
import com.bof.mobile.ui.accounts.CreateAccountScreen
import com.bof.mobile.ui.admin.AdminDashboardScreen
import com.bof.mobile.ui.activity.ActivityLogScreen
import com.bof.mobile.ui.auth.LoginScreen
import com.bof.mobile.ui.auth.RegisterScreen
import com.bof.mobile.ui.billpayment.BillPaymentScreen
import com.bof.mobile.ui.dashboard.DashboardScreen
import com.bof.mobile.ui.deposit.DepositScreen
import com.bof.mobile.ui.features.FeatureHubScreen
import com.bof.mobile.ui.funding.FundingScreen
import com.bof.mobile.ui.report.AccountOverviewReportScreen
import com.bof.mobile.ui.statement.StatementScreen
import com.bof.mobile.ui.transfers.TransferScreen
import com.bof.mobile.ui.withdraw.WithdrawScreen
import com.bof.mobile.viewmodel.AccountsViewModel
import com.bof.mobile.viewmodel.AdminViewModel
import com.bof.mobile.viewmodel.AuthViewModel
import com.bof.mobile.viewmodel.CreateAccountViewModel
import com.bof.mobile.viewmodel.DashboardViewModel
import com.bof.mobile.viewmodel.DepositViewModel
import com.bof.mobile.viewmodel.FeatureViewModel
import com.bof.mobile.viewmodel.TransferViewModel
import com.bof.mobile.viewmodel.WithdrawViewModel

private enum class MainTab {
    DASHBOARD,
    CREATE_ACCOUNT,
    ACCOUNTS,
    TRANSFERS,
    STATEMENT,
    REPORT,
    ACTIVITY,
    FEATURES,
    FUNDING,
    DEPOSIT,
    WITHDRAW,
    BILL_PAYMENT
}

@Composable
fun AppRoot() {
    val authViewModel = remember { AuthViewModel(AuthRepository(NetworkModule.createApiService { null })) }
    val authState by authViewModel.uiState.collectAsState()
    
    // Capture token as an immutable snapshot so OkHttp threads never read Compose state directly.
    val apiService = remember(authState.token) {
        val tokenSnapshot = authState.token
        NetworkModule.createApiService { tokenSnapshot }
    }

    // Recreate ViewModels when apiService changes (token changes)
    val accountRepository = remember(apiService) { AccountRepository(apiService) }
    val featureRepository = remember(apiService) { FeatureRepository(apiService) }
    val dashboardViewModel = remember(apiService) { DashboardViewModel(DashboardRepository(apiService)) }
    val accountsViewModel = remember(apiService) { AccountsViewModel(accountRepository) }
    val createAccountViewModel = remember(apiService) { CreateAccountViewModel(accountRepository) }
    val depositViewModel = remember(apiService) { DepositViewModel(accountRepository, featureRepository) }
    val withdrawViewModel = remember(apiService) { WithdrawViewModel(accountRepository, featureRepository) }
    val transferViewModel = remember(apiService, authState.customerId, authState.userId) {
        TransferViewModel(
            transferRepository = TransferRepository(apiService),
            accountRepository = accountRepository,
            loggedInCustomerId = authState.customerId ?: authState.userId
        )
    }
    val featureViewModel = remember(apiService) { FeatureViewModel(featureRepository) }
    val adminViewModel = remember(apiService) { AdminViewModel(AdminRepository(apiService)) }

    var showRegister by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    val navigationHistory = remember { mutableStateListOf<MainTab>() }

    fun navigateTo(tab: MainTab) {
        if (activeTab != tab) {
            navigationHistory.add(activeTab)
            activeTab = tab
            featureViewModel.logClientActivity(
                activityType = "NAVIGATION",
                description = "Opened ${tab.name} screen",
                status = "success"
            )
        }
    }

    fun goBack() {
        if (navigationHistory.isNotEmpty()) {
            activeTab = navigationHistory.removeAt(navigationHistory.lastIndex)
        }
    }

    fun logout() {
        featureViewModel.logClientActivity(
            activityType = "LOGOUT",
            description = "Customer logout",
            status = "success"
        )
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
            onNavigateToWithdraw = { navigateTo(MainTab.WITHDRAW) },
            onNavigateToBillPayment = { navigateTo(MainTab.BILL_PAYMENT) },
            onNavigateToStatement = { navigateTo(MainTab.STATEMENT) },
            onNavigateToReport = { navigateTo(MainTab.REPORT) },
            onNavigateToActivity = { navigateTo(MainTab.ACTIVITY) }
        )
        MainTab.CREATE_ACCOUNT -> CreateAccountScreen(
            viewModel = createAccountViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onAccountCreated = { createdAccount ->
                accountsViewModel.upsertAccount(createdAccount)
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
                depositViewModel.loadAccounts()
                withdrawViewModel.loadAccounts()
                transferViewModel.loadAccounts()
                featureViewModel.loadAccounts()
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
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onTransferCompleted = {
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
                transferViewModel.loadAccounts()
            }
        )
        MainTab.FEATURES -> FeatureHubScreen(
            viewModel = featureViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() }
        )
        MainTab.FUNDING -> FundingScreen(
            viewModel = featureViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() }
        )
        MainTab.STATEMENT -> StatementScreen(
            viewModel = featureViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() }
        )
        MainTab.REPORT -> AccountOverviewReportScreen(
            viewModel = featureViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() }
        )
        MainTab.ACTIVITY -> ActivityLogScreen(
            viewModel = featureViewModel,
            customerId = customerId,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() }
        )
        MainTab.DEPOSIT -> DepositScreen(
            viewModel = depositViewModel,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onDepositCompleted = {
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
            }
        )
        MainTab.WITHDRAW -> WithdrawScreen(
            viewModel = withdrawViewModel,
            canGoBack = navigationHistory.isNotEmpty(),
            onBack = { goBack() },
            onWithdrawCompleted = {
                dashboardViewModel.loadDashboard(customerId)
                accountsViewModel.loadAccounts()
                withdrawViewModel.loadAccounts()
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
            featureViewModel.loadInitialData(customerId)
            featureViewModel.loadAccounts()
            depositViewModel.loadAccounts()
            withdrawViewModel.loadAccounts()
            transferViewModel.loadAccounts()
        }
    }

    LaunchedEffect(activeTab, customerId) {
        if (activeTab == MainTab.ACCOUNTS && authState.isLoggedIn && !authState.isAdmin) {
            accountsViewModel.loadAccounts()
        }
    }
}
