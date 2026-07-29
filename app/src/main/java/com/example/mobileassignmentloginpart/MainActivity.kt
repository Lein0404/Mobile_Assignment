package com.example.mobileassignmentloginpart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobileassignmentloginpart.view.HomeScreen
import com.example.mobileassignmentloginpart.view.LoginScreen
import com.example.mobileassignmentloginpart.view.ProfileScreen
import com.example.mobileassignmentloginpart.view.RegisterScreen
import com.example.mobileassignmentloginpart.navigation.Screen
import com.example.mobileassignmentloginpart.ui.theme.MobileAssignmentTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileAssignmentTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Login.route
                ){
                    composable(Screen.Login.route){
                        LoginScreen(navController)
                    }

                    composable(Screen.Home.route){
                        HomeScreen(navController)
                    }

                    composable(Screen.Register.route){
                        RegisterScreen(navController)
                    }

                    composable(Screen.Profile.route){
                        ProfileScreen(navController)
                    }
                }
            }
        }
    }
}


