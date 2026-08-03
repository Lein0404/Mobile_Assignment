package com.example.foodieheal.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Home : Screen("home")
    object Recipes : Screen("recipes")
    object Planner : Screen("planner")
    object AddRecipeToPlanner: Screen("add_recipe_to_planner")
    object Hiring : Screen("hiring")
    object Profile : Screen("profile")
    object AddRecipe : Screen("add_recipe")

    // Ingredients module
    object Ingredients : Screen("ingredients")
    object IngredientDetail : Screen("ingredient_detail/{id}") {
        fun createRoute(id: String) = "ingredient_detail/$id"
    }
}
