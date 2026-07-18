package com.example.network

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties
import kotlin.random.Random

class SshClientManager {

    companion object {
        private const val TAG = "SshClientManager"
    }

    /**
     * Test connection to a router.
     */
    suspend fun testConnection(
        host: String,
        port: Int,
        username: String,
        password: String,
        isDemo: Boolean
    ): ConnectionResult = withContext(Dispatchers.IO) {
        if (isDemo) {
            return@withContext ConnectionResult.Success("Connected to Demo Router (Simulated)")
        }

        var session: Session? = null
        try {
            val jsch = JSch()
            session = jsch.getSession(username, host, port)
            session.setPassword(password)

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(7000) // 7 seconds timeout

            if (session.isConnected) {
                return@withContext ConnectionResult.Success("Successfully authenticated via SSH!")
            } else {
                return@withContext ConnectionResult.Failure("Connection failed to establish.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSH Connection testing failed", e)
            return@withContext ConnectionResult.Failure(e.localizedMessage ?: e.message ?: "Unknown socket error")
        } finally {
            session?.disconnect()
        }
    }

    /**
     * Run command on a router.
     */
    suspend fun runCommand(
        host: String,
        port: Int,
        username: String,
        password: String,
        command: String,
        isDemo: Boolean
    ): CommandResult = withContext(Dispatchers.IO) {
        if (isDemo) {
            val mockOut = getMockOutputForCommand(command)
            return@withContext CommandResult.Success(mockOut)
        }

        var session: Session? = null
        var channel: ChannelExec? = null
        try {
            val jsch = JSch()
            session = jsch.getSession(username, host, port)
            session.setPassword(password)

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)
            session.connect(10000) // 10 seconds connect timeout

            channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)

            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            channel.setOutputStream(outputStream)
            channel.setErrStream(errorStream)

            channel.connect(10000) // 10 seconds execution timeout

            // Poll until execution finishes or is interrupted
            while (!channel.isClosed) {
                kotlinx.coroutines.delay(100)
            }

            val resultOutput = outputStream.toString("UTF-8")
            val errorOutput = errorStream.toString("UTF-8")

            if (channel.exitStatus == 0) {
                return@withContext CommandResult.Success(resultOutput.ifBlank { "Command executed with no output." })
            } else {
                val errMsg = errorOutput.ifBlank { "Process exited with status ${channel.exitStatus}" }
                return@withContext CommandResult.Failure(errMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "SSH Command Execution failed", e)
            return@withContext CommandResult.Failure(e.localizedMessage ?: e.message ?: "SSH communication failure")
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    private fun getMockOutputForCommand(command: String): String {
        val trimmed = command.trim().lowercase()
        return when {
            trimmed.contains("/ip address print") || trimmed == "ip address print" || trimmed == "/ip address" -> """
Flags: X - disabled, I - invalid, D - dynamic 
 #   ADDRESS            NETWORK         INTERFACE                                 
 0   192.168.88.1/24    192.168.88.0    bridge                                    
 1 D 10.0.0.145/24      10.0.0.0        ether1                                    
            """.trimIndent()

            trimmed.contains("/interface print") || trimmed == "interface print" || trimmed == "/interface" -> """
Flags: D - dynamic, X - disabled, R - running, S - slave 
 #     NAME                                TYPE         ACTUAL-MTU L2MTU  MAX-L2MTU
 0  R  ether1                              ether              1500  1598       4074
 1  RS ether2                              ether              1500  1598       4074
 2  RS ether3                              ether              1500  1598       4074
 3  RS ether4                              ether              1500  1598       4074
 4  RS ether5                              ether              1500  1598       4074
 5  R  bridge                              bridge             1500  1598
            """.trimIndent()

            trimmed.contains("/system resource print") || trimmed == "system resource print" || trimmed == "/system resource" -> {
                val randCpu = Random.nextInt(2, 28)
                val freeMem = Random.nextInt(170, 195)
                """
                   uptime: 4w2d5h12m43s
                  version: 7.12.1 (stable)
               build-time: Nov/15/2023 11:24:55
         factory-software: 7.6
              free-memory: ${freeMem}.4MiB
             total-memory: 256.0MiB
                      cpu: MIPS 24Kc V7.4
                cpu-count: 1
            cpu-frequency: 650MHz
                 cpu-load: ${randCpu}%
           free-hdd-space: 11.2MiB
          total-hdd-space: 16.0MiB
  write-sect-since-reboot: 4921
         write-sect-total: 395821
               bad-blocks: 0%
             architecture: mipsbe
               board-name: hEX lite
                 platform: MikroTik
                """.trimIndent()
            }

            trimmed.contains("/system reboot") || trimmed == "system reboot" || trimmed == "/reboot" -> """
Connection closing...
Router is restarting... (Simulated)
Please wait for system resource load to stabilize.
            """.trimIndent()

            trimmed.contains("/log print") || trimmed == "log print" || trimmed == "/log" -> """
18:42:01 system,info,account user admin logged in from 192.168.88.243 via winbox
18:43:45 dhcp,info,debug dhcp1 deassigned 192.168.88.55 for 00:0C:29:85:2B:B3
18:43:45 dhcp,info,debug dhcp1 assigned 192.168.88.55 for 00:0C:29:85:2B:B3
18:44:00 system,info,account user admin logged in from 10.0.0.12 via ssh
18:44:12 ssh,info connection accepted from 10.0.0.12:54832
18:44:15 ssh,info user admin logged in from 10.0.0.12 via ssh
18:44:59 system,info,command '/log print' run by admin via ssh
            """.trimIndent()

            else -> """
[admin@RouterControl] > $command
Command executed successfully. (Simulated)
Output: 
- Executed on: Demo Router
- Status: Success
- Output Type: Generic response for simulated CLI inputs.
            """.trimIndent()
        }
    }
}

sealed interface ConnectionResult {
    data class Success(val message: String) : ConnectionResult
    data class Failure(val error: String) : ConnectionResult
}

sealed interface CommandResult {
    data class Success(val output: String) : CommandResult
    data class Failure(val error: String) : CommandResult
}
