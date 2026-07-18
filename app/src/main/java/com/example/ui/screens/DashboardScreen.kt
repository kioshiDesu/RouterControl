package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectionStatus
import com.example.ui.RouterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: RouterViewModel,
    modifier: Modifier = Modifier
) {
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val activeLogs by viewModel.activeLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var hotspotUser by remember { mutableStateOf("") }
    var hotspotPass by remember { mutableStateOf("") }
    var hotspotProfile by remember { mutableStateOf("default") }
    var espIp by remember { mutableStateOf("10.0.0.254") }

    var dialogTitle by remember { mutableStateOf("") }
    var dialogContent by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Parse resource print output from logs if found
    val systemResources = remember(activeLogs) {
        val resourceLog = activeLogs.firstOrNull {
            it.command.trim() == "/system resource print" || it.command.trim().lowercase().contains("resource print")
        }
        if (resourceLog != null) {
            parseSystemResources(resourceLog.output)
        } else {
            null
        }
    }

    // Trigger auto-refresh of system resources on connected
    LaunchedEffect(connectedDevice) {
        if (connectedDevice != null && connectionStatus == ConnectionStatus.Connected) {
            // Check if resources have been fetched recently
            val alreadyHasResources = activeLogs.any {
                it.command.trim() == "/system resource print"
            }
            if (!alreadyHasResources) {
                viewModel.runCommand("/system resource print")
            }
        }
    }

    // Gentle polling simulation or trigger helper
    var ticks by remember { mutableIntStateOf(0) }
    LaunchedEffect(connectedDevice, connectionStatus) {
        while (connectedDevice != null && connectionStatus == ConnectionStatus.Connected) {
            delay(5000)
            ticks++
            // Optionally auto refresh system metrics every 30 seconds
            if (ticks % 6 == 0) {
                viewModel.runCommand("/system resource print")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (connectionStatus == ConnectionStatus.Connected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (connectionStatus == ConnectionStatus.Connected) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = "Connection Indicator",
                        modifier = Modifier.size(32.dp),
                        tint = if (connectionStatus == ConnectionStatus.Connected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (connectionStatus == ConnectionStatus.Connected) "Active Router Connection" else "Router Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (connectionStatus == ConnectionStatus.Connected) {
                            "Authenticated as ${connectedDevice?.username} on ${connectedDevice?.host}"
                        } else {
                            "Connect to a router profile in the 'Devices' tab to start monitoring diagnostics."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (connectionStatus == ConnectionStatus.Connected && connectedDevice != null) {
            val device = connectedDevice!!

            // Action Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Health: ${device.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { viewModel.runCommand("/system resource print") },
                    enabled = !isLoading,
                    modifier = Modifier.testTag("refresh_resources_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Metrics")
                    }
                }
            }

            // Visual Diagnostics Block
            if (systemResources != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("system_resource_card"),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "Primary Node".uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = device.host,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 20.sp
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = systemResources.boardName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "v${systemResources.version} Stable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        val freeM = systemResources.freeMem.replace("MiB", "").toDoubleOrNull() ?: 0.0
                        val totalM = systemResources.totalMem.replace("MiB", "").toDoubleOrNull() ?: 256.0
                        val usedM = (totalM - freeM).coerceAtLeast(0.0)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CPU",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = systemResources.cpuLoad,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Memory",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${usedM.toInt()}MB",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Uptime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = systemResources.uptime.substringBefore("s").trim(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Grid Details Card
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Specifications & Resources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        ResourceRow(label = "Uptime", value = systemResources.uptime, icon = Icons.Default.Timer)
                        ResourceRow(label = "RouterOS Version", value = systemResources.version, icon = Icons.Default.Info)
                        ResourceRow(label = "CPU Architecture", value = systemResources.architecture, icon = Icons.Default.Memory)
                        ResourceRow(label = "Board Name", value = systemResources.boardName, icon = Icons.Default.Layers)
                        ResourceRow(label = "Free HDD Space", value = systemResources.freeHdd, icon = Icons.Default.Dns)
                    }
                }
            } else {
                // Loading state / Initial fetch placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Querying router specifications...",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Requesting '/system resource print' payload from your RouterOS session. Standard print returns will be parsed here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Quick actions / commands section (NEW)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("quick_commands_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Quick Commands",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val result = viewModel.executeQuickCommand("/system reboot")
                                    dialogTitle = "Reboot Command Sent"
                                    dialogContent = result ?: "Reboot command was sent to the router successfully. Session will terminate."
                                    showDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh, // Use standard Refresh icon for reboot/restart action
                                contentDescription = "Reboot",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reboot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Hotspot User manager
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "HOTSPOT USER MANAGER",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = hotspotUser,
                            onValueChange = { hotspotUser = it },
                            label = { Text("Username") },
                            placeholder = { Text("e.g. guest1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("hotspot_username_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = hotspotPass,
                                onValueChange = { hotspotPass = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("hotspot_password_input")
                            )

                            OutlinedTextField(
                                value = hotspotProfile,
                                onValueChange = { hotspotProfile = it },
                                label = { Text("Profile") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("hotspot_profile_input")
                            )
                        }

                        Button(
                            onClick = {
                                if (hotspotUser.isNotBlank() && hotspotPass.isNotBlank()) {
                                    val cmd = ":if ([:len [/ip hotspot user find name=\"$hotspotUser\"]] > 0) do={/ip hotspot user set [find name=\"$hotspotUser\"] password=\"$hotspotPass\" profile=\"$hotspotProfile\"} else={/ip hotspot user add name=\"$hotspotUser\" password=\"$hotspotPass\" profile=\"$hotspotProfile\"}"
                                    coroutineScope.launch {
                                        val result = viewModel.executeQuickCommand(cmd)
                                        dialogTitle = "Hotspot Configuration Applied"
                                        dialogContent = result ?: "Success: Hotspot user configuration processed."
                                        showDialog = true
                                        hotspotUser = ""
                                        hotspotPass = ""
                                    }
                                }
                            },
                            enabled = hotspotUser.isNotBlank() && hotspotPass.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("apply_hotspot_button")
                        ) {
                            Text("Apply / Create Hotspot User", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Custom Ping for ESP8266
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CUSTOM DEVICE PING (ESP8266)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = espIp,
                                onValueChange = { espIp = it },
                                label = { Text("IP Address") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("ping_ip_input")
                            )

                            Button(
                                onClick = {
                                    if (espIp.isNotBlank()) {
                                        val cmd = "/ping count=3 $espIp"
                                        coroutineScope.launch {
                                            val result = viewModel.executeQuickCommand(cmd)
                                            dialogTitle = "Ping Results: $espIp"
                                            dialogContent = result ?: "Ping request completed."
                                            showDialog = true
                                        }
                                    }
                                },
                                enabled = espIp.isNotBlank() && !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71), contentColor = Color.White),
                                modifier = Modifier.height(56.dp).testTag("ping_esp_button")
                            ) {
                                Text("Ping ESP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = {
                        Text(
                            text = dialogTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = dialogContent,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF2ECC71)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }

        } else {
            // Dashboard Empty State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = "Router offline",
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                Text(
                    text = "No Connected Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "A live SSH connection is required to monitor interface specifications or run remote diagnostics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
fun CircularProgressGauge(
    fraction: Float,
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        val sweepColor = color
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            // Draw background circle
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Draw progress circle arc
            drawArc(
                color = sweepColor,
                startAngle = 135f,
                sweepAngle = 270f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ResourceRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

data class SystemResources(
    val uptime: String,
    val version: String,
    val freeMem: String,
    val totalMem: String,
    val cpuLoad: String,
    val architecture: String = "mipsbe",
    val boardName: String = "RouterBoard",
    val freeHdd: String = "N/A",
    val cpuFreq: String = "600MHz"
)

fun parseSystemResources(output: String): SystemResources {
    var uptime = "Unknown"
    var version = "Unknown"
    var freeMem = "0MiB"
    var totalMem = "0MiB"
    var cpuLoad = "0%"
    var architecture = "mipsbe"
    var boardName = "RouterBoard"
    var freeHdd = "Unknown"
    var cpuFreq = "Unknown"

    output.lines().forEach { line ->
        val parts = line.split(":")
        if (parts.size >= 2) {
            val key = parts[0].trim().lowercase()
            val value = parts.subList(1, parts.size).joinToString(":").trim()
            when {
                key.contains("uptime") -> uptime = value
                key.contains("version") -> version = value
                key.contains("free-memory") -> freeMem = value
                key.contains("total-memory") -> totalMem = value
                key.contains("cpu-load") -> cpuLoad = value
                key.contains("architecture") -> architecture = value
                key.contains("board-name") -> boardName = value
                key.contains("free-hdd-space") -> freeHdd = value
                key.contains("cpu-frequency") -> cpuFreq = value
            }
        }
    }
    return SystemResources(uptime, version, freeMem, totalMem, cpuLoad, architecture, boardName, freeHdd, cpuFreq)
}
