package com.example.mobileassignmentloginpart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.mobileassignmentloginpart.Admin.AdminApprovalScreen
import com.example.mobileassignmentloginpart.Admin.ChefDetailScreen
import com.example.mobileassignmentloginpart.Chef.Home.ChefHomeScreen
import com.example.mobileassignmentloginpart.Chef.Register.ChefWelcomeScreen
import com.example.mobileassignmentloginpart.Chef.Register.addressInfo
import com.example.mobileassignmentloginpart.Chef.Register.basicInfo
import com.example.mobileassignmentloginpart.Chef.Register.contactInfo
import com.example.mobileassignmentloginpart.Chef.Register.descriptionInfo
import com.example.mobileassignmentloginpart.Chef.Register.reviewInfo
import com.example.mobileassignmentloginpart.Chef.ViewModel.chefRegisterViewModel
import com.example.mobileassignmentloginpart.View.HomeScreen
import com.example.mobileassignmentloginpart.View.LoginScreen
import com.example.mobileassignmentloginpart.View.ProfileScreen
import com.example.mobileassignmentloginpart.View.RegisterScreen
import com.example.mobileassignmentloginpart.navigation.Screen
import com.example.mobileassignmentloginpart.ui.theme.MobileAssignmentTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileAssignmentTheme {
                val navController = rememberNavController()
                val chefviewModel: chefRegisterViewModel = viewModel()

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

                    //Ivan part (admin site)
                    composable(Screen.AdminChefScreen.route) {
                        AdminApprovalScreen(navController)
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

                    composable(Screen.ChefHomeScreen.route) {
                        ChefHomeScreen(navController)
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

                        composable(Screen.Review.route) {
                            reviewInfo(
                                navController,
                                chefviewModel
                            )
                        }
                    }

                    composable(Screen.Profile.route){
                        ProfileScreen(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobileAssignmentTheme {
        Greeting("Android")
    }
}
