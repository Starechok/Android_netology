package ru.netology.nmedia.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.api.*
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ApiServiseModule {

    @Singleton
    @Provides
    fun provideApiServise(auth: AppAuth): ApiService {
        return retrofit(okhttp(loggingInterceptor(), authInterceptor(auth)))
            .create(ApiService::class.java)
    }

}