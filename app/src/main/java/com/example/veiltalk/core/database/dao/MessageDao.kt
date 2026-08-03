package com.example.veiltalk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.veiltalk.core.database.entity.PrivateMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: PrivateMessageEntity)

    @Query("""
        SELECT * FROM private_messages 
        WHERE ownerUsername = :owner 
        ORDER BY timestamp ASC, id ASC
    """)
    fun getAllForOwnerFlow(owner: String): Flow<List<PrivateMessageEntity>>

    @Query("""
        SELECT * FROM private_messages 
        WHERE ownerUsername = :owner AND (sender = :partner OR recipient = :partner)
        ORDER BY timestamp ASC, id ASC
    """)
    fun getConversationFlow(owner: String, partner: String): Flow<List<PrivateMessageEntity>>

    @Query("UPDATE private_messages SET status = :status WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun updateStatus(messageId: String, owner: String, status: String)
}