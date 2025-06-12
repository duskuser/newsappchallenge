package com.example.news_app_challenge.presentation

import com.example.news_app_challenge.data.local.entities.ArticleEntity

// Enumerated connection status indicators for better readability
import com.example.news_app_challenge.util.ConnectionStatus

// Manages UI state for clear outlining of what the program can and can't do
sealed interface NewsFeedUiState {
    val networkStatus: ConnectionStatus 
    val isLoadingMore: Boolean 
    val canLoadMore: Boolean 
    val isRefreshing: Boolean

    data class Loading(
        override val networkStatus: ConnectionStatus,
        val message: String? = null,
        val showProgressBar: Boolean = true,
        val isInitialLoad: Boolean = true,
        // Always false when initial loading for : isLoadingMore, canLoadMore, isRefreshing
        override val isLoadingMore: Boolean = false,
        override val canLoadMore: Boolean = false,
        override val isRefreshing: Boolean
    ) : NewsFeedUiState

    data class DisplayingArticles(
        override val networkStatus: ConnectionStatus,
        val articles: List<ArticleEntity>,
        override val isLoadingMore: Boolean, 
        override val canLoadMore: Boolean,
        override val isRefreshing: Boolean
    ) : NewsFeedUiState

    data class Error(
        override val networkStatus: ConnectionStatus,
        val message: String?,
        val articles: List<ArticleEntity> = emptyList(), 
        val showRetry: Boolean = true, 
        // Always false when error for : isLoadingMore, canLoadMore, isRefreshing
        override val isLoadingMore: Boolean = false, 
        override val canLoadMore: Boolean = false,
        override val isRefreshing: Boolean = false
    ) : NewsFeedUiState

    data class NoContent(
        override val networkStatus: ConnectionStatus,
        val message: String?,
        val showRefresh: Boolean = true,
        // Always false when no content for for : isLoadingMore, canLoadMore, isRefreshing
        override val isLoadingMore: Boolean = false, 
        override val canLoadMore: Boolean = false,
        override val isRefreshing: Boolean = false
    ) : NewsFeedUiState
}