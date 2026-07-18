package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.WarningYellow
import com.example.ui.theme.RouterBlue
import com.example.ui.theme.RouterOrange
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: RouterViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val context = LocalContext.current

    // Listen to UI notifications/toasts from the ViewModel
    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // High Density Logo
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                    .background(RouterOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                        .background(RouterBlue)
                                )
                            }
                            Text(
                                text = "RouterControl",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }

                        // Status dot LED indicator
                        val ledColor = when (connectionStatus) {
                            ConnectionStatus.Connected -> SuccessGreen
                            ConnectionStatus.Connecting -> WarningYellow
                            ConnectionStatus.Disconnected -> Color.Gray
                            ConnectionStatus.Error -> DangerRed
                        }

                        val statusLabel = when (connectionStatus) {
                            ConnectionStatus.Connected -> "Connected"
                            ConnectionStatus.Connecting -> "Connecting"
                            ConnectionStatus.Disconnected -> "Offline"
                            ConnectionStatus.Error -> "Error"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF162C48)),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ledColor)
                                )
                                Text(
                                    text = statusLabel.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ledColor
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RouterBlue,
                    titleContentColor = Color.White
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("tab_dashboard")
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 1) Icons.Filled.Router else Icons.Outlined.Router,
                            contentDescription = "Devices"
                        )
                    },
                    label = { Text("Devices") },
                    modifier = Modifier.testTag("tab_devices")
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 2) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                            contentDescription = "Terminal"
                        )
                    },
                    label = { Text("Terminal") },
                    modifier = Modifier.testTag("tab_terminal")
                )

                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 3) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Logs"
                        )
                    },
                    label = { Text("Logs") },
                    modifier = Modifier.testTag("tab_logs")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> DashboardScreen(viewModel = viewModel)
                1 -> DevicesScreen(viewModel = viewModel)
                2 -> TerminalScreen(viewModel = viewModel)
                3 -> LogsScreen(viewModel = viewModel)
            }
        }
    }
}
