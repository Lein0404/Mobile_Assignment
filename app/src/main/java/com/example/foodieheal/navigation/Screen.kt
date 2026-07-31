package com.example.foodieheal.navigation

import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Register

@Serializable
object Home

@Serializable
object Profile

@Serializable
object Ingredients

@Serializable
data class IngredientDetail(val id: String)
