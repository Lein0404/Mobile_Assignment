package com.example.foodieheal.Admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.foodieheal.Admin.ViewModel1.AdminApprovalViewModel
import com.example.foodieheal.NavigationItem
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalScreen(
    parentNavController: NavHostController,
    viewModel: AdminApprovalViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    // Using the passed-in authViewModel instead of creating a local one
    // to ensure we share login state with MainActivity

    val items = listOf(
        NavigationItem(Screen.AdminChefScreen.route, "Chef Approval", R.drawable.ic_outline_account_circle),
        NavigationItem(Screen.AdminIngredient.route, "Ingredients", R.drawable.ingredient)
    )

    LaunchedEffect(Unit) {
        viewModel.loadPendingChefs()
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FC),
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Text(
                        text = if (currentRoute == Screen.AdminIngredient.route) "Ingredient Requests" else "Chef Approval",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = item.label,
                                modifier = if (item.label == "Ingredients") Modifier.size(20.dp) else Modifier
                            )
                        },
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
                            contentDescription = "Logout"
                        )
                    },
                    label = { Text("Logout", fontSize = 10.sp) },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    onClick = {
                        // 🌟 FIX: Just call logout. MainActivity will swap screens automatically.
                        authViewModel.logout { }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.AdminChefScreen.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.AdminChefScreen.route) {
                AdminChefApprovalContent(viewModel, parentNavController)
            }
            composable(Screen.AdminIngredient.route) {
                AdminIngredientsScreen(parentNavController)
            }
        }
    }
}

@Composable
fun AdminChefApprovalContent(
    viewModel: AdminApprovalViewModel,
    navController: NavController
) {
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
                    tint = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    "No pending applications",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
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
                        navController.navigate(
                            "chefDetail/${chef.chefId}"
                        )
                    }
                )
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
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),

                    contentAlignment = Alignment.Center
                ) {
                    if (chef.profilePictureUrl.isNullOrEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_account_circle),
                            contentDescription = "Default Profile",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(75.dp)
                        )
                    } else {
                        AsyncImage(
                            model = chef.profilePictureUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(75.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = chef.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    StatusChip(
                        status = chef.status
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            ChefInfoRow(
                painter = painterResource(R.drawable.mail),
                text = chef.email
            )

            ChefInfoRow(
                painter = painterResource(R.drawable.telephone),
                text = chef.phoneNumber
            )

            ChefInfoRow(
                painter = painterResource(R.drawable.ic_clock),
                text = "${chef.experience} years experience"
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

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

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    "Review Application"
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    status: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFFFF3E0)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            color = Color(0xFFFF9800),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ChefInfoRow(
    painter: Painter,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            vertical = 6.dp
        )
    ) {

        Icon(
            painter = painter,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = text,
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}