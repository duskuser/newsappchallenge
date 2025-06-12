package com.example.news_app_challenge.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.news_app_challenge.data.local.converters.Converters
import com.example.news_app_challenge.data.local.daos.ArticleDao
import com.example.news_app_challenge.data.local.daos.UserDao
import com.example.news_app_challenge.data.local.entities.ArticleEntity
import com.example.news_app_challenge.data.local.entities.UserEntity


@Database(
    entities = [ArticleEntity::class, UserEntity::class], 
    version = 1, 
    exportSchema = false 
)
@TypeConverters(Converters::class) 
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile 
        private var INSTANCE: AppDatabase? = null

// Singleton for thread safe database init
        fun getInstance(context: Context): AppDatabase {
            // If instance is not null, return it
            // Synchronize for thread-safe instantiation
            return INSTANCE ?: synchronized(this) { 
                // If instance is still null, create it
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "news_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance 
                // Return instance
                instance 
            }
        }
    }
}