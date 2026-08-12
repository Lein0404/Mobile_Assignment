package com.example.foodieheal.database

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
    val bmi: Double?
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
