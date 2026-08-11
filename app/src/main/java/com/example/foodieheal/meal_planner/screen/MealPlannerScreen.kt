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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun MealPlannerScreen(viewModel: MealPlannerViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val isNetworkAvailable = viewModel.isNetworkAvailable

    val activeDailyPlan = viewModel.mealPlansCache[selectedDate]

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

    // Dynamic state computation layer tracking current active calendar totals safely above the pager
    val totalCaloriesForSelectedDate = remember(activeDailyPlan) {
        activeDailyPlan?.meals
            ?.flatMap { it.recipes }
            ?.sumOf { it.calories } ?: 0
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPasteDatePicker by remember { mutableStateOf(false) }
    var showWeeklyPasteDatePicker by remember { mutableStateOf(false) }

    val snackBarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { message ->
            snackBarHostState.showSnackbar(message = message)
        }
    }

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
            titleText = stringResource(R.string.dialog_title_select_date_view),
            onDateSelected = { newDate ->
                selectedDate = newDate
                currentWeekStart = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showPasteDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate.plusDays(1),
            titleText = stringResource(R.string.dialog_title_choose_paste_date),
            onDateSelected = { targetDate ->
                activeDailyPlan?.let { sourcePlan ->
                    viewModel.copyDailyPlanToDate(sourcePlan, targetDate)
                }
                showPasteDatePicker = false
            },
            onDismiss = { showPasteDatePicker = false }
        )
    }

    if (showWeeklyPasteDatePicker) {
        MealDatePickerDialog(
            initialDate = currentWeekStart.plusWeeks(1),
            titleText = stringResource(R.string.dialog_title_choose_weekly_paste_date),
            onDateSelected = { targetDate ->
                val targetWeekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                viewModel.copyWeeklyPlanToDate(weekDays, targetWeekStart)
                showWeeklyPasteDatePicker = false
            },
            onDismiss = { showWeeklyPasteDatePicker = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.meal_planner),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
                            contentDescription = stringResource(R.string.desc_calendar_back),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = stringResource(R.string.desc_calendar_icon),
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onBackground
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
                            contentDescription = stringResource(R.string.desc_calendar_forward),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { showWeeklyPasteDatePicker = true },
                        enabled = isNetworkAvailable
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_repeat),
                            contentDescription = stringResource(R.string.desc_copy_daily_plan),
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            WeeklyDateCardRow(
                weekDays = weekDays,
                selectedDate = selectedDate,
                onDateSelected = { newDate -> selectedDate = newDate },
                modifier = Modifier
            )

            Spacer(Modifier.height(12.dp))

            if (isNetworkAvailable) {
                CalorieProgressBar(
                    currentCalories = totalCaloriesForSelectedDate,
                    maxCalories = 1800,
                    onNavigateToProfile = { /* TODO : wait for profile */ }
                )
                Spacer(Modifier.height(12.dp))
            }
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val pageDate = remember(page) { anchorDate.plusDays(page.toLong()) }

                LaunchedEffect(pageDate) {
                    if (isNetworkAvailable) {
                        viewModel.loadPlanForDate(pageDate)
                    }
                }

                val dailyPlanForThisPage = viewModel.mealPlansCache[pageDate]

                // Formatted dynamic date string linked to this specific page's date context
                val dailyBannerText = remember(pageDate) {
                    val dayName = pageDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val dayOfMonth = pageDate.dayOfMonth
                    val monthName = pageDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    "$dayName, $dayOfMonth $monthName"
                }

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

                val pageScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState)
                ) {
                    if (!isNetworkAvailable) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.wifi_off),
                                contentDescription = stringResource(R.string.desc_no_network),
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(70.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.title_no_internet),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.desc_connect_internet_prompt),
                                fontSize = 15.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            Text(
                                text = dailyBannerText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 8.dp, start = 16.dp)
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                                elevation = CardDefaults.cardElevation(100.dp),
                                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                            ) {
                                Column {
                                    MealSection(
                                        title = stringResource(R.string.breakfast),
                                        recipes = breakfastRecipes,
                                        onAddClick = {
                                            /*
                                            val sampleRecipe = Recipe(
                                                recipe_id = "R999",
                                                recipeName = "Oatmeal",
                                                calories = 350,
                                                time = 10,
                                                recipeImage = R.drawable.ic_breakfast,
                                                recipeDescription = "A quick and healthy bowl of warm oats.",
                                                budget = 2.50,
                                                skillLevel = 1,
                                                recipeStep = "Cook oats in milk or water."
                                            )
                                            viewModel.addRecipeToMeal(pageDate, MealType.BREAKFAST, sampleRecipe)
                                            */
                                        },
                                        onDeleteClick = { recipe ->
                                            viewModel.deleteRecipeFromMeal(pageDate, MealType.BREAKFAST, recipe)
                                        }
                                    )

                                    MealSection(
                                        title = stringResource(R.string.lunch),
                                        recipes = lunchRecipes,
                                        onAddClick = {
                                            /*
                                            val sampleRecipe = Recipe(
                                                recipe_id = "R011",
                                                recipeName = "Chicken Wrap",
                                                calories = 340,
                                                time = 15,
                                                recipeImage = R.drawable.ic_lunch,
                                                recipeDescription = "A delicious wrap filled with grilled chicken.",
                                                budget = 5.80,
                                                skillLevel = 1,
                                                recipeStep = "Grill chicken, fill tortilla, roll."
                                            )
                                            viewModel.addRecipeToMeal(pageDate, MealType.LUNCH, sampleRecipe)
                                            */
                                        },
                                        onDeleteClick = { recipe ->
                                            viewModel.deleteRecipeFromMeal(pageDate, MealType.LUNCH, recipe)
                                        }
                                    )

                                    MealSection(
                                        title = stringResource(R.string.dinner),
                                        recipes = dinnerRecipes,
                                        onAddClick = {
                                            /*
                                            val sampleRecipe = Recipe(
                                                recipe_id = "R015",
                                                recipeName = "Thai Green Curry",
                                                calories = 550,
                                                time = 45,
                                                recipeImage = R.drawable.ic_dinner,
                                                recipeDescription = "A fragrant Thai green curry.",
                                                budget = 10.50,
                                                skillLevel = 5,
                                                recipeStep = "Fry curry paste, add coconut milk, simmer."
                                            )
                                            viewModel.addRecipeToMeal(pageDate, MealType.DINNER, sampleRecipe)
                                            */
                                        },
                                        onDeleteClick = { recipe ->
                                            viewModel.deleteRecipeFromMeal(pageDate, MealType.DINNER, recipe)
                                        }
                                    )

                                    MealSection(
                                        title = stringResource(R.string.snack),
                                        recipes = snackRecipes,
                                        onAddClick = {
                                            /*
                                            val sampleRecipe = Recipe(
                                                recipe_id = "R002",
                                                recipeName = "Fluffy Buttermilk Pancakes",
                                                calories = 200,
                                                time = 15,
                                                recipeImage = R.drawable.ic_snack,
                                                recipeDescription = "Golden, diner-style pancakes.",
                                                budget = 4.00,
                                                skillLevel = 2,
                                                recipeStep = "Mix ingredients, cook on griddle."
                                            )
                                            viewModel.addRecipeToMeal(pageDate, MealType.SNACK, sampleRecipe)
                                            */
                                        },
                                        onDeleteClick = { recipe ->
                                            viewModel.deleteRecipeFromMeal(pageDate, MealType.SNACK, recipe)
                                        }
                                    )

                                    val isPlanEmpty = breakfastRecipes.isEmpty() &&
                                            lunchRecipes.isEmpty() &&
                                            dinnerRecipes.isEmpty() &&
                                            snackRecipes.isEmpty()

                                    if (!isPlanEmpty) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(70.dp)
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.tertiary,
                                                    RoundedCornerShape(20.dp)
                                                ),
                                            onClick = { showPasteDatePicker = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.btn_copy_todays_plan),
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onTertiary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}