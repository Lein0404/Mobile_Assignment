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
    object HiringChefDetails : Screen("hiringChefDetails")
    object RateChef : Screen("rateChef/{appointmentId}") {
        fun createRoute(appointmentId: String) = "rateChef/$appointmentId"
    }
    object EditProfile : Screen("editProfile")
    object ChangePassword : Screen("changePassword")
    object EditBodyStatus : Screen("editBodyStatus")

    // 🌟 Added routes from your design
    object AddHiringAppointment : Screen("addHiringAppointment")
    object AppointmentReview : Screen("appointmentReview")
    object Ingredients : Screen("ingredients")
    object IngredientDetail : Screen("ingredient_detail/{id}/{isRequest}") {
        fun createRoute(id: String, isRequest: Boolean = false) = "ingredient_detail/$id/$isRequest"
    }
    object IngredientRequestForm : Screen("ingredient_request_form?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "ingredient_request_form?id=$id" else "ingredient_request_form"
    }
    object ShoppingList : Screen("shoppingList")
    object AddShoppingListItem : Screen("addShoppingListItem")
    object AdminIngredientDetail : Screen("admin_ingredient_detail/{id}") {
        fun createRoute(id: String) = "admin_ingredient_detail/$id"
    }
    object AdminIngredientReview : Screen("admin_ingredient_review/{id}") {
        fun createRoute(id: String) = "admin_ingredient_review/$id"
    }
}
