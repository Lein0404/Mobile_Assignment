package com.example.foodieheal.Admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.foodieheal.Admin.ViewModel1.AdminApprovalViewModel
import com.example.foodieheal.NavigationItem
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.User.viewModel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalScreen(
    parentNavController: NavHostController,
    viewModel: AdminApprovalViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    initialTab: Int = 0
) {
    val navController = rememberNavController()

    val startDestination = if (initialTab == 1) Screen.AdminIngredient.createRoute(tab = 1) else Screen.AdminChefScreen.route

    val items = listOf(
        NavigationItem(
            Screen.AdminChefScreen.route,
            stringResource(R.string.label_chef_approval),
            R.drawable.ic_outline_account_circle
        ),
        NavigationItem(
            Screen.AdminIngredient.route,
            stringResource(R.string.label_ingredients),
            R.drawable.ingredient
        )
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

                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = item.label,
                                modifier = if (item.route == Screen.AdminIngredient.route) Modifier.size(20.dp) else Modifier
                            )
                        },
                        label = { Text(item.label, fontSize = 10.sp) },
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

@Composable
fun AdminChefApprovalContent(
    viewModel: AdminApprovalViewModel,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Text(
                    text = stringResource(R.string.chef_approval),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                )
            }
        }

        if (viewModel.pendingChefs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_account_circle),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.no_pending_applications),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(viewModel.pendingChefs) { chef ->
                    ChefApprovalCard(
                        chef = chef,
                        onViewClick = {
                            navController.navigate("chefDetail/${chef.chefId}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChefApprovalCard(
    chef: Chef,
    onViewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chef.profilePictureUrl.isNullOrEmpty()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_account_circle),
                        contentDescription = stringResource(R.string.default_profile),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                } else {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chef.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    StatusChip(status = chef.status)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // Reused unified DetailRow
            DetailRow(
                painter = painterResource(R.drawable.mail),
                label = stringResource(R.string.label_email),
                value = chef.email
            )

            Spacer(modifier = Modifier.height(8.dp))

            DetailRow(
                painter = painterResource(R.drawable.telephone),
                label = stringResource(R.string.label_phone),
                value = chef.phoneNumber
            )

            Spacer(modifier = Modifier.height(8.dp))

            DetailRow(
                painter = painterResource(R.drawable.ic_clock),
                label = stringResource(R.string.label_experience),
                value = stringResource(R.string.experience_years_format, chef.experience)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onViewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_view),
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.review_application),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    painter: Painter,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusChip(status: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}