package com.example.foodieheal.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Home : Screen("home")
    object Recipes : Screen("recipes")
    object Planner : Screen("planner")
    object Hiring : Screen("hiring")
    object Profile : Screen("profile")
    object AddRecipe : Screen("add_recipe")
    object Welcome : Screen("welcome")
    object BasicInfo : Screen("basicInfo")
    object Contact : Screen("contactInfo")
    object Address : Screen("addressInfo")
    object Description : Screen("descriptionInfo")
    object ChefPicture : Screen("chefPicture")
    object Review : Screen("reviewInfo")
    object AdminChefScreen : Screen("adminChefApproval")
    object ChefHomeScreen : Screen("chefHome")
    object ChefDetailScreen : Screen("chefDetail/{chefId}")
}
