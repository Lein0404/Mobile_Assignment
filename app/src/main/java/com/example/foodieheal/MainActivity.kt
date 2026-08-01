package com.example.foodieheal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.foodieheal.view.HomeScreen
import com.example.foodieheal.view.LoginScreen
import com.example.foodieheal.view.ProfileScreen
import com.example.foodieheal.view.RegisterScreen
import com.example.foodieheal.navigation.*
import com.example.foodieheal.ingredients.view.IngredientsScreen
import com.example.foodieheal.ingredients.view.IngredientDetailScreen
import com.example.foodieheal.ui.theme.MobileAssignmentTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileAssignmentTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = NavRoute.Ingredients
                ){
                    composable<NavRoute.Login>{
                        LoginScreen(navController)
                    }

                    composable<NavRoute.Home>{
                        HomeScreen(navController)
                    }

                    composable<NavRoute.Register>{
                        RegisterScreen(navController)
                    }

                    composable<NavRoute.Profile>{
                        ProfileScreen(navController)
                    }

                    composable<NavRoute.Ingredients> {
                        IngredientsScreen(navController)
                    }

                    composable<NavRoute.IngredientDetail> { backStackEntry ->
                        val detail: NavRoute.IngredientDetail = backStackEntry.toRoute()
                        IngredientDetailScreen(navController, detail.id)
                    }
                }
            }
        }
    }
}


