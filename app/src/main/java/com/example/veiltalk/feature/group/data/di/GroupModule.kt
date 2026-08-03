package com.example.veiltalk.feature.group.data.di

import com.example.veiltalk.feature.group.data.GroupApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GroupModule {

    @Provides
    @Singleton
    fun provideGroupApi(retrofit: Retrofit): GroupApi =
        retrofit.create(GroupApi::class.java)
}