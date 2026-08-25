package com.example.foodieheal.Recipe.View

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ColorFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsScreen(
    navController: NavController,
    recipeId: String,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel
) {
    // 1. Fetch data if needed (Smart fetching)
    LaunchedEffect(recipeId) {
        viewModel.fetchRecipeById(recipeId)
    }

    val recipe = viewModel.selectedRecipe
    val user = authViewModel.currentUser
    val isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipeId)
    val isMyRecipe = recipe?.author_id == user?.id

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.bookmarkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("View Recipe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = Color.White)
                    }
                },
                actions = {
                    // 🌟 Bookmark is now enabled for all recipes, including your own
                    IconButton(onClick = {
                        user?.customId?.let { cid ->
                            recipe?.let { r -> viewModel.toggleBookmark(cid, r.recipe_id ?: "", r.recipeName) }
                        }
                    }) {
                        // 🌟 FIX: Use Image with specific size and tint for PNG icons to keep them sharp
                        Image(
                            painter = painterResource(id = if (isBookmarked) R.drawable.bookmark_fill else R.drawable.bookmark),
                            contentDescription = "Bookmark",
                            modifier = Modifier.size(22.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                    IconButton(onClick = { /* More menu */ }) {
                        Icon(painterResource(id = R.drawable.ic_vertical_more), "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (recipe == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface) // 🌟 Themed Background
            ) {
                // Recipe Image or Artistic Placeholder
                if (!recipe.recipeImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = recipe.recipeImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 🌟 Option 3: Artistic "Food Sketch" Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), // 🌟 Themed Background
                        contentAlignment = Alignment.Center
                    ) {
                        val iconRes = when (recipe.recipeCourse.lowercase()) {
                            "breakfast" -> R.drawable.ic_breakfast
                            "lunch" -> R.drawable.ic_lunch
                            "dinner" -> R.drawable.ic_dinner
                            else -> R.drawable.ic_snack
                        }
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            alpha = 0.4f // Light "sketch" effect
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Title and Course + Last Updated
                    Text(text = recipe.recipeName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(painterResource(id = R.drawable.recipe_category), null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = recipe.recipeCourse, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        // 🌟 Show Last Updated Time on the same line, right next to the course
                        if (!recipe.lastUpdated.isNullOrBlank()) {
                            val displayTime = recipe.lastUpdated.split("T").firstOrNull() ?: ""
                            Text(
                                text = "  •  Updated: $displayTime", 
                                fontSize = 13.sp, // Sized closer to course
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats Grid
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(R.drawable.ic_fire, "${recipe.calories} kcal", Modifier.weight(1f))
                        StatItem(R.drawable.ic_clock, "${recipe.time} minutes", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(R.drawable.skill, recipe.cookingSkill, Modifier.weight(1f)) 
                        StatItem(R.drawable.dollar_symbol, "RM ${recipe.estimatedBudget}", Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // 🌟 Themed Divider
                    Spacer(modifier = Modifier.height(16.dp))

                    // 🌟 Author Section
                    val author = viewModel.recipeAuthor
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (author != null && !author.profilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = author.profilePicUrl,
                                contentDescription = "Author Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_outline_account_circle),
                                contentDescription = "Default Author Profile",
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = "Recipe by",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = author?.name ?: "Unknown Chef",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    // 🌟 Description (Hidden if empty)
                    if (recipe.recipeDescription.isNotBlank()) {
                        Text(text = "Description", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        var isExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = recipe.recipeDescription,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateContentSize()
                                .clickable { isExpanded = !isExpanded },
                            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isExpanded && recipe.recipeDescription.length > 100) {
                            Text(
                                text = "... See More",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, // 🌟 Use primary for action text
                                modifier = Modifier.clickable { isExpanded = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Ingredients
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Ingredients", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { /* Add to cart */ }) {
                            Icon(painterResource(id = R.drawable.ic_shopping_cart), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ingredient.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${ingredient.quantity} ${ingredient.unit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Steps
                    Text(text = "Recipe Steps", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    recipe.recipeStep.split("\n").forEachIndexed { index, step ->
                        if (step.isNotBlank()) {
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(text = "${index + 1}. ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = step.trim(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun StatItem(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier, 
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(id = icon), 
            null, 
            modifier = Modifier.size(18.dp), 
            tint = MaterialTheme.colorScheme.primary // 🌟 Consistent Icon Color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Medium, 
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
