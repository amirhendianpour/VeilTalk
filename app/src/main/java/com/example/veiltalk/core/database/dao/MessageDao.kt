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

    @Query("""
        UPDATE private_messages
        SET status = :newStatus
        WHERE id = :messageId AND ownerUsername = :owner
        AND (CASE status WHEN 'READ' THEN 2 WHEN 'DELIVERED' THEN 1 ELSE 0 END)
            <= (CASE :newStatus WHEN 'READ' THEN 2 WHEN 'DELIVERED' THEN 1 ELSE 0 END)
    """)
    suspend fun updateStatusIfHigher(messageId: String, owner: String, newStatus: String)

    @Query("UPDATE private_messages SET status = 'READ' WHERE ownerUsername = :owner AND sender = :partner AND status != 'READ'")
    suspend fun markConversationAsRead(owner: String, partner: String)

    @Query("""
        SELECT * FROM private_messages
        WHERE ownerUsername = :owner AND sender = :partner AND status != 'READ'
    """)
    suspend fun getUnreadFromSender(owner: String, partner: String): List<PrivateMessageEntity>

    @Query("DELETE FROM private_messages WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun deleteMessage(messageId: String, owner: String)

    @Query("DELETE FROM private_messages WHERE id IN (:messageIds) AND ownerUsername = :owner")
    suspend fun deleteMessages(messageIds: List<String>, owner: String)

    @Query("UPDATE private_messages SET isPinned = :pinned WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun updatePinStatus(messageId: String, owner: String, pinned: Boolean)

    @Query("UPDATE private_messages SET content = :newContent WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun updateMessageContent(messageId: String, owner: String, newContent: String)

    @Query("DELETE FROM private_messages WHERE ownerUsername = :owner AND (sender = :partner OR recipient = :partner)")
    suspend fun deleteConversation(owner: String, partner: String)

    @Query("""
        SELECT * FROM private_messages 
        WHERE ownerUsername = :owner AND (sender = :partner OR recipient = :partner)
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getLastMessages(owner: String, partner: String, limit: Int): List<PrivateMessageEntity>
}
