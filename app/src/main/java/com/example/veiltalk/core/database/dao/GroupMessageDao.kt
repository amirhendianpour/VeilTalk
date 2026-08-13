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

    @Query("DELETE FROM group_messages WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun deleteMessage(messageId: String, owner: String)

    @Query("DELETE FROM group_messages WHERE id IN (:messageIds) AND ownerUsername = :owner")
    suspend fun deleteMessages(messageIds: List<String>, owner: String)

    @Query("UPDATE group_messages SET isPinned = :pinned WHERE id = :messageId AND ownerUsername = :owner")
    suspend fun updatePinStatus(messageId: String, owner: String, pinned: Boolean)
}