// Properties for secret file parsing
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android.plugin) 
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.news_app_challenge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.news_app_challenge"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val secretFile = project.rootProject.file("secret.properties")
        val properties = Properties()
        properties.load(secretFile.inputStream())
        val apiKey = properties.getProperty("NEWS_API_KEY") ?: ""
        val userID = properties.getProperty("USER_ID") ?: ""

        buildConfigField(
            type = "String",
            name = "NEWS_API_KEY",
            value = apiKey
        )

        buildConfigField(
            type = "String",
            name = "USER_ID",
            value = userID
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xopt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    buildFeatures {
        compose = true
    }

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.8.0")
        }
    }

}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
   // Lifecycle ViewModel for Compose
   implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
   implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

   // Kotlin Coroutines
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
   

   // Retrofit (Network)
   implementation("com.squareup.retrofit2:retrofit:2.9.0")
   implementation("com.squareup.retrofit2:converter-gson:2.9.0")
   implementation("com.squareup.okhttp3:okhttp:4.12.0")
   implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    // Room 
    val room_version = "2.7.1"

    ksp("androidx.room:room-compiler:$room_version")
        implementation("androidx.room:room-runtime:$room_version")
        implementation("androidx.room:room-ktx:$room_version")
        implementation("androidx.room:room-rxjava2:$room_version")
        implementation("androidx.room:room-rxjava3:$room_version")
        implementation("androidx.room:room-guava:$room_version")
        testImplementation("androidx.room:room-testing:$room_version")
        implementation("androidx.room:room-paging:$room_version")

        // Google Gson (TypeConverters for Room)
        implementation("com.google.code.gson:gson:2.10.1")

            // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Pull to refresh (from Material 1.6.7)
    implementation("androidx.compose.material:material:1.6.7")
    implementation(libs.androidx.compose.material.icons.extended)

    // Dagger Hilt
    val hilt = "2.50"
    implementation("com.google.dagger:hilt-android:$hilt")
    ksp("com.google.dagger:hilt-compiler:$hilt")
    implementation(libs.androidx.hilt.navigation.compose) 
}