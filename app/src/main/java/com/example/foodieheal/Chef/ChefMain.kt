package com.example.foodieheal.Chef

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.Chef.Home.EditChefProfileScreen
import com.example.foodieheal.R
import com.example.foodieheal.ViewModel.AuthViewModel
import com.example.foodieheal.navigation.Screen

sealed class ChefNavigationItem(val route: String, val title: String, val iconRes: Int) {
    object Home : ChefNavigationItem("chef_home", "Home", R.drawable.ic_home)
    object Appointments : ChefNavigationItem("chef_appointments", "Appointments", R.drawable.ic_planner)
    object Profile : ChefNavigationItem("chef_profile", "Profile", R.drawable.ic_outline_account_circle)
}

@Composable
fun ChefMainScreen(
    parentNavController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val chefNavController = rememberNavController()

    // Ensure chef data is loaded upon entry
    LaunchedEffect(Unit) {
        if (authViewModel.currentChef == null) {
            authViewModel.fetchChefData()
        }
    }

    val currentChef = authViewModel.currentChef

    val items = listOf(
        ChefNavigationItem.Home,
        ChefNavigationItem.Appointments,
        ChefNavigationItem.Profile
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by chefNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.title
                            )
                        },
                        label = { Text(text = item.title, fontSize = 10.sp) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            chefNavController.navigate(item.route) {
                                popUpTo(chefNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = chefNavController,
            startDestination = ChefNavigationItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Chef Home Tab
            composable(ChefNavigationItem.Home.route) {
                ChefHomeScreen(
                    navController = parentNavController,
                    onNavigateToAppointments = {
                        chefNavController.navigate(ChefNavigationItem.Appointments.route) {
                            popUpTo(chefNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            // 2. Chef Appointments Tab
            composable(ChefNavigationItem.Appointments.route) {
                AppointmentsScreen()
            }

            // 3. Chef Profile Tab
            composable(ChefNavigationItem.Profile.route) {
                ChefProfileScreen(
                    navController = parentNavController, // Pass parent controller to allow complete logout back to root/login graph
                    chef = currentChef,
                    onEditClick = { chefNavController.navigate(Screen.ChefEditProfile.route) }
                )
            }

            composable("chefEditProfile") {
                EditChefProfileScreen(
                    chef = authViewModel.currentChef,
                    onBack = { chefNavController.popBackStack() },
                    authViewModel = authViewModel,
                    navController = chefNavController
                )
            }
        }
    }
}