package com.example.veiltalk.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.veiltalk.core.database.entity.GroupMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: GroupMessageEntity)

    @Query("""
        SELECT * FROM group_messages 
        WHERE ownerUsername = :owner AND groupId = :groupId
        ORDER BY timestamp ASC, id ASC
    """)
    fun getGroupMessagesFlow(owner: String, groupId: Long): Flow<List<GroupMessageEntity>>

    @Query("""
        SELECT * FROM group_messages 
        WHERE ownerUsername = :owner
        ORDER BY timestamp ASC, id ASC
    """)
    fun getAllForOwnerFlow(owner: String): Flow<List<GroupMessageEntity>>

    @Query("SELECT * FROM group_messages WHERE id = :messageId AND ownerUsername = :owner LIMIT 1")
    suspend fun getMessageById(messageId: String, owner: String): GroupMessageEntity?

    @Query("DELETE FROM group_messages WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun deleteMessage(messageId: String, owner: String)

    @Query("DELETE FROM group_messages WHERE id IN (:messageIds) AND ownerUsername = :owner")
    suspend fun deleteMessages(messageIds: List<String>, owner: String)

    @Query("UPDATE group_messages SET isPinned = :pinned WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun updatePinStatus(messageId: String, owner: String, pinned: Boolean)

    @Query("UPDATE group_messages SET reactionsJson = :reactionsJson WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun updateReactions(messageId: String, owner: String, reactionsJson: String?)

    @Query("UPDATE group_messages SET content = :newContent WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun updateMessageContent(messageId: String, owner: String, newContent: String)

    @Query("""
        UPDATE group_messages
        SET status = :newStatus
        WHERE id = :messageId AND ownerUsername = :owner
        AND (CASE status WHEN 'READ' THEN 2 WHEN 'DELIVERED' THEN 1 ELSE 0 END)
            <= (CASE :newStatus WHEN 'READ' THEN 2 WHEN 'DELIVERED' THEN 1 ELSE 0 END)
    """)
    suspend fun updateStatusIfHigher(messageId: String, owner: String, newStatus: String)

    @Query("UPDATE group_messages SET status = 'READ' WHERE ownerUsername = :owner AND groupId = :groupId AND sender != :owner AND status != 'READ'")
    suspend fun markGroupAsRead(owner: String, groupId: Long)

    @Query("""
        SELECT * FROM group_messages
        WHERE ownerUsername = :owner AND groupId = :groupId AND sender != :owner AND status != 'READ'
    """)
    suspend fun getUnreadInGroup(owner: String, groupId: Long): List<GroupMessageEntity>

    @Query("""
        UPDATE group_messages 
        SET status = 'READ' 
        WHERE ownerUsername = :owner AND groupId = :groupId 
        AND sender = :owner AND status != 'READ'
    """)
    suspend fun markAllSentMessagesAsRead(owner: String, groupId: Long)

    @Query("DELETE FROM group_messages WHERE ownerUsername = :owner AND groupId = :groupId")
    suspend fun deleteGroupConversation(owner: String, groupId: Long)

    @Query("""
        SELECT * FROM group_messages 
        WHERE ownerUsername = :owner AND groupId = :groupId
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getLastMessages(owner: String, groupId: Long, limit: Int): List<GroupMessageEntity>
}