package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CalorieProgressBar(
    currentCalories: Int,
    maxCalories: Int,
    modifier: Modifier = Modifier
) {
    val weightage:Float = (currentCalories.toFloat() / maxCalories.toFloat())
    val progress:Float = weightage.coerceIn(0f, 1f)
    val calorieTextColor:Color = when {
        (weightage<0.8) -> Color.Red
        (weightage in 0.8 .. 0.94) -> Color.Yellow
        (weightage in 0.95 .. 1.05) -> Color.Green
        (weightage in 1.06 .. 1.20) -> Color(0XFFFA8F2A)//Orange color
        else -> Color.Red
    }
    val reminderText:String = when {
        (weightage<0.8) -> "Under Intake"
        (weightage in 0.8 .. 0.94) -> "Slightly Low"
        (weightage in 0.95 .. 1.05) -> "Ideal Intake"
        (weightage in 1.06 .. 1.20) -> "Slightly High"
        else->"Excess Intake"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
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
}