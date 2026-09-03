package com.example.foodieheal.meal_planner.screen

import android.content.Intent
import android.widget.Toast
import es.dmoral.toasty.Toasty
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.WeeklyCalendarState
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.hiring.model.UserAppointmentsUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import java.time.YearMonth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealPlannerScreen(
    mealPlannerViewModel: MealPlannerViewModel,
    authViewModel: AuthViewModel,
    onNavigateToProfile: () -> Unit,
    onRecipeDetails: (String) -> Unit,
    onAddMeal: (LocalDate, MealType) -> Unit,
    onAddTemplateClick: () -> Unit,
    onPlanDetails: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onAppointmentClick: (String) -> Unit
) {
    val context = LocalContext.current

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastShiftTime by remember { mutableLongStateOf(0L) }
    val shiftDebounceMs = 600L

    var selectedDate by rememberSaveable(
        stateSaver = Saver(
            save = { it.toString() },
            restore = { LocalDate.parse(it) }
        )
    ) {
        mutableStateOf(mealPlannerViewModel.deepLinkSourceDays?.firstOrNull() ?: LocalDate.now())
    }

    val appointmentsState by mealPlannerViewModel.userAppointmentsState.collectAsStateWithLifecycle()

    val appointmentsForSelectedDate = remember(appointmentsState, selectedDate) {
        if (appointmentsState is UserAppointmentsUiState.Success) {
            val success = appointmentsState as UserAppointmentsUiState.Success
            success.appointments.filter { 
                it.Date == selectedDate.toString() && 
                !it.Status.equals("cancelled", ignoreCase = true) &&
                !it.Status.equals("rejected", ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }

    val chefsMap = remember(appointmentsState) {
        if (appointmentsState is UserAppointmentsUiState.Success) {
            (appointmentsState as UserAppointmentsUiState.Success).usersMap
        } else {
            emptyMap()
        }
    }

    //  Derive week start from selected date automatically
    val currentWeekStart = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val isNetworkAvailable = mealPlannerViewModel.isNetworkAvailable
    val activeDailyPlan = mealPlannerViewModel.mealPlansCache[selectedDate]

    LaunchedEffect(selectedDate) {
        mealPlannerViewModel.loadPlanForDate(selectedDate)
    }

    val coroutineScope = rememberCoroutineScope()
    val dailyCopySuccessMessage = stringResource(R.string.daily_success_notify)
    val weeklyCopySuccessMessage = stringResource(R.string.weekly_success_notify)
    val recipeRemovedMsg = stringResource(R.string.msg_recipe_removed)
    val shareWeeklyPlanTitle = stringResource(R.string.share_weekly_plan)

    val weekDays = mealPlannerViewModel.getCurrentWeekDays(currentWeekStart)
    val weekEndDate = weekDays.last()
    val headerText = "${weekDays.first().dayOfMonth} - ${weekEndDate.dayOfMonth} ${
        weekEndDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
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
    var showConfirmWeeklyPaste by remember { mutableStateOf(false) }
    var targetWeeklyPasteDate by remember { mutableStateOf<LocalDate?>(null) }

    val deepLinkDays = mealPlannerViewModel.deepLinkSourceDays

    LaunchedEffect(deepLinkDays) {
        if (deepLinkDays != null) {
            showWeeklyPasteDatePicker = true
            //  Shift the calendar to the shared week so the user sees what they are copying
            deepLinkDays.firstOrNull()?.let { firstDay ->
                selectedDate = firstDay
            }
            //  Signal that we have consumed the deep link UI logic
            mealPlannerViewModel.consumeDeepLinkProcessed()
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = true) {
        mealPlannerViewModel.uiEvent.collect { message ->
            snackBarHostState.showSnackbar(message = message)
        }
    }

    val anchorDate = remember { LocalDate.now().minusYears(1) }

    val initialPage = remember { ChronoUnit.DAYS.between(anchorDate, selectedDate).toInt() }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 730 }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetDate = anchorDate.plusDays(page.toLong())
            //  Only sync pager -> state if it's NOT a deep link animation or if it's a real change
            // This prevents the "reset to today" bounce during initial deep link loading
            if (targetDate != selectedDate && !mealPlannerViewModel.isProcessingDeepLink) {
                selectedDate = targetDate
            }
        }
    }

    LaunchedEffect(selectedDate) {
        val targetPage = ChronoUnit.DAYS.between(anchorDate, selectedDate).toInt()
        if (pagerState.currentPage != targetPage && targetPage in 0 until 730) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    //  Track current month based on active selected date
    val currentMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val maxCalories = calculateSuggestedDailyCalories(authViewModel.currentUser)

    //  Load current and adjacent months immediately on screen launch
    LaunchedEffect(currentMonth, maxCalories) {
        mealPlannerViewModel.loadMonthConditions(currentMonth, maxCalories)
        mealPlannerViewModel.prefetchAdjacentMonths(currentMonth, maxCalories)
    }

    if (showDatePicker) {
        CustomizedDatePickerDialog(
            initialDate = selectedDate,
            title = stringResource(R.string.dialog_title_select_date_view),
            onDateSelected = { newDate ->
                selectedDate = newDate
            },
            onDismiss = { showDatePicker = false },
            mealPlannerViewModel = mealPlannerViewModel,
            maxCalories = calculateSuggestedDailyCalories(authViewModel.currentUser)
        )
    }

    if (showPasteDatePicker) {
        CustomizedDatePickerDialog(
            initialDate = selectedDate,
            title = stringResource(R.string.dialog_title_choose_paste_date),
            onDateSelected = { targetDate ->
                val sourcePlan = activeDailyPlan
                if (sourcePlan != null) {
                    mealPlannerViewModel.copyDailyPlanToDate(sourcePlan, targetDate)
                    Toasty.custom(context, dailyCopySuccessMessage, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                }
                showPasteDatePicker = false
            },
            onDismiss = { showPasteDatePicker = false },
            mealPlannerViewModel = mealPlannerViewModel,
            maxCalories = calculateSuggestedDailyCalories(authViewModel.currentUser)
        )
    }

    if (showWeeklyPasteDatePicker) {
        val title = if (mealPlannerViewModel.deepLinkSourceDays != null) {
            stringResource(R.string.dialog_title_choose_date_range_paste)
        } else {
            stringResource(R.string.dialog_title_choose_weekly_repeat_date)
        }

        CustomizedDatePickerDialog(
            initialDate = currentWeekStart,
            title = title,
            onDateSelected = { targetDate ->
                targetWeeklyPasteDate = targetDate
                showConfirmWeeklyPaste = true
                showWeeklyPasteDatePicker = false
            },
            onDismiss = {
                showWeeklyPasteDatePicker = false
                mealPlannerViewModel.clearDeepLinkState()
            },
            mealPlannerViewModel = mealPlannerViewModel,
            maxCalories = calculateSuggestedDailyCalories(authViewModel.currentUser),
            isRangeMode = true
        )
    }

    if (showConfirmWeeklyPaste && targetWeeklyPasteDate != null) {
        val targetWeekStart = targetWeeklyPasteDate!!.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekLabel = "${targetWeekStart.dayOfMonth} ${targetWeekStart.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"

        AlertDialog(
            onDismissRequest = { showConfirmWeeklyPaste = false },
            title = { Text(stringResource(R.string.dialog_title_confirm_paste_week)) },
            text = { Text(stringResource(R.string.dialog_msg_confirm_paste_week, weekLabel)) },
            confirmButton = {
                Button(
                    onClick = {
                        val daysToCopy = mealPlannerViewModel.deepLinkSourceDays ?: weekDays
                        mealPlannerViewModel.copyWeeklyPlanToDate(daysToCopy, targetWeeklyPasteDate!!)
                        Toasty.custom(context, weeklyCopySuccessMessage, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                        
                        showConfirmWeeklyPaste = false
                        targetWeeklyPasteDate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.btn_paste), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConfirmWeeklyPaste = false
                    showWeeklyPasteDatePicker = true // Go back to calendar
                }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                    context.startActivity(Intent.createChooser(sendIntent, shareWeeklyPlanTitle))
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(bottom = 16.dp).navigationBarsPadding()
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
                    monthConditions = mealPlannerViewModel.monthConditions,
                    pagerState = pagerState,
                    anchorDate = anchorDate,
                    appointments = appointmentsForSelectedDate,
                    chefsMap = chefsMap,
                    onCalendarClick = { showDatePicker = true },
                    onDateSelected = { newDate ->
                        val now = System.currentTimeMillis()
                        if (now - lastShiftTime > shiftDebounceMs) {
                            selectedDate = newDate
                            lastShiftTime = now
                        }
                    },
                    onDateShiftBackward = {
                        val now = System.currentTimeMillis()
                        if (now - lastShiftTime > shiftDebounceMs) {
                            selectedDate = selectedDate.minusWeeks(1)
                            lastShiftTime = now
                        }
                    },
                    onDateShiftForward = {
                        val now = System.currentTimeMillis()
                        if (now - lastShiftTime > shiftDebounceMs) {
                            selectedDate = selectedDate.plusWeeks(1)
                            lastShiftTime = now
                        }
                    },
                    onLoadPlanForDate = { date ->
                        if (isNetworkAvailable) mealPlannerViewModel.loadPlanForDate(date)
                    },
                    onAddMealRecipe = { date, type -> onAddMeal(date, type) },
                    onDeleteMealRecipe = { date, type, recipe ->
                        coroutineScope.launch {
                            mealPlannerViewModel.deleteRecipeFromMealSuspend(date, type, recipe)
                            Toasty.custom(context, recipeRemovedMsg, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                        }
                    },
                    onRecipeDetails = onRecipeDetails,
                    onCopyPlanClick = { showPasteDatePicker = true },
                    onNavigateToProfile = onNavigateToProfile,
                    onAppointmentClick = onAppointmentClick
                )
            }
            1 -> {
                TemplatesContent(
                    modifier = Modifier.padding(innerPadding),
                    onAddTemplateClick = { onAddTemplateClick() },
                    authViewModel = authViewModel,
                    onPlanDetails = onPlanDetails,
                    onEdit = onEdit,
                    isNetworkAvailable = isNetworkAvailable
                )
            }
        }
    }
}
