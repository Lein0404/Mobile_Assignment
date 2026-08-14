package com.example.foodieheal.view

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.viewmodel.RecipeViewModel
import androidx.annotation.DrawableRes

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
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
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
                    .background(Color.White)
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
                            .background(Color(0xFFFFF9E1)), // Soft cream color
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
                    Text(text = recipe.recipeName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(painterResource(id = R.drawable.recipe_category), null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = recipe.recipeCourse, fontSize = 14.sp, color = Color.Gray)
                        
                        // 🌟 Show Last Updated Time on the right side with smaller text
                        if (!recipe.lastUpdated.isNullOrBlank()) {
                            Spacer(modifier = Modifier.weight(1f))
                            val displayTime = recipe.lastUpdated.split("T").firstOrNull() ?: ""
                            Text(
                                text = "Last updated: $displayTime", 
                                fontSize = 11.sp, // Made it smaller
                                color = Color.Gray
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
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(20.dp))

                    // 🌟 Description (Hidden if empty)
                    if (recipe.recipeDescription.isNotBlank()) {
                        Text(text = "Description", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        var isExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = recipe.recipeDescription,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
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
                                color = Color.Black,
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
                        Text(text = "Ingredients", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        IconButton(onClick = { /* Add to cart */ }) {
                            Icon(painterResource(id = R.drawable.ic_shopping_cart), null, modifier = Modifier.size(20.dp))
                        }
                    }
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ingredient.name, fontSize = 14.sp, color = Color.DarkGray)
                            Text(text = "${ingredient.quantity} ${ingredient.unit}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Steps
                    Text(text = "Recipe Steps", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    recipe.recipeStep.split("\n").forEachIndexed { index, step ->
                        if (step.isNotBlank()) {
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(text = "${index + 1}. ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(text = step.trim(), fontSize = 14.sp, color = Color.DarkGray)
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
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(id = icon), null, modifier = Modifier.size(20.dp), tint = Color.Black)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}
