package com.example.data.repository

import com.example.data.local.CommandLogDao
import com.example.data.local.DeviceProfileDao
import com.example.data.model.CommandLog
import com.example.data.model.DeviceProfile
import kotlinx.coroutines.flow.Flow

class RouterRepository(
    private val deviceProfileDao: DeviceProfileDao,
    private val commandLogDao: CommandLogDao
) {
    val allProfiles: Flow<List<DeviceProfile>> = deviceProfileDao.getAllProfiles()
    val allLogs: Flow<List<CommandLog>> = commandLogDao.getAllLogs()

    suspend fun getProfileById(id: String): DeviceProfile? {
        return deviceProfileDao.getProfileById(id)
    }

    suspend fun insertProfile(profile: DeviceProfile) {
        deviceProfileDao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: DeviceProfile) {
        deviceProfileDao.deleteProfile(profile)
    }

    suspend fun updateLastConnected(id: String, timestamp: String) {
        deviceProfileDao.updateLastConnected(id, timestamp)
    }

    fun getLogsForDevice(deviceId: String): Flow<List<CommandLog>> {
        return commandLogDao.getLogsForDevice(deviceId)
    }

    suspend fun insertLog(log: CommandLog) {
        commandLogDao.insertLog(log)
        commandLogDao.pruneLogs()
    }

    suspend fun clearLogsForDevice(deviceId: String) {
        commandLogDao.clearLogsForDevice(deviceId)
    }
}
