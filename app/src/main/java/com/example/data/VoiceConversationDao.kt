package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceConversationDao {

    @Query("SELECT * FROM voice_conversation_history ORDER BY timestamp DESC")
    fun getAllConversationsFlow(): Flow<List<VoiceConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: VoiceConversationEntity)

    @Query("DELETE FROM voice_conversation_history")
    suspend fun clearAllHistory()

    @Query("DELETE FROM voice_conversation_history WHERE timestamp < :cutoff")
    suspend fun pruneConversations(cutoff: Long)
}
