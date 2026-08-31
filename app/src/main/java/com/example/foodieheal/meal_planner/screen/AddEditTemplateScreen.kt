package com.example.foodieheal.meal_planner.screen

import android.widget.Toast
import es.dmoral.toasty.Toasty
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.viewModel.AddEditTemplateViewModel
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.ui.components.DropDownList
import com.example.foodieheal.ui.components.DropDownListFromResources
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
            val message = if (viewModel.isEditMode) {
                templateUpdatedMsg
            } else {
                templateCreatedMsg
            }
            Toasty.custom(context, message, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()

            onBackClick()
        }
    }

    AddEditTemplateScreen(
        modifier = modifier,
        isEditMode = viewModel.isEditMode,
        planName = uiState.planName,
        onPlanNameChange = viewModel::updatePlanName,
        planDescription = uiState.planDescription,
        onPlanDescriptionChange = viewModel::updatePlanDescription,
        selectedCategory = uiState.category?.displayNameRes?:R.string.null_string,
        onCategorySelected = viewModel::updateCategoryByResId,
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
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    planName: String,
    onPlanNameChange: (String) -> Unit,
    planDescription: String,
    onPlanDescriptionChange: (String) -> Unit,
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit,
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
) {
    val daysOfWeek = remember { DayOfWeek.entries.toList() }
    val mainTabs = listOf(stringResource(R.string.tab_details), stringResource(R.string.tab_weekly_plan))
    
    //  State for the main swipable tabs
    val mainPagerState = rememberPagerState(pageCount = { mainTabs.size })
    //  State for the inner days pager
    val dayPagerState = rememberPagerState(pageCount = { daysOfWeek.size })
    val scope = rememberCoroutineScope()

    val currentSelectedDay = daysOfWeek[dayPagerState.currentPage]
    val selectedDayTotalCalories = remember(dailyPlans, currentSelectedDay) {
        dailyPlans[currentSelectedDay]
            ?.flatMap { slot -> slot.recipes }
            ?.sumOf { recipe -> recipe.calories } ?: 0
    }

    // Dialog state management
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Check if user has added any recipes across all days
    val hasEnteredData = remember(dailyPlans) {
        dailyPlans.values.any { slotList -> slotList.any { it.recipes.isNotEmpty() } }
    }

    val isFormDirty = hasEnteredData || selectedCategory != R.string.null_string || planName.isNotBlank() || planDescription.isNotBlank()

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
                    Text(stringResource(R.string.button_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.btn_continue_editing))
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
                        if (isFormDirty) {
                            showDiscardDialog = true
                        } else {
                            onBackClick()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrowback),
                        contentDescription = stringResource(R.string.topapp_back),
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
                            // Switch to details if name missing
                            scope.launch { mainPagerState.animateScrollToPage(0) }
                        } else {
                            onSave()
                        }
                    },
                    enabled = selectedCategory != R.string.null_string,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // --- Navigation Tabs ---
        TabRow(
            selectedTabIndex = mainPagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            mainTabs.forEachIndexed { index, title ->
                Tab(
                    selected = mainPagerState.currentPage == index,
                    onClick = { scope.launch { mainPagerState.animateScrollToPage(index) } },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // --- Swipable Content Pager ---
        HorizontalPager(
            state = mainPagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true // Enable swiping between Details and Plan
        ) { pageIndex ->
            if (pageIndex == 0) {
                // --- TAB 1: TEMPLATE DETAILS ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Template Name Input
                    OutlinedTextField(
                        value = planName,
                        onValueChange = onPlanNameChange,
                        label = { Text(stringResource(R.string.label_template_name)) },
                        placeholder = { Text(stringResource(R.string.placeholder_template_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selection Dropdown
                    DropDownListFromResources(
                        labelId = R.string.category,
                        placeholderId = R.string.category,
                        selectedValue = stringResource(selectedCategory),
                        onOptionSelected = onCategorySelected,
                        options = PlanCategory.catList,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Template Description Input
                    OutlinedTextField(
                        value = planDescription,
                        onValueChange = onPlanDescriptionChange,
                        label = { Text(stringResource(R.string.label_template_description)) },
                        placeholder = { Text(stringResource(R.string.placeholder_template_description)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Public / Private Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_make_plan_public),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
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
                }
            } else {
                // --- TAB 2: WEEKLY PLAN ---
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(8.dp))

                    // Weekly Day Selection Row
                    WeeklyDayCardRow(
                        selectedDay = currentSelectedDay,
                        onDaySelected = { selectedDay ->
                            val targetPage = daysOfWeek.indexOf(selectedDay)
                            if (targetPage != -1) {
                                scope.launch {
                                    dayPagerState.animateScrollToPage(targetPage)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Horizontal Pager for Days
                    HorizontalPager(
                        state = dayPagerState,
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
            .padding(vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        MealType.entries.forEach { mealType ->
            val currentSlot = mealSlots.find { it.mealType == mealType }
            val recipesInSlot = currentSlot?.recipes ?: emptyList()

            val title = when (mealType) {
                MealType.BREAKFAST -> stringResource(R.string.meal_title_breakfast)
                MealType.LUNCH -> stringResource(R.string.meal_title_lunch)
                MealType.DINNER -> stringResource(R.string.meal_title_dinner)
                MealType.SNACK -> stringResource(R.string.meal_title_snack)
            }

            MealSection(
                title = title,
                recipes = recipesInSlot,
                onAddClick = { onAddMealRecipe(mealType) },
                onDeleteClick = { recipe -> onDeleteMealRecipe(mealType, recipe) },
                onRecipeDetails = onRecipeClick,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
    }
}
