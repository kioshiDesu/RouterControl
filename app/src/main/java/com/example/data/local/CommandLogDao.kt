package com.example.data.local

import androidx.room.*
import com.example.data.model.CommandLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandLogDao {
    @Query("SELECT * FROM command_logs WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 100")
    fun getLogsForDevice(deviceId: String): Flow<List<CommandLog>>

    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<CommandLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CommandLog)

    @Query("DELETE FROM command_logs WHERE deviceId = :deviceId")
    suspend fun clearLogsForDevice(deviceId: String)

    @Query("DELETE FROM command_logs WHERE id NOT IN (SELECT id FROM command_logs ORDER BY timestamp DESC LIMIT 100)")
    suspend fun pruneLogs()
}
