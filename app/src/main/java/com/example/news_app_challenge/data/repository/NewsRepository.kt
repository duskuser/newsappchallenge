package com.example.news_app_challenge.data.repository

import android.util.Log
import com.example.news_app_challenge.data.local.daos.ArticleDao
import com.example.news_app_challenge.data.local.daos.UserDao
import com.example.news_app_challenge.data.local.entities.ArticleEntity
import com.example.news_app_challenge.data.local.entities.UserEntity
import com.example.news_app_challenge.data.remote.api.NewsApiService
import com.example.news_app_challenge.data.remote.models.ApiArticle
import com.example.news_app_challenge.util.ConnectionStatus
import com.example.news_app_challenge.util.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map 

import com.example.news_app_challenge.BuildConfig


class NewsRepository(
    private val apiService: NewsApiService,
    private val articleDao: ArticleDao,
    internal val userDao: UserDao,
    private val connectivityObserver: ConnectivityObserver
) {

    // In a production app, consider fetching this from BuildConfig fields, secrets.properties, or a backend.
    private val NEWS_API_KEY = BuildConfig.NEWS_API_KEY

    // Hardcoded user ID
    private val userId = BuildConfig.USER_ID

    private val _currentPage = MutableStateFlow(1)
    private val _totalResults = MutableStateFlow(0)

    // Page size is hard coded here
    private val API_PAGE_SIZE = 20 

    val apiPageSize: Int
    get() = API_PAGE_SIZE

    // Expose these as StateFlow for ViewModel to observe
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    val totalResults: StateFlow<Int> = _totalResults.asStateFlow()

    init {
        Log.d("NewsRepository", "Initializing repository...")
    }



    fun getNewsArticles(): Flow<List<ArticleEntity>> {
        return articleDao.getAllArticles()
    }


    fun getSavedArticles(): Flow<List<ArticleEntity>> {
        return articleDao.getSavedArticles()
    }


    fun getNetworkStatus(): Flow<ConnectionStatus> {
        return connectivityObserver.observe()
    }
    
    // Refreshes the news feed by fetching data from the API and saving it to the local database.
    // Returns true on successful refresh, false on failure (e.g., no internet, API error).
    suspend fun refreshNewsFeed(resetPage: Boolean = true): Boolean { 
        Log.d("NewsRepository", "Attempting to refresh news feed (resetPage=$resetPage)...")
        return try {
            val status = connectivityObserver.observe().first()

            if (status == ConnectionStatus.AVAILABLE || status == ConnectionStatus.LOSING) {
                if (resetPage) {
                    _currentPage.value = 1 
                }

                val response = apiService.getTopHeadlines(
                    country = "us",
                    apiKey = NEWS_API_KEY,
                    page = _currentPage.value,
                    pageSize = API_PAGE_SIZE
                )

                if (response.status == "ok") {
                    Log.d("NewsRepository", "API fetch successful. Articles received: ${response.articles.size}, Total Results: ${response.totalResults}")
                    // Sync total results
                    _totalResults.value = response.totalResults 

                    val currentUser = userDao.getUser(userId).first() ?: UserEntity(userId = userId)
                    val savedUrls = currentUser.savedArticleUrls.toSet()

                    val newArticleEntities = response.articles.mapNotNull { apiArticle ->
                        apiArticle.url?.let { articleUrl ->
                            val isSaved = savedUrls.contains(articleUrl)
                            apiArticle.toArticleEntity(isSaved)
                        }
                    }

                    if (resetPage) {
                        // Clear non-saved articles on full refresh
                        articleDao.deleteNonSavedArticles() 
                    }
                    if (newArticleEntities.isNotEmpty()) {
                        articleDao.insertArticles(newArticleEntities) 
                        Log.d("NewsRepository", "Saved ${newArticleEntities.size} articles to local DB.")
                    }
                    true
                } else {
                    Log.e("NewsRepository", "API error: ${response.message}")
                    false
                }
            } else {
                Log.d("NewsRepository", "Network is unavailable. Skipping API fetch.")
                false
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error refreshing news: ${e.message}", e)
            false
        }
    }


    suspend fun loadMoreNews(): Boolean { 
        Log.d("NewsRepository", "Attempting to load more news (page=${_currentPage.value + 1})...")
        // Get count of articles currently in DB
        val currentLoadedArticlesCount = articleDao.getAllArticlesCount() 
        val totalAvailable = _totalResults.value

        // Check if there are more articles to load based on API total results
        if (totalAvailable > 0 && currentLoadedArticlesCount >= totalAvailable) {
            Log.d("NewsRepository", "No more articles to load. Current loaded: $currentLoadedArticlesCount, Total available: $totalAvailable")
            return false // No more data to load
        }

        // Check if we are at the end of the calculated pages based on page size
        val maxPossiblePages = (totalAvailable / API_PAGE_SIZE) + if (totalAvailable % API_PAGE_SIZE > 0) 1 else 0
        if (_currentPage.value >= maxPossiblePages && totalAvailable > 0) {
            Log.d("NewsRepository", "Reached max possible pages. Current page: ${_currentPage.value}, Max pages: $maxPossiblePages")
            return false
        }


        return try {
            val status = connectivityObserver.observe().first()
            if (status == ConnectionStatus.AVAILABLE || status == ConnectionStatus.LOSING) {
                // Incrememt page for potential next fetch call
                _currentPage.value++
                val response = apiService.getTopHeadlines(
                    country = "us",
                    apiKey = NEWS_API_KEY,
                    page = _currentPage.value,
                    pageSize = API_PAGE_SIZE
                )

                if (response.status == "ok") {
                    Log.d("NewsRepository", "API loadMore successful. Articles received: ${response.articles.size}")
                    // Sync total results
                    _totalResults.value = response.totalResults

                    val currentUser = userDao.getUser(userId).first() ?: UserEntity(userId = userId)
                    val savedUrls = currentUser.savedArticleUrls.toSet()

                    val newArticleEntities = response.articles.mapNotNull { apiArticle ->
                        apiArticle.url?.let { articleUrl ->
                            val isSaved = savedUrls.contains(articleUrl)
                            apiArticle.toArticleEntity(isSaved)
                        }
                    }

                    if (newArticleEntities.isNotEmpty()) {
                        articleDao.insertArticles(newArticleEntities)
                        Log.d("NewsRepository", "Appended ${newArticleEntities.size} articles to local DB.")
                    }
                    true
                } else {
                    Log.e("NewsRepository", "API error during loadMore: ${response.message}")
                    _currentPage.value--
                    false
                }
            } else {
                Log.d("NewsRepository", "Network is unavailable. Skipping loadMore.")
                false
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error loading more news: ${e.message}", e)
            _currentPage.value-- 
            false
        }
    }

    // Toggles the saved status of an article in the local database and updates user's saved URLs.
    // Operates directly on ArticleEntity.
    suspend fun toggleArticleSavedStatus(article: ArticleEntity): Boolean {
        return try {
            // Update article in DB
            val newSavedStatus = !article.isSaved
            articleDao.updateArticleSavedStatus(article.url, newSavedStatus) 

            // Update the user's saved URLs list in the database
            val currentUser = userDao.getUser(userId).first() ?: UserEntity(userId = userId)
            val updatedSavedUrls = if (newSavedStatus) {
                currentUser.savedArticleUrls + article.url
            } else {
                currentUser.savedArticleUrls - article.url
            }.distinct() 
            // ^ Ensure no duplicate URLs ^

             // Save updated user
            userDao.updateUser(currentUser.copy(savedArticleUrls = updatedSavedUrls))
            Log.d("NewsRepository", "Toggled saved status for ${article.title} to $newSavedStatus")
            true
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error toggling saved status: ${e.message}", e)
            false
        }
    }

// Loads user on startup
    suspend fun initializeUser() {
        try {
            val userExists = userDao.getUser(userId).first() != null
            if (!userExists) {
                userDao.insertUser(UserEntity(userId = userId))
                Log.d("NewsRepository", "Default user initialized.")
            }
        } catch (e: Exception) {
            Log.e("NewsRepository", "Error initializing user: ${e.message}", e)
        }
    }

    internal suspend fun getSavedArticleUrls(): List<String> {
        return userDao.getUser(userId).first()?.savedArticleUrls ?: emptyList()
    }

}

// Extension function to map from ApiArticle (network) to ArticleEntity (database)
fun ApiArticle.toArticleEntity(isSaved: Boolean): ArticleEntity {
    return ArticleEntity(
        url = this.url ?: "no_url_${this.hashCode()}_${System.currentTimeMillis()}", 
        sourceId = this.source?.id,
        sourceName = this.source?.name,
        author = this.author,
        title = this.title,
        description = this.description,
        urlToImage = this.urlToImage,
        publishedAt = this.publishedAt,
        content = this.content,
        isSaved = isSaved
    )
}