package com.example.foodieheal.Chef.Home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.model.Appointment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    appointment: Appointment,
    userName: String,
    userPhone: String = "", // Need add on user model
    onBackClick: () -> Unit,
    onStatusChange: (newStatus: String, rejectionReason: String?) -> Unit = { _, _ -> }
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    var showAcceptDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    var rejectionReasonError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card: Status & Client Name
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (appointment.Status.lowercase()) {
                                "pending" -> Color(0xFFFFF3E0)
                                "confirmed" -> Color(0xFFE8F5E9)
                                "cancelled" -> Color(0xFFFFEBEE)
                                else -> Color(0xFFEEEEEE)
                            }
                        ) {
                            Text(
                                text = appointment.Status.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = when (appointment.Status.lowercase()) {
                                    "pending" -> Color(0xFFE65100)
                                    "confirmed" -> Color(0xFF2E7D32)
                                    "cancelled" -> Color(0xFFC62828)
                                    else -> Color.DarkGray
                                }
                            )
                        }
                    }

                    if (userPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Contact: $userPhone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Schedule & Time Section
            DetailSectionCard(title = "Date & Time") {
                DetailRow(
                    iconRes = R.drawable.ic_clock,
                    label = "Date",
                    value = appointment.Date
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(
                    iconRes = R.drawable.ic_clock,
                    label = "Time Slot",
                    value = "${appointment.Start_Time} - ${appointment.End_Time}"
                )
            }

            // Location & Navigation Section
            DetailSectionCard(title = "Event Location") {
                DetailRow(
                    iconRes = R.drawable.location,
                    label = "Address",
                    value = "${appointment.Address}, ${appointment.State}"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Open Maps Button
                OutlinedButton(
                    onClick = {
                        val mapUri =
                            Uri.parse("geo:0,0?q=${Uri.encode("${appointment.Address}, ${appointment.State}")}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                            setPackage("com.google.android.apps.maps") //Navigate to google maps
                        }
                        context.startActivity(mapIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open in Maps")
                }
            }

            // Dietary & Booking Details Section
            DetailSectionCard(title = "Booking Details") {
                DetailRow(
                    iconRes = R.drawable.ic_clock,
                    label = "Party Size",
                    value = "${appointment.Serving_Size} Pax"
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(
                    iconRes = R.drawable.ic_clock,
                    label = "Dietary Preference",
                    value = appointment.Health_Preference.ifBlank { "None Specified" }
                )

                if (appointment.Note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Special Notes:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appointment.Note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // Action Buttons for Chef (Accept/Cancel)
            if (appointment.Status.lowercase() == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            rejectionReason = ""
                            rejectionReasonError = false
                            showDeclineDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Text("Decline")
                    }

                    Button(
                        onClick = { showAcceptDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Accept Booking")
                    }
                }
            }
        }
    }

    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptDialog = false },
            title = {
                Text(
                    text = "Accept Booking?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to accept this appointment for $userName on ${appointment.Date}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAcceptDialog = false
                        Toast.makeText(
                            context,
                            "Booking accepted successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onStatusChange("Confirmed", null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Confirm Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = {
                Text(
                    text = "Decline Booking",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Please provide a reason for declining this appointment. This will be shared with $userName.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = {
                            rejectionReason = it
                            if (it.isNotBlank()) rejectionReasonError = false
                        },
                        placeholder = { Text("e.g. Fully booked, Location out of range...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        isError = rejectionReasonError,
                        supportingText = {
                            if (rejectionReasonError) {
                                Text(
                                    text = "Rejection reason cannot be empty",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isBlank()) {
                            rejectionReasonError = true
                        } else {
                            showDeclineDialog = false
                            Toast.makeText(context, "Booking declined.", Toast.LENGTH_SHORT).show()
                            onStatusChange("Rejected", rejectionReason.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Confirm Decline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

// Reusable Section Container
@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

// Reusable Information Row
@Composable
private fun DetailRow(
    iconRes: Int,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.Black
            )
        }
    }
}