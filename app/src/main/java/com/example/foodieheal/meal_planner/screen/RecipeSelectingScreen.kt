package com.example.foodieheal.meal_planner.screen

import android.app.Activity
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
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.model.Recipe
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesSelectingScreen(
    recipeViewModel: RecipeViewModel,
    authViewModel: AuthViewModel,
    onSave:(List<String>)->Unit,
    onBackClick:()-> Unit
) {
    val selectedTab = recipeViewModel.activeTab
    val tabs = listOf("Popular", "My Recipes", "Bookmarks")

    val currentUserId = authViewModel.currentUser?.id

    val currentDataList = when (selectedTab) {
        0 -> recipeViewModel.recipeList.filter { it.author_id != currentUserId }
        1 -> recipeViewModel.myRecipes
        2 -> recipeViewModel.bookmarkedRecipes
        else -> emptyList()
    }

    val isLoading = recipeViewModel.isLoading
    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedRecipeIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        recipeViewModel.bookmarkMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    LaunchedEffect(selectedTab, currentUserId) {
        val cid = authViewModel.currentUser?.customId
        if (cid != null) {
            if (recipeViewModel.bookmarkedRecipeIds.isEmpty()) {
                recipeViewModel.fetchBookmarkIds(cid)
            }

            when (selectedTab) {
                0 -> if (recipeViewModel.recipeList.isEmpty()) recipeViewModel.fetchAllRecipes()
                1 -> if (recipeViewModel.myRecipes.isEmpty()) recipeViewModel.fetchMyRecipes(cid)
                2 -> if (recipeViewModel.bookmarkedRecipes.isEmpty()) recipeViewModel.fetchBookmarkedRecipes(cid)
            }
        }
    }

    Scaffold(
//        containerColor = Color(0xFFF8F8F8),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrowback),
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp).clickable(onClick = {onBackClick()})
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (selectedRecipeIds.isEmpty()) "Select Recipes" else "${selectedRecipeIds.size} Selected",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        // 🌟 Add Button layout anchored cleanly at the bottom container zone
        bottomBar = {
            AnimatedVisibility(
                visible = selectedRecipeIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {onSave(selectedRecipeIds)},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Add Selected Recipes (${selectedRecipeIds.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        RecipeGridContent(
            paddingValues = paddingValues,
            currentDataList = currentDataList,
            isLoading = isLoading,
            selectedTab = selectedTab,
            onTabSelected = { index -> recipeViewModel.activeTab = index },
            tabs = tabs,
            selectedRecipeIds = selectedRecipeIds,
            onRecipeSelectedToggle = { id ->
                if (selectedRecipeIds.contains(id)) {
                    selectedRecipeIds.remove(id)
                } else {
                    selectedRecipeIds.add(id)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeGridContent(
    paddingValues: PaddingValues,
    currentDataList: List<Recipe>,
    isLoading: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    selectedRecipeIds: List<String>,
    onRecipeSelectedToggle: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("Breakfast") }
    val courses = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    var showFilterDialog by remember { mutableStateOf(false) }
    var filterMaxTime by remember { mutableFloatStateOf(120f) }
    var filterMaxCalories by remember { mutableFloatStateOf(2000f) }
    var filterSkill by remember { mutableStateOf<String?>(null) }
    var filterBudget by remember { mutableStateOf<String?>(null) }

    val filteredRecipes by remember(searchQuery, selectedCourse, currentDataList, filterMaxTime, filterMaxCalories, filterSkill, filterBudget) {
        derivedStateOf {
            currentDataList.filter { recipe ->
                val matchesSearch = recipe.recipeName.contains(searchQuery, ignoreCase = true)
                val matchesCourse = recipe.recipeCourse.equals(selectedCourse, ignoreCase = true)
                val matchesTime = recipe.time <= filterMaxTime.toInt()
                val matchesCalories = recipe.calories <= filterMaxCalories.toInt()
                val matchesSkill = filterSkill == null || recipe.cookingSkill.equals(filterSkill, ignoreCase = true)
                val matchesBudget = filterBudget == null || recipe.estimatedBudget == filterBudget

                matchesSearch && matchesCourse && matchesTime && matchesCalories && matchesSkill && matchesBudget
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = Color.White
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search recipes here", fontSize = 14.sp, color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.LightGray,
                        unfocusedBorderColor = Color.LightGray,
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.filter),
                                contentDescription = "Filter",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showFilterDialog = true }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Image(
                                painter = painterResource(id = R.drawable.search),
                                contentDescription = "Search",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { /* Handle search */ }
                            )
                        }
                    }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Text(text = "Course", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black, modifier = Modifier.padding(top = 8.dp))
            }

            item(span = { GridItemSpan(2) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    lazyItems(courses) { course: String ->
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
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = selectedCourse,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (isLoading && filteredRecipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredRecipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotEmpty() || filterMaxTime < 120f || filterMaxCalories < 2000f || filterSkill != null || filterBudget != null)
                                "No recipes match your filters."
                            else "No recipes found for '$selectedCourse'.",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                gridItems(filteredRecipes) { recipe: Recipe ->
                    val recipeId = recipe.recipe_id ?: ""
                    val isSelected = selectedRecipeIds.contains(recipeId)

                    RecipeCardItem(
                        recipe = recipe,
                        isSelected = isSelected,
                        onClick = { onRecipeSelectedToggle(recipeId) }
                    )
                }
            }
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            confirmButton = {
                Button(
                    onClick = { showFilterDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Apply Filters") }
            },
            dismissButton = {
                TextButton(onClick = {
                    filterMaxTime = 120f
                    filterMaxCalories = 2000f
                    filterSkill = null
                    filterBudget = null
                    showFilterDialog = false
                }) { Text("Clear All", color = Color.Red) }
            },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Filter Recipes", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showFilterDialog = false }) {
                        Icon(painterResource(id = R.drawable.cancel), "Close", modifier = Modifier.size(20.dp))
                    }
                }
            },
            text = {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())) {
                    Text("Max Prep Time (mins)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = filterMaxTime,
                            onValueChange = { filterMaxTime = it },
                            valueRange = 0f..120f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                        OutlinedTextField(
                            value = filterMaxTime.toInt().toString(),
                            onValueChange = {
                                val newVal = it.toFloatOrNull() ?: 0f
                                if (newVal in 0f..120f) filterMaxTime = newVal
                            },
                            modifier = Modifier
                                .width(70.dp)
                                .padding(start = 8.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Max Calories (kcal)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = filterMaxCalories,
                            onValueChange = { filterMaxCalories = it },
                            valueRange = 0f..2000f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                        )
                        OutlinedTextField(
                            value = filterMaxCalories.toInt().toString(),
                            onValueChange = {
                                val newVal = it.toFloatOrNull() ?: 0f
                                if (newVal in 0f..2000f) filterMaxCalories = newVal
                            },
                            modifier = Modifier
                                .width(80.dp)
                                .padding(start = 8.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Cooking Skill", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        listOf("Beginner", "Intermediate", "Master/Expert").forEach { skill ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = filterSkill == skill, onClick = { filterSkill = if (filterSkill == skill) null else skill })
                                Text(skill, fontSize = 14.sp)
                            }
                        }
                    }

                    Text("Budget (RM)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        listOf("0 - 20", "20 - 40", "40 - 60", "60 - 80", "80 - 100").forEach { budget ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = filterBudget == budget, onClick = { filterBudget = if (filterBudget == budget) null else budget })
                                Text(budget, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun RecipeCardItem(
    recipe: Recipe,
    isSelected: Boolean, // 🌟 Determines selection layout variant
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // 🌟 Sets dynamic border layout indicating selected state
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        onClick = onClick
    ) {
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFEEEEEE))) {
                if (!recipe.recipeImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = recipe.recipeImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        alpha = 0.3f
                    )
                }

                // 🌟 Check badge overlay variant when card selection is active
                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check), // Make sure ic_check exists in res/drawable
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp)
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
                        color = Color.Black,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(id = R.drawable.ic_fire), null, modifier = Modifier.size(12.dp), tint = Color.Black)
                    Text(text = " ${recipe.calories} kcal", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(painterResource(id = R.drawable.ic_clock), null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(text = " ${recipe.time} mins", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}