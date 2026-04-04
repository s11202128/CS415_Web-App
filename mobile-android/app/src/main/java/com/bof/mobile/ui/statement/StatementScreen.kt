package com.bof.mobile.ui.statement

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bof.mobile.ui.components.ScreenHeader
import com.bof.mobile.viewmodel.FeatureViewModel
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    viewModel: FeatureViewModel,
    customerId: Int,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var accountMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        viewModel.initialize(customerId)
        viewModel.loadCustomerAccounts()
        viewModel.initializeStatementDateDefaults()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader(
                title = "Bank Statement",
                subtitle = "View all your account transactions and download PDF.",
                onBack = onBack,
                enabled = canGoBack
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF1976D2), Color(0xFF5E35B1))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Customer Statement", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Pick your from and to dates to generate your statement.",
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                StatementBanner(
                    text = uiState.errorMessage ?: "",
                    isError = true,
                    onAction = viewModel::clearMessages,
                    actionLabel = "Clear"
                )
            }

            if (!uiState.successMessage.isNullOrBlank()) {
                StatementBanner(text = uiState.successMessage ?: "", isError = false)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Date Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = !accountMenuExpanded }
                    ) {
                        val selectedAccount = uiState.customerAccounts.firstOrNull {
                            it.id.toString() == uiState.statementAccountId
                        }
                        val selectedText = selectedAccount?.let { "${it.accountNumber} (${it.type})" }
                            ?: "Select account"

                        OutlinedTextField(
                            value = selectedText,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !uiState.isLoading && uiState.customerAccounts.isNotEmpty(),
                            label = { Text("Account") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            uiState.customerAccounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.accountNumber} (${account.type})") },
                                    onClick = {
                                        viewModel.onStatementAccountIdChanged(account.id.toString())
                                        viewModel.onStatementAccountNumberChanged(account.accountNumber)
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.statementFromDate,
                        onValueChange = viewModel::onStatementFromDateChanged,
                        readOnly = false,
                        enabled = !uiState.isLoading,
                        label = { Text("From Date") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Format: yyyy-MM-dd") }
                    )

                    OutlinedTextField(
                        value = uiState.statementToDate,
                        onValueChange = viewModel::onStatementToDateChanged,
                        readOnly = false,
                        enabled = !uiState.isLoading,
                        label = { Text("To Date") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Format: yyyy-MM-dd") }
                    )

                    Button(
                        onClick = viewModel::loadBankStatement,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isLoading) "Generating..." else "Generate Statement")
                    }

                    OutlinedButton(
                        enabled = !uiState.isLoading,
                        onClick = {
                            viewModel.downloadBankStatementPdf { bytes, fileName ->
                                saveStatementPdfAndOpen(context, bytes, fileName)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isLoading) "Please wait..." else "Download PDF")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (uiState.statementTransactions.isEmpty()) {
                        Text("No transactions found for the selected period", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.statementTransactions.forEach { row ->
                            Text(
                                text = "${row.date.take(10)}  ${row.transactionType.uppercase()}  FJD ${"%.2f".format(row.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${row.description} | Bal: FJD ${"%.2f".format(row.balance)} | Acct: ${row.accountNumber}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun saveStatementPdfAndOpen(context: Context, bytes: ByteArray, fileName: String) {
    val stampedFileName = buildDownloadFileName(fileName)
    val pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveToPublicDownloadsWithMediaStore(context, bytes, stampedFileName)
    } else {
        saveToPublicDownloadsLegacy(context, bytes, stampedFileName)
    }

    Toast.makeText(context, "Saved to Downloads/BankOfFiji/$stampedFileName", Toast.LENGTH_LONG).show()
    openPdfFile(context, pdfUri)
}

private fun buildDownloadFileName(baseName: String): String {
    val cleanBase = baseName.removeSuffix(".pdf")
    val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    return "$cleanBase-$stamp.pdf"
}

private fun saveToPublicDownloadsWithMediaStore(context: Context, bytes: ByteArray, fileName: String): Uri {
    val resolver = context.contentResolver
    val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/BankOfFiji"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
        ?: throw IOException("Failed to create Downloads entry")

    resolver.openOutputStream(uri)?.use { stream ->
        stream.write(bytes)
        stream.flush()
    } ?: throw IOException("Failed to open output stream")

    val finalizeValues = ContentValues().apply {
        put(MediaStore.MediaColumns.IS_PENDING, 0)
    }
    resolver.update(uri, finalizeValues, null, null)
    return uri
}

private fun saveToPublicDownloadsLegacy(context: Context, bytes: ByteArray, fileName: String): Uri {
    val downloadsDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "BankOfFiji"
    )
    if (!downloadsDir.exists()) {
        downloadsDir.mkdirs()
    }
    val outputFile = File(downloadsDir, fileName)
    outputFile.writeBytes(bytes)

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
    )
}

private fun openPdfFile(context: Context, fileUri: Uri) {
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(fileUri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (openIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(openIntent)
    } else {
        Toast.makeText(context, "No PDF app found to open file", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun StatementBanner(
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
