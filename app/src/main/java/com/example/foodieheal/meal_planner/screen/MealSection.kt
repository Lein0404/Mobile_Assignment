package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.ui.theme.BreakfastColor
import com.example.foodieheal.ui.theme.CaloriesColor
import com.example.foodieheal.ui.theme.DinnerColor
import com.example.foodieheal.ui.theme.LunchColor
import com.example.foodieheal.ui.theme.SnackColor

@Composable
fun RecipeCard(
    recipe: Recipe,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
    color: Color,
    isSelectionMode: Boolean = false,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(end = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(color),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(start = 5.5.dp)
                .clickable(enabled = !isSelectionMode, onClick = {onClick()}),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!recipe.recipeImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = recipe.recipeImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        alpha = 0.3f
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = recipe.recipeName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fire),
                            contentDescription = stringResource(R.string.calories),
                            modifier = Modifier.size(18.dp),
                            tint = CaloriesColor
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Text(
                            text = stringResource(R.string.format_recipe_calories, recipe.calories),
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_time),
                            contentDescription = stringResource(R.string.minutes),
                            modifier = Modifier.size(18.dp),
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Text(
                            text = stringResource(R.string.format_recipe_duration, recipe.time),
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!isSelectionMode) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.desc_remove_recipe)
                        )
                    }
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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},//empty lambda bcz in normal meal planner screen no need use, only in addRecipeToMealPlannerScreen
    onAddClick: () -> Unit ={} ,//only in normal meal planner
    onDeleteClick: (Recipe) -> Unit = {},//only in normal meal planner
    onRecipeDetails:(String)-> Unit = {}//only in normal meal planner
) {
    val color: Color = when (title) {
        stringResource(R.string.meal_title_breakfast) -> BreakfastColor
        stringResource(R.string.meal_title_lunch) -> LunchColor
        stringResource(R.string.meal_title_dinner) -> DinnerColor
        else -> SnackColor
    }
    val icon: Int = when (title) {
        stringResource(R.string.meal_title_breakfast) -> R.drawable.ic_breakfast
        stringResource(R.string.meal_title_lunch) -> R.drawable.ic_lunch
        stringResource(R.string.meal_title_dinner) -> R.drawable.ic_dinner
        else -> R.drawable.ic_snack
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                MaterialTheme.colorScheme.tertiary,
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = isSelectionMode) { onSelectionChange(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onTertiary
            )

            if (!isSelectionMode) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_circle_outline),
                        contentDescription = stringResource(R.string.desc_add_recipe),
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            } else {
                RoundCheckbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recipes.isEmpty()) {
            Text(
                text = stringResource(R.string.placeholder_empty_plan),
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 26.dp, bottom = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                recipes.forEach { recipeItem ->
                    RecipeCard(
                        recipe = recipeItem,
                        onDeleteClick = {
                            if (!isSelectionMode) {
                                onDeleteClick(recipeItem)
                            }
                        },
                        onClick = {onRecipeDetails(recipeItem.recipe_id?:"")},
                        color = color,
                        isSelectionMode = isSelectionMode,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun RoundCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.desc_checkbox_checked),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}