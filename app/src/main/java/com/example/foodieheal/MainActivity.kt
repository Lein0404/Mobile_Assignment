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
                    startDestination = Login
                ){
                    composable<Login>{
                        LoginScreen(navController)
                    }

                    composable<Home>{
                        HomeScreen(navController)
                    }

                    composable<Register>{
                        RegisterScreen(navController)
                    }

                    composable<Profile>{
                        ProfileScreen(navController)
                    }

                    composable<Ingredients> {
                        IngredientsScreen(navController)
                    }

                    composable<IngredientDetail> { backStackEntry ->
                        val detail: IngredientDetail = backStackEntry.toRoute()
                        IngredientDetailScreen(navController, detail.id)
                    }
                }
            }
        }
    }
}


