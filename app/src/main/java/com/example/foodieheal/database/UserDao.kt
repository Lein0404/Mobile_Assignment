package com.example.foodieheal.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM local_user LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM local_user")
    suspend fun deleteUser()

    @Query("SELECT * FROM local_chef LIMIT 1")
    suspend fun getChef(): ChefEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChef(chef: ChefEntity)

    @Query("DELETE FROM local_chef")
    suspend fun deleteChef()
}
