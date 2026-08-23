package com.example.foodieheal.meal_planner.screen

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    onPlanDetails: (recipeId: String, isMyTemplate: Boolean) -> Unit,
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
    val context = LocalContext.current
    val allPlans by templateViewModel.publicCommunityPlans.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filteredContacts = remember(query, allPlans) {
        if (query.isBlank()) allPlans
        else allPlans.filter { it.planName.contains(query, ignoreCase = true)
                || it.category.toString().contains(query, ignoreCase = true)
                || it.planId.contains(query, ignoreCase = false)}
    }

    fun shareTemplate(id: String) {
        val shareUrl = "https://tzh652.github.io/template?id=$id"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out this meal plan template on Foodie Heal: $shareUrl")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Template")
        context.startActivity(shareIntent)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Template") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        CategorizedTemplatesScreen(
            weeklyPlans = filteredContacts,
            onPlanDetails = {id ->  onPlanDetails(id,false) },
            onShare = { id -> shareTemplate(id) },
            onAdd = { id ->
                templateViewModel.duplicateTemplate(
                    sourcePlanId = id,
                    currentUserId = templateViewModel.currentUserId ?: "",
                    onSuccess = {
                        Toast.makeText(context, "Template added to your collection!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
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
    val context = LocalContext.current

    fun shareTemplate(id: String) {
        val shareUrl = "https://tzh652.github.io/template?id=$id"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out my meal plan template on Foodie Heal: $shareUrl")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Template")
        context.startActivity(shareIntent)
    }

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
                onDelete = { id ->
                    templateViewModel.deleteWeeklyPlan(
                        planId = id,
                        onSuccess = {
                            Toast.makeText(context, "Template deleted successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onPlanDetails = { id -> onPlanDetails(id,true) },
                onEdit =  onEdit,
                onShare = { id -> shareTemplate(id) },
                isMyTemplate = true
            )
        }
    }
}

@Composable
fun CategorizedTemplatesScreen(
    weeklyPlans: List<WeeklyPlan>,
    onPlanDetails:(String)->Unit,
    isMyTemplate: Boolean = false,
    onEdit: (String) -> Unit = {},
    onDelete:(String)-> Unit = {},
    onShare: (String) -> Unit = {},
    onAdd: (String) -> Unit = {}
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
                            isMyTemplate = isMyTemplate,
                            onEdit = { onEdit(plan.planId) },
                            onDelete = { onDelete(plan.planId) },
                            onShare = { onShare(plan.planId) },
                            onAdd = { onAdd(plan.planId) }
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
    isMyTemplate: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
    onAdd: () -> Unit = {}
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

                OtherIconButton(
                    showMenu = showMenu,
                    onShowMenuChange = {showMenu = it},
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onShare = onShare,
                    onAdd = onAdd,
                    isMyTemplate = isMyTemplate
                )
            }

            // 🌟 Extract unique recipe images to display a preview
            val recipeImages = remember(plan) {
                plan.dailyPlans.values
                    .flatten()
                    .flatMap { it.recipes }
                    .mapNotNull { it.recipeImageUrl }
                    .distinct()
                    .take(4)
            }

            if (recipeImages.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    recipeImages.forEach { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    }
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
    isMyTemplate: Boolean,
    onEdit:()->Unit,
    onDelete:()-> Unit,
    onAdd:()-> Unit = {},
    onShare:()-> Unit = {}
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
            if (isMyTemplate) {
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
            }else{
                DropdownMenuItem(
                    text = { Text("Add to my template") },
                    onClick = {
                        onShowMenuChange(false)
                        onAdd()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    onShowMenuChange(false)
                    onShare()
                }
            )
        }
    }
}