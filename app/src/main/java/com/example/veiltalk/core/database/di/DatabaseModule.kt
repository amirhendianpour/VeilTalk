package com.example.veiltalk.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.veiltalk.core.database.AppDatabase
import com.example.veiltalk.core.database.dao.GroupMessageDao
import com.example.veiltalk.core.database.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "veiltalk.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideGroupMessageDao(db: AppDatabase): GroupMessageDao = db.groupMessageDao()
}