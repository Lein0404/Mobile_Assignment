package com.example.foodieheal.Admin

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foodieheal.Admin.ViewModel1.AdminApprovalViewModel
import com.example.foodieheal.R
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.navigation.Screen

sealed class AdminNavigationItem(val route: String, val titleRes: Int, val iconRes: Int) {
    object ChefApproval : AdminNavigationItem(
        route = Screen.AdminChefScreen.route,
        titleRes = R.string.label_chef_approval,
        iconRes = R.drawable.ic_outline_account_circle
    )
    object Ingredients : AdminNavigationItem(
        route = Screen.AdminIngredient.route,
        titleRes = R.string.label_ingredients,
        iconRes = R.drawable.ingredient
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    parentNavController: NavHostController,
    viewModel: AdminApprovalViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    initialTab: Int = 0
) {
    val navController = rememberNavController()

    val startDestination = if (initialTab == 1) Screen.AdminIngredient.createRoute(tab = 1) else Screen.AdminChefScreen.route

    val items = listOf(
        AdminNavigationItem.ChefApproval,
        AdminNavigationItem.Ingredients
    )

    LaunchedEffect(Unit) {
        viewModel.loadPendingChefs()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { 
                        // Match either exact route or route without arguments
                        it.route?.split("?")?.firstOrNull() == item.route.split("?")?.firstOrNull() 
                    } == true

                    val label = stringResource(item.titleRes)

                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = label,
                                modifier = if (item == AdminNavigationItem.Ingredients) Modifier.size(20.dp) else Modifier
                            )
                        },
                        label = { Text(label, fontSize = 10.sp) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
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

                // Logout Item
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = stringResource(R.string.logout)
                        )
                    },
                    label = { Text(stringResource(R.string.label_logout), fontSize = 10.sp) },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = { viewModel.onShowLogoutDialog(true) }
                )
            }
        }
    ) { padding ->
        if (viewModel.showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onShowLogoutDialog(false) },
                title = { 
                    Text(
                        text = stringResource(R.string.logout_confirm_title),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = { Text(stringResource(R.string.logout_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onShowLogoutDialog(false)
                            authViewModel.logout {
                                parentNavController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.logout),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onShowLogoutDialog(false) }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.AdminChefScreen.route) {
                AdminChefApprovalContent(viewModel, parentNavController)
            }
            composable(
                route = Screen.AdminIngredient.route,
                arguments = listOf(navArgument("tab") { defaultValue = -1; type = NavType.IntType })
            ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getInt("tab") ?: -1
                AdminIngredientsScreen(parentNavController, initialTab = tab)
            }
        }
    }
}
