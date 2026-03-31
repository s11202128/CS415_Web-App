package com.bof.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bof.mobile.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel, 
    customerId: Int,
    onLogout: () -> Unit,
    onNavigateToTransfers: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToFeatures: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadDashboard(customerId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFF1F6FF), Color(0xFFE8FFF9), Color(0xFFFFFFFF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Top Row: Logout Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LogoutButton(onLogout = onLogout)
            }
            // Hero header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = Color.Transparent
            ) {
                Column {
                    Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Your account summary", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Features Tab Section
            FeaturesTabSection()

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                DashboardMessageBanner(
                    text = uiState.errorMessage ?: "",
                    isError = true,
                    onDismiss = { viewModel.loadDashboard(customerId) },
                    actionLabel = "Retry"
                )
                return@Column
            }

            val data = uiState.data
            if (data == null) {
                DashboardMessageBanner(text = "No dashboard data available", isError = true)
                return@Column
            }

            val totalBalance = data.accounts.sumOf { it.balance }
            val activeAccounts = data.accounts.count { it.status.equals("active", ignoreCase = true) }

            // Summary cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Welcome",
                    value = data.customer.fullName,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Total Balance",
                    value = "FJD ${"%.2f".format(totalBalance)}",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Active",
                    value = activeAccounts.toString(),
                    modifier = Modifier.weight(0.6f)
                )
            }

            // Recent Transactions Section with horizontal scroll and sticky header
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .horizontalScroll(scrollState)
                        .padding(12.dp)
                ) {
                    // Sticky header row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(vertical = 8.dp),
                    ) {
                        Text("Type", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold)
                        Text("Description", modifier = Modifier.width(220.dp), fontWeight = FontWeight.Bold)
                        Text("Amount", modifier = Modifier.width(120.dp), fontWeight = FontWeight.Bold)
                    }
                    // Data rows
                    data.recentTransactions.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tx.type.uppercase(), modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(tx.description, modifier = Modifier.width(220.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("FJD ${"%.2f".format(tx.amount)}", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturesTabSection() {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text("Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureButton("Transfer", onClick = { /* TODO: Navigate to Transfer */ })
            FeatureButton("Pay Bills", onClick = { /* TODO: Navigate to Pay Bills */ })
            FeatureButton("Statements", onClick = { /* TODO: Navigate to Statements */ })
            FeatureButton("Profile", onClick = { /* TODO: Navigate to Profile */ })
        }
    }
}

@Composable
private fun RowScope.FeatureButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED1C24)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(4.dp)
    ) {
        Text("Logout", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DashboardMessageBanner(
    text: String,
    isError: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onDismiss != null) {
                Button(onClick = onDismiss, modifier = Modifier.padding(start = 8.dp)) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
