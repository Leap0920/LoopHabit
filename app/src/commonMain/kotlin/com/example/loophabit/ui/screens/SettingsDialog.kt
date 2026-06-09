package com.example.loophabit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.loophabit.data.SyncState
import com.example.loophabit.ui.LoopHabitViewModel
import com.example.loophabit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: LoopHabitViewModel,
    onDismiss: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onResetData: () -> Unit,
    onLogout: () -> Unit
) {
    val autoBackupInterval by viewModel.autoBackupInterval.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var showResetConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Settings",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sync status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (syncState) {
                                is SyncState.Idle -> Icons.Default.CheckCircle
                                is SyncState.Syncing -> Icons.Default.Sync
                                is SyncState.Completed -> Icons.Default.CheckCircle
                                is SyncState.Error -> Icons.Default.Error
                                else -> Icons.Default.HelpOutline
                            },
                            contentDescription = null,
                            tint = when (syncState) {
                                is SyncState.Idle -> MaterialTheme.colorScheme.primary
                                is SyncState.Syncing -> MaterialTheme.colorScheme.tertiary
                                is SyncState.Completed -> MaterialTheme.colorScheme.primary
                                is SyncState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sync Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (syncState) {
                                    is SyncState.Idle -> "Idle"
                                    is SyncState.Syncing -> "Syncing: ${(syncState as SyncState.Syncing).message}"
                                    is SyncState.Completed -> "Completed"
                                    is SyncState.Error -> "Error: ${(syncState as SyncState.Error).message}"
                                    else -> "Unknown"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Data management section
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Export data
                OutlinedButton(
                    onClick = onExportData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Data")
                }

                // Import data
                OutlinedButton(
                    onClick = onImportData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Data")
                }

                HorizontalDivider()

                // Danger zone
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                // Reset all data
                Button(
                    onClick = { showResetConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Data")
                }

                HorizontalDivider()

                // Logout
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Reset confirmation dialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = {
                Text(
                    text = "Reset All Data",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text("This will permanently delete all your habits, completions, and focus sessions. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        onResetData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
