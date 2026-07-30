package com.example.foodieheal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.user.LoginScreen
import com.example.foodieheal.navigation.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.View.HomeScreen
import com.example.foodieheal.View.ProfileScreen
import com.example.foodieheal.View.RegisterScreen
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.ui.theme.MobileAssignmentTheme
import io.github.jan.supabase.postgrest.postgrest


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

                    composable(Screen.MealPlanner.route) {
                        val viewModel: MealPlannerViewModel = viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    // Accessing your global object here
                                    val repository = MealPlannerRepository(
                                        postgrest = SupabaseClient.client.postgrest,
                                        supabaseClient = SupabaseClient.client
                                    )
                                    return MealPlannerViewModel(repository) as T
                                }
                            }
                        )

                        MealPlannerScreen(
                            viewModel = viewModel,
                            modifier = Modifier
                        )
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
fun ZhScreen(navController: NavHostController){
    Scaffold{innerPadding->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Button(onClick = { navController.navigate(Screen.MealPlanner.route) }) {
                Text("Meal Planner")
            }
        }
    }
}