package com.example.veiltalk.feature.chat.data.di

import com.example.veiltalk.feature.chat.data.MediaApi
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
}