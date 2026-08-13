package com.example.foodieheal.meal_planner.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodieheal.R
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.meal_planner.data.PlanRepository
import com.example.foodieheal.meal_planner.model.PlanCategory
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.meal_planner.viewModel.TemplateViewModel
import com.example.foodieheal.repository.RecipeRepository
import com.example.foodieheal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesContent(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    onAddTemplateClick: () -> Unit,
    onPlanDetails: (String,Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    val tabs = listOf( "All", "My Templates")
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    val planRepository = remember { PlanRepository() }
    val recipeRepository = remember { RecipeRepository(SupabaseClient.client) }

    val currentUserIdFlow = remember(authViewModel) {
        snapshotFlow { authViewModel.currentUser?.id }
    }

    val templateViewModel: TemplateViewModel = viewModel(
        factory = remember(currentUserIdFlow) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    val savedStateHandle = extras.createSavedStateHandle()
                    return TemplateViewModel(
                        savedStateHandle = savedStateHandle,
                        planRepository = planRepository,
                        recipeRepository = recipeRepository,
                        currentUserIdFlow = currentUserIdFlow
                    ) as T
                }
            }
        }
    )

    Column(modifier = modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index

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
                    textDecoration = if(isSelected) TextDecoration.Underline else TextDecoration.None,
                    modifier = Modifier
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> AllTemplatesScreen(
                    templateViewModel = templateViewModel,
                    onPlanDetails = onPlanDetails
                )
                1 -> MyTemplatesScreen(
                    templateViewModel = templateViewModel,
                    onAddTemplateClick = onAddTemplateClick,
                    onPlanDetails = onPlanDetails,
                    onEdit = onEdit,
                )
            }
        }
    }
}

@Composable
fun AllTemplatesScreen(
    templateViewModel: TemplateViewModel,
    onPlanDetails:(String,Boolean)-> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val allPlans by templateViewModel.allWeeklyPlans.collectAsStateWithLifecycle()
        CategorizedTemplatesScreen(
            weeklyPlans = allPlans,
            onPlanDetails = {id ->  onPlanDetails(id,false) },
        )
    }
}

@Composable
fun MyTemplatesScreen(
    templateViewModel: TemplateViewModel,
    onAddTemplateClick: () -> Unit,
    onPlanDetails: (String,Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 120.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTemplateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_add),
                    contentDescription = "Add template"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val userPlans by templateViewModel.userWeeklyPlans.collectAsStateWithLifecycle()

            CategorizedTemplatesScreen(
                weeklyPlans = userPlans,
                onDelete = {id -> templateViewModel.deleteWeeklyPlan(id) },
                onPlanDetails = { id -> onPlanDetails(id,true) },
                onEdit =  onEdit,
                editable = true
            )
        }
    }
}

@Composable
fun CategorizedTemplatesScreen(
    weeklyPlans: List<WeeklyPlan>,
    onPlanDetails:(String)->Unit,
    editable: Boolean = false,
    onEdit: (String) -> Unit = {},
    onDelete:(String)-> Unit = {}
) {
    val categorizedPlans = remember(weeklyPlans) {
        weeklyPlans.groupBy { it.category }
    }

    val activeCategories = remember(categorizedPlans) {
        PlanCategory.entries.filter { category ->
            val plansForCategory = categorizedPlans[category]
            !plansForCategory.isNullOrEmpty()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(
            items = activeCategories,
            key = { it.dbKey }
        ) { category ->
            val plans = categorizedPlans[category].orEmpty()

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = category.displayNameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = plans,
                        key = { it.planId }
                    ) { plan ->
                        PlanCard(
                            plan = plan,
                            onPlanDetails = { onPlanDetails(plan.planId) },
                            editable = editable,
                            onEdit = { onEdit(plan.planId) },
                            onDelete = { onDelete(plan.planId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: WeeklyPlan,
    onPlanDetails: () -> Unit,
    editable: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable(onClick = onPlanDetails),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.planName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (editable) {
                    OtherIconButton(
                        showMenu = showMenu,
                        onShowMenuChange = {showMenu = it},
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }

            val totalMeals = plan.dailyPlans.values.sumOf { realMealSlots ->
                realMealSlots.sumOf { slot -> slot.recipes.size }
            }

            Text(
                text = "$totalMeals meals scheduled this week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OtherIconButton(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    showMenu: Boolean,
    onShowMenuChange:(Boolean)-> Unit,
    onEdit:()->Unit,
    onDelete:()-> Unit
){
    Box {
        Icon(
            painter = painterResource(R.drawable.ic_vertical_more),
            contentDescription = stringResource(R.string.more_options),
            modifier = modifier.clickable(onClick = { onShowMenuChange(true) }),
            tint = color
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onShowMenuChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    onShowMenuChange(false)
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onShowMenuChange(false)
                    onDelete()
                }
            )
        }
    }
}