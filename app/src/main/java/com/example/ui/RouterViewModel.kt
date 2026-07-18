package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CommandLog
import com.example.data.model.DeviceProfile
import com.example.data.repository.RouterRepository
import com.example.network.CommandResult
import com.example.network.ConnectionResult
import com.example.network.SshClientManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RouterViewModel(
    application: Application,
    private val repository: RouterRepository
) : AndroidViewModel(application) {

    private val sshManager = SshClientManager()

    // Saved device profiles from Room
    val profiles: StateFlow<List<DeviceProfile>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current state indicators
    private val _connectedDevice = MutableStateFlow<DeviceProfile?>(null)
    val connectedDevice: StateFlow<DeviceProfile?> = _connectedDevice.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentPassword = MutableStateFlow<String>("") // Saved purely in memory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pingStatuses = MutableStateFlow<Map<String, PingStatus>>(emptyMap())
    val pingStatuses: StateFlow<Map<String, PingStatus>> = _pingStatuses.asStateFlow()

    // Terminal session feed
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    // Filtered command logs from DB for the connected device
    val activeLogs: StateFlow<List<CommandLog>> = _connectedDevice
        .flatMapLatest { device ->
            if (device != null) {
                repository.getLogsForDevice(device.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI event notifications (one-shot flows)
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    fun saveProfile(
        id: String? = null,
        name: String,
        host: String,
        port: Int,
        username: String,
        isDemo: Boolean,
        routerModel: String = "hAP Lite (v6)",
        connectionType: String = "L2TP Tunnel",
        l2tpSecret: String = ""
    ) {
        viewModelScope.launch {
            if (name.isBlank() || host.isBlank() || username.isBlank()) {
                _uiEvents.emit(UiEvent.ShowToast("Please fill in all required profile fields.", isError = true))
                return@launch
            }
            val profile = if (id != null) {
                DeviceProfile(
                    id = id,
                    name = name,
                    host = host,
                    port = port,
                    username = username,
                    isDemo = isDemo,
                    routerModel = routerModel,
                    connectionType = connectionType,
                    l2tpSecret = l2tpSecret
                )
            } else {
                DeviceProfile(
                    name = name,
                    host = host,
                    port = port,
                    username = username,
                    isDemo = isDemo,
                    routerModel = routerModel,
                    connectionType = connectionType,
                    l2tpSecret = l2tpSecret
                )
            }
            repository.insertProfile(profile)
            val msg = if (id != null) "Profile '$name' updated successfully." else "Profile '$name' saved successfully."
            _uiEvents.emit(UiEvent.ShowToast(msg, isError = false))
        }
    }

    fun deleteProfile(profile: DeviceProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (_connectedDevice.value?.id == profile.id) {
                disconnect()
            }
            _uiEvents.emit(UiEvent.ShowToast("Profile '${profile.name}' deleted.", isError = false))
        }
    }

    fun connectDevice(profile: DeviceProfile, password: String) {
        viewModelScope.launch {
            if (!profile.isDemo && password.isEmpty()) {
                _uiEvents.emit(UiEvent.ShowToast("Password is required for non-demo SSH connection.", isError = true))
                return@launch
            }

            _isLoading.value = true
            _connectionStatus.value = ConnectionStatus.Connecting
            _terminalLines.value = listOf(TerminalLine("Connecting to ${profile.host}:${profile.port}..."))

            val result = sshManager.testConnection(
                host = profile.host,
                port = profile.port,
                username = profile.username,
                password = password,
                isDemo = profile.isDemo
            )

            _isLoading.value = false
            when (result) {
                is ConnectionResult.Success -> {
                    _connectedDevice.value = profile
                    _currentPassword.value = password
                    _connectionStatus.value = ConnectionStatus.Connected
                    val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    repository.updateLastConnected(profile.id, nowStr)

                    _terminalLines.value = _terminalLines.value + listOf(
                        TerminalLine(result.message, isSystem = true),
                        TerminalLine("Welcome to RouterControl terminal session! Type any command or click standard ones above.", isSystem = true)
                    )
                    _uiEvents.emit(UiEvent.ShowToast("Connected to ${profile.name}!", isError = false))
                }
                is ConnectionResult.Failure -> {
                    _connectionStatus.value = ConnectionStatus.Error
                    _terminalLines.value = _terminalLines.value + listOf(
                        TerminalLine("Connection Failed: ${result.error}", isError = true)
                    )
                    _uiEvents.emit(UiEvent.ShowToast("Authentication / connection failed.", isError = true))
                }
            }
        }
    }

    fun disconnect() {
        _connectedDevice.value = null
        _currentPassword.value = ""
        _connectionStatus.value = ConnectionStatus.Disconnected
        _terminalLines.value = _terminalLines.value + listOf(TerminalLine("Disconnected from session.", isSystem = true))
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowToast("Session closed.", isError = false))
        }
    }

    fun runCommand(commandString: String) {
        val device = _connectedDevice.value
        val password = _currentPassword.value

        if (device == null) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("No active router session.", isError = true))
            }
            return
        }

        if (commandString.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            // Append typed command to terminal buffer
            _terminalLines.value = _terminalLines.value + listOf(TerminalLine("[admin@${device.name}] > $commandString"))

            val result = sshManager.runCommand(
                host = device.host,
                port = device.port,
                username = device.username,
                password = password,
                command = commandString,
                isDemo = device.isDemo
            )

            _isLoading.value = false
            when (result) {
                is CommandResult.Success -> {
                    _terminalLines.value = _terminalLines.value + listOf(TerminalLine(result.output))
                    // Log command history in DB
                    repository.insertLog(
                        CommandLog(
                            deviceId = device.id,
                            command = commandString,
                            output = result.output
                        )
                    )
                    _uiEvents.emit(UiEvent.ShowToast("Command executed successfully", isError = false))
                }
                is CommandResult.Failure -> {
                    _terminalLines.value = _terminalLines.value + listOf(TerminalLine(result.error, isError = true))
                    _uiEvents.emit(UiEvent.ShowToast("Execution failed: ${result.error}", isError = true))
                }
            }
        }
    }

    suspend fun executeQuickCommand(commandString: String): String? {
        val device = _connectedDevice.value ?: return null
        val password = _currentPassword.value

        if (commandString.isBlank()) return null

        _isLoading.value = true
        _terminalLines.value = _terminalLines.value + listOf(TerminalLine("[admin@${device.name}] > $commandString"))

        val result = sshManager.runCommand(
            host = device.host,
            port = device.port,
            username = device.username,
            password = password,
            command = commandString,
            isDemo = device.isDemo
        )

        _isLoading.value = false
        return when (result) {
            is CommandResult.Success -> {
                _terminalLines.value = _terminalLines.value + listOf(TerminalLine(result.output))
                repository.insertLog(
                    CommandLog(
                        deviceId = device.id,
                        command = commandString,
                        output = result.output
                    )
                )
                _uiEvents.emit(UiEvent.ShowToast("Command executed successfully", isError = false))
                result.output
            }
            is CommandResult.Failure -> {
                _terminalLines.value = _terminalLines.value + listOf(TerminalLine(result.error, isError = true))
                _uiEvents.emit(UiEvent.ShowToast("Execution failed: ${result.error}", isError = true))
                null
            }
        }
    }

    fun clearLogs(deviceId: String) {
        viewModelScope.launch {
            repository.clearLogsForDevice(deviceId)
            _uiEvents.emit(UiEvent.ShowToast("Logs cleared", isError = false))
        }
    }

    fun pingDevice(profile: DeviceProfile) {
        viewModelScope.launch {
            _pingStatuses.value = _pingStatuses.value + (profile.id to PingStatus.Checking)
            
            if (profile.isDemo) {
                kotlinx.coroutines.delay(200)
                _pingStatuses.value = _pingStatuses.value + (profile.id to PingStatus.Online((5..20).random().toLong()))
                return@launch
            }

            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var reachable = false
                val startTime = System.currentTimeMillis()
                var latency = 0L
                try {
                    val socket = java.net.Socket()
                    val socketAddress = java.net.InetSocketAddress(profile.host, profile.port)
                    socket.connect(socketAddress, 1500)
                    socket.close()
                    latency = System.currentTimeMillis() - startTime
                    reachable = true
                } catch (e: java.io.IOException) {
                    if (e is java.net.ConnectException) {
                        latency = System.currentTimeMillis() - startTime
                        reachable = true
                    } else {
                        try {
                            val process = Runtime.getRuntime().exec("ping -c 1 -W 2 ${profile.host}")
                            val exitVal = process.waitFor()
                            reachable = (exitVal == 0)
                            latency = System.currentTimeMillis() - startTime
                        } catch (ex: Exception) {
                            reachable = false
                        }
                    }
                }
                Pair(reachable, latency)
            }

            if (result.first) {
                _pingStatuses.value = _pingStatuses.value + (profile.id to PingStatus.Online(result.second))
            } else {
                _pingStatuses.value = _pingStatuses.value + (profile.id to PingStatus.Offline)
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: RouterRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RouterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RouterViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Error
}

data class TerminalLine(
    val text: String,
    val isSystem: Boolean = false,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface UiEvent {
    data class ShowToast(val message: String, val isError: Boolean) : UiEvent
}

sealed interface PingStatus {
    object Idle : PingStatus
    object Checking : PingStatus
    data class Online(val latencyMs: Long) : PingStatus
    object Offline : PingStatus
}
