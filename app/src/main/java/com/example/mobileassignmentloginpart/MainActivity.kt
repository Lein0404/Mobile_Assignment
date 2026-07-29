package com.example.mobileassignmentloginpart

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
import com.example.mobileassignmentloginpart.meal_planner.MealPlannerScreen
import com.example.mobileassignmentloginpart.meal_planner.MealPlannerViewModel
import com.example.mobileassignmentloginpart.user.LoginScreen
import com.example.mobileassignmentloginpart.navigation.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobileassignmentloginpart.home.HomeScreen
import com.example.mobileassignmentloginpart.View.ProfileScreen
import com.example.mobileassignmentloginpart.View.RegisterScreen
import com.example.mobileassignmentloginpart.meal_planner.MealPlannerRepository
import com.example.mobileassignmentloginpart.ui.theme.MobileAssignmentTheme
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