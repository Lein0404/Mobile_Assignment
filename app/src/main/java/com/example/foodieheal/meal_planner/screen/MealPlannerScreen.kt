package com.example.foodieheal.meal_planner.screen


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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.time.temporal.TemporalAdjusters
import java.util.Locale


@Composable
fun MealPlannerScreen(viewModel: MealPlannerViewModel, modifier: Modifier) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

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
    var showPasteDatePicker by remember { mutableStateOf(false) }
    var showWeeklyPasteDatePicker by remember { mutableStateOf(false) }

    val snackBarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { message ->
            snackBarHostState.showSnackbar(message = message)
        }
    }

    val selectedDailyPlan = viewModel.selectedDailyPlan

    // 🌟 FIX 1: Explicitly specify parameter type to avoid type inference crash
    val totalDailyCalories = remember(selectedDailyPlan) {
        selectedDailyPlan?.meals
            ?.flatMap { meal -> meal.recipes }
            ?.sumOf { recipe: Recipe -> recipe.calories } ?: 0
    }

    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate,
            titleText = "Select Date to View",
            onDateSelected = { newDate ->
                selectedDate = newDate
                currentWeekStart = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // DIALOG ADDITION: Picker window to choose where to paste today's menu layout
    if (showPasteDatePicker) {
        MealDatePickerDialog(
            initialDate = selectedDate.plusDays(1), // Default option set to tomorrow
            titleText = "Choose a date to paste today's plan",
            onDateSelected = { targetDate ->
                selectedDailyPlan?.let { sourcePlan ->
                    viewModel.copyDailyPlanToDate(sourcePlan, targetDate)
                }
            },
            onDismiss = { showPasteDatePicker = false }
        )
    }

    // 🌟 DIALOG ADDITION: Picker window to choose where to paste this entire week's plan
    if (showWeeklyPasteDatePicker) {
        MealDatePickerDialog(
            initialDate = currentWeekStart.plusWeeks(1), // Default option set to next week
            titleText = "Choose any day within target week to paste",
            onDateSelected = { targetDate ->
                // Calculate the Sunday start boundary of the target selected week
                val targetWeekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                viewModel.copyWeeklyPlanToDate(weekDays, targetWeekStart)
            },
            onDismiss = { showWeeklyPasteDatePicker = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Handle edit profile, this icon is too big will block interaction ui,*/ },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(70.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = "edit profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Box(
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                currentWeekStart = currentWeekStart.minusWeeks(1)
                                selectedDate = selectedDate.minusWeeks(1)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Calendar Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDatePicker = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = "Calendar",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                currentWeekStart = currentWeekStart.plusWeeks(1)
                                selectedDate = selectedDate.plusWeeks(1)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = "Calendar Forward",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showWeeklyPasteDatePicker = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.repeat),
                                contentDescription = "copy daily plan",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                WeeklyDateCardRow(
                    weekDays = weekDays,
                    selectedDate = selectedDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                    },
                    modifier = Modifier
                )

                Spacer(Modifier.height(25.dp))

                CalorieProgressBar(totalDailyCalories, 1800, Modifier)

                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
                    elevation = CardDefaults.cardElevation(100.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                ) {
                    // 🌟 FIX 2: Explicitly declare the lambda parameter mapping here as well
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
                            onAddClick = {//TODO: wait for recipe module
                                // Updated to point to our newly inserted Oatmeal (R999)
                                val sampleRecipe = Recipe(
                                    recipe_id = "R999",
                                    recipeName = "Oatmeal",
                                    calories = 350,
                                    time = 10,
                                    recipeImage = R.drawable.breakfast, // Or whatever local fallback resource you use
                                    recipeDescription = "A quick and healthy bowl of warm oats.",
                                    budget = 2.50,
                                    skillLevel = 1,
                                    recipeStep = "Cook oats in milk or water."
                                )
                                viewModel.addRecipeToMeal(
                                    selectedDate,
                                    MealType.BREAKFAST,
                                    sampleRecipe
                                )
                            },
                            onDeleteClick = { recipe ->
                                viewModel.deleteRecipeFromMeal(
                                    selectedDate,
                                    MealType.BREAKFAST,
                                    recipe
                                )
                            }
                        )

                        MealSection(
                            title = "Lunch",
                            recipes = lunchRecipes,
                            onAddClick = {
                                // Uses R011 (Chicken Wrap) which already exists in your DB logs
                                val sampleRecipe = Recipe(
                                    recipe_id = "R011",
                                    recipeName = "Chicken Wrap",
                                    calories = 340,
                                    time = 15,
                                    recipeImage = R.drawable.lunch,
                                    recipeDescription = "A delicious wrap filled with grilled chicken.",
                                    budget = 5.80,
                                    skillLevel = 1,
                                    recipeStep = "Grill chicken, fill tortilla, roll."
                                )
                                viewModel.addRecipeToMeal(
                                    selectedDate,
                                    MealType.LUNCH,
                                    sampleRecipe
                                )
                            },
                            onDeleteClick = { recipe ->
                                viewModel.deleteRecipeFromMeal(selectedDate, MealType.LUNCH, recipe)
                            }
                        )

                        MealSection(
                            title = "Dinner",
                            recipes = dinnerRecipes,
                            onAddClick = {
                                // Uses R015 (Thai Green Curry) which already exists in your DB logs
                                val sampleRecipe = Recipe(
                                    recipe_id = "R015",
                                    recipeName = "Thai Green Curry",
                                    calories = 550,
                                    time = 45,
                                    recipeImage = R.drawable.dinner,
                                    recipeDescription = "A fragrant Thai green curry.",
                                    budget = 10.50,
                                    skillLevel = 5,
                                    recipeStep = "Fry curry paste, add coconut milk, simmer."
                                )
                                viewModel.addRecipeToMeal(
                                    selectedDate,
                                    MealType.DINNER,
                                    sampleRecipe
                                )
                            },
                            onDeleteClick = { recipe ->
                                viewModel.deleteRecipeFromMeal(
                                    selectedDate,
                                    MealType.DINNER,
                                    recipe
                                )
                            }
                        )

                        MealSection(
                            title = "Snack",
                            recipes = snackRecipes,
                            onAddClick = {
                                // Uses R002 (Fluffy Buttermilk Pancakes) which already exists in your DB logs
                                val sampleRecipe = Recipe(
                                    recipe_id = "R002",
                                    recipeName = "Fluffy Buttermilk Pancakes",
                                    calories = 200,
                                    time = 15,
                                    recipeImage = R.drawable.snack,
                                    recipeDescription = "Golden, diner-style pancakes.",
                                    budget = 4.00,
                                    skillLevel = 2,
                                    recipeStep = "Mix ingredients, cook on griddle."
                                )
                                viewModel.addRecipeToMeal(
                                    selectedDate,
                                    MealType.SNACK,
                                    sampleRecipe
                                )
                            },
                            onDeleteClick = { recipe ->
                                viewModel.deleteRecipeFromMeal(
                                    selectedDate,
                                    MealType.BREAKFAST,
                                    recipe
                                )
                            }
                        )


                        val isPlanEmpty = breakfastRecipes.isEmpty() &&
                                lunchRecipes.isEmpty() &&
                                dinnerRecipes.isEmpty() &&
                                snackRecipes.isEmpty()

                        if (!isPlanEmpty) {
                            Card(
                                modifier = modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary,
                                        RoundedCornerShape(20.dp)
                                    ),
                                onClick = {showPasteDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Copy Today's Plan?",
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

