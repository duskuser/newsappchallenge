package com.example.news_app_challenge.data.remote.api

import com.example.news_app_challenge.data.remote.models.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit instance for getting data from api
interface NewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("apiKey") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): NewsResponse
}