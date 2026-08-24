package com.example.foodieheal.meal_planner.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel
import com.example.foodieheal.meal_planner.viewModel.MealPlannerViewModel.DayCondition
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateDetailsScreen(
    plan: WeeklyPlan,
    isMyTemplate: Boolean,
    maxCalories: Int,
    mealPlannerViewModel: MealPlannerViewModel,
    onApply: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onRecipeDetails: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onRecipeAdd: (DayOfWeek, MealType) -> Unit = { _, _ -> },
    onRecipeDelete: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
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

    fun copyIdToClipboard(id: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Plan ID", id)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Plan ID copied to clipboard", Toast.LENGTH_SHORT).show()
    }

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
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plan.planName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (plan.public) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = if (plan.public) "Public Template" else "Private Template",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { copyIdToClipboard(plan.planId) }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "ID: ${plan.planId.take(8)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Plan ID",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    OtherIconButton(
                        modifier = Modifier.padding(end = 6.dp).size(36.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        showMenu = showMenu,
                        onShowMenuChange = { showMenu = it },
                        isMyTemplate = isMyTemplate,
                        onEdit = { onEdit() },
                        onDelete = { onDelete() },
                        onAdd = { onAdd() }
                    )
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
            CustomizedDatePickerDialog(
                initialDate = remember { LocalDate.now() },
                onDateSelected = { startDate ->
                    showDatePicker = false
                    onApply(startDate)
                },
                onDismiss = { showDatePicker = false },
                mealPlannerViewModel = mealPlannerViewModel,
                maxCalories = maxCalories,
                isRangeMode = true
            )
        }
    }
}