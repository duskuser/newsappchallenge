package com.example.news_app_challenge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.news_app_challenge.data.repository.NewsRepository
import com.example.news_app_challenge.data.local.entities.ArticleEntity
import com.example.news_app_challenge.util.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.flow.combine
import com.example.news_app_challenge.BuildConfig

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsFeedUiState>(
        NewsFeedUiState.Loading(
            networkStatus = ConnectionStatus.UNAVAILABLE,
            message = "Initializing...",
            isRefreshing = false
        )
    )

    // Manages ui state for ViewModel to update as needed
    val uiState: StateFlow<NewsFeedUiState> = _uiState.asStateFlow()

    // Manages current screen displaying for other controller functions to watch
    private val _currentScreen = MutableStateFlow(CurrentScreen.NEWS_FEED)
    val currentScreen: StateFlow<CurrentScreen> = _currentScreen.asStateFlow()

    // Internal cache of articles
    private var currentArticles: List<ArticleEntity> = emptyList()

    // Internal network status
    private var currentNetworkStatus: ConnectionStatus = ConnectionStatus.UNAVAILABLE
    private var currentSavedArticles: List<ArticleEntity> = emptyList()

    // Tracks if items are currently loading in 
    private val _isLoadingMore = MutableStateFlow(false)
    private val _canLoadMore = MutableStateFlow(true)

    // UI State for checking if the app is mid refresh
    private val _isCurrentlyRefreshing = MutableStateFlow(false) 

    private val maxArticles = 100

    init {
        // Initialize user (important for saved articles logic)
        viewModelScope.launch {
            repository.initializeUser()
        }

        // Observe network status
        viewModelScope.launch {
            // Update network status
            repository.getNetworkStatus().collect { status ->
                currentNetworkStatus = status 
                // Update the UI state based on the new network status
                updateUiStateBasedOnCurrentData(currentNetworkStatus)
                Log.d("NewsViewModel", "Network status changed to: $status")

                // If network becomes available and we had an error or no articles, try refreshing
                if (status == ConnectionStatus.AVAILABLE && currentArticles.isEmpty() && _uiState.value is NewsFeedUiState.Error) {
                    refreshNews()
                }
            }
        }

        // Observe articles from the repository
        viewModelScope.launch {
            repository.getNewsArticles()
                .combine(_isLoadingMore) { articles, isLoadingMore ->
                    // Apply saved status based on current user's saved URLs
                    val currentUser = repository.userDao.getUser(BuildConfig.USER_ID).first()
                    val savedUrls = currentUser?.savedArticleUrls?.toSet() ?: emptySet()
                    val updatedArticles = articles.map { article ->
                        article.copy(isSaved = savedUrls.contains(article.url))
                    }
                    Pair(updatedArticles, isLoadingMore)
                }
                .combine(_isCurrentlyRefreshing) { (articles, isLoadingMore), isRefreshing -> 
                    Triple(articles, isLoadingMore, isRefreshing)
                }
                .collect { (articles, isLoadingMore, isRefreshing) ->
                    currentArticles = articles
                    // When articles are collected, calculate if more can be loaded.
                    // This logic depends on total results from the API which is in repository.
                    val totalArticlesFromApi = repository.totalResults.value
                    _canLoadMore.value =
                        (totalArticlesFromApi == 0 || articles.size < totalArticlesFromApi)
                        // Capping article amount to prevent excessive loading, controlled by maxArticles value
                                && articles.size < maxArticles 
                                && repository.currentPage.value * repository.apiPageSize < totalArticlesFromApi

                    updateUiStateBasedOnCurrentData(currentNetworkStatus)
                    Log.d(
                        "NewsViewModel",
                        "Articles collected: ${currentArticles.size}. isLoadMore: $isLoadingMore. CanLoadMore: ${_canLoadMore.value}. UI State updated."
                    )
                }
        }

        // Observes saved articles from repository (for SavedArticlesScreen to use as needed)
        viewModelScope.launch {
            repository.getSavedArticles().collect { savedArticles ->
                currentSavedArticles = savedArticles

                Log.d("NewsViewModel", "Saved articles collected: ${currentSavedArticles.size}.")
            }
        }

        // Trigger initial data load
        refreshNews(isInitialLoad = true)
    }

    private fun updateUiStateBasedOnCurrentData(networkStatus: ConnectionStatus) {
        val currentState = _uiState.value

        _uiState.value = when {
            // Still in a loading state
            _uiState.value is NewsFeedUiState.Loading && currentArticles.isEmpty() -> {
                NewsFeedUiState.Loading(
                    networkStatus = networkStatus,
                    message = "Loading news...",
                    showProgressBar = true,
                    isInitialLoad = true,
                    isLoadingMore = _isLoadingMore.value,
                    canLoadMore = _canLoadMore.value,
                    isRefreshing = true
                )
            }
            // Displaying articles, things are well in the world
            currentArticles.isNotEmpty() -> {
                val isRefreshing =
                    (_uiState.value as? NewsFeedUiState.DisplayingArticles)?.isRefreshing ?: false
                NewsFeedUiState.DisplayingArticles(
                    networkStatus = networkStatus,
                    articles = currentArticles,
                    isRefreshing = false,
                    isLoadingMore = _isLoadingMore.value,
                    canLoadMore = _canLoadMore.value,
                )
            }
            // No articles and not loading, means no content, probably offline on first load
            !(_uiState.value is NewsFeedUiState.Loading) && currentArticles.isEmpty() -> {
                NewsFeedUiState.NoContent(
                    networkStatus = networkStatus,
                    message = "No news found. Try refreshing!",
                    showRefresh = true,
                    isLoadingMore = _isLoadingMore.value,
                    canLoadMore = _canLoadMore.value,
                    isRefreshing = _isCurrentlyRefreshing.value
                )
            }
            // Fallback for any other unhandled state (shouldn't happen but including for safety)
            else -> {
                NewsFeedUiState.Error(
                    networkStatus = networkStatus,
                    message = "An unexpected state occurred.",
                    showRetry = true,
                    isLoadingMore = _isLoadingMore.value,
                    canLoadMore = _canLoadMore.value,
                    isRefreshing = _isCurrentlyRefreshing.value
                )
            }
        }
    }

    fun refreshNews(isInitialLoad: Boolean = false) {
        viewModelScope.launch {
            // Set refreshing state immediately
            if (_uiState.value is NewsFeedUiState.DisplayingArticles) {
                _uiState.value =
                    (_uiState.value as NewsFeedUiState.DisplayingArticles).copy(isRefreshing = true)
            } else if (isInitialLoad || _uiState.value is NewsFeedUiState.Error || _uiState.value is NewsFeedUiState.NoContent) {
                _uiState.value = NewsFeedUiState.Loading(
                    networkStatus = currentNetworkStatus,
                    message = if (isInitialLoad) "Fetching latest news..." else "Refreshing news...",
                    showProgressBar = true,
                    isInitialLoad = isInitialLoad,
                    isLoadingMore = _isLoadingMore.value,
                    canLoadMore = _canLoadMore.value,
                    isRefreshing = _isCurrentlyRefreshing.value
                )
            }

            // Reset loadingMore and canLoadMore states on a full refresh
            // Assume can load more initially after refresh
            _isLoadingMore.value = false
            _canLoadMore.value = true


            val success = repository.refreshNewsFeed(resetPage = true) 
            if (!success) {
                val errorMessage = when (currentNetworkStatus) {
                    ConnectionStatus.UNAVAILABLE, ConnectionStatus.LOST -> "No internet connection. Please check your network."
                    else -> "Failed to load news. Please try again."
                }
                _uiState.value = NewsFeedUiState.Error(
                    networkStatus = currentNetworkStatus,
                    message = errorMessage,
                    // Keep showing previous articles if available
                    articles = currentArticles,
                    showRetry = true,
                    isLoadingMore = _isLoadingMore.value,
                    canLoadMore = _canLoadMore.value,
                    isRefreshing = _isCurrentlyRefreshing.value
                )
                Log.e("NewsViewModel", "Refresh failed: $errorMessage")
            } else {
                // If refresh was successful, articles will be updated via the DB Flow
                // The UI state will transition to DisplayingArticles (or NoContent)
                // when new data is collected from the DB.
                Log.d("NewsViewModel", "Refresh initiated successfully. Waiting for DB updates.")
            }
        }
    }

    fun toggleArticleSavedStatus(article: ArticleEntity) {
        viewModelScope.launch {
            val success = repository.toggleArticleSavedStatus(article)
            if (!success) {
                _uiState.value = NewsFeedUiState.Error(
                    networkStatus = currentNetworkStatus,
                    message = "Failed to save/unsave article.",
                    articles = currentArticles,
                    showRetry = false,
                    isLoadingMore = _isLoadingMore.value,
                    canLoadMore = _canLoadMore.value,
                    isRefreshing = _isCurrentlyRefreshing.value
                )
                Log.e("NewsViewModel", "Failed to toggle saved status for ${article.title}")
            }
            // UI will automatically update via the observed article Flow from the DB
        }
    }

    fun dismissError() {
        // When dismissing an error, we should return to the appropriate state.
        // If articles exist, go to DisplayingArticles; otherwise, NoContent.
        _uiState.value = if (currentArticles.isNotEmpty()) {
            NewsFeedUiState.DisplayingArticles(
                networkStatus = currentNetworkStatus,
                articles = currentArticles,
                isLoadingMore = _isLoadingMore.value,
                canLoadMore = _canLoadMore.value,
                isRefreshing = _isCurrentlyRefreshing.value
            )
        } else {
            NewsFeedUiState.NoContent(
                networkStatus = currentNetworkStatus,
                message = "No news found. Try refreshing!",
                showRefresh = true,
                isLoadingMore = _isLoadingMore.value,
                canLoadMore = _canLoadMore.value,
                isRefreshing = _isCurrentlyRefreshing.value
            )
        }
    }

    // Sets top level screen
    fun setCurrentScreen(screen: CurrentScreen) {
        _currentScreen.value = screen
        Log.d("NewsViewModel", "Current screen set to: $screen")
        // If switching to SAVED_ARTICLES, ensure we have the latest data
        // (the flow collector for getSavedArticles() already updates currentSavedArticles)
    }

    // Expose saved articles for SavedArticlesScreen to use as needed
    fun getSavedArticlesForDisplay(): List<ArticleEntity> {
        return currentSavedArticles
    }

    fun loadNextPage() {
        viewModelScope.launch {
            // Prevent multiple simultaneous loads
            if (!_isLoadingMore.value && _canLoadMore.value) {
                _isLoadingMore.value = true
                updateUiStateBasedOnCurrentData(currentNetworkStatus)

                val success = repository.loadMoreNews()
                _isLoadingMore.value = false

                // _canLoadMore will be updated by the repository's Flow or by the article collection logic
                if (!success && _canLoadMore.value) { // If it failed but there's still more to load, keep canLoadMore true.
                    // This handles cases like network issues during loadMore, allowing retry.
                    Log.e("NewsViewModel", "Failed to load next page.")
                    _uiState.value = NewsFeedUiState.Error(
                        networkStatus = currentNetworkStatus,
                        message = "Failed to load more news. Try again.",
                        articles = currentArticles,
                        showRetry = true,
                        isLoadingMore = _isLoadingMore.value,
                        canLoadMore = _canLoadMore.value
                    )
                } else if (success && !_canLoadMore.value) {
                    Log.d(
                        "NewsViewModel",
                        "Successfully loaded next page, but no more pages available."
                    )
                } else if (success) {
                    Log.d("NewsViewModel", "Successfully loaded next page.")
                }
                // The UI state will naturally update as new articles flow from the repository
                updateUiStateBasedOnCurrentData(currentNetworkStatus) 
            } else {
                Log.d(
                    "NewsViewModel",
                    "Skipping loadNextPage: isLoadingMore=${_isLoadingMore.value}, canLoadMore=${_canLoadMore.value}"
                )
            }
        }

    }
}

        // Custom ViewModel Factory for manual dependency injection (for MainActivity to use)
        class NewsViewModelFactory(private val repository: NewsRepository) :
            ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return NewsViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }