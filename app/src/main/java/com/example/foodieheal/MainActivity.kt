package com.example.foodieheal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.meal_planner.screen.AddRecipeToPlanScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModelFactory
import com.example.foodieheal.view.MainScreen
import com.example.foodieheal.view.AddRecipeScreen
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.theme.FoodieHealTheme
import com.example.foodieheal.view.LoginScreen
import com.example.foodieheal.view.RegisterScreen
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val mealPlannerViewModel: MealPlannerViewModel by viewModels {
        MealPlannerViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle cold start deep link
        intent?.data?.let { uri ->
            processDeepLink(uri)
        }

        setContent {
            FoodieHealTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val lifecycleOwner = LocalLifecycleOwner.current

                // 🌟 1. Listen for navigation events from ViewModel (Cold & Warm start safe!)
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        mealPlannerViewModel.navigationEvent.collect { route ->
                            val hasLoginOnStack = navController.currentBackStackEntry?.destination?.route == Screen.Login.route

                            navController.navigate(route) {
                                if (hasLoginOnStack) {
                                    // Cold start: clear login screen off stack
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                } else {
                                    // Warm start: keep user session, bring Main screen to top
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Login.route
                ) {
                    composable(Screen.Login.route) { LoginScreen(navController) }
                    composable(Screen.Register.route) { RegisterScreen(navController) }
                    composable(Screen.Main.route) {
                        MainScreen(navController, mealPlannerViewModel)
                    }
                    composable(Screen.AddRecipe.route) { AddRecipeScreen(navController) }

                    // Pass arguments via route parameters, avoid hardcoded domain models in NavHost
                    composable("${Screen.AddRecipeToPlanner.route}/{recipeId}") {
                        AddRecipeToPlanScreen(
                            viewModel = mealPlannerViewModel,
                            onExecutionComplete = { navController.popBackStack() },
                            recipe = Recipe(
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
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update activity intent
        intent.data?.let { uri ->
            processDeepLink(uri)
        }
    }

    private fun processDeepLink(uri: Uri) {
        Log.d("DeepLink", "Processing URI: $uri")

        val isHttpsLink = uri.scheme == "https" && uri.host == "tzh652.github.io" && uri.path?.startsWith("/share") == true
        val isCustomScheme = uri.scheme == "foodieheal" && uri.host == "share"

        if (isHttpsLink || isCustomScheme) {
            uri.getQueryParameter("sourceStart")?.let { dateStr ->
                runCatching {
                    LocalDate.parse(dateStr)
                }.onSuccess { startDate ->
                    mealPlannerViewModel.prepareSharedWeeklyPlan(startDate)
                }.onFailure { e ->
                    Log.e("DeepLink", "Failed to parse date: $dateStr", e)
                }
            }
        }
    }
}