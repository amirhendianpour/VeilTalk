package com.example.veiltalk.feature.chat.data.di

import com.example.veiltalk.feature.chat.data.MediaApi
import com.example.veiltalk.feature.chat.data.MessageApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideMediaApi(retrofit: Retrofit): MediaApi =
        retrofit.create(MediaApi::class.java)

    @Provides
    @Singleton
    fun provideMessageApi(retrofit: Retrofit): MessageApi =
        retrofit.create(MessageApi::class.java)
}
