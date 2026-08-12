package com.example.foodieheal.meal_planner.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.MainActivity
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.database.AppDatabase
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.viewModel.TemplateViewModel
import com.example.foodieheal.repository.RecipeRepository

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesContent(modifier: Modifier = Modifier) {
    val tabs = listOf("Hot", "Templates", "My Template")
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp) // Gap between the text items
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index

                // Animate color changes smoothly between active/inactive states
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    label = "TabTextColor"
                )

                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable(
                            interactionSource = null,
                            indication = null // Removes standard gray ripple circle for a cleaner look
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // 🌟 Swappable Container Body
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> HotTemplatesScreen()
                1 -> AllTemplatesScreen()
                2 -> MyTemplatesScreen()
            }
        }
    }
}

@Composable
fun HotTemplatesScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fetch your background instances safely
        val context = MainActivity.appContext ?: throw IllegalStateException("Application context is missing")
        val database = AppDatabase.getDatabase(context)

        val planRepository = remember { PlanRepository(database.planDao()) }
        val recipeRepository = remember { RecipeRepository(SupabaseClient.client) }

        // 2. Instantiate your custom viewmodel using an inline Factory
        val viewModel: TemplateViewModel = viewModel(
            factory = remember {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TemplateViewModel(planRepository, recipeRepository) as T
                    }
                }
            }
        )

        // 3. Unpack and observe the Flow safely with UI lifecycle constraints
        val allPlans by viewModel.allWeeklyPlans.collectAsStateWithLifecycle()

        // 4. Pass the plain list to your component layout
        CategorizedTemplatesScreen(weeklyPlans = allPlans)
    }
}

@Composable
fun AllTemplatesScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("🗂️ Browse All Templates", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MyTemplatesScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("👤 Your Custom Saved Templates", style = MaterialTheme.typography.bodyLarge)
    }
}



@Composable
fun CategorizedTemplatesScreen(
    weeklyPlans: List<WeeklyPlan>,
    modifier: Modifier = Modifier
) {
    // 🌟 1. Group the plans by category
    val categorizedPlans = remember(weeklyPlans) {
        weeklyPlans.groupBy { it.category }
    }

    // 🌟 2. Get all defined categories, but only keep those that actually contain plans
    val activeCategories = remember(categorizedPlans) {
        PlanCategory.entries.filter { category ->
            val plansForCategory = categorizedPlans[category]
            !plansForCategory.isNullOrEmpty()
        }
    }

    // 🌟 3. Outer list containing the headers and horizontal rows
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(
            items = activeCategories,
            key = { it.dbKey } // Use dbKey as a stable key for smooth animations
        ) { category ->
            val plans = categorizedPlans[category].orEmpty()

            Column(modifier = Modifier.fillMaxWidth()) {
                // 🏷️ Category Section Header (Skips entirely if category was empty)
                Text(
                    text = stringResource(id = category.displayNameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 🔄 Inner Row displaying the customized data cards
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = plans,
                        key = { it.planId }
                    ) { plan ->
                        PlanCard(plan = plan)
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCard(plan: WeeklyPlan, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(260.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = plan.planName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            // Calculate total items across all days in the map safely
            val totalMeals = plan.dailyPlans.values.sumOf { it.size }
            Text(
                text = "$totalMeals meals scheduled this week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}