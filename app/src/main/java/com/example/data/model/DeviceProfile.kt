package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "device_profiles")
data class DeviceProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val lastConnected: String? = null,
    val isDemo: Boolean = false,
    val routerModel: String = "hAP Lite (v6)",
    val connectionType: String = "SSH",
    val l2tpSecret: String = ""
)
