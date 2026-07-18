package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectionStatus
import com.example.ui.RouterViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalScreen(
    viewModel: RouterViewModel,
    modifier: Modifier = Modifier
) {
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val terminalLines by viewModel.terminalLines.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var customCommand by remember { mutableStateOf("") }
    var showRebootConfirm by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    // Automatically scroll to bottom on new terminal lines
    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(terminalLines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (connectionStatus != ConnectionStatus.Connected || connectedDevice == null) {
            // Not connected placeholder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal Locked",
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Terminal Session Locked",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Establish a router profile connection inside the 'Devices' screen to open the terminal CLI session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            return
        }

        val device = connectedDevice!!

        // Quick Command Buttons Flow
        Text(
            text = "Quick Diagnostics (RouterOS CLI)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { viewModel.runCommand("/ip address print") },
                enabled = !isLoading,
                modifier = Modifier.testTag("cmd_ip_addresses")
            ) {
                Text("/ip address print", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            FilledTonalButton(
                onClick = { viewModel.runCommand("/interface print") },
                enabled = !isLoading,
                modifier = Modifier.testTag("cmd_interfaces")
            ) {
                Text("/interface print", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            FilledTonalButton(
                onClick = { viewModel.runCommand("/system resource print") },
                enabled = !isLoading,
                modifier = Modifier.testTag("cmd_resources")
            ) {
                Text("/system resource print", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            FilledTonalButton(
                onClick = { viewModel.runCommand("/log print") },
                enabled = !isLoading,
                modifier = Modifier.testTag("cmd_logs")
            ) {
                Text("/log print", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = { showRebootConfirm = true },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.testTag("cmd_reboot")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("/system reboot", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Output Shell Area Header with Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Console Logs: [admin@${device.name}]",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val fullLog = terminalLines.joinToString("\n") { it.text }
                        clipboardManager.setText(AnnotatedString(fullLog))
                    },
                    modifier = Modifier.size(36.dp).testTag("copy_terminal_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Terminal stdout",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.size(36.dp).testTag("exit_terminal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Close session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Terminal Log Console Viewport
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF121212), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2A2A2A), shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            // macOS-style Terminal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF252525))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F56), shape = CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), shape = CircleShape))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF27C93F), shape = CircleShape))
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "terminal_session.01",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                Text(
                    text = "SSH-RSA-2048",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(terminalLines) { line ->
                        val textColor = when {
                            line.isError -> Color(0xFFF87171)   // Soft red
                            line.isSystem -> Color(0xFF60A5FA)  // Soft light-blue
                            line.text.startsWith("[") -> Color(0xFF2ECC71) // User input prompt green
                            else -> Color(0xFFE2E8F0)           // Slate white
                        }
                        Text(
                            text = line.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = textColor,
                            lineHeight = 15.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2ECC71)
                    )
                }
            }
        }

        // CLI Manual Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customCommand,
                onValueChange = { customCommand = it },
                placeholder = { Text("Type custom RouterOS CLI command...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_command_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        if (customCommand.isNotBlank() && !isLoading) {
                            viewModel.runCommand(customCommand)
                            customCommand = ""
                        }
                    }
                )
            )

            FloatingActionButton(
                onClick = {
                    if (customCommand.isNotBlank() && !isLoading) {
                        viewModel.runCommand(customCommand)
                        customCommand = ""
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Execute Command",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Reboot Confirmation Modal (PRD #4.4 mandate)
    if (showRebootConfirm) {
        AlertDialog(
            onDismissRequest = { showRebootConfirm = false },
            title = { Text("Confirm System Reboot") },
            text = {
                Text("Are you sure you want to run the '/system reboot' command? This action will immediately terminate the SSH session and restart the physical MikroTik router. Active routing paths may experience temporary downtime.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.runCommand("/system reboot")
                        showRebootConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reboot Router")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
