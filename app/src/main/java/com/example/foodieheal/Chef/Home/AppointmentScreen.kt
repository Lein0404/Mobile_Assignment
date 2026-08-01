package com.example.foodieheal.Chef

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppointmentCard(clientName: String, event: String, time: String, location: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = clientName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "🕒 $time", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = "📍 $location", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

// Placeholder for full Appointments view
@Composable
fun AppointmentsScreen() {
    val sampleList = listOf(
        Triple("Sarah Jenkins", "Dinner Party", "7:00 PM"),
        Triple("Mark Davis", "Cooking Workshop", "Tomorrow, 2:00 PM"),
        Triple("Elena Rostova", "Corporate Event", "Fri, 6:00 PM")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Appointments", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sampleList) { (name, event, time) ->
                AppointmentCard(clientName = name, event = event, time = time, location = "Client Address")
            }
        }
    }
}