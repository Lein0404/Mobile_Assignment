package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R

@Composable
fun CalorieProgressBar(
    currentCalories: Int,
    maxCalories: Int,
    onNavigateToProfile: () -> Unit, // 🌟 Added navigation callback
    modifier: Modifier = Modifier
) {
    var showHelpDialog by remember { mutableStateOf(false) } // 🌟 State to toggle popup window

    val weightage: Float = if (maxCalories > 0) currentCalories.toFloat() / maxCalories.toFloat() else 0f
    val progress: Float = weightage.coerceIn(0f, 1f)

    // 🛠️ Fixed Overlap Bug logic to match your custom table structure accurately:
    val calorieTextColor: Color = when {
        weightage < 0.80f -> Color.Red
        weightage in 0.80f..0.949f -> Color(0XFFCC9900) // Yellow
        weightage in 0.95f..1.059f -> Color.Green
        weightage in 1.06f..1.20f -> Color(0XFFFA8F2A) // Orange
        else -> Color.Red
    }

    val reminderText: String = when {
        weightage < 0.80f -> "Under Intake"
        weightage in 0.80f..0.949f -> "Slightly Low"
        weightage in 0.95f..1.059f -> "Ideal Intake"
        weightage in 1.06f..1.20f -> "Slightly High"
        else -> "Excess Intake"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Today's Calories: ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$currentCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = calorieTextColor
            )
            Text(
                text = " / $maxCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.padding(2.dp))
            Icon(
                painter = painterResource(R.drawable.help),
                contentDescription = "Show calorie breakdown guide",
                modifier = Modifier
                    .size(18.dp)
                    .clickable { showHelpDialog = true } // 🌟 Show dialog window layout on tap
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = reminderText,
                style = MaterialTheme.typography.bodyMedium,
                color = calorieTextColor
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = calorieTextColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    // 🌟 CALORIE GUIDE POPUP DIALOG WINDOW IMPLEMENTATION
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false }, // Tapping outside bounds automatically dismisses
            title = {
                Text(
                    text = "How Calorie Target Works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Your recommended calorie status ranges are calculated relative to your target goals:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Guide Matrix List Items
                    CalorieGuideRow(percent = "< 80%", status = "Under Intake", color = Color.Red, description = "Calories too low. May not be meeting nutritional needs.")
                    CalorieGuideRow(percent = "80% - 94%", status = "Slightly Low", color = Color(0XFFCC9900), description = "Slightly below target. Acceptable but room to adjust.")
                    CalorieGuideRow(percent = "95% - 105%", status = "Ideal Intake", color = Color.Green, description = "Very close to recommended targets.")
                    CalorieGuideRow(percent = "106% - 120%", status = "Slightly High", color = Color(0XFFFA8F2A), description = "Slightly over standard recommendation limit.")
                    CalorieGuideRow(percent = "> 120%", status = "Excess Intake", color = Color.Red, description = "Significantly above target parameters.")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // BMI Reference Notice Footer with clickable action hook trigger
                    val annotatedString = buildAnnotatedString {
                        append("Your unique recommended daily calorie intake limits are configured dynamically based on your recorded BMI, height, and body weight targets. You can update these parameters anytime via the ")

                        pushStringAnnotation(tag = "PROFILE", annotation = "navigate")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Profile Page")
                        }
                        pop()
                        append(".")
                    }

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            showHelpDialog = false // Close modal immediately
                            onNavigateToProfile() // Route to target screen
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CalorieGuideRow(
    percent: String,
    status: String,
    color: Color,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "• $percent",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.7f)
        )
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}