package com.example.foodieheal.navigation

import kotlinx.serialization.Serializable

/** This handles all navigation routes.
 *
 */

@Serializable
sealed interface NavRoute {
    @Serializable object Login : NavRoute
    @Serializable object Register : NavRoute
    @Serializable object Home : NavRoute
    @Serializable object Profile : NavRoute

    // Ingredients module
    @Serializable object Ingredients : NavRoute
    @Serializable data class IngredientDetail(val id: String) : NavRoute
}