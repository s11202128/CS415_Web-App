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
import com.bof.mobile.ui.accounts.AccountsScreen
import com.bof.mobile.ui.admin.AdminDashboardScreen
import com.bof.mobile.ui.auth.LoginScreen
import com.bof.mobile.ui.auth.RegisterScreen
import com.bof.mobile.ui.dashboard.DashboardScreen
import com.bof.mobile.ui.features.FeatureHubScreen
import com.bof.mobile.ui.transfers.TransferScreen
import com.bof.mobile.viewmodel.AccountsViewModel
import com.bof.mobile.viewmodel.AdminViewModel
import com.bof.mobile.viewmodel.AuthViewModel
import com.bof.mobile.viewmodel.DashboardViewModel
import com.bof.mobile.viewmodel.FeatureViewModel
import com.bof.mobile.viewmodel.TransferViewModel

private enum class MainTab {
    DASHBOARD,
    ACCOUNTS,
    TRANSFERS,
    FEATURES
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
    val dashboardViewModel = remember(apiService) { DashboardViewModel(DashboardRepository(apiService)) }
    val accountsViewModel = remember(apiService) { AccountsViewModel(AccountRepository(apiService)) }
    val transferViewModel = remember(apiService) { TransferViewModel(TransferRepository(apiService)) }
    val featureViewModel = remember(apiService) { FeatureViewModel(FeatureRepository(apiService)) }
    val adminViewModel = remember(apiService) { AdminViewModel(AdminRepository(apiService)) }

    var showRegister by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }

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

    val customerId = authState.customerId ?: 0

    if (authState.isAdmin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Welcome Admin ${authState.fullName}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    authViewModel.logout()
                    showRegister = false
                    activeTab = MainTab.DASHBOARD
                }) { Text("Logout") }
            }

            Spacer(modifier = Modifier.height(12.dp))
            AdminDashboardScreen(viewModel = adminViewModel)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
            verticalArrangement = Arrangement.Top
    ) {
        Text("Welcome ${authState.fullName}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { activeTab = MainTab.DASHBOARD }) { Text("Dashboard") }
            Button(onClick = { activeTab = MainTab.ACCOUNTS }) { Text("Accounts") }
            Button(onClick = { activeTab = MainTab.TRANSFERS }) { Text("Transfers") }
            Button(onClick = { activeTab = MainTab.FEATURES }) { Text("Features") }
            Button(onClick = {
                authViewModel.logout()
                showRegister = false
                activeTab = MainTab.DASHBOARD
            }) { Text("Logout") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            MainTab.DASHBOARD -> DashboardScreen(
                viewModel = dashboardViewModel, 
                customerId = customerId,
                onLogout = { 
                    authViewModel.logout()
                    showRegister = false
                    activeTab = MainTab.DASHBOARD
                },
                onNavigateToTransfers = { activeTab = MainTab.TRANSFERS },
                onNavigateToAccounts = { activeTab = MainTab.ACCOUNTS },
                onNavigateToFeatures = { activeTab = MainTab.FEATURES }
            )
            MainTab.ACCOUNTS -> AccountsScreen(viewModel = accountsViewModel)
            MainTab.TRANSFERS -> TransferScreen(viewModel = transferViewModel)
            MainTab.FEATURES -> FeatureHubScreen(viewModel = featureViewModel, customerId = customerId)
        }
    }
}
