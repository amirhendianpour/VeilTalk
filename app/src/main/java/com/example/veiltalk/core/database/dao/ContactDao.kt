package com.example.veiltalk.core.database.dao

import androidx.room.*
import com.example.veiltalk.core.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE ownerUsername = :owner ORDER BY firstName ASC, lastName ASC")
    fun getContactsFlow(owner: String): Flow<List<ContactEntity>>

    @Query("DELETE FROM contacts WHERE username = :username AND ownerUsername = :owner")
    suspend fun deleteContact(username: String, owner: String)
}
