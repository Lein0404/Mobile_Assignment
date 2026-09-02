package com.example.foodieheal.Recipe.View

import android.app.Activity
import android.widget.Toast
import es.dmoral.toasty.Toasty
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
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
    // 3 Main Tabs to keep it from looking "scratchy"
    val tabs = listOf(
        stringResource(R.string.tab_popular_recipes),
        stringResource(R.string.tab_my_recipes_title),
        stringResource(R.string.tab_favorites_title)
    )
    var showFollowingFeed by remember { mutableStateOf(true) } // Toggle between Following and Saved

    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("All") }
    val courseKeys = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")
    val courses = courseKeys.map { key ->
        when(key) {
            "All" -> stringResource(R.string.tab_all)
            "Breakfast" -> stringResource(R.string.breakfast)
            "Lunch" -> stringResource(R.string.lunch)
            "Dinner" -> stringResource(R.string.dinner)
            "Snack" -> stringResource(R.string.snack)
            else -> key
        }
    }

    val selectedRecipeIds = remember { mutableStateListOf<String>() }

    // Filter State
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterMaxTime by remember { mutableFloatStateOf(240f) }
    var filterMaxCalories by remember { mutableFloatStateOf(5000f) }
    var filterSkill by remember { mutableStateOf<String?>(null) }
    var filterBudget by remember { mutableStateOf<String?>(null) }
    var filterIngredients by remember { mutableStateOf(setOf<String>()) }

    // Reset all filters when switching tabs
    LaunchedEffect(selectedTab) {
        searchQuery = ""
        selectedCourse = "All"
        filterMaxTime = 240f
        filterMaxCalories = 5000f
        filterSkill = null
        filterBudget = null
        filterIngredients = emptySet()
    }

    // Use the short customId (U001) for all filtering to stay consistent
    val currentUserId = authViewModel.currentUser?.customId

    // Filtering based on your instructions
    val currentDataList = when (selectedTab) {
        // Tab 0: Popular -> Show ALL recipes for testing sync (Include mine)
        // Selection Mode Adjustment: Filter out my recipes from Popular tab as per RecipesSelectingScreen
        0 -> if (isSelectionMode) viewModel.recipeList.filter { it.author_id != currentUserId } else viewModel.recipeList
        // Tab 1: My Recipes -> Created by ME
        1 -> viewModel.myRecipes
        // Tab 2: Social -> Toggle between Following and Bookmarks
        2 -> if (showFollowingFeed) viewModel.followingRecipes else viewModel.bookmarkedRecipes
        else -> emptyList()
    }

    val isLoading = viewModel.isLoading

    val filteredRecipes by remember(searchQuery, selectedCourse, currentDataList, filterMaxTime, filterMaxCalories, filterSkill, filterBudget, viewModel.followedUserIds) {
        derivedStateOf {
            currentDataList.filter { recipe ->
                // Privacy & Visibility Logic
                val isVisible = when {
                    recipe.author_id == currentUserId -> true // My recipes are always visible to me
                    recipe.visibility == "public" -> true // Public recipes are visible to everyone
                    recipe.visibility == "followers" -> viewModel.followedUserIds.contains(recipe.author_id) // Visible only if following
                    else -> false // Private recipes (or any other status) are hidden from others
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
            // Use currentSnackbarData?.dismiss() to show new messages instantly
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    var recipeToShare by remember { mutableStateOf<Recipe?>(null) }


    SideEffect {
        val window = (context as? Activity)?.window
        window?.let {
            it.statusBarColor = primaryColor.toArgb()
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
        }
    }

    LaunchedEffect(selectedTab, currentUserId) {
        val cid = authViewModel.currentUser?.customId
        
        // Tab 0: Popular recipes -> Always fetch in background to sync newly added recipes from other accounts without flashing
        if (selectedTab == 0) {
            viewModel.fetchAllRecipes()
        }

        if (cid != null) {
            // Always refresh bookmarks if the owner has changed
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
                    // Always fetch following list to keep 'followedUserIds' updated for privacy checks
                    viewModel.fetchFollowingRecipes(cid)
                    
                    if (!showFollowingFeed) {
                        // Use customId (cid) to match the Supabase table and local Room DB
                        viewModel.fetchBookmarkedRecipes(cid)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
                            } else stringResource(R.string.title_recipe_main),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(selectedTab),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
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
                                        color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                        .offset(y = (-32).dp) // Pushed the button higher up by using a negative offset
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.placeholder_search_recipes_authors), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        onClick = { showFilterSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val activeFilterCount = (if (filterMaxTime < 240f) 1 else 0) +
                                    (if (filterMaxCalories < 5000f) 1 else 0) +
                                    (if (filterSkill != null) 1 else 0) +
                                    (if (filterBudget != null) 1 else 0) +
                                    (if (filterIngredients.isNotEmpty()) 1 else 0)

                            BadgedBox(
                                badge = {
                                    if (activeFilterCount > 0) {
                                        Badge(containerColor = primaryColor, contentColor = Color.White) {
                                            Text(activeFilterCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.filter),
                                    contentDescription = "Filter",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (activeFilterCount > 0) primaryColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }


            if (selectedTab == 2) {
                item(span = { GridItemSpan(2) }) {
                    // Compact Followed/Bookmarks Toggle (Connected Pill style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
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
                                stringResource(R.string.label_followed),
                                color = if (showFollowingFeed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = subTabModifier
                                .background(if (!showFollowingFeed) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { showFollowingFeed = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.label_bookmarks_toggle),
                                color = if (!showFollowingFeed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item { Spacer(modifier = Modifier.width(12.dp)) }
                    lazyItems(courseKeys.zip(courses)) { (key, display) ->
                        val isSelected = selectedCourse == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCourse = key },
                            label = {
                                Text(
                                    text = display,
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
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = Color.Transparent,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 0.dp
                            )
                        )
                    }
                    item { Spacer(modifier = Modifier.width(20.dp)) }
                }
            }

            // Vertical space cleanup

            if (isLoading && filteredRecipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredRecipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    val isFilterActive = filterMaxTime < 240f || filterMaxCalories < 5000f || filterSkill != null || filterBudget != null || filterIngredients.isNotEmpty()
                    
                    val (icon, title, subtitle) = when {
                        searchQuery.isNotEmpty() || isFilterActive -> Triple(
                            R.drawable.filter, 
                            stringResource(R.string.empty_no_recipes_match), 
                            stringResource(R.string.empty_no_recipes_match_sub)
                        )
                        selectedTab == 0 -> Triple(
                            R.drawable.ic_recipe, 
                            stringResource(R.string.empty_no_popular_recipes), 
                            stringResource(R.string.empty_no_popular_recipes_sub)
                        )
                        selectedTab == 1 -> Triple(
                            R.drawable.ic_recipe, 
                            stringResource(R.string.empty_no_my_recipes), 
                            stringResource(R.string.empty_my_recipes_sub)
                        )
                        selectedTab == 2 && showFollowingFeed -> Triple(
                            R.drawable.follower, 
                            stringResource(R.string.empty_no_followed_recipes), 
                            stringResource(R.string.empty_no_followed_recipes_sub)
                        )
                        selectedTab == 2 && !showFollowingFeed -> Triple(
                            R.drawable.bookmark, 
                            stringResource(R.string.empty_no_bookmarked_recipes), 
                            stringResource(R.string.empty_bookmarked_recipes_sub)
                        )
                        else -> Triple(R.drawable.ic_recipe, stringResource(R.string.label_recipe_not_found), "")
                    }

                    EmptyState(iconRes = icon, title = title, subtitle = subtitle)
                }
            } else {
                // Using gridItemsIndexed to alternate padding between columns
                gridItemsIndexed(filteredRecipes) { index, recipe ->
                    val isSelected = selectedRecipeIds.contains(recipe.recipe_id)
                    RecipeCardItem(
                        recipe = recipe,
                        // Alternate padding: Left col gets start padding, Right col gets end padding
                        modifier = Modifier.padding(
                            start = if (index % 2 == 0) 16.dp else 0.dp,
                            end = if (index % 2 != 0) 16.dp else 0.dp
                        ),
                        // Pass current user to ensure live name sync
                        currentUser = authViewModel.currentUser,
                        // Only show menu for Tab 1 (My Recipes) and NOT in selection mode
                        showMenu = selectedTab == 1 && !isSelectionMode,
                        isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipe.recipe_id),
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onBookmarkClick = {
                            // Revert to customId (U001) as per Supabase table screenshot
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
            // Added back the 80dp spacer to lift the Floating Action Button up
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Manually anchor the Snackbar at the bottom, perfectly into the "empty place" under the FAB
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
                        text = stringResource(R.string.title_filter_recipes),
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
                        Text(stringResource(R.string.btn_reset_all), color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Max Prep Time
                FilterSectionHeader(icon = R.drawable.ic_clock, title = stringResource(R.string.label_max_prep_time))
                val timeDisplay = if (filterMaxTime >= 240f) stringResource(R.string.label_any_time) else "${filterMaxTime.toInt()} ${stringResource(R.string.label_mins_suffix)}"
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
                FilterSectionHeader(icon = R.drawable.ic_fire, title = stringResource(R.string.label_max_calories))
                val calDisplay = if (filterMaxCalories >= 5000f) stringResource(R.string.label_any_calories) else "${filterMaxCalories.toInt()} ${stringResource(R.string.label_kcal_suffix)}"
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
                FilterSectionHeader(icon = R.drawable.ic_ingredient_list, title = stringResource(R.string.label_ingredients_list))

                // ENLARGED: Ingredient Search Bar
                OutlinedTextField(
                    value = ingredientSearchQuery,
                    onValueChange = { ingredientSearchQuery = it },
                    placeholder = { Text(stringResource(R.string.placeholder_search_ingredients_filter), fontSize = 14.sp) },
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
                    // Use LazyRow to prevent lag when there are many ingredients
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

                    // Show Selected Ingredients List
                    if (filterIngredients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.label_selected_ingredients, filterIngredients.size),
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
                    Text(stringResource(R.string.msg_loading_ingredients), fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Cooking Skill
                FilterSectionHeader(icon = R.drawable.skill, title = stringResource(R.string.label_cooking_skill_level))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val skillKeys = listOf("Beginner", "Intermediate", "Master/Expert")
                    skillKeys.forEach { skill ->
                        FilterChip(
                            selected = filterSkill == skill,
                            onClick = { filterSkill = if (filterSkill == skill) null else skill },
                            label = { 
                                val label = when(skill) {
                                    "Beginner" -> stringResource(R.string.skill_beginner)
                                    "Intermediate" -> stringResource(R.string.skill_intermediate)
                                    else -> stringResource(R.string.skill_expert)
                                }
                                Text(label) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Budget
                FilterSectionHeader(icon = R.drawable.dollar_symbol, title = stringResource(R.string.label_budget_rm))
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
                    Text(stringResource(R.string.btn_apply_filters_action), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Share Dialog
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
                        Text(stringResource(R.string.title_no_internet))
                    }
                },
                text = { Text(stringResource(R.string.msg_no_internet_delete_warning)) },
                confirmButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { recipeToDelete = null },
                title = { Text(stringResource(R.string.dialog_delete_recipe_title)) },
                text = { Text(stringResource(R.string.dialog_delete_recipe_msg, recipeToDelete?.recipeName ?: "")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val rid = recipeToDelete?.recipe_id
                            // Ensure we use the short customId for deletion
                            val uid = authViewModel.currentUser?.customId
                            if (rid != null && uid != null) {
                                viewModel.deleteRecipe(rid, uid)
                            }
                            recipeToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.dialog_delete_recipe_confirm), color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recipeToDelete = null }) {
                        Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
                    }
                }
            )
        }
    }
    }
}

@Composable
fun EmptyState(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
