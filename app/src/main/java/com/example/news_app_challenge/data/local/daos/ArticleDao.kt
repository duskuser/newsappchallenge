package com.example.news_app_challenge.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.news_app_challenge.data.local.entities.ArticleEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ArticleDao {
// Inserts list of Article entry objets into the database
// Repeats (e.g, if an article is found with the same URL, are overwritten in this implementation)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

// Get all article entries, using Flow to monitor real time changes
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>

// Get all saved article from DB, using Flow to monitor real time changes
    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY publishedAt DESC")
    fun getSavedArticles(): Flow<List<ArticleEntity>>

    // Updates isSaved status for an article as needed
    @Query("UPDATE articles SET isSaved = :isSaved WHERE url = :articleUrl")
    suspend fun updateArticleSavedStatus(articleUrl: String, isSaved: Boolean)


    @Query("DELETE FROM articles WHERE isSaved = 0")
    suspend fun deleteNonSavedArticles()

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getAllArticlesCount() : Int

    // Deletes all articles
    // Not sure why you would ever use this but it's here for now 
    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()
}