package com.example.foodieheal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.Admin.AdminApprovalScreen
import com.example.foodieheal.Admin.AdminIngredientDetailScreen
import com.example.foodieheal.Admin.AdminIngredientRequestFormScreen
import com.example.foodieheal.Admin.ChefDetailScreen
import com.example.foodieheal.Chef.ChefHomeScreen
import com.example.foodieheal.Chef.ChefMainScreen
import com.example.foodieheal.Chef.ChefProfileScreen
import com.example.foodieheal.Chef.Register.ChefPictureScreen
import com.example.foodieheal.Chef.Register.ChefWelcomeScreen
import com.example.foodieheal.Chef.Register.addressInfo
import com.example.foodieheal.Chef.Register.basicInfo
import com.example.foodieheal.Chef.Register.contactInfo
import com.example.foodieheal.Chef.Register.descriptionInfo
import com.example.foodieheal.Chef.Register.reviewInfo
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.view.LoginScreen
import com.example.foodieheal.view.RegisterScreen
import androidx.navigation.navArgument
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.screen.AddRecipeToPlanScreen
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.view.MainScreen
import com.example.foodieheal.view.AddRecipeScreen
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.theme.FoodieHealTheme
import io.github.jan.supabase.postgrest.postgrest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodieHealTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val chefviewModel: chefRegisterViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Login.route
                ){
                    composable(Screen.Login.route){
                        LoginScreen(navController)
                    }

                    composable(Screen.Register.route){
                        RegisterScreen(navController)
                    }

                    composable(Screen.Main.route) {
                        MainScreen(navController)
                    }

                    composable(Screen.AddRecipe.route) {
                        AddRecipeScreen(navController)
                    }

                    //Ivan part (admin site)
                    composable(Screen.AdminChefScreen.route) {
                        AdminApprovalScreen(navController)
                    }

                    composable(
                        route = Screen.AdminIngredientDetail.route,
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        AdminIngredientDetailScreen(navController, id)
                    }

                    composable(
                        route = Screen.AdminIngredientReview.route,
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        AdminIngredientRequestFormScreen(navController, id)
                    }

                    composable(Screen.ChefMain.route) {
                        ChefMainScreen(navController)
                    }

                    composable(
                        "chefDetail/{chefId}"
                    ) { backStackEntry ->
                        ChefDetailScreen(
                            chefId = backStackEntry.arguments
                                ?.getString("chefId") ?: "",
                            navController = navController
                        )
                    }

                    navigation(
                        startDestination = Screen.Welcome.route,
                        route = "chefRegisterRoute"
                    ) {
                        composable(Screen.Welcome.route) {
                            ChefWelcomeScreen(
                                navController,
                                chefviewModel
                            )
                        }

                        composable(Screen.BasicInfo.route) {
                            basicInfo(
                                navController,
                                chefviewModel
                            )
                        }

                        composable(Screen.Contact.route) {
                            contactInfo(
                                navController,
                                chefviewModel
                            )
                        }

                        composable(Screen.Address.route) {
                            addressInfo(
                                navController,
                                chefviewModel
                            )
                        }

                        composable(Screen.Description.route) {
                            descriptionInfo(
                                navController,
                                chefviewModel)
                        }

                        composable(Screen.ChefPicture.route) {
                            ChefPictureScreen(
                                navController,
                                chefviewModel
                            )
                        }

                        composable(Screen.Review.route) {
                            reviewInfo(
                                navController,
                                chefviewModel
                            )
                        }
                    }

                    composable (Screen.AddRecipeToPlanner.route){
                        val viewModel: MealPlannerViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    val repository = MealPlannerRepository(
                                        postgrest = SupabaseClient.client.postgrest,
                                        supabaseClient = SupabaseClient.client
                                    )
                                    // 🌟Passed 'application' context alongside the repository here too
                                    return MealPlannerViewModel(application, repository) as T
                                }
                            }
                        )

                        AddRecipeToPlanScreen(
                            viewModel = viewModel,
                            modifier = Modifier,
                            recipe = Recipe(//TODO pass specific recipe here, wait for recipe module
                                recipe_id = "R011",
                                recipeName = "Chicken Wrap",
                                calories = 340,
                                time = 15,
                                recipeImage = R.drawable.ic_lunch,
                                recipeDescription = "A delicious wrap filled with grilled chicken.",
                                budget = 5.80,
                                skillLevel = 1,
                                recipeStep = "Grill chicken, fill tortilla, roll."
                            ),
                            onExecutionComplete = {navController.popBackStack()},
                        )
                    }
                }
            }
        }
    }
}