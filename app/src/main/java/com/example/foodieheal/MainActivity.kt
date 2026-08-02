package com.example.foodieheal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.Admin.AdminApprovalScreen
import com.example.foodieheal.Admin.ChefDetailScreen
import com.example.foodieheal.Chef.ChefHomeScreen
import com.example.foodieheal.Chef.ChefMainScreen
import com.example.foodieheal.Chef.ChefProfileScreen
import com.example.foodieheal.Chef.Register.ChefPictureScreen
import com.example.foodieheal.Chef.Register.ChefWelcomeScreen
import com.example.foodieheal.Chef.Register.addressInfo
import com.example.foodieheal.Chef.Register.basicInfo
import com.example.foodieheal.Chef.Register.contactInfo
import com.example.foodieheal.Chef.Register.descriptionInfo
import com.example.foodieheal.Chef.Register.reviewInfo
import com.example.foodieheal.Chef.ViewModel.chefRegisterViewModel
import com.example.foodieheal.view.LoginScreen
import com.example.foodieheal.view.RegisterScreen
import com.example.foodieheal.view.MainScreen
import com.example.foodieheal.view.AddRecipeScreen
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.ui.theme.FoodieHealTheme
import com.example.mobileassignmentloginpart.Model.Chef


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodieHealTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val chefviewModel: chefRegisterViewModel = viewModel()

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

                    //Ivan part (admin site)
                    composable(Screen.AdminChefScreen.route) {
                        AdminApprovalScreen(navController)
                    }

                    composable(Screen.ChefMain.route) {
                        ChefMainScreen(navController)
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

                        composable(Screen.ChefPicture.route) {
                            ChefPictureScreen(
                                navController,
                                chefviewModel
                            )
                        }

                        composable(Screen.Review.route) {
                            reviewInfo(
                                navController,
                                chefviewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
