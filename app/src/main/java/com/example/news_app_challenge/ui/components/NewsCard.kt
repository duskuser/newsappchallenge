package com.example.news_app_challenge.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.news_app_challenge.R
import com.example.news_app_challenge.data.local.entities.ArticleEntity
import com.example.news_app_challenge.ui.theme.NewsappchallengeTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.runtime.remember 
import androidx.compose.runtime.mutableStateOf 
import androidx.compose.runtime.getValue 
import androidx.compose.runtime.setValue 
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsCard(
    article: ArticleEntity,
    isSelected: Boolean,
    onArticleClick: (ArticleEntity) -> Unit,
    onSaveClick: (ArticleEntity) -> Unit
) {
    val context = LocalContext.current

    // Likely would consolidate these in a production environment for added clarity, e.g in a NewsCardValues file or similar
    val cardHeight = if (isSelected) 400.dp else 280.dp

    val maxDescriptionLines = if (isSelected) 3 else 1

    // local state for handling the front-end display of 'isSaved' 
    var localIsSaved by remember { mutableStateOf(article.isSaved) }

    // LaunchedEffect to manage state, ensure no desyncs
    LaunchedEffect(article.isSaved) {
        if (localIsSaved != article.isSaved) { 
            localIsSaved = article.isSaved
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.wrapContentHeight()
                } else {
                    Modifier.height(cardHeight)
                }
            )
            .padding(1.dp)
            .clickable(onClick = { onArticleClick(article) }),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            article.urlToImage?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .error(R.drawable.ic_broken_image)
                        .fallback(R.drawable.ic_no_image_placeholder)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSelected) 180.dp else 120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = article.title ?: "No Title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = maxDescriptionLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = article.sourceName ?: "Unknown Source",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            article.publishedAt?.let { dateString ->
                val formattedDate = remember(dateString) {
                    try {
                        val dateTime = ZonedDateTime.parse(dateString)
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(dateTime)
                    } catch (e: Exception) {
                        Log.e("NewsCard", "Error parsing date: $dateString", e)
                        "Invalid Date"
                    }
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }

            if (isSelected) {
                article.description?.let { desc ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { 
                            onSaveClick(article)
                            localIsSaved = !localIsSaved
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (localIsSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (localIsSaved) "Unsave" else "Save"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (localIsSaved) "Saved!" else "Save")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            article.url.let {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Go to Article")
                    }
                }
            } else {
                article.description?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// Previews for NewsCard (same as before)
@Preview(showBackground = true, widthDp = 360)
@Composable
fun NewsCardExpandedPreview() {
    val sampleArticle = ArticleEntity(
        url = "https://example.com/news/expanded",
        sourceId = "google-news", sourceName = "Google News",
        author = "John Doe",
        title = "This is a test article for testing purposes, I am testing right now!",
        description = "Local developer needs text in a box for testing previews of components reports Google. 'He may even be trying to put placeholder text in right now' said someone familiar with the scene. News sources are unsure why this matters, or why we're reporting on it.",
        urlToImage = "https://placehold.co/600x400/E0E0E0/333333?text=Expanded+Image",
        publishedAt = "2024-06-10T14:30:00Z",
        content = "Full content here...",
        isSaved = true
    )
    NewsappchallengeTheme {
        NewsCard(article = sampleArticle, isSelected = true, onArticleClick = {}, onSaveClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun NewsCardCollapsedPreview() {
    val sampleArticle = ArticleEntity(
        url = "https://example.com/news/collapsed",
        sourceId = "nyt", sourceName = "New York Times",
        author = "Jane Smith",
        title = "This is a test article for testing purposes, I am testing right now!",
        description = "Local developer needs text in a box for testing previews of components reports Google. 'He may even be trying to put placeholder text in right now' said someone familiar with the scene. News sources are unsure why this matters, or why we're reporting on it.",
        urlToImage = "https://placehold.co/600x400/C0D9EE/333333?text=Collapsed+Image",
        publishedAt = "2024-06-09T10:00:00Z",
        content = "More content here...",
        isSaved = false
    )
    NewsappchallengeTheme {
        NewsCard(article = sampleArticle, isSelected = false, onArticleClick = {}, onSaveClick = {})
    }
}