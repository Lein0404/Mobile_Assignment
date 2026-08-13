package com.example.foodieheal.meal_planner.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailsScreen(
    plan: WeeklyPlan,
    isMyTemplate: Boolean,
    maxCalories: Int,
    onApply: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onRecipeDetails: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onRecipeAdd: (DayOfWeek, MealType) -> Unit = { _, _ -> },
    onRecipeDelete: (String) -> Unit,
    onEdit:()->Unit,
    onDelete:()-> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val daysOfWeek = remember { DayOfWeek.entries }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { daysOfWeek.size }
    )
    val selectedDay = daysOfWeek[pagerState.currentPage]

    val selectedDayTotalCalories = remember(plan.dailyPlans, selectedDay) {
        plan.dailyPlans[selectedDay]
            ?.flatMap { slot -> slot.recipes }
            ?.sumOf { recipe -> recipe.calories } ?: 0
    }

    var showDatePicker by remember { mutableStateOf(false) }

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrowback),
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable(onClick = onBack)
                    )
                },
                title = { Text(plan.planName) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (isMyTemplate) {
                        OtherIconButton(
                            modifier = Modifier.padding(end = 6.dp).size(36.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            showMenu = showMenu,
                            onShowMenuChange = { showMenu = it },
                            onEdit = { onEdit() },
                            onDelete = { onDelete() }
                        )
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Apply template to your plan")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            WeeklyDayCardRow(
                selectedDay = selectedDay,
                onDaySelected = { day ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(day.ordinal)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                val currentDay = daysOfWeek[page]
                val dailySlots = plan.dailyPlans[currentDay] ?: emptyList()

                val breakfastRecipes = dailySlots
                    .filter { it.mealType == MealType.BREAKFAST }
                    .flatMap { it.recipes }
                val lunchRecipes = dailySlots
                    .filter { it.mealType == MealType.LUNCH }
                    .flatMap { it.recipes }
                val dinnerRecipes = dailySlots
                    .filter { it.mealType == MealType.DINNER }
                    .flatMap { it.recipes }
                val snackRecipes = dailySlots
                    .filter { it.mealType == MealType.SNACK }
                    .flatMap { it.recipes }

                val sections = remember(breakfastRecipes, lunchRecipes, dinnerRecipes, snackRecipes) {
                    listOf(
                        Triple(R.string.breakfast, breakfastRecipes, MealType.BREAKFAST),
                        Triple(R.string.lunch, lunchRecipes, MealType.LUNCH),
                        Triple(R.string.dinner, dinnerRecipes, MealType.DINNER),
                        Triple(R.string.snack, snackRecipes, MealType.SNACK)
                    )
                }

                val isDayEmpty = breakfastRecipes.isEmpty() && lunchRecipes.isEmpty() &&
                        dinnerRecipes.isEmpty() && snackRecipes.isEmpty()

                if (isDayEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No meals scheduled for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        sections.forEach { (titleRes, recipes, mealType) ->
                            MealSection(
                                viewMode = true,
                                title = stringResource(titleRes),
                                recipes = recipes,
                                onRecipeDetails = onRecipeDetails,
                                modifier = Modifier.padding(bottom = 5.dp),
                                onAddClick = { onRecipeAdd(currentDay, mealType) },
                                onDeleteClick = { recipe -> onRecipeDelete(recipe.recipe_id ?: "") }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        if (showDatePicker) {
            MealDatePickerDialog(
                initialDate = remember { LocalDate.now() },
                titleText = "Select Start Date",
                onDateSelected = { startDate ->
                    showDatePicker = false
                    onApply(startDate)
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}