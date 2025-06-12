package com.example.news_app_challenge.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.news_app_challenge.data.local.entities.ArticleEntity
import com.example.news_app_challenge.ui.components.NewsCard
import com.example.news_app_challenge.ui.theme.NewsappchallengeTheme
import androidx.compose.foundation.lazy.grid.rememberLazyGridState 
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect 
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.size
import android.content.res.Configuration
import com.example.news_app_challenge.data.local.entities.uniqueKey

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ArticleListScreen( 
    articles: List<ArticleEntity>, 
    isRefreshing: Boolean = false, 
    onRefresh: (() -> Unit)? = null, 
    onArticleClick: (ArticleEntity) -> Unit, 
    onSaveClick: (ArticleEntity) -> Unit,
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    currentOrientation: Int
) {
    var selectedArticleKey by remember { mutableStateOf<String?>(null) }

    // Only set up pull-to-refresh state if onRefresh callback is provided
    val pullRefreshState = if (onRefresh != null) {
        rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = onRefresh
        )
    } else {
        // No pull-to-refresh
        null 
    }

    val lazyGridState = rememberLazyGridState()

    // Only triggers if can load more and user is not already loading
    LaunchedEffect(lazyGridState, canLoadMore, isLoadingMore) {
        if (canLoadMore && !isLoadingMore && onLoadMore != null) {
            snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { lastVisibleIndex ->
                    // Trigger load more when user is 5 items away from the end
                    if (lastVisibleIndex != null && lastVisibleIndex >= articles.size - 5) {
                        Log.d("ArticleListScreen", "Reached end of list, attempting to load more.")
                        onLoadMore.invoke()
                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (pullRefreshState != null) Modifier.pullRefresh(pullRefreshState) else Modifier)
            // Apply pullRefresh modifier conditionally
    ) {
        if (articles.isEmpty() && !isRefreshing) {
            // Empty states (e.g., no news, no saved articles)
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No articles found here. Check another section or refresh!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = when (currentOrientation) { 
                    Configuration.ORIENTATION_PORTRAIT -> GridCells.Fixed(2) 
                    Configuration.ORIENTATION_LANDSCAPE -> GridCells.Fixed(3)
                    else -> GridCells.Fixed(2)
                },
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
                state = lazyGridState
            ) {
                items(
                    articles,
                    key = { it.uniqueKey }, 
                    span = { article ->
                        if (article.uniqueKey == selectedArticleKey) {
                            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                                GridItemSpan(2) 
                            } else {
                                GridItemSpan(3)
                            }
                        } else {
                            GridItemSpan(1)
                        }
                    }
                ) { article ->
                    val isSelected = article.uniqueKey == selectedArticleKey

                    NewsCard(
                        article = article,
                        isSelected = isSelected,
                        onArticleClick = { clickedArticle ->
                            Log.d("ArticleListScreen", "Article clicked: ${clickedArticle.title}")
                            selectedArticleKey = if (isSelected) null else clickedArticle.uniqueKey
                            // Pass the click event up for opening URLs
                            onArticleClick(clickedArticle) 
                        },
                        onSaveClick = onSaveClick
                    )
                }

                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }


        // Pull-to-refresh indicator, only if pullRefreshState is available
        if (pullRefreshState != null) {
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                scale = true
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun ArticleListScreenPreview() {
    val sampleArticles = List(6) { index ->
        ArticleEntity(
            url = "https://example.com/article-$index",
            sourceId = "source-$index", sourceName = "Source ${index + 1}",
            author = "Author $index",
            title = "Headline ${index + 1}: This is a test article for testing purposes, I am testing right now!",
            description = "Local developer needs text in a box for testing previews of components reports Google. 'He may even be trying to put placeholder text in right now' said someone familiar with the scene. News sources are unsure why this matters, or why we're reporting on it.",
            urlToImage = if (index % 2 == 0) "https://placehold.co/600x400/C0D9EE/333333?text=Image+${index+1}" else null,
            publishedAt = "2024-06-10T${10 + index}:00:00Z",
            content = "Detailed content of article ${index + 1}.",
            isSaved = index % 3 == 0
        )
    }
    NewsappchallengeTheme {
        ArticleListScreen(
            articles = sampleArticles,
            onArticleClick = {},
            onSaveClick = {},
            isLoadingMore = true,
            canLoadMore = true,
            // Can change as needed
            currentOrientation = Configuration.ORIENTATION_PORTRAIT
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun ArticleListScreenEmptyPreview() {
    NewsappchallengeTheme {
        ArticleListScreen(
            articles = emptyList(),
            onArticleClick = {},
            onSaveClick = {},
            isLoadingMore = false,
            canLoadMore = false,
            // Can change as needed
            currentOrientation = Configuration.ORIENTATION_PORTRAIT
        )
    }
}