package com.example.foodieheal.Recipe.View

import android.app.Activity
import android.widget.Toast
import es.dmoral.toasty.Toasty
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.User.Model.User
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.ui.components.ShareRecipeDialog
import kotlin.collections.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    parentNavController: NavController,
    viewModel: RecipeViewModel,
    authViewModel: AuthViewModel,
    isSelectionMode: Boolean = false,
    onSave: (List<String>) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val recipeAddedSingularMsg = stringResource(R.string.msg_recipes_added_singular)
    val recipeAddedPluralFormat = stringResource(R.string.msg_recipes_added_plural)

    val selectedTab = viewModel.activeTab
    // 🌟 3 Main Tabs to keep it from looking "scratchy"
    val tabs = listOf("Popular", "My Recipes", "Favorites")
    var showFollowingFeed by remember { mutableStateOf(true) } // Toggle between Following and Saved

    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("All") }
    val courses = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")

    val selectedRecipeIds = remember { mutableStateListOf<String>() }

    // Filter State
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterMaxTime by remember { mutableFloatStateOf(240f) }
    var filterMaxCalories by remember { mutableFloatStateOf(5000f) }
    var filterSkill by remember { mutableStateOf<String?>(null) }
    var filterBudget by remember { mutableStateOf<String?>(null) }
    var filterIngredients by remember { mutableStateOf(setOf<String>()) }

    // 🌟 FIX: Reset all filters when switching tabs
    LaunchedEffect(selectedTab) {
        searchQuery = ""
        selectedCourse = "All"
        filterMaxTime = 240f
        filterMaxCalories = 5000f
        filterSkill = null
        filterBudget = null
        filterIngredients = emptySet()
    }

    // 🌟 FIX: Use the short customId (U001) for all filtering to stay consistent
    val currentUserId = authViewModel.currentUser?.customId

    // 🌟 LOGIC: Filtering based on your instructions
    val currentDataList = when (selectedTab) {
        // Tab 0: Popular -> Show ALL recipes for testing sync (Include mine)
        // 🌟 Selection Mode Adjustment: Filter out my recipes from Popular tab as per RecipesSelectingScreen
        0 -> if (isSelectionMode) viewModel.recipeList.filter { it.author_id != currentUserId } else viewModel.recipeList
        // Tab 1: My Recipes -> Created by ME
        1 -> viewModel.myRecipes
        // Tab 2: Social -> Toggle between Following and Bookmarks
        2 -> if (showFollowingFeed) viewModel.followingRecipes else viewModel.bookmarkedRecipes
        else -> emptyList()
    }

    val isLoading = viewModel.isLoading

    val filteredRecipes by remember(searchQuery, selectedCourse, currentDataList, filterMaxTime, filterMaxCalories, filterSkill, filterBudget) {
        derivedStateOf {
            currentDataList.filter { recipe ->
                // 🌟 Visibility check for Popular tab
                val isVisible = when {
                    selectedTab != 0 -> true // Other tabs already represent filtered sets (My, Following, Bookmarks)
                    recipe.author_id == currentUserId -> true // My recipes in popular are visible to me
                    recipe.visibility == "public" -> true // Public is visible to everyone
                    else -> false // Private/Followers only not shown in Popular to non-owners
                }
                if (!isVisible) return@filter false

                val matchesSearch = recipe.recipeName.contains(searchQuery, ignoreCase = true) ||
                                    (recipe.authorName?.contains(searchQuery, ignoreCase = true) == true) ||
                                    (recipe.authorInfo?.name?.contains(searchQuery, ignoreCase = true) == true)

                val matchesCourse = selectedCourse == "All" || recipe.recipeCourse.equals(selectedCourse, ignoreCase = true)

                val matchesTime = recipe.time <= filterMaxTime.toInt()
                val matchesCalories = recipe.calories <= filterMaxCalories.toInt()
                val matchesSkill = filterSkill == null || recipe.cookingSkill.equals(filterSkill, ignoreCase = true)
                val matchesBudget = filterBudget == null || recipe.estimatedBudget == filterBudget
                val matchesIngredients = filterIngredients.isEmpty() || recipe.ingredients.any { it.name in filterIngredients }
                
                matchesSearch && matchesCourse && (filterMaxTime == 240f || matchesTime) && (filterMaxCalories == 5000f || matchesCalories) && matchesSkill && matchesBudget && matchesIngredients
            }
        }
    }

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.bookmarkMessage.collect { message ->
            // 🌟 Use currentSnackbarData?.dismiss() to show new messages instantly
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    var recipeToShare by remember { mutableStateOf<Recipe?>(null) }


    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    LaunchedEffect(selectedTab, currentUserId) {
        val cid = authViewModel.currentUser?.customId
        
        // 🌟 FIX: Always fetch Popular recipes even if not logged in yet
        if (selectedTab == 0 && viewModel.recipeList.isEmpty()) {
            viewModel.fetchAllRecipes()
        }

        if (cid != null) {
            // 🌟 FIX: Always refresh bookmarks if the owner has changed (Leon vs KK)
            // We check if the current IDs in memory actually belong to the current user
            viewModel.fetchBookmarkIds(cid)

            when (selectedTab) {
                1 -> {
                     val belongsToSomeoneElse = viewModel.myRecipes.any { it.author_id != cid }
                        if (viewModel.myRecipes.isEmpty() || belongsToSomeoneElse) {
                            viewModel.fetchMyRecipes(cid)
                        }
                }
                2 -> {
                    if (showFollowingFeed) {
                        viewModel.fetchFollowingRecipes(cid)
                    } else {
                        // 🌟 FIX: Use customId (cid) to match the Supabase table and local Room DB
                        viewModel.fetchBookmarkedRecipes(cid)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background, // 🌟 Themed Background
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 20.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionMode) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrowback),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp).clickable(onClick = { onBackClick() })
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Text(
                            text = if (isSelectionMode) {
                                if (selectedRecipeIds.isEmpty()) stringResource(R.string.title_select_recipes)
                                else stringResource(R.string.title_selected_count, selectedRecipeIds.size)
                            } else "Recipe",
                            color = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Text
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary, // 🌟 Themed Content
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.onPrimary // 🌟 Themed Indicator
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { viewModel.activeTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) // 🌟 Themed Tab
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                AnimatedVisibility(
                    visible = selectedRecipeIds.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    val count = selectedRecipeIds.size
                                    val message = if (count == 1) recipeAddedSingularMsg else {
                                        recipeAddedPluralFormat.format(count)
                                    }
                                    Toasty.custom(context, message, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                                    onSave(selectedRecipeIds)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_add_selected_recipes, selectedRecipeIds.size),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1 && !isSelectionMode) {
                FloatingActionButton(
                    onClick = { parentNavController.navigate(Screen.AddRecipe.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-32).dp) // 🌟 Pushed the button higher up by using a negative offset
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_add),
                        contentDescription = "Add",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search recipes/authors", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                )
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Course",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, start = 20.dp)
                )
            }
                Column {
                    // 🌟 Compact Followed/Bookmarks Toggle (Connected Pill style)
                    if (selectedTab == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val subTabModifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))

                            Box(
                                modifier = subTabModifier
                                    .background(if (showFollowingFeed) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { showFollowingFeed = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Followed",
                                    color = if (showFollowingFeed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

            item(span = { GridItemSpan(2) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    courses.forEach { course: String ->
                        val isSelected = selectedCourse == course
                        Surface(
                            onClick = { selectedCourse = course },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                        ) {
                            Text(
                                text = course,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            Box(
                                modifier = subTabModifier
                                    .background(if (!showFollowingFeed) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { showFollowingFeed = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Bookmarks",
                                    color = if (!showFollowingFeed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lazyItems(courses) { course: String ->
                            val isSelected = selectedCourse == course
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCourse = course },
                                label = {
                                    Text(
                                        text = course,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = selectedCourse,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp) // 🌟 Added padding
                )
            }
            // Vertical space cleanup

            if (isLoading && filteredRecipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (selectedTab == 2 && !showFollowingFeed && currentDataList.isEmpty()) {
                // 🌟 Saved Empty State
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp, start = 16.dp, end = 16.dp), // 🌟 Added padding
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bookmark),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.empty_no_bookmarked_recipes),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.empty_bookmarked_recipes_sub),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (filteredRecipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp), contentAlignment = Alignment.Center) {
                        val isFilterActive = filterMaxTime < 240f || filterMaxCalories < 5000f || filterSkill != null || filterBudget != null || filterIngredients.isNotEmpty()
                        Text(
                            text = if (searchQuery.isNotEmpty() || isFilterActive)
                                "No recipes match your filters."
                            else "No recipes found for '$selectedCourse'.",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // 🌟 Using gridItemsIndexed to alternate padding between columns
                gridItemsIndexed(filteredRecipes) { index, recipe ->
                    val isSelected = selectedRecipeIds.contains(recipe.recipe_id)
                    RecipeCardItem(
                        recipe = recipe,
                        // 🌟 Alternate padding: Left col gets start padding, Right col gets end padding
                        modifier = Modifier.padding(
                            start = if (index % 2 == 0) 16.dp else 0.dp,
                            end = if (index % 2 != 0) 16.dp else 0.dp
                        ),
                        // 🌟 FIX: Pass current user to ensure live name sync
                        currentUser = authViewModel.currentUser,
                        // 🌟 ONLY show menu for Tab 1 (My Recipes) and NOT in selection mode
                        showMenu = selectedTab == 1 && !isSelectionMode,
                        isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipe.recipe_id),
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onBookmarkClick = {
                            // 🌟 FIX: Revert to customId (U001) as per Supabase table screenshot
                            authViewModel.currentUser?.customId?.let { cid ->
                                recipe.recipe_id?.let { rid ->
                                    viewModel.toggleBookmark(cid, rid, recipe.recipeName)
                                }
                            }
                        },
                        onDeleteClick = { recipeToDelete = recipe },
                        onEditClick = {
                            recipe.recipe_id?.let { id ->
                                parentNavController.navigate(Screen.EditRecipe.createRoute(id))
                            }
                        },
                        onShareClick = { recipeToShare = it },
                        onAddClick = {
                            if (viewModel.isNetworkAvailable) {
                                recipe.recipe_id?.let { id ->
                                    parentNavController.navigate(Screen.AddRecipeToPlanner.createRoute(id))
                                }
                            } else {
                                viewModel.showOfflinePlannerMessage()
                            }
                        },
                        onClick = {
                            recipe.recipe_id?.let { id ->
                                if (isSelectionMode) {
                                    if (isSelected) selectedRecipeIds.remove(id)
                                    else selectedRecipeIds.add(id)
                                } else {
                                    parentNavController.navigate(Screen.RecipeDetails.createRoute(id))
                                }
                            }
                        }
                    )
                }
            }
            // 🌟 Added back the 80dp spacer to lift the Floating Action Button up
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // 🌟 Manually anchor the Snackbar at the bottom, perfectly into the "empty place" under the FAB
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }
    )
}

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            var ingredientSearchQuery by remember { mutableStateOf("") }
            val availableIngredients = remember(viewModel.availableIngredients) {
                viewModel.availableIngredients.mapNotNull { it.name }.distinct().sorted()
            }
            val filteredIngredientList = remember(ingredientSearchQuery, availableIngredients) {
                if (ingredientSearchQuery.isEmpty()) availableIngredients
                else availableIngredients.filter { it.contains(ingredientSearchQuery, ignoreCase = true) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Recipes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        filterMaxTime = 240f
                        filterMaxCalories = 5000f
                        filterSkill = null
                        filterBudget = null
                        filterIngredients = emptySet()
                        ingredientSearchQuery = ""
                    }) {
                        Text("Reset All", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Max Prep Time
                FilterSectionHeader(icon = R.drawable.ic_clock, title = "Max Prep Time")
                val timeDisplay = if (filterMaxTime >= 240f) "Any Time" else "${filterMaxTime.toInt()} mins"
                Text(timeDisplay, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(
                    value = filterMaxTime,
                    onValueChange = { filterMaxTime = it },
                    valueRange = 10f..240f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Max Calories
                FilterSectionHeader(icon = R.drawable.ic_fire, title = "Max Calories")
                val calDisplay = if (filterMaxCalories >= 5000f) "Any Calories" else "${filterMaxCalories.toInt()} kcal"
                Text(calDisplay, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(
                    value = filterMaxCalories,
                    onValueChange = { filterMaxCalories = it },
                    valueRange = 100f..5000f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Ingredients
                FilterSectionHeader(icon = R.drawable.ic_ingredient_list, title = "Ingredients")

                // 🌟 ENLARGED: Ingredient Search Bar
                OutlinedTextField(
                    value = ingredientSearchQuery,
                    onValueChange = { ingredientSearchQuery = it },
                    placeholder = { Text("Search ingredients...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(56.dp), // Increased height
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(24.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (availableIngredients.isNotEmpty()) {
                    // 🌟 FIXED: Use LazyRow to prevent lag when there are many ingredients
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        lazyItems(filteredIngredientList) { ingredient ->
                            FilterChip(
                                selected = ingredient in filterIngredients,
                                onClick = {
                                    filterIngredients = if (ingredient in filterIngredients) {
                                        filterIngredients - ingredient
                                    } else {
                                        filterIngredients + ingredient
                                    }
                                },
                                label = { Text(ingredient) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // 🌟 NEW: Show Selected Ingredients List
                    if (filterIngredients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selected (${filterIngredients.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterIngredients.forEach { selected ->
                                InputChip(
                                    selected = true,
                                    onClick = { filterIngredients = filterIngredients - selected },
                                    label = { Text(selected, fontSize = 11.sp) },
                                    trailingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.cancel),
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text("Loading ingredients...", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Cooking Skill
                FilterSectionHeader(icon = R.drawable.skill, title = "Cooking Skill")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Beginner", "Intermediate", "Master/Expert").forEach { skill ->
                        FilterChip(
                            selected = filterSkill == skill,
                            onClick = { filterSkill = if (filterSkill == skill) null else skill },
                            label = { Text(skill) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Budget
                FilterSectionHeader(icon = R.drawable.dollar_symbol, title = "Budget (RM)")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("0 - 20", "20 - 40", "40 - 60", "60 - 80", "80 - 100").forEach { budget ->
                        FilterChip(
                            selected = filterBudget == budget,
                            onClick = { filterBudget = if (filterBudget == budget) null else budget },
                            label = { Text(budget) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Apply Button
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 🌟 Share Dialog
    recipeToShare?.let { recipe ->
        ShareRecipeDialog(
            recipe = recipe,
            authorName = if (recipe.author_id == authViewModel.currentUser?.customId) authViewModel.currentUser?.name else recipe.authorName,
            onDismiss = { recipeToShare = null }
        )
    }

    if (recipeToDelete != null) {
        if (!viewModel.isNetworkAvailable) {
            AlertDialog(
                onDismissRequest = { recipeToDelete = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.wifi_off),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No Internet Connection")
                    }
                },
                text = { Text("You cannot delete recipes while offline. Please check your network settings.") },
                confirmButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { recipeToDelete = null },
                title = { Text("Delete Recipe") },
                text = { Text("Are you sure you want to delete '${recipeToDelete?.recipeName}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val rid = recipeToDelete?.recipe_id
                            // 🌟 Ensure we use the short customId for deletion
                            val uid = authViewModel.currentUser?.customId
                            if (rid != null && uid != null) {
                                viewModel.deleteRecipe(rid, uid)
                            }
                            recipeToDelete = null
                        }
                    ) {
                        Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun RecipeCardItem(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    currentUser: User? = null, // 🌟 Added for live name sync
    showMenu: Boolean = false,
    isBookmarked: Boolean = false,
    isSelected: Boolean = false, // 🌟 Added for Selection Mode
    isSelectionMode: Boolean = false, // 🌟 Added for Selection Mode
    onBookmarkClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // 🌟 Themed Card
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelectionMode && isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.height(310.dp).clickable { onClick() } // 🌟 Increased height for 2-line names + Author
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.surfaceVariant)) { // 🌟 Optimized image height
                if (!recipe.recipeImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = recipe.recipeImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 🌟 Artistic Placeholder
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)), // 🌟 Themed Background
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
                            modifier = Modifier.size(60.dp),
                            alpha = 0.3f,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiaryContainer) // 🌟 Themed Icon
                        )
                    }
                }

                // 🌟 Selection Check Badge
                if (isSelectionMode && isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = stringResource(R.string.desc_checkbox_checked),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                // 🌟 Bookmark icon (Hidden or shown depending on preference, plan says "don't decrease features")
                if (!isSelectionMode) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), // 🌟 Themed Surface
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp)
                    ) {
                        IconButton(onClick = onBookmarkClick) {
                            Image(
                                painter = painterResource(
                                    id = if (isBookmarked) R.drawable.bookmark_fill else R.drawable.bookmark
                                ),
                                contentDescription = "Bookmark",
                                modifier = Modifier.size(16.dp),
                                colorFilter = ColorFilter.tint(
                                    if (isBookmarked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ) // 🌟 Themed Icon matching Chef Bookmark logic
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), // 🌟 Themed Surface
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_recipe),
                            contentDescription = "Add to Planner",
                            modifier = Modifier.padding(6.dp).clickable { onAddClick() },
                            tint = MaterialTheme.colorScheme.onSurface // 🌟 Themed Icon
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.recipeName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, // 🌟 Themed Text
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis, // 🌟 Limit to 2 lines with ...
                        modifier = Modifier.weight(1f)
                    )
                    if (showMenu) {
                        Box {
                            IconButton(onClick = { expanded = true }, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_vertical_more),
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface // 🌟 Themed Icon
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Recipe") },
                                    onClick = {
                                        expanded = false
                                        onEditClick()
                                    },
                                    leadingIcon = { Icon(painterResource(id = R.drawable.ic_square_edit), null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Recipe", color = MaterialTheme.colorScheme.error) }, // 🌟 Themed Error
                                    onClick = {
                                        expanded = false
                                        onDeleteClick()
                                    },
                                    leadingIcon = { Icon(painterResource(id = R.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp)) // 🌟 Tighter spacing
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_fire),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp), // 🌟 Better sized icon
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${recipe.calories} kcal",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp), // 🌟 Better sized icon
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${recipe.time} mins",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // 🌟 Use the live name from currentUser if this is the user's own recipe
                    val authorToDisplay = if (recipe.author_id == currentUser?.customId && currentUser != null) {
                        currentUser.name
                    } else {
                        recipe.authorInfo?.name ?: recipe.authorName ?: "Chef"
                    }

                    if (!authorToDisplay.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.author),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = authorToDisplay,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 4.dp).weight(1f, fill = false)
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun FilterSectionHeader(@DrawableRes icon: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
