package com.example.data.local

import androidx.room.*
import com.example.data.model.DeviceProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceProfileDao {
    @Query("SELECT * FROM device_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<DeviceProfile>>

    @Query("SELECT * FROM device_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): DeviceProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DeviceProfile)

    @Delete
    suspend fun deleteProfile(profile: DeviceProfile)

    @Query("UPDATE device_profiles SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: String, timestamp: String)
}
