package com.example.foodieheal.hiring.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.hiring.model.SelectedAppointmentRecipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBookmarkSelectorSheet(
    bookmarkedRecipes: List<Recipe> = emptyList(),
    selectedRecipes: List<SelectedAppointmentRecipe>,
    isLoading: Boolean = false,
    recipeViewModel: RecipeViewModel? = null,
    authViewModel: AuthViewModel? = null,
    onToggleSelect: (Recipe) -> Unit,
    onUpdateServings: (recipeId: String, servings: Int) -> Unit,
    onUpdateNote: (recipeId: String, note: String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("All") }
    val courses = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")

    val tabs = listOf("Popular", "My Recipes", "Bookmarks")
    var localSelectedTab by remember { mutableIntStateOf(recipeViewModel?.activeTab ?: 0) }
    val currentUserId = authViewModel?.currentUser?.id

    // Fetch data via RecipeViewModel when tab changes (like meal_planner)
    LaunchedEffect(localSelectedTab, currentUserId) {
        val cid = authViewModel?.currentUser?.customId
        if (recipeViewModel != null && cid != null) {
            when (localSelectedTab) {
                0 -> if (recipeViewModel.recipeList.isEmpty()) recipeViewModel.fetchAllRecipes()
                1 -> if (recipeViewModel.myRecipes.isEmpty()) recipeViewModel.fetchMyRecipes(cid)
                2 -> if (recipeViewModel.bookmarkedRecipes.isEmpty()) recipeViewModel.fetchBookmarkedRecipes(cid)
            }
        }
    }

    val currentDataList: List<Recipe> = remember(recipeViewModel, localSelectedTab, bookmarkedRecipes) {
        if (recipeViewModel != null) {
            when (localSelectedTab) {
                0 -> recipeViewModel.recipeList.filter { it.author_id != currentUserId }
                1 -> recipeViewModel.myRecipes
                2 -> recipeViewModel.bookmarkedRecipes
                else -> emptyList()
            }
        } else {
            bookmarkedRecipes
        }
    }

    val isDataLoading = (recipeViewModel?.isLoading ?: false) || isLoading
    val focusManager = LocalFocusManager.current

    val filteredRecipes by remember(currentDataList, searchQuery, selectedCourse) {
        derivedStateOf {
            currentDataList.filter { recipe ->
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    val query = searchQuery.trim().lowercase()
                    recipe.recipeName.lowercase().contains(query) ||
                            recipe.recipeCourse.lowercase().contains(query) ||
                            recipe.cookingSkill.lowercase().contains(query)
                }
                val matchesCourse = if (selectedCourse == "All") true else {
                    recipe.recipeCourse.equals(selectedCourse, ignoreCase = true)
                }
                matchesSearch && matchesCourse
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attach Recipes / Menu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select dishes for your chef to prepare",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedRecipes.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${selectedRecipes.size} selected",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TabRow (Popular, My Recipes, Bookmarks)
            if (recipeViewModel != null) {
                PrimaryTabRow(
                    selectedTabIndex = localSelectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(localSelectedTab),
                            color = MaterialTheme.colorScheme.primary,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = localSelectedTab == index,
                            onClick = {
                                localSelectedTab = index
                                recipeViewModel.activeTab = index
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = if (localSelectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = "Search recipes by name...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                painter = painterResource(id = R.drawable.cancel),
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Course Filter Chips (All, Breakfast, Lunch, Dinner, Snack)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(courses) { course ->
                    val isSelected = selectedCourse == course
                    Surface(
                        onClick = { selectedCourse = course },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = course,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recipe List Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 380.dp)
            ) {
                when {
                    isDataLoading && currentDataList.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                    currentDataList.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.bookmark),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No recipes found in ${tabs.getOrElse(localSelectedTab) { "this tab" }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Explore and bookmark recipes in the Recipes tab to easily attach them here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    filteredRecipes.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No matching recipes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No recipe matches \"$searchQuery\" in $selectedCourse",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredRecipes, key = { it.recipe_id ?: it.hashCode().toString() }) { recipe ->
                                val selectedItem = selectedRecipes.find { it.recipe.recipe_id == recipe.recipe_id }
                                val isSelected = selectedItem != null

                                RecipeSelectableCard(
                                    recipe = recipe,
                                    isSelected = isSelected,
                                    selectedRecipeState = selectedItem,
                                    onToggle = { onToggleSelect(recipe) },
                                    onUpdateServings = { servings ->
                                        recipe.recipe_id?.let { onUpdateServings(it, servings) }
                                    },
                                    onUpdateNote = { note ->
                                        recipe.recipe_id?.let { onUpdateNote(it, note) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (selectedRecipes.isEmpty()) "Close" else "Done (${selectedRecipes.size} selected)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun RecipeSelectableCard(
    recipe: Recipe,
    isSelected: Boolean,
    selectedRecipeState: SelectedAppointmentRecipe?,
    onToggle: () -> Unit,
    onUpdateServings: (Int) -> Unit,
    onUpdateNote: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Checkbox
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Thumbnail Image
                AsyncImage(
                    model = recipe.recipeImageUrl,
                    contentDescription = recipe.recipeName,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_recipe),
                    placeholder = painterResource(R.drawable.ic_recipe)
                )

                // Recipe details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.recipeName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (recipe.recipeCourse.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = recipe.recipeCourse,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (recipe.calories > 0) {
                            Text(
                                text = "${recipe.calories} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (recipe.time > 0) {
                            Text(
                                text = "• ${recipe.time} mins",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Expandable customization for portions and custom note
            AnimatedVisibility(
                visible = isSelected && selectedRecipeState != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (selectedRecipeState != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Spacer(modifier = Modifier.height(10.dp))

                        // Servings Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Portions for this dish:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable(enabled = selectedRecipeState.serviceCount > 1) {
                                            if (selectedRecipeState.serviceCount > 1) {
                                                onUpdateServings(selectedRecipeState.serviceCount - 1)
                                            }
                                        },
                                    shape = CircleShape,
                                    color = if (selectedRecipeState.serviceCount > 1) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "-",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "${selectedRecipeState.serviceCount}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.widthIn(min = 20.dp),
                                    textAlign = TextAlign.Center
                                )

                                Surface(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            onUpdateServings(selectedRecipeState.serviceCount + 1)
                                        },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom note input for chef
                        OutlinedTextField(
                            value = selectedRecipeState.customNote,
                            onValueChange = { onUpdateNote(it) },
                            placeholder = {
                                Text(
                                    text = "Optional note for chef (e.g. less salt, spicy)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }
    }
}
