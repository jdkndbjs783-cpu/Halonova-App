package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceLogDao {
    @Query("SELECT * FROM service_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ServiceLogEntity>>

    @Insert
    suspend fun insertLog(log: ServiceLogEntity)

    @Query("DELETE FROM service_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM service_logs WHERE timestamp < :cutoff")
    suspend fun pruneLogs(cutoff: Long)
}
