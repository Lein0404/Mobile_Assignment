package com.example.foodieheal.meal_planner.screen

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.ui.theme.BreakfastColor
import com.example.foodieheal.ui.theme.DinnerColor
import com.example.foodieheal.ui.theme.LunchColor
import com.example.foodieheal.ui.theme.SnackColor

@Composable
fun TemplateMealSlotCard(
    mealType: MealType,
    recipes: List<Recipe>,
    onAddClick: () -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolve Title according to MealType Enum
    val title = when (mealType) {
        MealType.BREAKFAST -> stringResource(R.string.meal_title_breakfast)
        MealType.LUNCH -> stringResource(R.string.meal_title_lunch)
        MealType.DINNER -> stringResource(R.string.meal_title_dinner)
        MealType.SNACK -> stringResource(R.string.meal_title_snack)
    }

    // Resolve Icon Color
    val color: Color = when (mealType) {
        MealType.BREAKFAST -> BreakfastColor
        MealType.LUNCH -> LunchColor
        MealType.DINNER -> DinnerColor
        MealType.SNACK -> SnackColor
    }

    // Resolve Icon Drawable Resource
    val iconRes: Int = when (mealType) {
        MealType.BREAKFAST -> R.drawable.ic_breakfast
        MealType.LUNCH -> R.drawable.ic_lunch
        MealType.DINNER -> R.drawable.ic_dinner
        MealType.SNACK -> R.drawable.ic_snack
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon, Meal Title, and Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onTertiary
                )

                IconButton(onClick = onAddClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_circle_outline),
                        contentDescription = stringResource(R.string.desc_add_recipe),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Empty placeholder or recipe list
            if (recipes.isEmpty()) {
                Text(
                    text = stringResource(R.string.placeholder_empty_plan),
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)
                )
            } else {
                recipes.forEach { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onDeleteClick = { onDeleteRecipe(recipe) },
                        onClick = { onRecipeClick(recipe.recipe_id ?: "") },
                        color = color,
                        isSelectionMode = false,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}