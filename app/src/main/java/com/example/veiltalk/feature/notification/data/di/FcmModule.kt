package com.example.veiltalk.feature.notification.data.di

import com.example.veiltalk.feature.notification.data.FcmApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FcmModule {

    @Provides
    @Singleton
    fun provideFcmApi(retrofit: Retrofit): FcmApi =
        retrofit.create(FcmApi::class.java)
}