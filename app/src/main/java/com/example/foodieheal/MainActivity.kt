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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foodieheal.ingredients.view.IngredientsScreen
import com.example.foodieheal.ingredients.view.IngredientDetailScreen
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.screen.AddRecipeToPlanScreen
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.view.MainScreen
import com.example.foodieheal.view.AddRecipeScreen
import com.example.foodieheal.ui.theme.FoodieHealTheme
import com.example.foodieheal.view.LoginScreen
import com.example.foodieheal.view.RegisterScreen
import io.github.jan.supabase.postgrest.postgrest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodieHealTheme(dynamicColor = false) {
                val navController = rememberNavController()

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

                    composable(Screen.Ingredients.route){
                        IngredientsScreen(navController)
                    }

                    composable(
                        route = Screen.IngredientDetail.route,
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        IngredientDetailScreen(navController, id)
                    }
                }
            }
        }
    }
}