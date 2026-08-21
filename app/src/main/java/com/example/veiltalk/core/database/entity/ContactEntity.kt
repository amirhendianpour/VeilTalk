package com.example.veiltalk.core.database.entity

import androidx.room.Entity

@Entity(tableName = "contacts", primaryKeys = ["username", "ownerUsername"])
data class ContactEntity(
    val username: String,
    val ownerUsername: String,
    val firstName: String,
    val lastName: String,
    val profilePictureUrl: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val bio: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
