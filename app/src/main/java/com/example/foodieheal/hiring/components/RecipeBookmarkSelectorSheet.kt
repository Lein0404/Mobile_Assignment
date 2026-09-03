package com.example.foodieheal.hiring.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.example.foodieheal.Chef.getRecipeCourseResId
import com.example.foodieheal.R
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.ui.components.getHighlightedText
import com.example.foodieheal.Recipe.viewModel.RecipeViewModel
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.hiring.model.SelectedAppointmentRecipe

enum class RecipeSelectorTab {
    MY_RECIPES,
    BOOKMARKS,
    FOLLOWED
}

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
    onUpdateChefProvidesIngredients: (recipeId: String, provides: Boolean) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf("All") }
    val courses = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")

    var previewingRecipe by remember { mutableStateOf<Recipe?>(null) }

    // Tab Selection (My Recipes, Bookmarks, Follow)
    var selectedTab by remember { mutableStateOf(RecipeSelectorTab.BOOKMARKS) }
    val currentUserId = authViewModel?.currentUser?.id?.ifBlank { authViewModel?.currentUser?.customId } ?: authViewModel?.currentUser?.customId

    // Fetch my recipes, following and bookmarked recipes
    LaunchedEffect(currentUserId) {
        val cid = currentUserId
        if (recipeViewModel != null && !cid.isNullOrBlank()) {
            recipeViewModel.fetchBookmarkIds(cid)
            recipeViewModel.fetchMyRecipes(cid)
            recipeViewModel.fetchBookmarkedRecipes(cid)
            recipeViewModel.fetchFollowingRecipes(cid)
        }
    }

    LaunchedEffect(selectedTab, currentUserId) {
        val cid = currentUserId
        if (recipeViewModel != null && !cid.isNullOrBlank()) {
            when (selectedTab) {
                RecipeSelectorTab.MY_RECIPES -> {
                    if (recipeViewModel.myRecipes.isEmpty()) {
                        recipeViewModel.fetchMyRecipes(cid)
                    }
                }
                RecipeSelectorTab.BOOKMARKS -> {
                    if (recipeViewModel.bookmarkedRecipes.isEmpty()) {
                        recipeViewModel.fetchBookmarkedRecipes(cid)
                    }
                }
                RecipeSelectorTab.FOLLOWED -> {
                    if (recipeViewModel.followingRecipes.isEmpty()) {
                        recipeViewModel.fetchFollowingRecipes(cid)
                    }
                }
            }
        }
    }

    val currentDataList: List<Recipe> = remember(
        recipeViewModel,
        selectedTab,
        bookmarkedRecipes,
        recipeViewModel?.myRecipes,
        recipeViewModel?.followingRecipes,
        recipeViewModel?.bookmarkedRecipes
    ) {
        if (recipeViewModel != null) {
            when (selectedTab) {
                RecipeSelectorTab.MY_RECIPES -> recipeViewModel.myRecipes
                RecipeSelectorTab.BOOKMARKS -> {
                    if (recipeViewModel.bookmarkedRecipes.isNotEmpty()) {
                        recipeViewModel.bookmarkedRecipes
                    } else {
                        bookmarkedRecipes
                    }
                }
                RecipeSelectorTab.FOLLOWED -> recipeViewModel.followingRecipes
            }
        } else {
            when (selectedTab) {
                RecipeSelectorTab.MY_RECIPES -> emptyList()
                RecipeSelectorTab.BOOKMARKS -> bookmarkedRecipes
                RecipeSelectorTab.FOLLOWED -> emptyList()
            }
        }
    }

    val isDataLoading = (recipeViewModel?.isLoading ?: false) || isLoading
    val focusManager = LocalFocusManager.current

    val courseTranslations = mapOf(
        "breakfast" to stringResource(R.string.recipe_course_breakfast),
        "lunch" to stringResource(R.string.recipe_course_lunch),
        "dinner" to stringResource(R.string.recipe_course_dinner),
        "snack" to stringResource(R.string.recipe_course_snack),
        "dessert" to stringResource(R.string.recipe_course_dessert),
        "beverage" to stringResource(R.string.recipe_course_beverage)
    )

    val filteredRecipes by remember(
        currentDataList,
        searchQuery,
        selectedCourse,
        courseTranslations,
        currentUserId,
        recipeViewModel?.followedUserIds
    ) {
        derivedStateOf {
            currentDataList.filter { recipe ->
                // Visibility & Privacy check
                val isVisible = when {
                    recipe.author_id == currentUserId -> true
                    recipe.visibility.isNullOrBlank() || recipe.visibility.equals("public", ignoreCase = true) -> true
                    recipe.visibility.equals("followers", ignoreCase = true) -> recipeViewModel?.followedUserIds?.contains(recipe.author_id) == true
                    else -> false
                }
                if (!isVisible) return@filter false

                val matchesSearch = if (searchQuery.isBlank()) true else {
                    val query = searchQuery.trim().lowercase()
                    val localizedCourse = courseTranslations[recipe.recipeCourse.trim().lowercase()]?.lowercase().orEmpty()
                    recipe.recipeName.lowercase().contains(query) ||
                            (recipe.authorName?.lowercase()?.contains(query) == true) ||
                            (recipe.authorInfo?.name?.lowercase()?.contains(query) == true) ||
                            recipe.recipeCourse.lowercase().contains(query) ||
                            localizedCourse.contains(query) ||
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
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.attach_recipes_menu_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.select_dishes_for_chef_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (selectedRecipes.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.dishes_selected_count, selectedRecipes.size),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3-Way Tab Selector: My Recipes, Bookmarks, Followed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        RoundedCornerShape(22.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    RecipeSelectorTab.MY_RECIPES to stringResource(R.string.tab_my_recipes_title),
                    RecipeSelectorTab.BOOKMARKS to stringResource(R.string.label_bookmarks_toggle),
                    RecipeSelectorTab.FOLLOWED to stringResource(R.string.label_followed)
                )

                tabs.forEach { (tab, label) ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_recipes_placeholder),
                        fontSize = 14.sp,
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
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(courses) { course ->
                    val isSelected = selectedCourse == course
                    val courseDisplay = getRecipeCourseResId(course)?.let { stringResource(it) } ?: course
                    Surface(
                        onClick = { selectedCourse = course },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = courseDisplay,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
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
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = when (selectedTab) {
                                        RecipeSelectorTab.MY_RECIPES -> R.drawable.ic_recipe
                                        RecipeSelectorTab.BOOKMARKS -> R.drawable.bookmark
                                        RecipeSelectorTab.FOLLOWED -> R.drawable.follower
                                    }
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (selectedTab) {
                                    RecipeSelectorTab.MY_RECIPES -> stringResource(R.string.empty_no_my_recipes)
                                    RecipeSelectorTab.BOOKMARKS -> stringResource(R.string.empty_no_bookmarked_recipes)
                                    RecipeSelectorTab.FOLLOWED -> stringResource(R.string.empty_no_followed_recipes)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (selectedTab) {
                                    RecipeSelectorTab.MY_RECIPES -> stringResource(R.string.empty_my_recipes_sub)
                                    RecipeSelectorTab.BOOKMARKS -> stringResource(R.string.empty_bookmarked_recipes_sub)
                                    RecipeSelectorTab.FOLLOWED -> stringResource(R.string.empty_no_followed_recipes_sub)
                                },
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
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.filter),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_matching_recipes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.no_recipe_matches_query, searchQuery, selectedCourse),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(filteredRecipes, key = { it.recipe_id ?: it.hashCode().toString() }) { recipe ->
                                val selectedItem = selectedRecipes.find { it.recipe.recipe_id == recipe.recipe_id }
                                val isSelected = selectedItem != null

                                RecipeSelectableCard(
                                    recipe = recipe,
                                    searchQuery = searchQuery,
                                    isSelected = isSelected,
                                    selectedRecipeState = selectedItem,
                                    onToggle = { onToggleSelect(recipe) },
                                    onViewDetails = { previewingRecipe = recipe },
                                    onUpdateServings = { servings ->
                                        recipe.recipe_id?.let { onUpdateServings(it, servings) }
                                    },
                                    onUpdateNote = { note ->
                                        recipe.recipe_id?.let { onUpdateNote(it, note) }
                                    },
                                    onUpdateChefProvidesIngredients = { provides ->
                                        recipe.recipe_id?.let { onUpdateChefProvidesIngredients(it, provides) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (selectedRecipes.isEmpty()) stringResource(R.string.btn_close) else stringResource(R.string.done_selected_count, selectedRecipes.size),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Recipe Details Sheet Modal
    previewingRecipe?.let { targetRecipe ->
        val selectedState = selectedRecipes.find { it.recipe.recipe_id == targetRecipe.recipe_id }
        RecipeDetailPreviewSheet(
            recipe = targetRecipe,
            selectedRecipeState = selectedState,
            onDismiss = { previewingRecipe = null },
            onToggleSelect = { recipe -> onToggleSelect(recipe) },
            onUpdateServings = { recipeId, servings -> onUpdateServings(recipeId, servings) },
            onUpdateNote = { recipeId, note -> onUpdateNote(recipeId, note) },
            onUpdateChefProvidesIngredients = { recipeId, provides -> onUpdateChefProvidesIngredients(recipeId, provides) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeSelectableCard(
    recipe: Recipe,
    searchQuery: String = "",
    isSelected: Boolean,
    selectedRecipeState: SelectedAppointmentRecipe?,
    onToggle: () -> Unit,
    onViewDetails: () -> Unit,
    onUpdateServings: (Int) -> Unit,
    onUpdateNote: (String) -> Unit,
    onUpdateChefProvidesIngredients: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Checkbox
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Thumbnail Image (Clickable for details)
                AsyncImage(
                    model = recipe.recipeImageUrl,
                    contentDescription = recipe.recipeName,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onViewDetails() },
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_recipe),
                    placeholder = painterResource(R.drawable.ic_recipe)
                )

                // Recipe details (Clickable for full preview)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewDetails() }
                ) {
                    Text(
                        text = getHighlightedText(
                            fullText = recipe.recipeName,
                            query = searchQuery
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )

                    val authorName = recipe.authorName ?: recipe.authorInfo?.name
                    if (!authorName.isNullOrBlank()) {
                        Text(
                            text = "by $authorName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (recipe.recipeCourse.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                val courseDisplay = getRecipeCourseResId(recipe.recipeCourse)?.let { stringResource(it) } ?: recipe.recipeCourse
                                Text(
                                    text = getHighlightedText(
                                        fullText = courseDisplay,
                                        query = searchQuery
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (recipe.calories > 0) {
                            Text(
                                text = stringResource(R.string.format_recipe_calories, recipe.calories),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (recipe.time > 0) {
                            Text(
                                text = "• ${stringResource(R.string.format_recipe_duration, recipe.time)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Info / Details Button
                IconButton(
                    onClick = onViewDetails,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recipe),
                        contentDescription = stringResource(R.string.view_details),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
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
                                text = stringResource(R.string.portions_for_this_dish),
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

                        // Chef Provide Ingredients Checkbox
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onUpdateChefProvidesIngredients(!selectedRecipeState.chefProvidesIngredients)
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selectedRecipeState.chefProvidesIngredients) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedRecipeState.chefProvidesIngredients,
                                    onCheckedChange = { onUpdateChefProvidesIngredients(it) },
                                    modifier = Modifier.size(20.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.chef_provides_ingredients_checkbox),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val estimatedBudget = com.example.foodieheal.hiring.model.AppointmentPricingBreakdown.parseEstimatedBudget(recipe.estimatedBudget)
                                    val dishIngredientTotal = estimatedBudget * selectedRecipeState.serviceCount
                                    Text(
                                        text = if (selectedRecipeState.chefProvidesIngredients) {
                                            if (dishIngredientTotal > 0) {
                                                "+ RM ${String.format(java.util.Locale.US, "%.2f", dishIngredientTotal)} estimated cost"
                                            } else {
                                                stringResource(R.string.chef_provides_ingredients_sub)
                                            }
                                        } else {
                                            stringResource(R.string.user_provides_ingredients_sub)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedRecipeState.chefProvidesIngredients) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 10.sp
                                    )
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
                                    text = stringResource(R.string.optional_note_chef_placeholder),
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
