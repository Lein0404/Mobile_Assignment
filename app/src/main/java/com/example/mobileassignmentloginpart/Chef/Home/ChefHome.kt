package com.example.mobileassignmentloginpart.Chef.Home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.mobileassignmentloginpart.Model.Chef
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ViewModel.AuthViewModel

sealed class Screen(val route : String, val title : String, @DrawableRes val iconRes: Int){
    object HomeScreen : Screen("ChefHome", "Home", R.drawable.ic_home)
    object ProfileScreen : Screen("Profile", "Profile", R.drawable.ic_outline_account_circle)
    object AppointsScreen : Screen("Appointments", "Appointments", R.drawable.ic_planner)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefHomeScreen(
    navController: NavController
) {
    // Fetch the parent NavBackStackEntry (or current destination entry)
    val currentEntry = navController.currentBackStackEntry
    val viewModel: AuthViewModel = if (currentEntry != null) {
        viewModel(currentEntry)
    } else {
        viewModel()
    }

    // Double validation of fetching chef data
    LaunchedEffect(Unit) {
        if (viewModel.currentChef == null) {
            viewModel.fetchChefData()
        }
    }

    var selectedScreen by remember { mutableStateOf<Screen>(Screen.HomeScreen) }
    val chef = viewModel.currentChef

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Chef Portal") }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
                val screens = listOf(
                    Screen.HomeScreen,
                    Screen.AppointsScreen,
                    Screen.ProfileScreen
                )
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(painter = painterResource(id = screen.iconRes), contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedScreen) {
                is Screen.HomeScreen -> ChefHomeScreenContent(
                    chef = chef,
                    onNavigateToAppointments = { selectedScreen = Screen.AppointsScreen }
                )
                is Screen.AppointsScreen -> AppointmentsScreen()
                // Pass the chef object directly into your ChefProfileScreen!
                is Screen.ProfileScreen -> ChefProfileScreen(chef = chef)
            }
        }
    }
}

@Composable
fun ChefHomeScreenContent(
    onNavigateToAppointments: () -> Unit,
    chef: Chef?,
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Header Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, Chef Gordon 👋",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Ready for today's bookings?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_account_circle),
                        contentDescription = "Profile Pic",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = "H",
                onValueChange = {},
                placeholder = { Text("Search dishes, events, clients...") },
                leadingIcon = { Icon(painter = painterResource(R.drawable.ic_vertical_more), contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Summary Card Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Today's Schedule",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3 Appointments",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Button(
                        onClick = onNavigateToAppointments,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("View All", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Upcoming Appointments Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next Appointment",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        item {
            AppointmentCard(
                clientName = "Sarah Jenkins",
                event = "Private Dinner Party (6 Guests)",
                time = "7:00 PM - 10:00 PM",
                location = "Downtown Penthouse"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}



@Preview(showBackground = true)
@Composable
fun ChefHomeScreenPreview(){
    ChefHomeScreen(navController = rememberNavController())
}
