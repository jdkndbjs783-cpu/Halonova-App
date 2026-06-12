package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizedMemoryDao {
    @Query("SELECT * FROM categorized_memories ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<CategorizedMemoryEntity>>

    @Query("SELECT * FROM categorized_memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategoryFlow(category: String): Flow<List<CategorizedMemoryEntity>>

    @Query("SELECT * FROM categorized_memories ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<CategorizedMemoryEntity>

    @Query("SELECT * FROM categorized_memories WHERE category = :category")
    suspend fun getMemoriesByCategory(category: String): List<CategorizedMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: CategorizedMemoryEntity)

    @Update
    suspend fun updateMemory(memory: CategorizedMemoryEntity)

    @Query("DELETE FROM categorized_memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM categorized_memories")
    suspend fun clearAllMemories()
}
