package com.example.mobileassignmentloginpart.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Zh : Screen("zh")
    object Meal_Planner : Screen("meal_planner")
}