package com.example.foodieheal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.view.LoginScreen
import com.example.foodieheal.view.RegisterScreen
import com.example.foodieheal.view.MainScreen
import com.example.foodieheal.view.AddRecipeScreen
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.theme.FoodieHealTheme


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
                }
            }
        }
    }
}
