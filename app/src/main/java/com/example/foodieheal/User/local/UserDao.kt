package com.example.foodieheal.User.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

//This is the data access object for the user, the bridge between the kotlin and database on the phone
@Dao
interface UserDao {
    //NORMAL USER
    //suspend is like this operation can be blocked without affect the main thread
    //get data from the local user database, get one record
    @Query("SELECT * FROM local_user LIMIT 1")
    suspend fun getUser(): UserEntity?

    //save commend, if the user exist dy update their info with new data, no create any duplicate
    //save update into local database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    //delete all record from the local user table, used by the logout button
    @Query("DELETE FROM local_user")
    suspend fun deleteUser()

    //CHEF
    //select all record from the local chef, get one record
    @Query("SELECT * FROM local_chef LIMIT 1")
    suspend fun getChef(): ChefEntity?

    //save commend, if the chef exist dy update their info with new data, no create any duplicate
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChef(chef: ChefEntity)

    //delete all record from the local chef
    @Query("DELETE FROM local_chef")
    suspend fun deleteChef()

    // PUBLIC USER CACHE
    // This is used for other user profile, it gets the data of that user based on the id of it
    // Search the local database for other user info
    // :customId means it is the parameter for the function to put in the id and call this function
    @Query("SELECT * FROM public_users WHERE customId = :customId")
    suspend fun getPublicUser(customId: String): PublicUserEntity?

    //Saves other user information to the local database, reduce network request
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublicUser(user: PublicUserEntity)

    // FOLLOW AND SOCIAL
    // Check the local database for current relationship status between two people (accepted or pending)
    // Used by Profile screen and Recipe Detail Screen. Use to decide show follow button or unfollow button
    // If return null = follow, if return accepted = unfollow, if return pending =  request sent
    @Query("SELECT status FROM local_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun getFollowStatus(followerId: String, followingId: String): String?

    // Get the list of the user that the current user is following
    // Make the following list screen quickly show data
    @Query("SELECT * FROM local_follows WHERE followerId = :userId")
    suspend fun getFollowing(userId: String): List<FollowEntity>

    // Get the list of the user that current user is followed by
    // Make the follower list screen quickly show data
    @Query("SELECT * FROM local_follows WHERE followingId = :userId")
    suspend fun getFollowers(userId: String): List<FollowEntity>

    //Save bulk list of follow relationships for offline viewing
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollows(follows: List<FollowEntity>)

    //Used to remove one follow relationship, used by unfollow button
    @Query("DELETE FROM local_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)
}
