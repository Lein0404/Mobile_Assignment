package com.example.foodieheal.meal_planner.screen

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.WeeklyCalendarState
import com.example.foodieheal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealPlannerScreen(
    mealPlannerViewModel: MealPlannerViewModel,
    authViewModel: AuthViewModel,
    onNavigateToProfile: () -> Unit,
    onRecipeDetails: (String) -> Unit,
    onAddMeal:(LocalDate, MealType)->Unit,
    onAddTemplateClick: () -> Unit,
    onPlanDetails:(String,Boolean)-> Unit,
    onEdit:(String)-> Unit
) {
    val context = LocalContext.current

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    var selectedDate by rememberSaveable(
        stateSaver = Saver(
            save = { it.toString() },
            restore = { LocalDate.parse(it) }
        )
    ) {
        mutableStateOf(LocalDate.now())
    }

    var currentWeekStart by rememberSaveable(
        stateSaver = Saver(
            save = { it.toString() },
            restore = { LocalDate.parse(it) }
        )
    ) {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
    }

    val isNetworkAvailable = mealPlannerViewModel.isNetworkAvailable
    val activeDailyPlan = mealPlannerViewModel.mealPlansCache[selectedDate]

    LaunchedEffect(selectedDate) {
        if (isNetworkAvailable) {
            mealPlannerViewModel.loadPlanForDate(selectedDate)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val dailyCopySuccessMessage = stringResource(R.string.daily_success_notify)
    val weeklyCopySuccessMessage = stringResource(R.string.weekly_success_notify)


    val weekDays = mealPlannerViewModel.getCurrentWeekDays(currentWeekStart)
    val weekEndDate = weekDays.last()
    val headerText = "${weekDays.first().dayOfMonth} - ${weekEndDate.dayOfMonth} ${
        weekEndDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } ${weekEndDate.year}"

    val calendarState = remember(currentWeekStart, weekDays, selectedDate, headerText) {
        WeeklyCalendarState(currentWeekStart, weekDays, selectedDate, headerText)
    }

    val totalCaloriesForSelectedDate = remember(activeDailyPlan) {
        activeDailyPlan?.meals?.flatMap { it.recipes }?.sumOf { it.calories } ?: 0
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPasteDatePicker by remember { mutableStateOf(false) }
    var showWeeklyPasteDatePicker by remember { mutableStateOf(false) }

    val deepLinkDays = mealPlannerViewModel.deepLinkSourceDays
    LaunchedEffect(deepLinkDays) {
        if (deepLinkDays != null) {
            showWeeklyPasteDatePicker = true
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
// Calculate initial page based on saved selectedDate instead of forcing LocalDate.now()
    val initialPage = remember(selectedDate) { ChronoUnit.DAYS.between(anchorDate, selectedDate).toInt() }
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
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(dailyCopySuccessMessage)
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
                val daysToCopy = mealPlannerViewModel.deepLinkSourceDays ?: weekDays
                mealPlannerViewModel.copyWeeklyPlanToDate(daysToCopy, targetWeekStart)
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(weeklyCopySuccessMessage)
                }
                showWeeklyPasteDatePicker = false
                mealPlannerViewModel.clearDeepLinkState()
            },
            onDismiss = {
                showWeeklyPasteDatePicker = false
                mealPlannerViewModel.clearDeepLinkState()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            MealPlannerHeader(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                isNetworkAvailable = isNetworkAvailable,
                onRepeatClick = { showWeeklyPasteDatePicker = true },
                onShareClick = {
                    val shareUrl = mealPlannerViewModel.generateShareLink(currentWeekStart)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Weekly Meal Plan"))
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> {
                MealPlannerContent(
                    modifier = Modifier.padding(innerPadding),
                    calendarState = calendarState,
                    isNetworkAvailable = isNetworkAvailable,
                    totalCalories = totalCaloriesForSelectedDate,
                    maxCalories = calculateSuggestedDailyCalories(authViewModel.currentUser),
                    mealPlansCache = mealPlannerViewModel.mealPlansCache,
                    pagerState = pagerState,
                    anchorDate = anchorDate,
                    onCalendarClick = { showDatePicker = true },
                    onDateSelected = { newDate -> selectedDate = newDate },
                    onDateShiftBackward = {
                        selectedDate = selectedDate.minusWeeks(1)
                        currentWeekStart = currentWeekStart.minusWeeks(1)
                    },
                    onDateShiftForward = {
                        selectedDate = selectedDate.plusWeeks(1)
                        currentWeekStart = currentWeekStart.plusWeeks(1)
                    },
                    onLoadPlanForDate = { date ->
                        if (isNetworkAvailable) mealPlannerViewModel.loadPlanForDate(date)
                    },
                    onAddMealRecipe = { date, type -> onAddMeal(date,type) },
                    onDeleteMealRecipe = { date, type, recipe -> mealPlannerViewModel.deleteRecipeFromMeal(date, type, recipe) },
                    onRecipeDetails = onRecipeDetails,
                    onCopyPlanClick = { showPasteDatePicker = true },
                    onNavigateToProfile = onNavigateToProfile
                )
            }
            1 -> {
                TemplatesContent(
                    modifier = Modifier.padding(innerPadding),
                    onAddTemplateClick = { onAddTemplateClick() },
                    authViewModel = authViewModel,
                    onPlanDetails = onPlanDetails,
                    onEdit = onEdit
                )
            }
        }
    }
}