package com.example.news_app_challenge.data.remote.models

import com.google.gson.annotations.SerializedName

// Data class for containing API responses
data class NewsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("articles") val articles: List<ApiArticle>,
    // Contains error messages
    @SerializedName("message") val message: String? 
)