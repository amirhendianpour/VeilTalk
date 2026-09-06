package com.example.veiltalk.feature.user.data.di

import com.example.veiltalk.feature.user.data.UserApi
import com.example.veiltalk.feature.user.data.BlockApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideBlockApi(retrofit: Retrofit): BlockApi =
        retrofit.create(BlockApi::class.java)
}