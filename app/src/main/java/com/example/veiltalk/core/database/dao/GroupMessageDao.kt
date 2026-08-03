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
}