package com.example.veiltalk.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.veiltalk.core.database.dao.ContactDao
import com.example.veiltalk.core.database.dao.GroupMessageDao
import com.example.veiltalk.core.database.dao.MessageDao
import com.example.veiltalk.core.database.entity.ContactEntity
import com.example.veiltalk.core.database.entity.GroupMessageEntity
import com.example.veiltalk.core.database.entity.PrivateMessageEntity

@Database(
    entities = [PrivateMessageEntity::class, GroupMessageEntity::class, ContactEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun groupMessageDao(): GroupMessageDao
    abstract fun contactDao(): ContactDao
}