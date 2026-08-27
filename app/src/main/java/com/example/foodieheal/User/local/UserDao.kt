package com.example.foodieheal.User.local

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

    // 🌟 Cache for other users
    @Query("SELECT * FROM public_users WHERE customId = :customId")
    suspend fun getPublicUser(customId: String): PublicUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublicUser(user: PublicUserEntity)

    // 🌟 Cache for follows
    @Query("SELECT status FROM local_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun getFollowStatus(followerId: String, followingId: String): String?

    @Query("SELECT * FROM local_follows WHERE followerId = :userId")
    suspend fun getFollowing(userId: String): List<FollowEntity>

    @Query("SELECT * FROM local_follows WHERE followingId = :userId")
    suspend fun getFollowers(userId: String): List<FollowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollows(follows: List<FollowEntity>)

    @Query("DELETE FROM local_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)
}
