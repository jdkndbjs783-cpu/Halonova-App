package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantMemoryDao {
    @Query("SELECT * FROM assistant_memory WHERE id = 1")
    fun getMemoryFlow(): Flow<AssistantMemoryEntity?>

    @Query("SELECT * FROM assistant_memory WHERE id = 1")
    suspend fun getMemory(): AssistantMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMemory(memory: AssistantMemoryEntity)
}
