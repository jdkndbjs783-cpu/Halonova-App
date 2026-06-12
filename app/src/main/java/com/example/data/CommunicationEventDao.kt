package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunicationEventDao {
    @Query("SELECT * FROM communication_events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<CommunicationEventEntity>>

    @Insert
    suspend fun insertEvent(event: CommunicationEventEntity)

    @Query("DELETE FROM communication_events")
    suspend fun clearEvents()

    @Query("DELETE FROM communication_events WHERE timestamp < :cutoff")
    suspend fun pruneEvents(cutoff: Long)
}
