package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceControlCommandDao {
    @Query("SELECT * FROM device_control_commands ORDER BY timestamp DESC")
    fun getAllCommandsFlow(): Flow<List<DeviceControlCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: DeviceControlCommandEntity)

    @Update
    suspend fun updateCommand(command: DeviceControlCommandEntity)

    @Query("DELETE FROM device_control_commands")
    suspend fun clearHistory()
}
