package com.example.mobileassignmentloginpart.meal_planner

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileassignmentloginpart.R
import com.example.mobileassignmentloginpart.ui.theme.MobileAssignmentLoginPartTheme

@Composable
fun DateCard(
    modifier: Modifier = Modifier,
    day: String,
    date: String,
    selected: Boolean
) {
    val cardContainerColor:Color = when(selected) {
        true -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.background
    }
    val textColor:Color = when(selected){
        true-> MaterialTheme.colorScheme.onPrimary
        else-> MaterialTheme.colorScheme.onBackground
    }

    Card(
        modifier = modifier.size(height = 70.dp, width = 51.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Day text
            Text(
                text = day,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            // Date text (e.g., "14")
            Text(
                text = date,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun CalorieProgressBar(
    currentCalories: Int,
    maxCalories: Int,
    calorieTextColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (currentCalories.toFloat() / maxCalories.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row{
            Text(text = "Today's Calories: ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$currentCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = calorieTextColor
            )
            Text(text = " / $maxCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Horizontal Progress Line
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

data class MealItem(
    val name: String,
    val calories: Int,
    val cookingTime: Int,
    @DrawableRes val image: Int
)

@Composable
fun MealCard(
    meal: MealItem,
    onDeleteClick: () -> Unit,
    onClick:() -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(color)
    )
    {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(start = 6.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            onClick = onClick
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(meal.image),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = meal.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            painter = painterResource(R.drawable.fire),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text("${meal.calories} kcal", fontSize = 12.sp, maxLines = 1)

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            painter = painterResource(R.drawable.time),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text("${meal.cookingTime} mins", fontSize = 12.sp, maxLines = 1)
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Delete",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MealSection(
    modifier:Modifier = Modifier,
    title: String,
    meal: MealItem,
    onAddClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    val color:Color = when (title) {
        "Breakfast" -> Color(0XFFF4A260)
        "Lunch" -> Color(0XFF65B960)
        "Dinner" -> Color(0XFF4F6D7A)
        else -> Color.Gray
    }
    val icon:Int = when(title){
        "Breakfast" -> R.drawable.breakfast
        "Lunch" -> R.drawable.lunch
        "Dinner" -> R.drawable.dinner
        else -> R.drawable.breakfast
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                Color(0xFFE8E8E8),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onAddClick) {
                Icon(
                    painter = painterResource(R.drawable.add_circle),
                    contentDescription = "Add",
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MealCard(
            meal = meal,
            onDeleteClick = onDeleteClick,
            onClick = {/*TODO*/},
            color = color
        )
    }
}

@Composable
fun MealPlannerScreenPreview(modifier:Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Handle edit profile */ },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(70.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = "edit profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(45.dp)
                )
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Box(
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "13 - 19 Jul 2026",//TODO make this depends on the date
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    )
                    {
                        IconButton(
                            onClick = {/*TODO*/ }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Calendar Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {/*TODO*/ }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = "Calendar",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(
                            onClick = {/*TODO*/ }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = "Calendar Forward",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                //date card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateCard(
                        day = "Mon",
                        date = "14",
                        modifier = Modifier,
                        selected = false
                    )
                    DateCard(
                        day = "Mon",
                        date = "14",
                        modifier = Modifier,
                        selected = true
                    )
                    DateCard(
                        day = "Mon",
                        date = "14",
                        modifier = Modifier,
                        selected = false
                    )
                    DateCard(
                        day = "Mon",
                        date = "14",
                        modifier = Modifier,
                        selected = false
                    )
                    DateCard(
                        day = "Mon",
                        date = "14",
                        modifier = Modifier,
                        selected = false
                    )
                    DateCard(
                        day = "Mon",
                        date = "14",
                        modifier = Modifier,
                        selected = false
                    )
                    DateCard(
                        day = "Mon",
                        date = "14",
                        modifier = Modifier,
                        selected = false
                    )
                }
                Spacer(Modifier.height(12.dp))
                CalorieProgressBarPreview()
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                    elevation = CardDefaults.cardElevation(100.dp)
                ) {
                    MealSectionPreview()

                    val meal = MealItem(
                        name = "Greek Yogurt",
                        calories = 400,
                        cookingTime = 120,
                        image = R.drawable.food
                    )

                    MealSection(
                        title = "Lunch",
                        meal = meal
                    )

                    MealSection(
                        title = "Dinner",
                        meal = meal
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobileAssignmentLoginPartTheme {
        DateCard(Modifier,"Mon","14",false)
    }
}

@Preview(showBackground = true)
@Composable
fun CalorieProgressBarPreview() {
    MobileAssignmentLoginPartTheme {
        CalorieProgressBar(250,600,Color.Green,Modifier)
    }
}

@Preview(showBackground = true)
@Composable
fun MealSectionPreview() {

    val meal = MealItem(
        name = "Greek Yogurt",
        calories = 200,
        cookingTime = 20,
        image = R.drawable.food
    )

    MealSection(
        title = "Breakfast",
        meal = meal
    )
}

@Preview(showBackground = true)
@Composable
fun MealPlannerPreview() {
    MobileAssignmentLoginPartTheme{
        MealPlannerScreenPreview(Modifier)
    }
}