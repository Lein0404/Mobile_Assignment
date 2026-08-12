package com.example.foodieheal.meal_planner.screen

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.R
import com.example.foodieheal.Recipe
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.viewmodel.AuthViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealPlannerScreen(
    mealPlannerViewModel: MealPlannerViewModel,
    authViewModel: AuthViewModel,
    onNavigateToProfile:()-> Unit,
    onRecipeDetails: (String) -> Unit
) {
    // 🌟 FIX: Fetch the local environment layout context safely inside a Composable boundary
    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val isNetworkAvailable = mealPlannerViewModel.isNetworkAvailable

    val activeDailyPlan = mealPlannerViewModel.mealPlansCache[selectedDate]

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
    val headerText = "${weekDays.first().dayOfMonth} - ${weekEndDate.dayOfMonth} ${weekEndDate.month.getDisplayName(
        TextStyle.SHORT, Locale.getDefault())} ${weekEndDate.year}"

    val totalCaloriesForSelectedDate = remember(activeDailyPlan) {
        activeDailyPlan?.meals
            ?.flatMap { it.recipes }
            ?.sumOf { it.calories } ?: 0
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPasteDatePicker by remember { mutableStateOf(false) }
    var showWeeklyPasteDatePicker by remember { mutableStateOf(false) }

    val deepLinkDays = mealPlannerViewModel.deepLinkSourceDays

    LaunchedEffect(deepLinkDays) {
        if (deepLinkDays != null) {
            // 1. 🌟 ACTUALLY ENABLE THE DIALOG SO COMPOSE WILL DRAW IT!
            showWeeklyPasteDatePicker = true

            // 2. Consume the link trigger context immediately to prevent the double prompt trap
            mealPlannerViewModel.clearDeepLinkState()
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = true) {
        mealPlannerViewModel.uiEvent.collect { message ->
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
                    mealPlannerViewModel.copyDailyPlanToDate(sourcePlan, targetDate)
                }
                showPasteDatePicker = false
            },
            onDismiss = { showPasteDatePicker = false }
        )
    }

    if (showWeeklyPasteDatePicker) {
        MealDatePickerDialog(
            initialDate = currentWeekStart.plusWeeks(1),
            titleText = stringResource(R.string.dialog_title_choose_weekly_repeat_date),
            onDateSelected = { targetDate ->
                val targetWeekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

                // 🌟 FIX: Use the deep link days if available, otherwise fallback to local visible week days
                val daysToCopy = mealPlannerViewModel.deepLinkSourceDays ?: weekDays
                mealPlannerViewModel.copyWeeklyPlanToDate(daysToCopy, targetWeekStart)

                showWeeklyPasteDatePicker = false
                mealPlannerViewModel.clearDeepLinkState() // Clear buffer data
            },
            onDismiss = {
                showWeeklyPasteDatePicker = false
                mealPlannerViewModel.clearDeepLinkState()
            }
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
                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = { showWeeklyPasteDatePicker = true },
                        enabled = isNetworkAvailable
                    ){
                        Icon(
                            painter = painterResource(R.drawable.ic_repeat),
                            contentDescription = stringResource(R.string.desc_copy_daily_plan),
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareUrl = mealPlannerViewModel.generateShareLink(currentWeekStart)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareUrl)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Weekly Meal Plan")
                            context.startActivity(shareIntent)
                        },
                        enabled = isNetworkAvailable
                    ){
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = stringResource(R.string.share),
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
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
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.desc_calendar_back),
                        modifier = Modifier
                            .size(35.dp)
                            .clickable {
                                selectedDate = selectedDate.minusWeeks(1)
                                currentWeekStart = currentWeekStart.minusWeeks(1)
                            },
                        tint = MaterialTheme.colorScheme.onBackground
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = stringResource(R.string.desc_calendar_icon),
                        modifier = Modifier
                            .size(35.dp)
                            .clickable { showDatePicker = true },
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = stringResource(R.string.desc_calendar_forward),
                        modifier = Modifier
                            .size(35.dp)
                            .clickable {
                                selectedDate = selectedDate.plusWeeks(1)
                                currentWeekStart = currentWeekStart.plusWeeks(1)
                            },
                        tint = MaterialTheme.colorScheme.onBackground
                    )
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
                val currentUser = authViewModel.currentUser
                val maxCalories = calculateSuggestedDailyCalories(currentUser)

                CalorieProgressBar(
                    currentCalories = totalCaloriesForSelectedDate,
                    maxCalories = maxCalories,
                    onNavigateToProfile = { onNavigateToProfile() },
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
                        mealPlannerViewModel.loadPlanForDate(pageDate)
                    }
                }

                val dailyPlanForThisPage = mealPlannerViewModel.mealPlansCache[pageDate]

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
                            Column {
                                MealSection(
                                    title = stringResource(R.string.breakfast),
                                    recipes = breakfastRecipes,
                                    onAddClick = {//TODO a screen that can choose recipe
                                        val sampleRecipe = Recipe(
                                            recipe_id = "R001",
                                            recipeName = "Oatmeal",
                                            calories = 350,
                                            time = 10,
                                            recipeDescription = "A quick and healthy bowl of warm oats.",
                                            recipeStep = "Cook oats in milk or water.",
                                            author_id = "",
                                            recipeCourse = "",
                                            cookingSkill = "",
                                            estimatedBudget = "",
                                            recipeImageUrl = "",
                                            ingredients = emptyList()
                                        )
                                        mealPlannerViewModel.addRecipeToMeal(pageDate, MealType.BREAKFAST, sampleRecipe)
                                    },
                                    onDeleteClick = { recipe ->
                                        mealPlannerViewModel.deleteRecipeFromMeal(pageDate, MealType.BREAKFAST, recipe)
                                    },
                                    onRecipeDetails = { recipeId -> onRecipeDetails(recipeId)},
                                )

                                MealSection(
                                    title = stringResource(R.string.lunch),
                                    recipes = lunchRecipes,
                                    onAddClick = {
                                        val sampleRecipe = Recipe(
                                            recipe_id = "R002",
                                            recipeName = "Oatmeal",
                                            calories = 350,
                                            time = 10,
                                            recipeDescription = "A quick and healthy bowl of warm oats.",
                                            recipeStep = "Cook oats in milk or water.",
                                            author_id = "",
                                            recipeCourse = "",
                                            cookingSkill = "",
                                            estimatedBudget = "",
                                            recipeImageUrl = "",
                                            ingredients = emptyList()
                                        )
                                        mealPlannerViewModel.addRecipeToMeal(pageDate, MealType.LUNCH, sampleRecipe)
                                    },
                                    onDeleteClick = { recipe ->
                                        mealPlannerViewModel.deleteRecipeFromMeal(pageDate, MealType.LUNCH, recipe)
                                    },
                                    onRecipeDetails = { recipeId -> onRecipeDetails(recipeId)},
                                )

                                MealSection(
                                    title = stringResource(R.string.dinner),
                                    recipes = dinnerRecipes,
                                    onAddClick = {
                                        val sampleRecipe = Recipe(
                                            recipe_id = "R003",
                                            recipeName = "Oatmeal",
                                            calories = 350,
                                            time = 10,
                                            recipeDescription = "A quick and healthy bowl of warm oats.",
                                            recipeStep = "Cook oats in milk or water.",
                                            author_id = "",
                                            recipeCourse = "",
                                            cookingSkill = "",
                                            estimatedBudget = "",
                                            recipeImageUrl = "",
                                            ingredients = emptyList()
                                        )
                                        mealPlannerViewModel.addRecipeToMeal(pageDate, MealType.DINNER, sampleRecipe)
                                    },
                                    onDeleteClick = { recipe ->
                                        mealPlannerViewModel.deleteRecipeFromMeal(pageDate, MealType.DINNER, recipe)
                                    },
                                    onRecipeDetails = { recipeId -> onRecipeDetails(recipeId)},
                                )

                                MealSection(
                                    title = stringResource(R.string.snack),
                                    recipes = snackRecipes,
                                    onAddClick = {
                                        val sampleRecipe = Recipe(
                                            recipe_id = "R004",
                                            recipeName = "Oatmeal",
                                            calories = 350,
                                            time = 10,
                                            recipeDescription = "A quick and healthy bowl of warm oats.",
                                            recipeStep = "Cook oats in milk or water.",
                                            author_id = "",
                                            recipeCourse = "",
                                            cookingSkill = "",
                                            estimatedBudget = "",
                                            recipeImageUrl = "",
                                            ingredients = emptyList()
                                        )
                                        mealPlannerViewModel.addRecipeToMeal(pageDate, MealType.SNACK, sampleRecipe)
                                    },
                                    onDeleteClick = { recipe ->
                                        mealPlannerViewModel.deleteRecipeFromMeal(pageDate, MealType.SNACK, recipe)
                                    },
                                    onRecipeDetails = { recipeId -> onRecipeDetails(recipeId)},
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
            Spacer(Modifier.height(30.dp)            )
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}