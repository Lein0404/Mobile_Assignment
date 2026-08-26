package com.example.foodieheal.meal_planner.screen

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.ui.theme.Green
import com.example.foodieheal.ui.theme.Orange
import com.example.foodieheal.ui.theme.Red
import com.example.foodieheal.ui.theme.Yellow

@Composable
fun CalorieProgressBar(
    currentCalories: Int,
    maxCalories: Int,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    val weightage: Float = if (maxCalories > 0) currentCalories.toFloat() / maxCalories.toFloat() else 0f
    val progress: Float = weightage.coerceIn(0f, 1f)

    val calorieTextColor: Color = when {
        weightage < 0.80f -> Red
        weightage in 0.80f..0.949f -> Yellow
        weightage in 0.95f..1.059f -> Green
        weightage in 1.06f..1.20f -> Orange
        else -> Red
    }

    val reminderText: String = when {
        weightage < 0.80f -> stringResource(R.string.status_under_intake)
        weightage in 0.80f..0.949f -> stringResource(R.string.status_slightly_low)
        weightage in 0.95f..1.059f -> stringResource(R.string.status_ideal_intake)
        weightage in 1.06f..1.20f -> stringResource(R.string.status_slightly_high)
        else -> stringResource(R.string.status_excess_intake)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.label_todays_calories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.format_kcal_current, currentCalories),
                style = MaterialTheme.typography.bodyMedium,
                color = calorieTextColor
            )
            Text(
                text = stringResource(R.string.format_kcal_max, maxCalories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.padding(2.dp))
            Icon(
                painter = painterResource(R.drawable.ic_help),
                contentDescription = stringResource(R.string.desc_show_calorie_guide),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { showHelpDialog = true },
                tint = Color.Gray
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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_title_calorie_works),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_description_ranges),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    CalorieGuideRow(
                        percent = stringResource(R.string.percent_under_80),
                        status = stringResource(R.string.status_under_intake),
                        color = Red,
                        description = stringResource(R.string.desc_under_intake)
                    )
                    CalorieGuideRow(
                        percent = stringResource(R.string.percent_80_94),
                        status = stringResource(R.string.status_slightly_low),
                        color = Yellow,
                        description = stringResource(R.string.desc_slightly_low)
                    )
                    CalorieGuideRow(
                        percent = stringResource(R.string.percent_95_105),
                        status = stringResource(R.string.status_ideal_intake),
                        color = Green,
                        description = stringResource(R.string.desc_ideal_intake)
                    )
                    CalorieGuideRow(
                        percent = stringResource(R.string.percent_106_120),
                        status = stringResource(R.string.status_slightly_high),
                        color = Orange,
                        description = stringResource(R.string.desc_slightly_high)
                    )
                    CalorieGuideRow(
                        percent = stringResource(R.string.percent_over_120),
                        status = stringResource(R.string.status_excess_intake),
                        color = Red,
                        description = stringResource(R.string.desc_excess_intake)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val partOne = stringResource(R.string.profile_notice_part_1)
                    val profileLink = stringResource(R.string.profile_notice_link)
                    val partTwo = stringResource(R.string.profile_notice_part_2)

                    val annotatedString = buildAnnotatedString {
                        append(partOne)
                        pushStringAnnotation(tag = "PROFILE", annotation = "navigate")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(profileLink)
                        }
                        pop()
                        append(partTwo)
                    }

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable {
                            showHelpDialog = false
                            onNavigateToProfile()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(
                        stringResource(R.string.btn_close),
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
            text = stringResource(R.string.format_bullet_percent, percent),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.7f),
            color = MaterialTheme.colorScheme.onSurface
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
 fun calculateSuggestedDailyCalories(user: User?): Int {
    if (user == null) return 2000 // Generic baseline value if profile isn't cached yet

    val weight = user.weight ?: 70.0 // kg
    val height = user.height ?: 170.0 // cm
    val age = user.age ?: 25
    val gender = user.gender?.lowercase() ?: "male"
    val bmi = user.bmi ?: 22.0

    // 1. Calculate Basal Metabolic Rate (BMR) using Mifflin-St Jeor Formula
    val bmr = if (gender == "male") {
        (10 * weight) + (6.25 * height) - (5 * age) + 5
    } else {
        (10 * weight) + (6.25 * height) - (5 * age) - 161
    }

    // 2. Add assumed baseline activity multiplier (1.2 = Sedentary lifestyle baseline)
    val tdee = bmr * 1.2

    // 3. Apply caloric adjustment modifications derived from the user's BMI tier category
    return when {
        bmi < 18.5 -> (tdee + 400).toInt()                 // Underweight: Surplus to gain
        bmi in 25.0..<30.0 -> (tdee - 300).toInt()  // Overweight: Safe light deficit
        bmi >= 30.0 -> (tdee - 500).toInt()                // Obese: Steady weight loss target
        else -> tdee.toInt()                               // Normal Range: Maintenance level
    }
}