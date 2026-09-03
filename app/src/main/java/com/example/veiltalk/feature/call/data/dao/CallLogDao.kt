package com.example.veiltalk.feature.call.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.veiltalk.feature.call.data.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Insert
    suspend fun insert(log: CallLogEntity)

    @Query("SELECT * FROM call_logs WHERE ownerUsername = :owner ORDER BY startTime DESC")
    fun getAllLogsFlow(owner: String): Flow<List<CallLogEntity>>

    @Query("DELETE FROM call_logs WHERE ownerUsername = :owner")
    suspend fun clearLogs(owner: String)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)
}
