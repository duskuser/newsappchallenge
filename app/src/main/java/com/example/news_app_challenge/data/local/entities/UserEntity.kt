package com.example.news_app_challenge.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.example.news_app_challenge.BuildConfig

@Entity(tableName = "users")
data class UserEntity(
    // UserID is hard coded as this is an example application
    // In practice this would likely be attached to a user signup
    @PrimaryKey val userId: String = BuildConfig.USER_ID,
    val savedArticleUrls: List<String> = emptyList()
)