package com.example.veiltalk.core.websocket.di

import com.example.veiltalk.core.websocket.StompClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebSocketModule {

    @Provides
    @Singleton
    fun provideStompClient(okHttpClient: OkHttpClient): StompClient =
        StompClient(okHttpClient)
}