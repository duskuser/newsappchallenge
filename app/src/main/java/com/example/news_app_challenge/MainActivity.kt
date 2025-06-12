package com.example.news_app_challenge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp 
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.res.Configuration

import com.example.news_app_challenge.presentation.NewsViewModel
import com.example.news_app_challenge.presentation.NewsViewModelFactory
import com.example.news_app_challenge.presentation.NewsFeedUiState
import com.example.news_app_challenge.util.ConnectionStatus
import com.example.news_app_challenge.ui.components.DateHeader
import com.example.news_app_challenge.ui.components.ErrorScreen
import com.example.news_app_challenge.ui.screens.ArticleListScreen
import com.example.news_app_challenge.ui.theme.NewsappchallengeTheme
import com.example.news_app_challenge.presentation.CurrentScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsappchallengeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val application = LocalContext.current.applicationContext as App
                    val viewModel: NewsViewModel = viewModel(
                        factory = NewsViewModelFactory(application.newsRepository)
                    )
                    NewsAppUI(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NewsAppUI(viewModel: NewsViewModel) {
    val newsFeedUiState by viewModel.uiState.collectAsState()
    val localNewsFeedUiState = newsFeedUiState

    val currentScreen by viewModel.currentScreen.collectAsState()
    val localCurrentScreen = currentScreen

    // use context so Composable doesn't freak out
    val context = LocalContext.current 

    val configuration = LocalContext.current.resources.configuration
    val currentOrientation = configuration.orientation 

    Column(modifier = Modifier.fillMaxSize()) {
        // May revisit adding this later if I have time (I didn't)
        // DateHeader()
        Spacer(modifier = Modifier.height(16.dp))

        // Network Status Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val statusText = when (localNewsFeedUiState.networkStatus) {
                ConnectionStatus.AVAILABLE -> "Online"
                ConnectionStatus.LOSING -> "Connection unstable..."
                ConnectionStatus.LOST -> "Offline (lost connection)"
                ConnectionStatus.UNAVAILABLE -> "Offline (no network)"
            }
            val statusColor = when (localNewsFeedUiState.networkStatus) {
                ConnectionStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                ConnectionStatus.LOSING -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Text(
                // Refreshing text here is mainly for debugging, but I'm going to keep it long term for no good reason
                text = "$statusText${if (localNewsFeedUiState.isRefreshing) " & Refreshing..." else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                textAlign = TextAlign.Center
            )

            // Show loading on pull up 
            if (localNewsFeedUiState is NewsFeedUiState.Loading || (localNewsFeedUiState is NewsFeedUiState.DisplayingArticles && localNewsFeedUiState.isRefreshing)) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(start = 8.dp)
                        .height(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        // Main content display, conditionally rendered based on localCurrentScreen
        Box(modifier = Modifier.weight(1f)) {
            when (localCurrentScreen) {
                CurrentScreen.NEWS_FEED -> {
                    when (localNewsFeedUiState) {
                        is NewsFeedUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (localNewsFeedUiState.showProgressBar) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(text = localNewsFeedUiState.message ?: "Loading...", textAlign = TextAlign.Center)
                                }
                            }
                        }
                        is NewsFeedUiState.DisplayingArticles -> {
                            ArticleListScreen(
                                articles = localNewsFeedUiState.articles,
                                isRefreshing = localNewsFeedUiState.isRefreshing,
                                onRefresh = { viewModel.refreshNews() },
                                onArticleClick = { clickedArticle ->
                                    Log.d("NewsAppUI", "News Feed Article clicked: ${clickedArticle.title}")
                                    clickedArticle.url.let { url ->
                                        if (url.isNotEmpty() && url == null) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            if (intent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(intent)
                                            } else {
                                                Log.e("NewsAppUI", "No activity found to handle intent for URL: $url")
                                            }
                                        }
                                    }
                                },
                                onSaveClick = { article -> viewModel.toggleArticleSavedStatus(article) },
                                isLoadingMore = localNewsFeedUiState.isLoadingMore,
                                canLoadMore = localNewsFeedUiState.canLoadMore,  
                                onLoadMore = { viewModel.loadNextPage() } ,
                                currentOrientation = currentOrientation 
                            )
                        }
                        is NewsFeedUiState.Error -> {
                            if (localNewsFeedUiState.articles.isEmpty()) {
                                ErrorScreen(
                                    message = localNewsFeedUiState.message ?: "An unknown error occurred.",
                                    onRetry = { viewModel.refreshNews() }
                                )
                            } else {
                                ArticleListScreen(
                                    articles = localNewsFeedUiState.articles,
                                    isRefreshing = false,
                                    onRefresh = { viewModel.refreshNews() },
                                    onArticleClick = { clickedArticle ->
                                        Log.d("NewsAppUI", "News Feed Article clicked (with error): ${clickedArticle.title}")
                                        clickedArticle.url.let { url ->
                                            if (url.isNotEmpty() && url == null) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                if (intent.resolveActivity(context.packageManager) != null) {
                                                    context.startActivity(intent)
                                                } else {
                                                    Log.e("NewsAppUI", "No activity found to handle intent for URL: $url")
                                                }
                                            }
                                        }
                                    },
                                    onSaveClick = { article -> viewModel.toggleArticleSavedStatus(article) },
                                    isLoadingMore = false, 
                                    canLoadMore = false,   
                                    onLoadMore = null,                                
                                    currentOrientation = currentOrientation 
                                )
                                Log.e("NewsAppUI", "Error while displaying articles: ${localNewsFeedUiState.message}")
                            }
                        }
                        is NewsFeedUiState.NoContent -> {
                            ErrorScreen(
                                message = localNewsFeedUiState.message ?: "No content found.",
                                onRetry = { viewModel.refreshNews() }
                            )
                        }
                    }
                }
                CurrentScreen.SAVED_ARTICLES -> {
                    ArticleListScreen(
                        articles = viewModel.getSavedArticlesForDisplay(),
                        // No need for refreshing / refresh variables in saved articles
                        isRefreshing = false,
                        onRefresh = null,
                        onArticleClick = { clickedArticle ->
                            Log.d("NewsAppUI", "Saved Article clicked: ${clickedArticle.title}")
                            clickedArticle.url.let { url ->
                                if (url.isNotEmpty() && url == null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        Log.e("NewsAppUI", "No activity found to handle intent for URL: $url")
                                    }
                                }
                            }
                        },
                        onSaveClick = { article -> viewModel.toggleArticleSavedStatus(article) },
                        currentOrientation = currentOrientation 
                    )
                }
            }
        }

        // Bottom Nav panel, may be put into its own component later for added design clarity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.CenterHorizontally), 
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier
                .weight(1f)
                    .height(50.dp),
                onClick = { viewModel.setCurrentScreen(CurrentScreen.NEWS_FEED) },
                enabled = localCurrentScreen != CurrentScreen.NEWS_FEED
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "News Feed",
                        modifier = Modifier
                            .size(20.dp) 
                    )
                    Text("News Feed", fontSize = 12.sp) 
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                modifier = Modifier
                .weight(1f)
                    .height(50.dp),
                onClick = { viewModel.setCurrentScreen(CurrentScreen.SAVED_ARTICLES) },
                enabled = localCurrentScreen != CurrentScreen.SAVED_ARTICLES
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "Saved Articles",
                        modifier = Modifier
                            .size(20.dp)
                    )
                    Text("Saved", fontSize = 12.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NewsappchallengeTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("App Preview Placeholder", textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            }
        }
    }
}