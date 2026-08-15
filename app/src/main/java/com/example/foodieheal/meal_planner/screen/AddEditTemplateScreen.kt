package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.viewModel.AddEditTemplateViewModel
import com.example.foodieheal.model.Recipe
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.theme.Green
import com.example.foodieheal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import kotlin.Unit

@Composable
fun AddEditTemplateRoute(
    modifier: Modifier,
    viewModel: AddEditTemplateViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onNavigateToAddRecipe: (DayOfWeek, MealType) -> Unit,
    onRecipeClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSavedSuccess) {
        if (uiState.isSavedSuccess) {
            onBackClick()
        }
    }

    AddEditTemplateScreen(
        modifier = modifier,
        isEditMode = viewModel.isEditMode, // Pass edit mode state
        planName = uiState.planName,
        onPlanNameChange = viewModel::updatePlanName,
        selectedCategory = uiState.category,
        onCategorySelected = viewModel::updateCategoryByString,
        maxCalories = calculateSuggestedDailyCalories(authViewModel.currentUser),
        dailyPlans = uiState.dailyPlans,
        onAddMealRecipe = { day, mealType -> onNavigateToAddRecipe(day, mealType) },
        onDeleteMealRecipe = { day, mealType, recipe ->
            viewModel.removeRecipeFromSlot(day, mealType, recipe)
        },
        onRecipeClick = onRecipeClick,
        onBackClick = onBackClick,
        onSave = {
            viewModel.saveTemplate()
            navController.popBackStack()
        },
        onNavigateToProfile = onNavigateToProfile
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddEditTemplateScreen(
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    planName: String,
    onPlanNameChange: (String) -> Unit,
    selectedCategory: PlanCategory?,
    onCategorySelected: (String) -> Unit,
    maxCalories: Int,
    dailyPlans: Map<DayOfWeek, List<RealMealSlot>>,
    onAddMealRecipe: (DayOfWeek, MealType) -> Unit,
    onDeleteMealRecipe: (DayOfWeek, MealType, Recipe) -> Unit,
    onRecipeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onSave: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val daysOfWeek = remember { DayOfWeek.entries.toList() }
    val pagerState = rememberPagerState(pageCount = { daysOfWeek.size })
    val scope = rememberCoroutineScope()

    val currentSelectedDay = daysOfWeek[pagerState.currentPage]
    val selectedDayTotalCalories = remember(dailyPlans, currentSelectedDay) {
        dailyPlans[currentSelectedDay]
            ?.flatMap { slot -> slot.recipes }
            ?.sumOf { recipe -> recipe.calories } ?: 0
    }

    // Dialog state management
    var showNameDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Check if user has added any recipes across all days
    val hasEnteredData = remember(dailyPlans) {
        dailyPlans.values.any { slotList -> slotList.any { it.recipes.isNotEmpty() } }
    }

    // Convert PlanCategory enum to string for DropDownList display
    val categoryDisplayString = selectedCategory?.let {
        stringResource(it.displayNameRes)
    } ?: stringResource(R.string.category_others)

    // --- Discard Confirmation Dialog ---
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (isEditMode) "Discard Changes?" else "Discard Draft?") },
            text = {
                Text(
                    if (isEditMode) "Are you sure you want to discard your edits? Changes will not be saved."
                    else "Are you sure you want to discard this template? Unsaved changes will be lost."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continue Editing")
                }
            }
        )
    }

    // --- Template Name Input Dialog ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(if (isEditMode) "Edit Template Name" else "Set Template Name") },
            text = {
                Column {
                    Text(
                        text = "Please enter a name for this meal plan template:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = planName,
                        onValueChange = onPlanNameChange,
                        label = { Text("Template Name") },
                        placeholder = { Text("e.g. High Protein Week") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNameDialog = false
                        onSave()
                    },
                    enabled = planName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // --- Top Bar Header ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (hasEnteredData || selectedCategory != null || planName.isNotBlank()) {
                            showDiscardDialog = true
                        } else {
                            onBackClick()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrowback),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = if (isEditMode) "Edit Template" else "Create Template",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        if (planName.isBlank()) {
                            showNameDialog = true
                        } else {
                            onSave()
                        }
                    },
                    enabled = selectedCategory != null,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        // --- Category Selection Dropdown ---
        DropDownList(
            labelId = R.string.category,
            placeholderId = R.string.category,
            selectedValue = categoryDisplayString,
            onOptionSelected = onCategorySelected,
            options = PlanCategory.catList,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        // --- Weekly Day Selection Row ---
        WeeklyDayCardRow(
            selectedDay = currentSelectedDay,
            onDaySelected = { selectedDay ->
                val targetPage = daysOfWeek.indexOf(selectedDay)
                if (targetPage != -1) {
                    scope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        CalorieProgressBar(
            currentCalories = selectedDayTotalCalories,
            maxCalories = maxCalories,
            onNavigateToProfile = onNavigateToProfile,
            modifier = Modifier
        )

        Spacer(Modifier.height(8.dp))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // --- Horizontal Pager for Days ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val dayOfWeek = daysOfWeek[page]
            val mealSlotsForDay = dailyPlans[dayOfWeek] ?: emptyList()

            TemplateDayPageContent(
                mealSlots = mealSlotsForDay,
                onAddMealRecipe = { mealType -> onAddMealRecipe(dayOfWeek, mealType) },
                onDeleteMealRecipe = { mealType, recipe -> onDeleteMealRecipe(dayOfWeek, mealType, recipe) },
                onRecipeClick = onRecipeClick
            )
        }
    }
}

@Composable
private fun TemplateDayPageContent(
    mealSlots: List<RealMealSlot>,
    onAddMealRecipe: (MealType) -> Unit,
    onDeleteMealRecipe: (MealType, Recipe) -> Unit,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        MealType.entries.forEach { mealType ->
            val currentSlot = mealSlots.find { it.mealType == mealType }
            val recipesInSlot = currentSlot?.recipes ?: emptyList()

            TemplateMealSlotCard(
                mealType = mealType,
                recipes = recipesInSlot,
                onAddClick = { onAddMealRecipe(mealType) },
                onDeleteRecipe = { recipe -> onDeleteMealRecipe(mealType, recipe) },
                onRecipeClick = onRecipeClick
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}