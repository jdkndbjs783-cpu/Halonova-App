package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeEventDao {
    @Query("SELECT * FROM wake_events ORDER BY timestamp DESC")
    fun getAllWakeEventsFlow(): Flow<List<WakeEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWakeEvent(event: WakeEventEntity)

    @Query("DELETE FROM wake_events")
    suspend fun clearAllWakeEvents()
}
