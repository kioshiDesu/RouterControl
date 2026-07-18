package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeviceProfile
import com.example.ui.ConnectionStatus
import com.example.ui.RouterViewModel
import com.example.ui.PingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    viewModel: RouterViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsState()
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val pingStatuses by viewModel.pingStatuses.collectAsState()

    var showAddForm by remember { mutableStateOf(false) }
    var editingProfileId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var portString by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var isDemo by remember { mutableStateOf(false) }
    var routerModel by remember { mutableStateOf("hAP Lite (v6)") }
    var connectionType by remember { mutableStateOf("L2TP Tunnel") }
    var l2tpSecret by remember { mutableStateOf("") }

    // State for password entry dialog
    var selectedDeviceForPassword by remember { mutableStateOf<DeviceProfile?>(null) }
    var enteredPassword by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Router Profiles",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Button(
                        onClick = {
                            if (showAddForm) {
                                showAddForm = false
                                editingProfileId = null
                                name = ""
                                host = ""
                                portString = "22"
                                username = ""
                                isDemo = false
                                routerModel = "hAP Lite (v6)"
                                connectionType = "L2TP Tunnel"
                                l2tpSecret = ""
                            } else {
                                showAddForm = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showAddForm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("toggle_add_profile_button")
                    ) {
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Toggle Add Profile Form"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddForm) "Cancel" else "Add Router")
                    }
                }
            }

            // Expandable Add Router form
            item {
                AnimatedVisibility(
                    visible = showAddForm,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (editingProfileId != null) "Edit Router Details" else "New Router Details",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Profile Name (e.g. Home Router)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("IP / Hostname (e.g. 192.168.88.1)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_host_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = portString,
                                    onValueChange = { portString = it },
                                    label = { Text("Port") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("profile_port_input"),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Username") },
                                    modifier = Modifier
                                        .weight(2f)
                                        .testTag("profile_user_input"),
                                    singleLine = true
                                )
                            }

                            Text("Router Hardware Model", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("hAP Lite (v6)", "hEX (v7)", "Generic").forEach { model ->
                                    val isSelected = routerModel == model
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                            )
                                            .clickable { routerModel = model }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = model,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Text("Connection Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text("L2TP VPN Tunnel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Connects securely via remote L2TP Tunnel gateway", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = l2tpSecret,
                                onValueChange = { l2tpSecret = it },
                                label = { Text("L2TP IPsec Secret (Optional)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_l2tp_secret_input"),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Simulation Mode",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Test features using a realistic mock router",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = isDemo,
                                    onCheckedChange = { isDemo = it },
                                    modifier = Modifier.testTag("demo_mode_switch")
                                )
                            }

                            Button(
                                onClick = {
                                    val port = portString.toIntOrNull() ?: 22
                                    viewModel.saveProfile(
                                        id = editingProfileId,
                                        name = name,
                                        host = host,
                                        port = port,
                                        username = username,
                                        isDemo = isDemo,
                                        routerModel = routerModel,
                                        connectionType = connectionType,
                                        l2tpSecret = l2tpSecret
                                    )
                                    // Reset form and close
                                    name = ""
                                    host = ""
                                    portString = "22"
                                    username = ""
                                    isDemo = false
                                    routerModel = "hAP Lite (v6)"
                                    connectionType = "L2TP Tunnel"
                                    l2tpSecret = ""
                                    editingProfileId = null
                                    showAddForm = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_profile_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save Profile")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (editingProfileId != null) "Update Profile" else "Save Profile")
                            }
                        }
                    }
                }
            }

            if (profiles.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Computer,
                            contentDescription = "No devices",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No Saved Routers",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Add your first MikroTik router details to monitor resources, test SSH execution and run diagnostics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Button(onClick = { showAddForm = true }) {
                            Text("Create Router Profile")
                        }
                    }
                }
            } else {
                items(profiles) { profile ->
                    val isCurrent = connectedDevice?.id == profile.id
                    val isConnected = isCurrent && connectionStatus == ConnectionStatus.Connected

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_card_${profile.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        imageVector = if (profile.isDemo) Icons.Default.Dns else Icons.Default.Router,
                                        contentDescription = "Router",
                                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (profile.isDemo) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Simulation") },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                labelColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    } else {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("SSH") }
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            editingProfileId = profile.id
                                            name = profile.name
                                            host = profile.host
                                            portString = profile.port.toString()
                                            username = profile.username
                                            isDemo = profile.isDemo
                                            routerModel = profile.routerModel
                                            connectionType = profile.connectionType
                                            l2tpSecret = profile.l2tpSecret
                                            showAddForm = true
                                        },
                                        modifier = Modifier.testTag("edit_profile_${profile.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Profile",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteProfile(profile) },
                                        modifier = Modifier.testTag("delete_profile_${profile.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Profile",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Host: ${profile.host}:${profile.port}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Username: ${profile.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (profile.lastConnected != null) {
                                    Text(
                                        text = "Last Connected: ${profile.lastConnected}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = "Model: ${profile.routerModel} (${profile.connectionType})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (profile.connectionType == "L2TP Tunnel" && profile.l2tpSecret.isNotEmpty()) {
                                    Text(
                                        text = "L2TP IPsec: Active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                                
                                val pingStatus = pingStatuses[profile.id] ?: PingStatus.Idle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val (indicatorColor, statusText) = when (pingStatus) {
                                            is PingStatus.Idle -> Pair(MaterialTheme.colorScheme.outline, "Status: Tap to check")
                                            is PingStatus.Checking -> Pair(androidx.compose.ui.graphics.Color(0xFFE67E22), "Pinging...")
                                            is PingStatus.Online -> Pair(androidx.compose.ui.graphics.Color(0xFF2ECC71), "Online (${pingStatus.latencyMs}ms)")
                                            is PingStatus.Offline -> Pair(MaterialTheme.colorScheme.error, "Offline / Unreachable")
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(indicatorColor, shape = androidx.compose.foundation.shape.CircleShape)
                                        )
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.pingDevice(profile) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("ping_device_${profile.id}")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Ping", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ping", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (isConnected) {
                                    Button(
                                        onClick = { viewModel.disconnect() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.testTag("disconnect_button_${profile.id}")
                                    ) {
                                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Disconnect")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Disconnect")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            if (profile.isDemo) {
                                                viewModel.connectDevice(profile, "")
                                            } else {
                                                selectedDeviceForPassword = profile
                                            }
                                        },
                                        modifier = Modifier.testTag("connect_button_${profile.id}")
                                    ) {
                                        Icon(Icons.Default.Power, contentDescription = "Connect")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Connect")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SSH Password Entry Dialog
        selectedDeviceForPassword?.let { profile ->
            AlertDialog(
                onDismissRequest = {
                    selectedDeviceForPassword = null
                    enteredPassword = ""
                },
                title = { Text("SSH Password for ${profile.username}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "To establish a secure SSH tunnel to ${profile.host}, enter your RouterOS credentials. This password stays strictly in memory for this session and is never persisted to disk.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = enteredPassword,
                            onValueChange = { enteredPassword = it },
                            label = { Text("Password") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ssh_password_input"),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.connectDevice(profile, enteredPassword)
                            selectedDeviceForPassword = null
                            enteredPassword = ""
                        },
                        modifier = Modifier.testTag("submit_password_button")
                    ) {
                        Text("Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedDeviceForPassword = null
                        enteredPassword = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
