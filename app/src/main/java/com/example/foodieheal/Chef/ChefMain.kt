package com.example.foodieheal.Chef

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.core.content.ContextCompat
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Chef.ViewModel.AppointmentsUiState
import com.example.foodieheal.Chef.ViewModel.HomeUiState
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
import com.example.foodieheal.Chef.ViewModel.ChefPortalViewModel
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val chefNavController = rememberNavController()
    val homeViewModel: ChefPortalViewModel = viewModel()
    val pendingCount by homeViewModel.pendingAppointmentsCount.collectAsState()

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    // Ensure chef data is loaded, cleanup service is active, and notification permission requested
    LaunchedEffect(Unit) {
        if (authViewModel.currentChef == null) {
            authViewModel.fetchChefData()
        }
        try {
            val cleanupIntent = android.content.Intent(context, com.example.foodieheal.Chef.local.ChefCacheCleanupService::class.java)
            context.startService(cleanupIntent)
        } catch (e: Exception) {
            android.util.Log.e("ChefMainScreen", "Failed to start ChefCacheCleanupService", e)
        }

        // Request post notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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
                    val isAppointmentsTab = item is ChefNavigationItem.Appointments

                    NavigationBarItem(
                        icon = {
                            if (isAppointmentsTab && pendingCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(
                                                text = if (pendingCount > 99) "99+" else "$pendingCount",
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = item.iconRes),
                                        contentDescription = labelText
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = labelText
                                )
                            }
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

                    val clientUser = usersMap[appointment.userId]
                    val userName = clientUser?.name ?: stringResource(R.string.unknown_client)
                    val userProfilePicUrl = clientUser?.profilePicUrl

                    val isNetworkAvailable by homeViewModel.isNetworkAvailable.collectAsState()
                    val attachedRecipesMap by homeViewModel.attachedRecipes.collectAsState()
                    val isLoadingRecipes by homeViewModel.isLoadingRecipes.collectAsState()

                    val apptId = appointment.AppointmentID.orEmpty()
                    val attachedRecipes = attachedRecipesMap[apptId] ?: emptyList()

                    LaunchedEffect(apptId) {
                        if (apptId.isNotBlank() && !attachedRecipesMap.containsKey(apptId)) {
                            homeViewModel.loadRecipesForAppointment(apptId)
                        }
                    }

                    AppointmentDetailScreen(
                        appointment = appointment,
                        userName = userName,
                        userProfilePicUrl = userProfilePicUrl,
                        isNetworkAvailable = isNetworkAvailable,
                        attachedRecipes = attachedRecipes,
                        isLoadingRecipes = isLoadingRecipes,
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