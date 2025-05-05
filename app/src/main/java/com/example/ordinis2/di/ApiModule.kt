package com.example.ordinis2.di

import com.example.ordinis2.data.SecretsProvider
import com.example.ordinis2.domain.IWorkPlanRepository
import com.example.ordinis2.domain.OpenAiWorkPlanRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideOpenAiKey(secretsProvider: SecretsProvider): String {
        return secretsProvider.getOpenAiKey()
    }

    @Provides
    @Singleton
    fun provideWorkPlanRepository(
        apiKey: String
    ): IWorkPlanRepository {
        return OpenAiWorkPlanRepository(apiKey)
    }
}
