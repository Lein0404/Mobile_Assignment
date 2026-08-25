package com.example.foodieheal.Chef

import android.app.Activity
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.Chef.ViewModel.ChefPortalViewModel
import com.example.foodieheal.Chef.ViewModel.HomeUiState
import com.example.foodieheal.R
import com.example.foodieheal.model.Appointment
import com.example.foodieheal.User.viewModel.AuthViewModel


@Composable
fun ChefHomeScreen(
    navController: NavController,
    homeViewModel: ChefPortalViewModel,
    onNavigateToAppointments: () -> Unit = {},
    onCardClick: (Appointment) -> Unit = {}
) {
    val authViewModel: AuthViewModel = viewModel()
    val homeUiState by homeViewModel.homeUiState.collectAsState()

    // Refresh chef info and appointment data on initial launch
    LaunchedEffect(Unit) {
        if (authViewModel.currentChef == null) {
            authViewModel.fetchChefData()
        }
        homeViewModel.loadDashboardData()
    }

    val chef = authViewModel.currentChef
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.greeting_good_morning),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = chef?.name ?: stringResource(R.string.default_chef_name),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Main Surface Content
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // Responsive & Flexible Summary Schedule Banner
                item {
                    val appointmentCountText = when (val state = homeUiState) {
                        is HomeUiState.Success -> stringResource(R.string.banner_appointment_count_format, state.totalCount)
                        is HomeUiState.Loading -> stringResource(R.string.msg_loading)
                        is HomeUiState.Error -> stringResource(R.string.banner_zero_appointments)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.banner_title_schedule),
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = appointmentCountText,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = onNavigateToAppointments,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.widthIn(min = 90.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.view_all),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                // Next Appointment Title
                item {
                    Text(
                        text = stringResource(R.string.next_appointment),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Dynamic Next Appointment Display
                item {
                    when (val state = homeUiState) {
                        is HomeUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = primaryColor)
                            }
                        }

                        is HomeUiState.Error -> {
                            Text(
                                text = stringResource(R.string.error_loading_appointments),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        is HomeUiState.Success -> {
                            val nextAppointment = state.nextAppointment

                            if (nextAppointment != null) {
                                val chefUser = state.usersMap[nextAppointment.userId]
                                val userName = chefUser?.name ?: stringResource(R.string.unknown_client)

                                AppointmentCard(
                                    appointment = nextAppointment,
                                    userName = userName,
                                    onCardClick = { onCardClick(nextAppointment) }
                                )
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_upcoming_appointments),
                                        modifier = Modifier.padding(20.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

/*@Preview(showBackground = true)
@Composable
fun ChefHomeScreenPreview(){
    ChefHomeScreen(navController = rememberNavController())
}*/
