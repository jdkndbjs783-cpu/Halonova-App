package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_search_history")
data class ContactSearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val contactName: String,
    val searchTime: Long,
    val status: String, // "SUCCESS" or "FAILED"
    val matchedPhoneNumber: String? = null
)
