package com.example.foodieheal.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Home : Screen("home")
    object Recipes : Screen("recipes")
    object Planner : Screen("planner")
    object AddRecipeToPlanner: Screen("add_recipe_to_planner/{recipeId}")
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
    object ChefMain : Screen("chefMain")
    object AppointmentDetails : Screen("appointment_details/{appointmentId}") {
        fun createRoute(appointmentId: String) = "appointment_details/$appointmentId"
    }
    object ChefEditProfile : Screen("chefEditProfile")
    object HiringAppointment : Screen("HiringAppointment")
    object EditProfile : Screen("editProfile")
    object ChangePassword : Screen("changePassword")
    object EditBodyStatus : Screen("editBodyStatus")

    // 🌟 Added routes from your design
    object AddHiringAppointment : Screen("addHiringAppointment")
    object AppointmentReview : Screen("appointmentReview")
    object Ingredients : Screen("ingredients")
    object IngredientDetail : Screen("ingredientDetail/{id}/{isRequest}")
    object ShoppingList : Screen("shoppingList")
    object AddShoppingListItem : Screen("addShoppingListItem")
    object IngredientRequestForm : Screen("ingredientRequestForm")
    object AdminIngredientDetail : Screen("adminIngredientDetail/{id}")
    object AdminIngredientReview : Screen("adminIngredientReview/{id}")
}
