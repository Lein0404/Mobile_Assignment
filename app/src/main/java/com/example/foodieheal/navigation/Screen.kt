package com.example.foodieheal.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object AddRecipeToPlanScreen :Screen("add_recipe_to_plan")
    object MealPlanner:Screen("meal_planner")
}
