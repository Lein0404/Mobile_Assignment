package com.example.foodieheal.User.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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

@Entity(tableName = "local_follows")
data class FollowEntity(
    @PrimaryKey val id: String,
    val followerId: String,
    val followingId: String,
    val status: String,
    val createdAt: String? = null
)

fun PublicUserEntity.toDomain(): com.example.foodieheal.User.Model.User {
    return com.example.foodieheal.User.Model.User(
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

fun com.example.foodieheal.User.Model.User.toPublicEntity(): PublicUserEntity {
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

fun FollowEntity.toDomain(): com.example.foodieheal.User.Model.Follow {
    return com.example.foodieheal.User.Model.Follow(
        id = this.id,
        followerId = this.followerId,
        followingId = this.followingId,
        status = this.status,
        createdAt = this.createdAt
    )
}

fun com.example.foodieheal.User.Model.Follow.toEntity(): FollowEntity {
    return FollowEntity(
        id = this.id ?: "",
        followerId = this.followerId ?: "",
        followingId = this.followingId ?: "",
        status = this.status,
        createdAt = this.createdAt
    )
}
