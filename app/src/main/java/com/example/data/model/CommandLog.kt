package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "command_logs")
data class CommandLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)
