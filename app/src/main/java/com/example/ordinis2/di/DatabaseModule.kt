package com.example.ordinis2.di

import android.content.Context
import com.example.ordinis2.data.local.AppDatabase
import com.example.ordinis2.data.local.WorkPlanDao
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
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getDatabase(appContext)
    }

    @Provides
    fun provideWorkPlanDao(appDatabase: AppDatabase): WorkPlanDao {
        return appDatabase.workPlanDao()
    }


}
