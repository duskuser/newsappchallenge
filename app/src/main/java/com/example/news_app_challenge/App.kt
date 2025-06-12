package com.example.news_app_challenge

import android.app.Application
import com.example.news_app_challenge.data.local.database.AppDatabase
import com.example.news_app_challenge.data.remote.api.NewsApiService
import com.example.news_app_challenge.data.repository.NewsRepository
import com.example.news_app_challenge.util.ConnectivityObserverImpl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.util.Log

class App : Application() {
    // Lazily initialized database instance
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    // Lazily initialized API service instance
    private val newsApiService: NewsApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val userAgentInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", "News App Challenge/1.0 (Android)")
                .build()
            chain.proceed(requestWithUserAgent)
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(userAgentInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl("https://newsapi.org/v2/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }

    // Lazily initialized ConnectivityObserver
    private val connectivityObserver by lazy {
        ConnectivityObserverImpl(applicationContext)
    }

    // Lazily initialized repository instance
    val newsRepository: NewsRepository by lazy {
        NewsRepository(
            apiService = newsApiService,
            articleDao = database.articleDao(),
            userDao = database.userDao(),
            connectivityObserver = connectivityObserver
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("App", "Application onCreate called.")
        // Initialize the default user in the database when the app starts
        // Using GlobalScope for app-wide, fire-and-forget work
        GlobalScope.launch { 
            newsRepository.initializeUser()
        }
    }
}