package com.example.foodieheal.view

import android.app.Activity
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.repository.RecipeRepository
import com.example.foodieheal.viewmodel.AuthViewModel
import com.example.foodieheal.viewmodel.RecipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    parentNavController: NavController,
    viewModel: RecipeViewModel = viewModel(viewModelStoreOwner = androidx.compose.ui.platform.LocalContext.current as androidx.lifecycle.ViewModelStoreOwner),
    authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = androidx.compose.ui.platform.LocalContext.current as androidx.lifecycle.ViewModelStoreOwner)
) {
    val selectedTab = viewModel.activeTab
    val tabs = listOf("Popular", "My Recipes", "Bookmarks")
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("Breakfast") }
    val courses = listOf("Breakfast", "Lunch", "Dinner", "Snack")
    
    // Filter State
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterMaxTime by remember { mutableFloatStateOf(120f) }
    var filterMaxCalories by remember { mutableFloatStateOf(2000f) }
    var filterSkill by remember { mutableStateOf<String?>(null) }
    var filterBudget by remember { mutableStateOf<String?>(null) }
    
    val currentUserId = authViewModel.currentUser?.id

    // 🌟 LOGIC: Filtering based on your instructions
    val currentDataList = when (selectedTab) {
        // Tab 0: Popular -> Share by OTHER people (Exclude mine)
        0 -> viewModel.recipeList.filter { it.author_id != currentUserId }
        // Tab 1: My Recipes -> Created by ME
        1 -> viewModel.myRecipes
        // Tab 2: Bookmarks -> Saved by ME
        2 -> viewModel.bookmarkedRecipes
        else -> emptyList()
    }
    
    val isLoading = viewModel.isLoading
    
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

    val view = LocalView.current
    val primaryColor = MaterialTheme.colorScheme.primary

    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = primaryColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    LaunchedEffect(selectedTab, currentUserId) {
        if (currentUserId != null) {
            viewModel.fetchBookmarkIds(currentUserId)
            when (selectedTab) {
                0 -> if (viewModel.recipeList.isEmpty()) viewModel.fetchAllRecipes()
                1 -> if (viewModel.myRecipes.isEmpty()) viewModel.fetchMyRecipes(currentUserId)
                2 -> if (viewModel.bookmarkedRecipes.isEmpty()) viewModel.fetchBookmarkedRecipes(currentUserId)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
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
                            painter = painterResource(id = R.drawable.ic_hamburger_menu),
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Recipe",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                                onClick = { viewModel.activeTab = index },
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
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { parentNavController.navigate(Screen.AddRecipe.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = 20.dp) 
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
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search recipes here", fontSize = 14.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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
                                modifier = Modifier.size(20.dp).clickable { showFilterDialog = true }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Image(
                                painter = painterResource(id = R.drawable.search), 
                                contentDescription = "Search", 
                                modifier = Modifier.size(20.dp)
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
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredRecipes.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
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
                    RecipeCardItem(
                        recipe = recipe, 
                        // 🌟 ONLY show menu for Tab 1 (My Recipes)
                        showMenu = selectedTab == 1,
                        isBookmarked = viewModel.bookmarkedRecipeIds.contains(recipe.recipe_id),
                        onBookmarkClick = {
                            currentUserId?.let { uid ->
                                recipe.recipe_id?.let { rid -> viewModel.toggleBookmark(uid, rid) }
                            }
                        },
                        onDeleteClick = { recipeToDelete = recipe },
                        onEditClick = {
                            // TODO: Navigate to Edit screen
                        },
                        onAddClick = {
                            recipe.recipe_id?.let { id ->
                                parentNavController.navigate("add_recipe_to_planner/$id")
                            }
                        }
                    )
                }
            }
            
            item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(80.dp)) }
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
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
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
                            modifier = Modifier.width(70.dp).padding(start = 8.dp),
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
                            modifier = Modifier.width(80.dp).padding(start = 8.dp),
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

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Delete Recipe") },
            text = { Text("Are you sure you want to delete '${recipeToDelete?.recipeName}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rid = recipeToDelete?.recipe_id
                        val uid = currentUserId
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

@Composable
fun RecipeCardItem(
    recipe: Recipe,
    showMenu: Boolean = false,
    isBookmarked: Boolean = false,
    onBookmarkClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().height(260.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color(0xFFEEEEEE))) {
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
                        modifier = Modifier.size(40.dp).align(Alignment.Center),
                        alpha = 0.3f
                    )
                }

                if (!showMenu) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(28.dp)
                    ) {
                        IconButton(onClick = onBookmarkClick) {
                            Image(
                                painter = painterResource(
                                    id = if (isBookmarked) R.drawable.bookmark_fill else R.drawable.bookmark
                                ),
                                contentDescription = "Bookmark",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_recipe),
                        contentDescription = "Add to Planner",
                        modifier = Modifier.padding(6.dp).clickable { onAddClick() },
                        tint = Color.Black
                    )
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
                    if (showMenu) {
                        Box {
                            IconButton(onClick = { expanded = true }, modifier = Modifier.size(20.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_vertical_more),
                                    contentDescription = "Menu",
                                    tint = Color.Black
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
                                    text = { Text("Delete Recipe", color = Color(0xFFD32F2F)) },
                                    onClick = {
                                        expanded = false
                                        onDeleteClick()
                                    },
                                    leadingIcon = { Icon(painterResource(id = R.drawable.ic_delete), null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
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
