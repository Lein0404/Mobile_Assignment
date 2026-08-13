package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.model.Recipe
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddRecipeToPlanScreen(
    recipe: Recipe?,
    mealPlannerViewModel: MealPlannerViewModel,
    authViewModel: AuthViewModel,
    onExecutionComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit
) {
    if (recipe == null) {
        Box(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val isNetworkAvailable = mealPlannerViewModel.isNetworkAvailable
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedDate) {
        if (isNetworkAvailable) {
            mealPlannerViewModel.loadPlanForDate(selectedDate)
        }
    }

    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
    }

    val weekDays = mealPlannerViewModel.getCurrentWeekDays(currentWeekStart)
    val weekEndDate = weekDays.last()
    val headerText = "${weekDays.first().dayOfMonth} - ${weekEndDate.dayOfMonth} ${
        weekEndDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } ${weekEndDate.year}"

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        mealPlannerViewModel.uiEvent.collect { message ->
            snackBarHostState.showSnackbar(message = message)
        }
    }

    val selectedSlots = remember { mutableStateMapOf<String, Set<MealType>>() }
    val anchorDate = remember { LocalDate.now().minusYears(1) }
    val initialPage = remember { ChronoUnit.DAYS.between(anchorDate, LocalDate.now()).toInt() }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 730 }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetDate = anchorDate.plusDays(page.toLong())
            if (targetDate != selectedDate) {
                selectedDate = targetDate
                currentWeekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            }
        }
    }

    LaunchedEffect(selectedDate) {
        val targetPage = ChronoUnit.DAYS.between(anchorDate, selectedDate).toInt()
        if (pagerState.currentPage != targetPage && targetPage in 0 until 730) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate,
            titleText = stringResource(R.string.daily_date_picker_dialog),
            onDateSelected = { newDate ->
                selectedDate = newDate
                currentWeekStart = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    val totalSelectionsCount = if (isNetworkAvailable) selectedSlots.values.sumOf { it.size } else 0
    val successMessage = stringResource(R.string.meals_added_successfully)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        bottomBar = {
            AddRecipeBottomActionBar(
                isNetworkAvailable = isNetworkAvailable,
                totalSelectionsCount = totalSelectionsCount,
                onCancelClick = {
                    selectedSlots.clear()
                    onExecutionComplete()
                },
                onConfirmClick = {
                    coroutineScope.launch {
                        selectedSlots.forEach { (savedDateStr, mealTypesList) ->
                            val targetDateParsed = LocalDate.parse(savedDateStr)
                            mealTypesList.forEach { mealType ->
                                mealPlannerViewModel.addRecipeToMeal(targetDateParsed, mealType, recipe)
                            }
                        }
                        selectedSlots.clear()
//                        snackBarHostState.showSnackbar(message = successMessage)
                        onExecutionComplete()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CalendarControls(
                headerText = headerText,
                weekDays = weekDays,
                selectedDate = selectedDate,
                onDateBackward = {
                    selectedDate = selectedDate.minusWeeks(1)
                    currentWeekStart = currentWeekStart.minusWeeks(1)
                },
                onDateForward = {
                    selectedDate = selectedDate.plusWeeks(1)
                    currentWeekStart = currentWeekStart.plusWeeks(1)
                },
                onCalendarClick = { showDatePicker = true },
                onDateSelected = { newDate -> selectedDate = newDate },
                topContent = {
                    Text(
                        text = stringResource(R.string.adding_recipe, recipe.recipeName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            )

            Spacer(Modifier.height(25.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val pageDate = remember(page) { anchorDate.plusDays(page.toLong()) }

                LaunchedEffect(pageDate) {
                    if (isNetworkAvailable) {
                        mealPlannerViewModel.loadPlanForDate(pageDate)
                    }
                }

                val dailyPlanForThisPage = mealPlannerViewModel.mealPlansCache[pageDate]

                AddRecipePageContent(
                    pageDate = pageDate,
                    recipe = recipe,
                    isNetworkAvailable = isNetworkAvailable,
                    dailyPlan = dailyPlanForThisPage,
                    selectedSlots = selectedSlots,
                    currentUser = authViewModel.currentUser,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
fun AddRecipeBottomActionBar(
    isNetworkAvailable: Boolean,
    totalSelectionsCount: Int,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(android.R.string.cancel),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onConfirmClick,
            enabled = totalSelectionsCount > 0,
            modifier = Modifier
                .weight(1.5f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = when {
                    !isNetworkAvailable -> stringResource(R.string.offline)
                    totalSelectionsCount > 0 -> stringResource(R.string.add_slots, totalSelectionsCount)
                    else -> stringResource(R.string.select_slots_below)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddRecipePageContent(
    pageDate: LocalDate,
    recipe: Recipe,
    isNetworkAvailable: Boolean,
    dailyPlan: DailyPlan?, // <-- Fix: Using shared package DailyPlan model explicitly
    selectedSlots: SnapshotStateMap<String, Set<MealType>>,
    currentUser: com.example.foodieheal.model.User?,
    onNavigateToProfile: () -> Unit
) {
    val pageDateStr = remember(pageDate) { pageDate.toString() }
    val activeSelectionsForThisPage = selectedSlots[pageDateStr] ?: emptySet()
    val pageScrollState = rememberScrollState()

    if (!isNetworkAvailable) {
        OfflinePlaceholder()
    } else {
        val totalCaloriesForSelectedDate = remember(dailyPlan, activeSelectionsForThisPage) {
            val existingCalories = dailyPlan?.meals
                ?.flatMap { meal -> meal.recipes }
                ?.sumOf { r -> r.calories } ?: 0
            val pendingSelectionsCalories = activeSelectionsForThisPage.size * recipe.calories
            existingCalories + pendingSelectionsCalories
        }

        Column(modifier = Modifier.fillMaxSize()) {
            val maxCalories = calculateSuggestedDailyCalories(currentUser)

            CalorieProgressBar(
                currentCalories = totalCaloriesForSelectedDate,
                maxCalories = maxCalories,
                onNavigateToProfile = onNavigateToProfile,
            )

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val breakfastRecipes = remember(dailyPlan, activeSelectionsForThisPage) {
                    val baseline = dailyPlan?.meals?.filter { it.mealType == MealType.BREAKFAST }?.flatMap { it.recipes } ?: emptyList()
                    if (activeSelectionsForThisPage.contains(MealType.BREAKFAST)) baseline + recipe else baseline
                }

                val lunchRecipes = remember(dailyPlan, activeSelectionsForThisPage) {
                    val baseline = dailyPlan?.meals?.filter { it.mealType == MealType.LUNCH }?.flatMap { it.recipes } ?: emptyList()
                    if (activeSelectionsForThisPage.contains(MealType.LUNCH)) baseline + recipe else baseline
                }

                val dinnerRecipes = remember(dailyPlan, activeSelectionsForThisPage) {
                    val baseline = dailyPlan?.meals?.filter { it.mealType == MealType.DINNER }?.flatMap { it.recipes } ?: emptyList()
                    if (activeSelectionsForThisPage.contains(MealType.DINNER)) baseline + recipe else baseline
                }

                val snackRecipes = remember(dailyPlan, activeSelectionsForThisPage) {
                    val baseline = dailyPlan?.meals?.filter { it.mealType == MealType.SNACK }?.flatMap { it.recipes } ?: emptyList()
                    if (activeSelectionsForThisPage.contains(MealType.SNACK)) baseline + recipe else baseline
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState)
                ) {
                    MealSectionSlot(
                        title = stringResource(R.string.breakfast),
                        recipes = breakfastRecipes,
                        isSelected = activeSelectionsForThisPage.contains(MealType.BREAKFAST),
                        onSelectionToggle = {
                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.BREAKFAST)) currentSet - MealType.BREAKFAST else currentSet + MealType.BREAKFAST
                        }
                    )

                    MealSectionSlot(
                        title = stringResource(R.string.lunch),
                        recipes = lunchRecipes,
                        isSelected = activeSelectionsForThisPage.contains(MealType.LUNCH),
                        onSelectionToggle = {
                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.LUNCH)) currentSet - MealType.LUNCH else currentSet + MealType.LUNCH
                        }
                    )

                    MealSectionSlot(
                        title = stringResource(R.string.dinner),
                        recipes = dinnerRecipes,
                        isSelected = activeSelectionsForThisPage.contains(MealType.DINNER),
                        onSelectionToggle = {
                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.DINNER)) currentSet - MealType.DINNER else currentSet + MealType.DINNER
                        }
                    )

                    MealSectionSlot(
                        title = stringResource(R.string.snack),
                        recipes = snackRecipes,
                        isSelected = activeSelectionsForThisPage.contains(MealType.SNACK),
                        onSelectionToggle = {
                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.SNACK)) currentSet - MealType.SNACK else currentSet + MealType.SNACK
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun MealSectionSlot(
    title : String,
    recipes: List<Recipe>,
    isSelected: Boolean,
    onSelectionToggle: () -> Unit
) {
    // Reuses the public shared MealSection design architecture perfectly
    MealSection(
        title = title,
        recipes = recipes,
        isSelectionMode = true,
        isSelected = isSelected,
        onSelectionChange = { onSelectionToggle() },
        onAddClick = {},
        onDeleteClick = {}
    )
}