package com.example.veiltalk.feature.call.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUsername: String,
    val remoteUser: String,
    val callType: String, // AUDIO, VIDEO
    val direction: String, // INCOMING, OUTGOING
    val status: String, // CONNECTED, MISSED, REJECTED
    val startTime: Long,
    val duration: Long // in seconds
)
