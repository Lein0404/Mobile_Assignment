package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel.DayCondition
import com.example.foodieheal.meal_planner.viewModel.WeeklyCalendarState
import com.example.foodieheal.Recipe.Model.Recipe
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealPlannerContent(
    calendarState: WeeklyCalendarState,
    isNetworkAvailable: Boolean,
    totalCalories: Int,
    maxCalories: Int,
    mealPlansCache: Map<LocalDate, DailyPlan?>,
    monthConditions: Map<LocalDate, DayCondition> = emptyMap(), // 🌟 Added month conditions
    pagerState: PagerState,
    anchorDate: LocalDate,
    onCalendarClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDateShiftBackward: () -> Unit,
    onDateShiftForward: () -> Unit,
    onLoadPlanForDate: (LocalDate) -> Unit,
    onAddMealRecipe: (LocalDate, MealType) -> Unit,
    onDeleteMealRecipe: (LocalDate, MealType, Recipe) -> Unit,
    onRecipeDetails: (String) -> Unit,
    onCopyPlanClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {

        Spacer(Modifier.height(8.dp))

        CalendarControls(
            headerText = calendarState.headerText,
            weekDays = calendarState.weekDays,
            selectedDate = calendarState.selectedDate,
            onDateBackward = onDateShiftBackward,
            onDateForward = onDateShiftForward,
            onCalendarClick = onCalendarClick,
            onDateSelected = onDateSelected,
            monthConditions = monthConditions // 🌟 Pass conditions down
        )

        Spacer(Modifier.height(12.dp))

        if (isNetworkAvailable) {
            CalorieProgressBar(
                currentCalories = totalCalories,
                maxCalories = maxCalories,
                onNavigateToProfile = onNavigateToProfile,
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
            val pageDate = remember(page, anchorDate) { anchorDate.plusDays(page.toLong()) }

            LaunchedEffect(pageDate) {
                onLoadPlanForDate(pageDate)
            }

            val dailyPlanForThisPage = mealPlansCache[pageDate]

            MealPageContent(
                pageDate = pageDate,
                isNetworkAvailable = isNetworkAvailable,
                dailyPlan = dailyPlanForThisPage,
                onAddMealRecipe = { type -> onAddMealRecipe(pageDate, type) },
                onDeleteMealRecipe = { type, recipe -> onDeleteMealRecipe(pageDate, type, recipe) },
                onRecipeDetails = onRecipeDetails,
                onCopyPlanClick = onCopyPlanClick
            )
        }
    }
}


@Composable
fun MealPlannerHeader(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    isNetworkAvailable: Boolean,
    onRepeatClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // --- TOP ROW (Title + Actions) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.meal_planner),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))

                if (selectedTabIndex == 0) {//only shows in Planner Tab
                    Icon(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = stringResource(R.string.desc_copy_daily_plan),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(30.dp)
                            .clickable(onClick = onRepeatClick, enabled = isNetworkAvailable),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.share),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(onClick = onShareClick, enabled = isNetworkAvailable),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // --- BOTTOM ROW (Tabs) ---
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text(stringResource(R.string.tab_planner), fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text(stringResource(R.string.tab_template), fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    }
}

@Composable
fun MealPageContent(
    pageDate: LocalDate,
    isNetworkAvailable: Boolean,
    dailyPlan: DailyPlan?,
    onAddMealRecipe: (MealType) -> Unit,
    onDeleteMealRecipe: (MealType, Recipe) -> Unit,
    onRecipeDetails: (String) -> Unit,
    onCopyPlanClick: () -> Unit
) {
    val pageScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
    ) {
        if (!isNetworkAvailable) {
            OfflinePlaceholder()
        } else {
            val dailyBannerText = remember(pageDate) {
                val dayName = pageDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val dayOfMonth = pageDate.dayOfMonth
                val monthName = pageDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                "$dayName, $dayOfMonth $monthName"
            }

            val breakfastRecipes = dailyPlan?.meals?.filter { it.mealType == MealType.BREAKFAST }?.flatMap { it.recipes } ?: emptyList()
            val lunchRecipes = dailyPlan?.meals?.filter { it.mealType == MealType.LUNCH }?.flatMap { it.recipes } ?: emptyList()
            val dinnerRecipes = dailyPlan?.meals?.filter { it.mealType == MealType.DINNER }?.flatMap { it.recipes } ?: emptyList()
            val snackRecipes = dailyPlan?.meals?.filter { it.mealType == MealType.SNACK }?.flatMap { it.recipes } ?: emptyList()

            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                Text(
                    text = dailyBannerText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp)
                )

                val sections = remember(breakfastRecipes, lunchRecipes, dinnerRecipes, snackRecipes) {
                    listOf(
                        Triple(R.string.breakfast, breakfastRecipes, MealType.BREAKFAST),
                        Triple(R.string.lunch, lunchRecipes, MealType.LUNCH),
                        Triple(R.string.dinner, dinnerRecipes, MealType.DINNER),
                        Triple(R.string.snack, snackRecipes, MealType.SNACK)
                    )
                }
                Column {
                    sections.forEach { (title, recipes, type) ->
                        MealSection(
                            title = stringResource(title),
                            recipes = recipes,
                            onAddClick = { onAddMealRecipe(type) },
                            onDeleteClick = { recipe -> onDeleteMealRecipe(type, recipe) },
                            onRecipeDetails = onRecipeDetails,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }

                val isPlanEmpty = breakfastRecipes.isEmpty() && lunchRecipes.isEmpty() &&
                        dinnerRecipes.isEmpty() && snackRecipes.isEmpty()

                if (!isPlanEmpty) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = onCopyPlanClick
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.tertiary),
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
                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }
}