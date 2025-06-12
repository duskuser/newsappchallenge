package com.example.news_app_challenge.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Basic type converters
class Converters {
// JSON String -> List of Strings
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

// List of Strings -> JSON Strings
    @TypeConverter
    fun fromList(list: List<String>?): String {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().toJson(list ?: emptyList<String>(), listType)
    }
}