package com.example.foodieheal.User.View

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.User.Model.User
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat

import coil.compose.AsyncImage
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Chef.model.Chef

import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.Recipe.View.RecipeCardItem
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.hiring.viewmodel.HiringViewModel
import androidx.compose.ui.res.stringResource

@Composable
fun  HomeScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    recipeViewModel: RecipeViewModel,
    chefViewModel: HiringViewModel = viewModel(),
    onChefClick: (Chef) -> Unit
) {
    val user = viewModel.currentUser
    
    // Stable Random Selection
    // We use a separate state to "latch" the random recipes once they are loaded.
    // This prevents them from reshuffling every time the database refreshes
    // or when you return from the Detail screen.
    var randomRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    // Sync with the master list once data is available
    LaunchedEffect(recipeViewModel.recipeList) {
        // If names are fetched later, update our local random selection automatically
        if (randomRecipes.isNotEmpty()) {
            val updated = randomRecipes.map { old ->
                recipeViewModel.recipeList.find { it.recipe_id == old.recipe_id } ?: old
            }
            // Only update state if something actually changed to avoid recomposition loops
            if (updated != randomRecipes) {
                randomRecipes = updated
            }
        }

        if (randomRecipes.isEmpty() && recipeViewModel.recipeList.isNotEmpty()) {
            // 🌟 Only show PUBLIC recipes from the master list on Home Screen
            randomRecipes = recipeViewModel.recipeList
                .filter { it.visibility.lowercase() == "public" }
                .shuffled()
                .take(5)
        }
    }

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    val chefList by chefViewModel.chefList.collectAsState()
    val isChefLoading by chefViewModel.isProcessing.collectAsState()
    val chefErrorMessage by chefViewModel.errorMessage.collectAsState()
    val isNetworkAvailable by chefViewModel.isNetworkAvailable.collectAsState()

    LaunchedEffect(Unit) {
        chefViewModel.fetchAllChefs()
        recipeViewModel.fetchAllRecipes()
    }

    LaunchedEffect(user?.customId) {
        user?.customId?.let { cid ->
            recipeViewModel.fetchBookmarkIds(cid)
            recipeViewModel.fetchBookmarkedRecipes(cid)
        }
    }

    // Auto-reload when network reconnects
    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable) {
            chefViewModel.fetchAllChefs(forceRefresh = false)
            recipeViewModel.fetchAllRecipes()
        }
    }

    LaunchedEffect(Unit) {
        recipeViewModel.bookmarkMessage.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    // Set Status Bar color to match the orange header
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.primary // Match orange header
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Top Header (Orange Background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Text(text = stringResource(id = viewModel.greetingResId), color = Color.White, fontSize = 14.sp)
                    Text(
                        text = user?.name ?: stringResource(id = R.string.default_username),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Themed Background Sheet
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp)
                ) {
                    // Chef Section
                    Text(
                        text = stringResource(id = R.string.home_section_chef),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    ChefListSection(
                        chefs = chefList,
                        isLoading = isChefLoading,
                        errorMessage = chefErrorMessage,
                        isNetworkAvailable = isNetworkAvailable,
                        onRetry = { chefViewModel.fetchAllChefs(forceRefresh = true) },
                        onChefClick = onChefClick
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Promo Banner
                    PromoBanner(navController)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Popular Recipes Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_section_popular_recipes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = {
                                navController.navigate(Screen.Recipes.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_btn_see_all),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RecipeListSection(
                        recipes = randomRecipes,
                        isLoading = recipeViewModel.isLoading,
                        currentUser = user,
                        bookmarkedIds = recipeViewModel.bookmarkedRecipeIds,
                        onRecipeClick = { recipe ->
                            recipe.recipe_id?.let { id ->
                                navController.navigate(Screen.RecipeDetails.createRoute(id))
                            }
                        },
                        onBookmarkClick = { recipe ->
                            user?.customId?.let { cid ->
                                recipe.recipe_id?.let { rid ->
                                    recipeViewModel.toggleBookmark(cid, rid, recipe.recipeName)
                                }
                            }
                        },
                        onAddClick = { recipe ->
                            if (recipeViewModel.isNetworkAvailable) {
                                recipe.recipe_id?.let { rid ->
                                    navController.navigate(Screen.AddRecipeToPlanner.createRoute(rid))
                                }
                            } else {
                                recipeViewModel.showOfflinePlannerMessage()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ChefListSection(
    chefs: List<Chef>,
    isLoading: Boolean,
    errorMessage: String?,
    isNetworkAvailable: Boolean = true,
    onRetry: () -> Unit = {},
    onChefClick: (Chef) -> Unit
) {
    when {
        // Data State (Prioritize displaying cached or fetched chefs immediately)
        chefs.isNotEmpty() -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = chefs,
                    key = { chef -> chef.chefId.ifEmpty { chef.id } }
                ) { chef ->
                    ChefCard(
                        chef = chef,
                        onClick = { onChefClick(chef) }
                    )
                }
            }
        }

        // Loading State (Only when list is empty)
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Error State with Retry Button
        errorMessage != null -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.tap_to_retry),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Empty State
        else -> {
            Text(
                text = if (!isNetworkAvailable) stringResource(id = R.string.no_cached_chefs_offline) else stringResource(id = R.string.no_chefs_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant, // 🌟 Themed Text
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
fun ChefCard(
    chef: Chef,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .width(165.dp)
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Profile Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!chef.profilePictureUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = chef.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Price Tag Badge
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "$${chef.Pricing?.toInt() ?: 0}/hr",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = chef.name?.ifEmpty { stringResource(id = R.string.home_section_chef) } ?: stringResource(id = R.string.home_section_chef),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Rating Display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = "Rating",
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${chef.averagerating ?: "N/A"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Experience Tag
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${chef.experience ?: "0"} yrs exp",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PromoBanner(navController: NavController) {
    Card(
        onClick = { navController.navigate(Screen.Ingredients.createRoute(1)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(165.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.home_banner_title), 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 17.sp, 
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.home_banner_desc), 
                fontSize = 13.sp, 
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f), 
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(14.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.home_banner_btn),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


@Composable
fun RecipeListSection(
    recipes: List<Recipe>,
    isLoading: Boolean,
    currentUser: User? = null,
    bookmarkedIds: Set<String>,
    onRecipeClick: (Recipe) -> Unit,
    onBookmarkClick: (Recipe) -> Unit,
    onAddClick: (Recipe) -> Unit
) {
    when {
        // Data State: Show recipes if we have them
        recipes.isNotEmpty() -> {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recipes) { recipe ->
                    RecipeCardItem(
                        recipe = recipe,
                        modifier = Modifier.width(165.dp),
                        currentUser = currentUser,
                        isBookmarked = bookmarkedIds.contains(recipe.recipe_id),
                        onBookmarkClick = { onBookmarkClick(recipe) },
                        onAddClick = { onAddClick(recipe) },
                        onClick = { onRecipeClick(recipe) }
                    )
                }
            }
        }

        // Loading State: Only when list is empty
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Empty State
        else -> {
            Text(
                text = stringResource(id = R.string.no_recipes_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                fontSize = 13.sp
            )
        }
    }
}


