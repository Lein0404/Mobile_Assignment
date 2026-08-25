package com.example.foodieheal.Chef

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.Chef.Home.AppointmentDetailScreen
import com.example.foodieheal.Chef.Home.ChefChangePasswordScreen
import com.example.foodieheal.Chef.Home.EditChefProfileScreen
import com.example.foodieheal.Chef.ViewModel.AppointmentsUiState
import com.example.foodieheal.Chef.ViewModel.ChefPortalViewModel
import com.example.foodieheal.Chef.ViewModel.HomeUiState
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.User.viewModel.AuthViewModel

sealed class ChefNavigationItem(val route: String, val titleRes: Int, val iconRes: Int) {
    object Home : ChefNavigationItem("chef_home", R.string.nav_home, R.drawable.ic_home)
    object Appointments : ChefNavigationItem("chef_appointments", R.string.nav_appointments, R.drawable.ic_planner)
    object Profile : ChefNavigationItem("chef_profile", R.string.nav_profile, R.drawable.ic_outline_account_circle)
}

@Composable
fun ChefMainScreen(
    parentNavController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val chefNavController = rememberNavController()
    val homeViewModel: ChefPortalViewModel = viewModel()

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
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets.navigationBars
            ) {
                val navBackStackEntry by chefNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val labelText = stringResource(id = item.titleRes)
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = labelText
                            )
                        },
                        label = { Text(labelText, fontSize = 10.sp) },
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

                composable(ChefNavigationItem.Home.route) {
                    ChefHomeScreen(
                        navController = parentNavController,
                        homeViewModel = homeViewModel,
                        onNavigateToAppointments = {
                            chefNavController.navigate(ChefNavigationItem.Appointments.route) {
                                popUpTo(chefNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onCardClick = { appointment ->

                            homeViewModel.selectAppointment(appointment)
                            chefNavController.navigate(Screen.AppointmentDetails.route)
                        }
                    )
                }

            composable(ChefNavigationItem.Appointments.route) {
                AppointmentsScreen(
                    viewModel = homeViewModel,
                    onCardClick = { appointment ->
                        homeViewModel.selectAppointment(appointment)
                        chefNavController.navigate(Screen.AppointmentDetails.route)
                    }
                )
            }

            composable(Screen.AppointmentDetails.route) {

                val appointment = homeViewModel.selectedAppointment

                if (appointment != null) {
                    val apptUiState by homeViewModel.appointmentsUiState.collectAsState()
                    val homeUiState by homeViewModel.homeUiState.collectAsState()

                    val usersMap = (apptUiState as? AppointmentsUiState.Success)?.usersMap
                        ?: (homeUiState as? HomeUiState.Success)?.usersMap
                        ?: emptyMap()

                    val userName = usersMap[appointment.userId]?.name ?: "Unknown Client"

                    val isNetworkAvailable by homeViewModel.isNetworkAvailable.collectAsState()

                    AppointmentDetailScreen(
                        appointment = appointment,
                        userName = userName,
                        isNetworkAvailable = isNetworkAvailable,
                        onBackClick = { chefNavController.popBackStack() },
                        onStatusChange = { newStatus, rejectionReason ->
                            val id = appointment.AppointmentID.orEmpty()
                            if (id.isNotBlank()) {
                                homeViewModel.updateAppointmentStatus(
                                    appointmentId = id,
                                    newStatus = newStatus,
                                    rejectionReason = rejectionReason
                                )
                                chefNavController.popBackStack()
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No appointment selected", color = Color.Gray)
                    }
                }
            }

            composable(ChefNavigationItem.Profile.route) {
                ChefProfileScreen(
                    navController = parentNavController, // Pass parent controller to allow complete logout back to root/login graph
                    chef = currentChef,
                    viewModel = authViewModel,
                    onEditClick = { chefNavController.navigate(Screen.ChefEditProfile.route) },
                    onChangePasswordClick = { chefNavController.navigate(Screen.ChefChangePassword.route) },
                    onLogoutSuccess = {
                        // Navigate using parent controller at the top level
                        parentNavController.navigate(Screen.Login.route) {
                            popUpTo(parentNavController.graph.id) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ChefEditProfile.route) {
                EditChefProfileScreen(
                    chef = authViewModel.currentChef,
                    onBack = { chefNavController.popBackStack() },
                    authViewModel = authViewModel,
                    navController = chefNavController
                )
            }

            composable(Screen.ChefChangePassword.route) {
                ChefChangePasswordScreen(
                    navController = chefNavController,
                    authViewModel = authViewModel
                )
            }
        }
    }
}