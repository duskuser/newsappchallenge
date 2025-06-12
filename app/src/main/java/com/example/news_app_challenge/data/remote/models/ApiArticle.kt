package com.example.news_app_challenge.data.remote.models

import com.google.gson.annotations.SerializedName

// Network model for Article, directly from API call
data class ApiArticle(
    @SerializedName("source") val source: ApiSource?,
    @SerializedName("author") val author: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("content") val content: String?
)

// "Source" data class
data class ApiSource(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

