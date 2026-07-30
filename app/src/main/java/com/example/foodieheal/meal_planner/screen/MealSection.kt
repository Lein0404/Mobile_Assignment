package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.Recipe
import kotlin.collections.forEach

@Composable
fun RecipeCard(
    recipe: Recipe,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(end = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(color)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(start = 5.5.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            onClick = onClick,
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(recipe.recipeImage),
                    contentDescription = null,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = recipe.recipeName,
                        fontSize = 17.sp,
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
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Text("${recipe.calories} kcal", fontSize = 14.sp, maxLines = 1)

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            painter = painterResource(R.drawable.time),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Text("${recipe.time} mins", fontSize = 14.sp, maxLines = 1)
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
    modifier: Modifier = Modifier,
    title: String,
    recipes: List<Recipe>,
    onAddClick: () -> Unit = {},
    onDeleteClick: (Recipe) -> Unit = {},
) {
    val color: Color = when (title) {
        "Breakfast" -> Color(0XFFF4A260)
        "Lunch" -> Color(0XFF65B960)
        "Dinner" -> Color(0XFF4F6D7A)
        else -> Color(0XFFFCBA03)
    }
    val icon: Int = when(title){
        "Breakfast" -> R.drawable.breakfast
        "Lunch" -> R.drawable.lunch
        "Dinner" -> R.drawable.dinner
        else -> R.drawable.snack
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                MaterialTheme.colorScheme.tertiary,
                RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
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
                    painter = painterResource(R.drawable.ic_add_circle_outline),
                    contentDescription = "Add",
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recipes.isEmpty()) {
            Text(
                text = "Planning Something?",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 26.dp, bottom = 16.dp)
            )
        } else {
            recipes.forEach { recipeItem ->
                RecipeCard(
                    recipe = recipeItem,
                    onDeleteClick = { onDeleteClick(recipeItem) },
                    onClick = {/*TODO*/ },
                    color = color,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}