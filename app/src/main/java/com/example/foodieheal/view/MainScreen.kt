package com.example.foodieheal.view

import android.app.Application
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.Hiring.Screen.AddAppointmentFormScreen
import com.example.foodieheal.Hiring.Screen.AppointmentReviewScreen
import com.example.foodieheal.Hiring.Screen.HiringAppointment
import com.example.foodieheal.Hiring.Screen.HiringChefDetails
import com.example.foodieheal.Hiring.Screen.HiringScreen
import com.example.foodieheal.Hiring.ViewModel.BookmarkViewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.R
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.screen.MealPlannerScreen
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel
import io.github.jan.supabase.postgrest.postgrest

@Composable
fun MainScreen(parentNavController: NavHostController) {
    val navController = rememberNavController()

    val context = LocalContext.current
    val application = context.applicationContext as Application

    val hiringViewModel: HiringViewModel = viewModel()

    val items = listOf(
        NavigationItem(Screen.Home.route, "Home", R.drawable.ic_home),
        NavigationItem(Screen.Recipes.route, "Recipes", R.drawable.ic_recipe),
        NavigationItem(Screen.Planner.route, "Planner", R.drawable.ic_planner),
        NavigationItem(Screen.Hiring.route, "Hiring", R.drawable.ic_hiring),
        NavigationItem(Screen.Profile.route, "Profile", R.drawable.ic_outline_account_circle)
    )

    Scaffold(
        containerColor = Color(0xFFF8F8F8), // Match the app background
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
                            indicatorColor = Color.Transparent // Remove the selection oval
                        ),
                        onClick = {
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
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Recipes.route) { RecipesScreen(parentNavController) }
            composable(Screen.Planner.route) { PlannerScreen() }

            composable(Screen.Hiring.route) { HiringScreen(
                onChefClick = { chef ->
                    hiringViewModel.selectChef(chef)
                    navController.navigate("hiringChefDetails")
                }
            ) }

            composable(Screen.HiringChefDetails.route) {
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
                        onHireClick = {navController.navigate(Screen.HiringAppointment.route)}
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            composable(Screen.HiringAppointment.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.HiringChefDetails.route)
                }
                val chef = hiringViewModel.selectedChef

                if (chef != null) {
                    HiringAppointment(
                        chef = chef,
                        onBackClick = { navController.popBackStack() },
                        onAddAppointmentClick = { chosenDate ->
                            hiringViewModel.updateSelectedDate(chosenDate) // Update selected date (passing data)
                            navController.navigate(Screen.AddHiringAppointment.route)
                        }
                    )
                }
            }

            composable(Screen.AddHiringAppointment.route) {
                AddAppointmentFormScreen(
                    viewModel = hiringViewModel, // 🟢 PASS SHARED VIEWMODEL
                    onBackClick = { navController.popBackStack() },
                    onSuccessConfirm = {
                        navController.navigate(Screen.AppointmentReview.route)
                    }
                )
            }

            composable(Screen.AppointmentReview.route) {
                AppointmentReviewScreen(
                    viewModel = hiringViewModel,
                    onBackClick = { navController.popBackStack() },
                    onFinalConfirm = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    }
                )
            }

            composable(Screen.Planner.route) {
                val viewModel: MealPlannerViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            val repository = MealPlannerRepository(
                                postgrest = SupabaseClient.client.postgrest,
                                supabaseClient = SupabaseClient.client
                            )
                            return MealPlannerViewModel(application, repository) as T
                        }
                    }
                )

                MealPlannerScreen(
                    viewModel = viewModel,
                )
            }
            composable(Screen.Profile.route) { ProfileScreen(parentNavController) }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: Int)
