package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactSearchHistoryDao {
    @Query("SELECT * FROM contact_search_history ORDER BY searchTime DESC")
    fun getAllSearchHistoryFlow(): Flow<List<ContactSearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchEvent(event: ContactSearchHistoryEntity)

    @Query("DELETE FROM contact_search_history")
    suspend fun clearHistory()
}
