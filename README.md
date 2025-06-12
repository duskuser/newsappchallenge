## Setup:

Create a ```secret.properties``` file in the module level.

Add ```NEWS_API_KEY``` corresponding to your own key

Add ```USER_ID``` (can be anything, in my local version it is simply "example_user")

## Basic Writeup / Thought Process:

Coming into this, I had very little experience with Kotlin so I had to rely on my understanding of design principles and a fair amount of resources to get me through the development process. As is often the case when starting out with a new framework, the general setup, figuring out boilerplate, dependencies, etc. was one of the bigger holdups. 

For this, I used a combination of AI, generic resources (e.g Android documentation), and reading code examples from others who have done similar challenges. 

The majority of all code was written by me, with minor additions from aforementioned various resources to help streamline the process as needed.

I also wanted to add a few key features in order to learn more about how Kotlin works in practice, including:

- Full database setup, includes offline article caching and viewing saved articles online.
- Intuitive refresh
- Offline / Online monitoring with view states that reflect the current status

All of the above are in place.

-----

## MVVM archetecture:

### Model Layer:

data/local/converters/Converters.kt (Data / Local Persistence Helper)

data/local/daos/ArticleDao.kt (Data /Local Data Source - DAO)

data/local/daos/UserDao.kt (Data / Local Data Source - DAO)

data/local/database/AppDatabase.kt ( Data /Local Data Source - Database)

data/local/entities/ArticleEntity.kt (Data / Local Data Model - Structure of article as stored in local DB)

data/local/entities/UserEntity.kt (Data / Local Data Model - Structure of user data in DB)

data/remote/api/NewsApiService.kt (Data / Remote Data Source - API - Defines API calls using Retrofit)

data/remote/models/ApiArticle.kt (Data / Remote Data Model - Structure of article directly from the API)

data/remote/models/NewsResponse.kt (Data / Remote Data Model / Structure of overall response from network API)

data/repository/NewsRepository.kt (Central Repository for Model layer - abstracts data sources, handles data fetching, caching, etc.)

di/NetworkModule.kt (Dependency Injection (Setup for Model)

util/ConnectivityObserver.kt (Utility / Service for consumption by Model)


### ViewModel Layer:


presentation/NewsViewModel.kt

### View Layer:

MainActivity.kt (Main entry point)

presentation/CurrentScreen.kt (Defines high level navigation for consumption by View to switch screens)

presentation/NewsFeedUiState.kt (Sealed interface which defines all possible UI states, observed by View to render accordingly)

ui/components/DateHeader.kt (not in use at the moment)

ui/components/ErrorScreen.kt (Generic reusuable UI Component for error messages)

ui/components/LoadingIndicator.kt (Generic reusable UI Component for showing loading states)

ui/components/NewsCard.kt (Reusable UI component that displays news articles as needed)

ui/screens/ArticleListScreen.kt (Top level UI screen composable that displays the list of articles in NewsCard objects)


Network layer (```NewsApiService```) sends data to ```NewsRepository``` (Model), ```NewsRepository``` communicates with ```NewsViewModel``` (ViewModel) via reactive data streams (Flows), ```NewsViewModel``` consumes these streams, transforming the raw data into usable ```NewsFeedUiState```'s for the Views to update as needed.

## Known issues:

Sometimes NewsCard.kt's "localIsSaved" boolean can desync from the back-end, causing unexpected behavior

It is possible to open a new article right on the trigger to load new articles and cause a strange hold up in article refreshing where you're not allowed to scroll down.

## Resources Used: 

This list is not exhaustive, I made an effort to save some of the URLs I read through in my learning process, however I did not save everything.  

- https://square.github.io/retrofit/
- https://developer.android.com/develop/ui/compose/setup#kotlin
- https://newsapi.org/docs/endpoints/top-headlines
- https://developer.android.com/training/data-storage/room#kts
- https://developer.android.com/training/dependency-injection/dagger-android
- https://stackoverflow.com/questions/46665621/android-room-persistent-appdatabase-impl-does-not-exist
- https://kotlinlang.org/docs/lambdas.html
- https://developer.android.com/develop/ui/compose/designsystems/material3
- https://stackoverflow.com/questions/72030397/in-my-android-app-how-can-i-define-properties-that-wont-get-checked-into-the-ve
- https://stackoverflow.com/questions/74634321/fixing-the-build-type-contains-custom-buildconfig-fields-but-the-feature-is-di


