package com.example.foodieheal.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.Hiring.Screen.HiringAppointment
import com.example.foodieheal.Hiring.Screen.HiringChefDetails
import com.example.foodieheal.Hiring.Screen.HiringScreen
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel

@Composable
fun MainScreen(
    parentNavController: NavHostController,
    activityViewModel: MealPlannerViewModel
) {
    val navController = rememberNavController()
    val hiringViewModel: HiringViewModel = viewModel()

    // 🌟 1. Sync inner NavController when ViewModel changes the target tab (e.g. via deep link)
    LaunchedEffect(activityViewModel.selectedTabRoute) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != activityViewModel.selectedTabRoute) {
            navController.navigate(activityViewModel.selectedTabRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val items = listOf(
        NavigationItem(Screen.Home.route, "Home", R.drawable.ic_home),
        NavigationItem(Screen.Recipes.route, "Recipes", R.drawable.ic_recipe),
        NavigationItem(Screen.Planner.route, "Planner", R.drawable.ic_planner),
        NavigationItem(Screen.Hiring.route, "Hiring", R.drawable.ic_hiring),
        NavigationItem(Screen.Profile.route, "Profile", R.drawable.ic_outline_account_circle)
    )

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(painterResource(id = item.icon), contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 10.sp) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            // 🌟 2. Notify ViewModel when tab is clicked manually
                            activityViewModel.onTabSelected(item.route)

                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Recipes.route) { RecipesScreen(parentNavController) }
            composable(Screen.Planner.route) {
                MealPlannerScreen(
                    viewModel = activityViewModel
                )
            }


            composable(Screen.Hiring.route) { HiringScreen(
                HiringViewModel = hiringViewModel,
                onChefClick = { chef ->
                    hiringViewModel.selectChef(chef)
                    navController.navigate("hiringChefDetails")
                }
            ) }

            composable("hiringChefDetails") {
                val authViewModel: AuthViewModel = viewModel()
                val selectedChef = hiringViewModel.selectedChef
                val currentUserId = authViewModel.currentUser?.id.orEmpty()
                val profileViewModel: BookmarkViewModel = viewModel()

                if (selectedChef != null) {
                    HiringChefDetails(
                        chef = selectedChef,
                        userId = currentUserId,
                        viewModel = profileViewModel,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onHireClick = {chef ->  navController.navigate("HiringAppointment")}
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            composable("HiringAppointment{chefId}") {
                HiringAppointment(
                    onBackClick = { navController.popBackStack() },
                    onAddAppointmentClick = {
                        // Handle adding a new appointment slot
                    }
                )
            }

            composable(Screen.Profile.route) { ProfileScreen(parentNavController) }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: Int)
