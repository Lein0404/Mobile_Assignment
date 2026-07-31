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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.Recipe
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.model.MealType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddRecipeToPlanScreen(
    recipe: Recipe,
    viewModel: MealPlannerViewModel,
    onExecutionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showSuccessDialog by remember { mutableStateOf(false) } // 🌟 Dialog trigger state variable

    LaunchedEffect(selectedDate) {
        viewModel.loadPlanForDate(selectedDate)
    }

    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
    }

    val weekDays = viewModel.getCurrentWeekDays(currentWeekStart)
    val weekEndDate = weekDays.last()
    val headerText = "${weekDays.first().dayOfMonth} - ${weekEndDate.dayOfMonth} ${weekEndDate.month.getDisplayName(
        TextStyle.SHORT, Locale.getDefault())} ${weekEndDate.year}"

    var showDatePicker by remember { mutableStateOf(false) }
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { message ->
            snackBarHostState.showSnackbar(message = message)
        }
    }

    val selectedDailyPlan = viewModel.selectedDailyPlan

    val totalDailyCalories = remember(selectedDailyPlan) {
        selectedDailyPlan?.meals
            ?.flatMap { meal -> meal.recipes }
            ?.sumOf { r: Recipe -> r.calories } ?: 0
    }

    // STATE TRACKER MAP: Maps unique (Date String -> List of selected MealTypes) across pages
    val selectedSlots = remember { mutableStateMapOf<String, Set<MealType>>() }

    val anchorDate = remember { LocalDate.now().minusYears(1) }
    val initialPage = remember { ChronoUnit.DAYS.between(anchorDate, LocalDate.now()).toInt() }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 730 }
    )

    // Sync Pager Swipes → State updates (Fixed race loop by using settledPage)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetDate = anchorDate.plusDays(page.toLong())
            if (targetDate != selectedDate) {
                selectedDate = targetDate
                currentWeekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            }
        }
    }

    // Sync Programmatic Dates → Pager layout shifts
    LaunchedEffect(selectedDate) {
        val targetPage = ChronoUnit.DAYS.between(anchorDate, selectedDate).toInt()
        if (pagerState.currentPage != targetPage && targetPage in 0 until 730) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate,
            titleText = "Select Target Date Window",
            onDateSelected = { newDate ->
                selectedDate = newDate
                currentWeekStart = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Helper functions to handle ticking states cleanly inside our components
    val dateKey = selectedDate.toString()
    val activeSelectionsForToday = selectedSlots[dateKey] ?: emptySet()

    fun toggleSlotSelection(mealType: MealType) {
        val currentSet = selectedSlots[dateKey] ?: emptySet()
        val updatedSet = if (currentSet.contains(mealType)) {
            currentSet - mealType
        } else {
            currentSet + mealType
        }
        if (updatedSet.isEmpty()) {
            selectedSlots.remove(dateKey)
        } else {
            selectedSlots[dateKey] = updatedSet
        }
    }

    // Calculate total checked slots item count globally across all checked pages
    val totalSelectionsCount = selectedSlots.values.sumOf { it.size }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        bottomBar = {
            // BOTTOM PERSISTENT TERMINAL INTERACTION LAYER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        // Batch write tasks systematically across recorded dates
                        selectedSlots.forEach { (savedDateStr, mealTypesList) ->
                            val targetDateParsed = LocalDate.parse(savedDateStr)
                            mealTypesList.forEach { mealType ->
                                viewModel.addRecipeToMeal(targetDateParsed, mealType, recipe)
                            }
                        }
                        showSuccessDialog = true // 🌟 Show the success dialog on successful execution completion
                    },
                    enabled = totalSelectionsCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (totalSelectionsCount > 0) "Add ($totalSelectionsCount)" else "Select Slots Below",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Simplified Header Layout without copy/paste actions
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Adding: ${recipe.recipeName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                selectedDate = selectedDate.minusWeeks(1)
                                currentWeekStart = currentWeekStart.minusWeeks(1)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Calendar Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = "Calendar",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                selectedDate = selectedDate.plusWeeks(1)
                                currentWeekStart = currentWeekStart.plusWeeks(1)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = "Calendar Forward",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                WeeklyDateCardRow(
                    weekDays = weekDays,
                    selectedDate = selectedDate,
                    onDateSelected = { newDate -> selectedDate = newDate },
                    modifier = Modifier
                )

                Spacer(Modifier.height(25.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val isLoading = viewModel.isLoading

                    if (isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(40.dp))
                            CircularProgressIndicator(modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading plans...", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(200.dp))
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CalorieProgressBar(
                                currentCalories = totalDailyCalories,
                                maxCalories = 1800,
                                onNavigateToProfile = {/*TODO*/},
                            )

                            Spacer(Modifier.height(20.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                            ) {
                                val breakfastRecipes = selectedDailyPlan?.meals
                                    ?.filter { it.mealType == MealType.BREAKFAST }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                val lunchRecipes = selectedDailyPlan?.meals
                                    ?.filter { it.mealType == MealType.LUNCH }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                val dinnerRecipes = selectedDailyPlan?.meals
                                    ?.filter { it.mealType == MealType.DINNER }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                val snackRecipes = selectedDailyPlan?.meals
                                    ?.filter { it.mealType == MealType.SNACK }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                Column {
                                    MealSection(
                                        title = "Breakfast",
                                        recipes = breakfastRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForToday.contains(MealType.BREAKFAST),
                                        onSelectionChange = { toggleSlotSelection(MealType.BREAKFAST) },
                                        onAddClick = {},//empty bcs ui do not appear
                                        onDeleteClick = {}
                                    )

                                    MealSection(
                                        title = "Lunch",
                                        recipes = lunchRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForToday.contains(MealType.LUNCH),
                                        onSelectionChange = { toggleSlotSelection(MealType.LUNCH) },
                                        onAddClick = {},
                                        onDeleteClick = {}
                                    )

                                    MealSection(
                                        title = "Dinner",
                                        recipes = dinnerRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForToday.contains(MealType.DINNER),
                                        onSelectionChange = { toggleSlotSelection(MealType.DINNER) },
                                        onAddClick = {},
                                        onDeleteClick = {}
                                    )

                                    MealSection(
                                        title = "Snack",
                                        recipes = snackRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForToday.contains(MealType.SNACK),
                                        onSelectionChange = { toggleSlotSelection(MealType.SNACK) },
                                        onAddClick = {},
                                        onDeleteClick = {}
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🌟 Embed dialog state inside screen boundary to draw correctly atop Scaffold framework layers
        SuccessDialog(
            showDialog = showSuccessDialog,
            onDismiss = {
                showSuccessDialog = false
                onExecutionComplete() // 🌟 Cleanly trigger screen navigation back after dismissal click completes
            }
        )
    }
}

@Composable
fun SuccessDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.check),
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(60.dp)
                )
            },
            title = {
                Text(
                    text = "Success!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            text = {
                Text(
                    text = "Your meals have been successfully added to your planner.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}