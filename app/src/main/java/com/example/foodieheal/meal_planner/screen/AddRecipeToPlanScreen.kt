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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showSuccessDialog by remember { mutableStateOf(false) }

    val isNetworkAvailable = viewModel.isNetworkAvailable

    LaunchedEffect(selectedDate) {
        if (isNetworkAvailable) {
            viewModel.loadPlanForDate(selectedDate)
        }
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
            titleText = stringResource(R.string.daily_date_picker_dialog) ,
            onDateSelected = { newDate ->
                selectedDate = newDate
                currentWeekStart = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    val totalSelectionsCount = if (isNetworkAvailable) selectedSlots.values.sumOf { it.size } else 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        selectedSlots.forEach { (savedDateStr, mealTypesList) ->
                            val targetDateParsed = LocalDate.parse(savedDateStr)
                            mealTypesList.forEach { mealType ->
                                viewModel.addRecipeToMeal(targetDateParsed, mealType, recipe)
                            }
                        }
                        showSuccessDialog = true
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
                        text = when {
                            !isNetworkAvailable -> stringResource(R.string.offline)
                            totalSelectionsCount > 0 -> stringResource(
                                R.string.add_slots,
                                totalSelectionsCount
                            )
                            else -> stringResource(R.string.select_slots_below)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = stringResource(R.string.adding_recipe, recipe.recipeName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
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
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.previous_week),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = stringResource(R.string.calendar),
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
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = stringResource(R.string.next_week),
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
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1 // 🌟 Preload adjacent frames
            ) { page ->
                // 🌟 FIX: Calculate localized configuration for each specific page block inside the pager loop
                val pageDate = remember(page) { anchorDate.plusDays(page.toLong()) }
                val pageDateStr = pageDate.toString()

                LaunchedEffect(pageDate) {
                    if (isNetworkAvailable) {
                        viewModel.loadPlanForDate(pageDate)
                    }
                }

                // 🌟 FIX: Pull layout state safely from the dictionary cache map instead of deleted global variable
                val dailyPlanForThisPage = viewModel.mealPlansCache[pageDate]
                val activeSelectionsForThisPage = selectedSlots[pageDateStr] ?: emptySet()

                val totalCaloriesForPage = remember(dailyPlanForThisPage, isNetworkAvailable) {
                    if (!isNetworkAvailable) 0 else {
                        dailyPlanForThisPage?.meals
                            ?.flatMap { meal -> meal.recipes }
                            ?.sumOf { r: Recipe -> r.calories } ?: 0
                    }
                }

                val pageScrollState = rememberScrollState()

                // 🌟 SCROLL FIX: Each page canvas handles its own vertical scroll independently
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState)
                ) {
                    if (!isNetworkAvailable) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.wifi_off),
                                contentDescription = stringResource(R.string.no_network),
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(70.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_internet_connection),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.connect_to_internet_message),
                                fontSize = 15.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CalorieProgressBar(
                                currentCalories = totalCaloriesForPage,
                                maxCalories = 1800,
                                onNavigateToProfile = {/*TODO*/},
                            )

                            Spacer(Modifier.height(20.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                            ) {
                                val breakfastRecipes = dailyPlanForThisPage?.meals
                                    ?.filter { it.mealType == MealType.BREAKFAST }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                val lunchRecipes = dailyPlanForThisPage?.meals
                                    ?.filter { it.mealType == MealType.LUNCH }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                val dinnerRecipes = dailyPlanForThisPage?.meals
                                    ?.filter { it.mealType == MealType.DINNER }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                val snackRecipes = dailyPlanForThisPage?.meals
                                    ?.filter { it.mealType == MealType.SNACK }
                                    ?.flatMap { meal -> meal.recipes } ?: emptyList()

                                Column {
                                    MealSection(
                                        title = stringResource(R.string.breakfast),
                                        recipes = breakfastRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForThisPage.contains(MealType.BREAKFAST),
                                        onSelectionChange = {
                                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.BREAKFAST)) currentSet - MealType.BREAKFAST else currentSet + MealType.BREAKFAST
                                        },
                                        onAddClick = {},
                                        onDeleteClick = {}
                                    )

                                    MealSection(
                                        title = stringResource(R.string.lunch),
                                        recipes = lunchRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForThisPage.contains(MealType.LUNCH),
                                        onSelectionChange = {
                                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.LUNCH)) currentSet - MealType.LUNCH else currentSet + MealType.LUNCH
                                        },
                                        onAddClick = {},
                                        onDeleteClick = {}
                                    )

                                    MealSection(
                                        title = stringResource(R.string.dinner),
                                        recipes = dinnerRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForThisPage.contains(MealType.DINNER),
                                        onSelectionChange = {
                                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.DINNER)) currentSet - MealType.DINNER else currentSet + MealType.DINNER
                                        },
                                        onAddClick = {},
                                        onDeleteClick = {}
                                    )

                                    MealSection(
                                        title = stringResource(R.string.snack),
                                        recipes = snackRecipes,
                                        isSelectionMode = true,
                                        isSelected = activeSelectionsForThisPage.contains(MealType.SNACK),
                                        onSelectionChange = {
                                            val currentSet = selectedSlots[pageDateStr] ?: emptySet()
                                            selectedSlots[pageDateStr] = if (currentSet.contains(MealType.SNACK)) currentSet - MealType.SNACK else currentSet + MealType.SNACK
                                        },
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

        SuccessDialog(
            showDialog = showSuccessDialog,
            onDismiss = {
                showSuccessDialog = false
                onExecutionComplete()
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
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(70.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.success),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.meals_added_successfully),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.ok),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}