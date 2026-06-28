package com.example.mobileassignmentloginpart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobileassignmentloginpart.home.HomeScreen
import com.example.mobileassignmentloginpart.user.LoginScreen
import com.example.mobileassignmentloginpart.user.ProfileScreen
import com.example.mobileassignmentloginpart.user.RegisterScreen
import com.example.mobileassignmentloginpart.meal_planner.MealPlannerPreview
import com.example.mobileassignmentloginpart.navigation.Screen
import com.example.mobileassignmentloginpart.ui.theme.MobileAssignmentLoginPartTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileAssignmentLoginPartTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Zh.route
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
                    composable(Screen.Meal_Planner.route){
                        MealPlannerPreview()
                    }
                    composable (Screen.Zh.route){
                        ZhScreen(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun ZhScreen(navController: NavHostController,){
    Scaffold{innerPadding->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Button(onClick = { navController.navigate(Screen.Meal_Planner.route) }) {
                Text("Meal Planner")
            }
        }
    }
}