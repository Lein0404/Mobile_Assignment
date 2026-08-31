package com.example.foodieheal.User.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodieheal.User.Model.Follow
import com.example.foodieheal.User.Model.User

//This file configure the pattern of the folder in the local database file which name "User database"

//This table stores the data for the private profile data of the user
@Entity(tableName = "local_user")
data class UserEntity(
    @PrimaryKey val id: String,
    val customId: String?,
    val email: String?,
    val name: String?,
    val profilePicUrl: String?,
    val description: String?,
    val weight: Double?,
    val height: Double?,
    val age: Int?,
    val gender: String?,
    val bmi: Double?,
    val isPrivate: Boolean? = false,
    val followerCount: Int? = 0,
    val followingCount: Int? = 0
)

//This table stores the data for the private profile data of the chef
@Entity(tableName = "local_chef")
data class ChefEntity(
    @PrimaryKey val id: String,
    val chefId: String,
    val name: String,
    val gender: String,
    val age: Int,
    val phoneNumber: String,
    val email: String,
    val address: String,
    val state: String,
    val postcode: String,
    val experience: Int,
    val description: String,
    val status: String,
    val profilePictureUrl: String?,
    val averagerating: Double?,
    val Pricing: Double?
)

//This table stores the data for the public profile data of the user,
// we can see the name of the author on the recipe card and view the profile of that user
@Entity(tableName = "public_users")
data class PublicUserEntity(
    @PrimaryKey val customId: String,
    val id: String?,
    val name: String?,
    val profilePicUrl: String?,
    val description: String?,
    val isPrivate: Boolean? = false,
    val followerCount: Int? = 0,
    val followingCount: Int? = 0
)

//This table maps the relationships between users locally, keep track the status of the relationship
@Entity(tableName = "local_follows")
data class FollowEntity(
    @PrimaryKey val id: String,
    val followerId: String,
    val followingId: String,
    val status: String,
    val createdAt: String? = null
)


//toDomain = "I am reading from the phone to show the user."
//toEntity = "I am saving to the phone to remember for later."


// Database -> UI
// When we want to view other user profile, this function take his data out and show us, used in view other people profile screen
fun PublicUserEntity.toDomain(): User {
    return User(
        id = this.id,
        customId = this.customId,
        name = this.name,
        profilePicUrl = this.profilePicUrl,
        description = this.description,
        isPrivate = this.isPrivate,
        followerCount = this.followerCount,
        followingCount = this.followingCount
    )
}

//UI -> Database
//When we save data it updates the ui instantly and save it to the database
fun User.toPublicEntity(): PublicUserEntity {
    return PublicUserEntity(
        customId = this.customId ?: "",
        id = this.id,
        name = this.name,
        profilePicUrl = this.profilePicUrl,
        description = this.description,
        isPrivate = this.isPrivate,
        followerCount = this.followerCount,
        followingCount = this.followingCount
    )
}
//Database -> UI
//When we open follow list, it takes out the data from the database and show it to the user
fun FollowEntity.toDomain(): Follow {
    return Follow(
        id = this.id,
        followerId = this.followerId,
        followingId = this.followingId,
        status = this.status,
        createdAt = this.createdAt
    )
}

//UI -> Database
//When we perform action like follow it immediately store into the local database
fun Follow.toEntity(): FollowEntity {
    return FollowEntity(
        id = this.id ?: "",
        followerId = this.followerId ?: "",
        followingId = this.followingId ?: "",
        status = this.status,
        createdAt = this.createdAt
    )
}
