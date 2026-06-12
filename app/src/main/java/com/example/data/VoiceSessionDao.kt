package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceSessionDao {

    @Query("SELECT * FROM voice_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<VoiceSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: VoiceSessionEntity)

    @Query("DELETE FROM voice_sessions")
    suspend fun clearAllSessions()
}
