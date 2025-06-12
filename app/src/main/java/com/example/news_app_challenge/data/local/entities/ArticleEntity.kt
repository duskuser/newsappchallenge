package com.example.news_app_challenge.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    // URL serves as primary key
    @PrimaryKey val url: String,
    val sourceId: String?,
    val sourceName: String?,
    val author: String?,
    val title: String?,
    val description: String?,
    val urlToImage: String?,
    val publishedAt: String?,
    val content: String?,
    // Flag used for tracking if user has saved the article or not 
    val isSaved: Boolean = false
)

val ArticleEntity.uniqueKey: String
get() = url?: title ?: hashCode().toString()