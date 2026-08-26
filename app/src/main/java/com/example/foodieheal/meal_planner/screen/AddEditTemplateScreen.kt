package com.example.foodieheal.meal_planner.screen

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.theme.Green
import com.example.foodieheal.User.viewModel.AuthViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@Composable
fun AddEditTemplateRoute(
    modifier: Modifier,
    viewModel: AddEditTemplateViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onNavigateToAddRecipe: (DayOfWeek, MealType) -> Unit,
    onRecipeClick: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val templateUpdatedMsg = stringResource(R.string.msg_template_updated)
    val templateCreatedMsg = stringResource(R.string.msg_template_created)

    LaunchedEffect(uiState.isSavedSuccess) {
        if (uiState.isSavedSuccess) {
            // 👈 Show dynamic Toast based on create vs edit mode
            val message = if (viewModel.isEditMode) {
                templateUpdatedMsg
            } else {
                templateCreatedMsg
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

            onBackClick()
        }
    }

    AddEditTemplateScreen(
        modifier = modifier,
        isEditMode = viewModel.isEditMode,
        planName = uiState.planName,
        onPlanNameChange = viewModel::updatePlanName,
        selectedCategory = uiState.category?.displayNameRes?:R.string.null_string,
        onCategorySelected = viewModel::updateCategoryByString,
        isPublic = uiState.isPublic,
        onPublicChange = viewModel::updateIsPublic,
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
        },
        onNavigateToProfile = onNavigateToProfile
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddEditTemplateScreen(
    isEditMode: Boolean = false,
    planName: String,
    onPlanNameChange: (String) -> Unit,
    selectedCategory: Int,
    onCategorySelected: (String) -> Unit,
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    maxCalories: Int,
    dailyPlans: Map<DayOfWeek, List<RealMealSlot>>,
    onAddMealRecipe: (DayOfWeek, MealType) -> Unit,
    onDeleteMealRecipe: (DayOfWeek, MealType, Recipe) -> Unit,
    onRecipeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onSave: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
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

    // --- Discard Confirmation Dialog ---
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (isEditMode) stringResource(R.string.dialog_discard_changes_title) else stringResource(R.string.dialog_discard_draft_title)) },
            text = {
                Text(
                    if (isEditMode) stringResource(R.string.dialog_discard_changes_msg)
                    else stringResource(R.string.dialog_discard_draft_msg)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBackClick()
                    }
                ) {
                    Text(stringResource(R.string.btn_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.btn_continue_editing))
                }
            }
        )
    }

    // --- Template Name Input Dialog ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(if (isEditMode) stringResource(R.string.dialog_edit_template_name_title) else stringResource(R.string.dialog_set_template_name_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_template_name_prompt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = planName,
                        onValueChange = onPlanNameChange,
                        label = { Text(stringResource(R.string.label_template_name)) },
                        placeholder = { Text(stringResource(R.string.placeholder_template_name_hint)) },
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
                TextButton(onClick = {
                    onPlanNameChange("")
                    showNameDialog = false
                }) {
                    Text(stringResource(R.string.dialog_cancel))
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
                        if (hasEnteredData || selectedCategory != R.string.null_string || planName.isNotBlank()) {
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
                    text = if (isEditMode) stringResource(R.string.title_edit_template) else stringResource(R.string.title_create_template),
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
                    enabled = selectedCategory != R.string.null_string,
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
            selectedValue = stringResource(selectedCategory),
            onOptionSelected = onCategorySelected,
            options = PlanCategory.catList,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // --- Public / Private Toggle Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_make_plan_public),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isPublic) stringResource(R.string.msg_visible_to_community) else stringResource(R.string.msg_only_visible_to_you),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isPublic,
                onCheckedChange = onPublicChange
            )
        }

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