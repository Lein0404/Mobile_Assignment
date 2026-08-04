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
    object ChefProfileScreen : Screen("chefProfile")
    object ChefMain : Screen("chefMain")
    object ChefEditProfile : Screen("chefEditProfile")
    object HiringChefDetails : Screen("hiringChefDetails")
    object HiringAppointment : Screen("hiringAppointment")
    object AddHiringAppointment : Screen("AddAppointment")
    object AppointmentReview : Screen("appointmentReview")

    // Ingredients module
    object Ingredients : Screen("ingredients")
    object IngredientDetail : Screen("ingredient_detail/{id}") {
        fun createRoute(id: String) = "ingredient_detail/$id"
    }
    object ShoppingList : Screen("shopping_list")
    object AddShoppingListItem : Screen("add_shopping_list_item")
}
