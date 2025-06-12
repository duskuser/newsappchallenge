package com.example.news_app_challenge.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.news_app_challenge.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUser(userId: String): Flow<UserEntity?>

    @Update
    suspend fun updateUser(user: UserEntity)
}
