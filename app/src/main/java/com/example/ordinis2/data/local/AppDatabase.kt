package com.example.ordinis2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // Import TypeConverters

// Add WorkPlanEntity to the entities array
// Add WorkPlanTypeConverters::class to @TypeConverters
@Database(entities = [User::class, WorkPlanEntity::class], version = 2, exportSchema = false) // Increment version
@TypeConverters(WorkPlanTypeConverters::class) // Add your new type converters
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workPlanDao(): WorkPlanDao // Add abstract fun for the new DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )

                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
