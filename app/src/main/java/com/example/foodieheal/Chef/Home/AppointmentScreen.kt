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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.Chef.ViewModel.AppointmentsUiState
import com.example.foodieheal.Chef.ViewModel.ChefPortalViewModel
import com.example.foodieheal.R
import com.example.foodieheal.model.Appointment

@Composable
fun AppointmentsScreen(
    viewModel: ChefPortalViewModel = viewModel(),
    onCardClick: (Appointment) -> Unit = {}
) {
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val uiState by viewModel.appointmentsUiState.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf("All") }

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
            Column {
                Text(
                    text = stringResource(R.string.bookings_and_events),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(R.string.your_appointments),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main Surface Container
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            when (val state = uiState) {
                is AppointmentsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }

                is AppointmentsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.fetchAppointmentsForCurrentChef() }) {
                                Text(stringResource(R.string.btn_retry))
                            }
                        }
                    }
                }

                is AppointmentsUiState.Success -> {
                    val statusOptions = listOf("All", "Pending", "Confirmed", "Completed", "Cancelled")

                    // Apply status filter
                    val filteredAppointments = state.appointments.filter { appointment ->
                        selectedStatusFilter == "All" ||
                                appointment.Status.equals(selectedStatusFilter, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        // Filter Chips Row
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(statusOptions) { status ->
                                    val isSelected = selectedStatusFilter.equals(status, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedStatusFilter = status },
                                        label = { Text(text = status) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }

                        // Appointments List or Empty State
                        if (filteredAppointments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.appointments.isEmpty()) {
                                            stringResource(R.string.no_appointments_found)
                                        } else {
                                            stringResource(R.string.no_matching_appointments)
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            items(filteredAppointments) { appointment ->
                                val clientUser = state.usersMap[appointment.userId]
                                val userName = clientUser?.name ?: stringResource(R.string.unknown_client)

                                AppointmentCard(
                                    appointment = appointment,
                                    userName = userName,
                                    onCardClick = { onCardClick(appointment) }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    userName: String,
    onCardClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Client Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status Badge
                val (badgeContainerColor, badgeContentColor) = when (appointment.Status.lowercase()) {
                    "completed" -> Color(0xFFE3F2FD) to Color(0xFF1565C0) // Soft Blue
                    "confirmed" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // Soft Green
                    "cancelled" -> Color(0xFFFFEBEE) to Color(0xFFC62828) // Soft Red
                    "rejected"  -> Color(0xFFFBE9E7) to Color(0xFFD84315) // Soft Deep Orange / Rust Red
                    "unpaid"    -> Color(0xFFFFF8E1) to Color(0xFFF57F17) // Soft Amber / Yellow-Orange
                    "pending"   -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Soft Orange
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeContainerColor
                ) {
                    Text(
                        text = appointment.Status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeContentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Serving Size & Diet Preference
            val dietText = appointment.Health_Preference.ifBlank { stringResource(R.string.none) }
            Text(
                text = stringResource(
                    R.string.appointment_serving_and_diet_format,
                    appointment.Serving_Size,
                    dietText
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Optional Note
            if (appointment.Note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.appointment_note_format, appointment.Note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Date & Time Stacked Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clock),
                    contentDescription = stringResource(R.string.time),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.appointment_datetime_format,
                        appointment.Date,
                        appointment.Start_Time,
                        appointment.End_Time
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location Stacked Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.location),
                    contentDescription = stringResource(R.string.location),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.appointment_address_format,
                        appointment.Address,
                        appointment.State
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class AppointmentItem(
    val clientName: String,
    val event: String,
    val time: String,
    val location: String
)