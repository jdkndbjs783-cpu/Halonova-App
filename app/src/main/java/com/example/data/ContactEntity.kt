package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val phoneNumber: String = "",
    val email: String = "",
    val isStarred: Boolean = false,
    val lastContacted: Long = 0L
)
