package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartActionDao {
    @Query("SELECT * FROM smart_actions_history ORDER BY timestamp DESC")
    fun getAllActionsFlow(): Flow<List<SmartActionEntity>>

    @Insert
    suspend fun insertAction(action: SmartActionEntity)

    @Query("DELETE FROM smart_actions_history")
    suspend fun clearActionHistory()

    @Query("DELETE FROM smart_actions_history WHERE timestamp < :cutoff")
    suspend fun pruneActions(cutoff: Long)
}
