package com.example.foodieheal.Hiring.Screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.Hiring.ViewModel.HiringViewModel
import com.example.foodieheal.R
import com.example.foodieheal.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentReviewScreen(
    viewModel: HiringViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onBackClick: () -> Unit,
    onFinalConfirm: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val chef = viewModel.selectedChef
    val selectedChef = viewModel.selectedChef
    val chefId = selectedChef?.let { it.chefId.ifEmpty { it.id } }.orEmpty()
    val chefName = selectedChef?.name ?: "Selected Chef"
    val appointmentTime = uiState.appointmentTime.ifBlank { "Not Selected" }
    val address = uiState.address.ifBlank { "Not Provided" }

    val location = listOf(uiState.postcode, uiState.state)
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { "Not Provided" }

    val servingSize = uiState.servingSize.ifBlank { "0" }
    val healthPref = uiState.healthPreference.ifBlank { "None" }
    val description = uiState.description.ifBlank { "None" }
    val hourlyRate = selectedChef?.Pricing ?: 0.0

    val currentUserId = authViewModel.currentUser?.id.orEmpty()

    // Extract the start time, end time and selected date
    val selectedDateString = viewModel.selectedDate.toString()
    val timeRange = uiState.appointmentTime.split(" - ")
    val startTime = timeRange.getOrNull(0).orEmpty()
    val endTime = timeRange.getOrNull(1).orEmpty()

    val totalPrice = viewModel.calculateTotalPrice()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Review Details",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReviewRow(label = "Chef Name", value = chefName)
                    ReviewRow(label = "Date", value = selectedDateString)
                    ReviewRow(label = "Time", value = appointmentTime)
                    ReviewRow(label = "Address", value = address)
                    ReviewRow(label = "Postcode & State", value = location)
                    ReviewRow(label = "Serving Size", value = "$servingSize Pax")
                    ReviewRow(label = "Health Preference", value = healthPref)
                    ReviewRow(label = "Note / Instructions", value = description)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(text = "Total Price", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "$${"%.2f".format(totalPrice)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Confirm & Book Button
            Button(
                onClick = {
                    viewModel.createAppointment(
                        userId = currentUserId,
                        chefId = chefId,
                        selectedDate = selectedDateString,
                        startTime = startTime,
                        endTime = endTime,
                        totalPrice = totalPrice,
                        onSuccess = onFinalConfirm,
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Confirm & Book",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
}

fun calculateHoursBetween(startTime: String, endTime: String): Double {
    if (startTime.isBlank() || endTime.isBlank()) return 1.0 // Default fallback

    val format = SimpleDateFormat("hh:mm a", Locale.US)

    return try {
        val start = format.parse(startTime.trim())
        val end = format.parse(endTime.trim())

        if (start != null && end != null) {
            val diffInMillis = end.time - start.time
            val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)

            val diffInHours = diffInMinutes.toDouble() / 60.0

            if (diffInHours > 0) diffInHours else 1.0
        } else {
            1.0
        }
    } catch (e: Exception) {
        1.0
    }
}