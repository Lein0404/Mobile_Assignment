package com.example.foodieheal.navigation

import com.example.foodieheal.meal_planner.model.MealType
import kotlinx.datetime.DayOfWeek
import java.time.LocalDate


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Recipes : Screen("recipes")
    object RecipeDetails : Screen("recipe_details/{recipeId}") {
        fun createRoute(recipeId: String): String = "recipe_details/$recipeId"
    }
    object Planner : Screen("planner")
    object AddRecipeToPlanner: Screen("add_recipe_to_planner/{recipeId}"){
        fun createRoute(id: String) = "add_recipe_to_planner/$id"
    }
    object RecipeSelection : Screen("recipe_selection/{date}/{type}") {
        fun createRoute(date: LocalDate, type: MealType) = "recipe_selection/$date/$type"
        fun createRoute(date: DayOfWeek, type: MealType) = "recipe_selection/${date.name}/$type"
    }
    object TemplateDetails:Screen("template_details/{planId}/{isMyTemplate}"){
        fun createRoute(planId: String,isMyTemplate: Boolean) = "template_details/$planId/$isMyTemplate"

    }
    object AddEditTemplate: Screen("add_edit_template?planId={planId}") {
        fun createRoute(planId: String? = null): String {
            return if (planId != null) {
                "add_edit_template?planId=$planId"
            } else {
                "add_edit_template"
            }
        }
    }

    object Hiring : Screen("hiring")
    object Profile : Screen("profile")
    object AddRecipe : Screen("add_recipe")
    object EditRecipe : Screen("edit_recipe/{recipeId}") {
        fun createRoute(recipeId: String): String = "edit_recipe/$recipeId"
    }
    object Welcome : Screen("welcome")
    object BasicInfo : Screen("basicInfo")
    object Contact : Screen("contactInfo")
    object Address : Screen("addressInfo")
    object Description : Screen("descriptionInfo")
    object ChefPicture : Screen("chefPicture")
    object Review : Screen("reviewInfo")
    object AdminChefScreen : Screen("adminChefApproval?tab={tab}") {
        fun createRoute(tab: Int = 0) = "adminChefApproval?tab=$tab"
    }
    object ChefMain : Screen("chefMain")
    object AppointmentDetails : Screen("appointment_details/{appointmentId}") {
        fun createRoute(appointmentId: String) = "appointment_details/$appointmentId"
    }
    object UserAppointmentDetail : Screen("appointmentDetail/{appointmentId}") {
        fun createRoute(appointmentId: String) = "appointmentDetail/$appointmentId"
    }
    object RescheduleAppointment : Screen("rescheduleAppointment/{appointmentId}") {
        fun createRoute(appointmentId: String) = "rescheduleAppointment/$appointmentId"
    }
    object ChefEditProfile : Screen("chefEditProfile")
    object ChefChangePassword : Screen("chefChangePassword")
    object HiringAppointment : Screen("HiringAppointment")
    object HiringChefDetails : Screen("hiringChefDetails")
    object RateChef : Screen("rateChef/{appointmentId}") {
        fun createRoute(appointmentId: String) = "rateChef/$appointmentId"
    }
    object EditProfile : Screen("editProfile")
    object AppoinmtmentHistory : Screen("appointmentHistory")
    object ChangePassword : Screen("changePassword")
    object EditBodyStatus : Screen("editBodyStatus")

    // 🌟 Added routes from your design
    object AddHiringAppointment : Screen("addHiringAppointment")
    object AppointmentReview : Screen("appointmentReview")

    // Ingredients module
    object Ingredients : Screen("ingredients?tab={tab}") {
        fun createRoute(tab: Int = 0) = "ingredients?tab=$tab"
    }
    object IngredientDetail : Screen("ingredient_detail/{id}/{isRequest}?showAddToCart={showAddToCart}") {
        fun createRoute(id: String, isRequest: Boolean = false, showAddToCart: Boolean = true) = "ingredient_detail/$id/$isRequest?showAddToCart=$showAddToCart"
    }
    object IngredientRequestForm : Screen("ingredient_request_form?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "ingredient_request_form?id=$id" else "ingredient_request_form"
    }
    object ShoppingList : Screen("shopping_list")
    object AddShoppingListItem : Screen("add_shopping_list_item")
    object AdminIngredient: Screen("admin_ingredient?tab={tab}") {
        fun createRoute(tab: Int = 0) = "admin_ingredient?tab=$tab"
    }
    object AdminIngredientDetail : Screen("admin_ingredient_detail/{id}") {
        fun createRoute(id: String) = "admin_ingredient_detail/$id"
    }
    object AdminAddIngredient : Screen("admin_add_ingredient")
    object AdminIngredientReview : Screen("admin_ingredient_review/{id}") {
        fun createRoute(id: String) = "admin_ingredient_review/$id"
    }

    object PaymentMethod : Screen("payment_method")
}
