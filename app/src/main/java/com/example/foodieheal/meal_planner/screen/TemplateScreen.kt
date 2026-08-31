package com.example.foodieheal.meal_planner.screen

import android.content.Intent
import android.widget.Toast
import es.dmoral.toasty.Toasty
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.ui.components.getHighlightedText
import kotlinx.coroutines.launch
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.foodieheal.Recipe.Repo.RecipeRepository

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplatesContent(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    isNetworkAvailable: Boolean,
    onAddTemplateClick: () -> Unit,
    onPlanDetails: (recipeId: String, isMyTemplate: Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    if (!isNetworkAvailable) {
        OfflinePlaceholder(modifier = modifier)
        return
    }

    val tabs = listOf(stringResource(R.string.tab_all), stringResource(R.string.tab_my_templates))
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val planRepository = remember { PlanRepository() }
    val recipeRepository = remember { RecipeRepository(com.example.foodieheal.Recipe.local.RecipeDatabase.getDatabase(context).recipeDao()) }

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
        .fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 0.dp, start = 16.dp, end = 16.dp),
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
    val focusManager = LocalFocusManager.current
    val templateAddedMsg = stringResource(R.string.msg_template_added)
    val allPlans by templateViewModel.publicCommunityPlans.collectAsStateWithLifecycle()
    val isLoading by templateViewModel.isLoading.collectAsStateWithLifecycle()

    //  Pre-resolve category names for better search matching
    val categoryNames = PlanCategory.entries.associateWith { stringResource(it.displayNameRes) }

    var query by remember { mutableStateOf("") }
    val filteredContacts = remember(query, allPlans, categoryNames) {
        if (query.isBlank()) allPlans
        else allPlans.filter { plan ->
            val categoryName = categoryNames[plan.category] ?: ""
            plan.planName.contains(query, ignoreCase = true)
                || categoryName.contains(query, ignoreCase = true)
                || plan.planId.contains(query, ignoreCase = false)
        }
    }

    val shareText = stringResource(R.string.msg_share_template_text)
    val shareTitle = stringResource(R.string.title_share_template)

    fun shareTemplate(id: String) {
        val shareUrl = "https://tzh652.github.io/template?id=$id"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, String.format(shareText, shareUrl))
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, shareTitle)
        context.startActivity(shareIntent)
    }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 50.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_template)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            trailingIcon = {
                IconButton(onClick = { focusManager.clearFocus() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (isLoading && allPlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            CategorizedTemplatesScreen(
                weeklyPlans = filteredContacts,
                searchQuery = query,
                onPlanDetails = { id -> onPlanDetails(id, false) },
                onShare = { id -> shareTemplate(id) },
                onAdd = { id ->
                    templateViewModel.duplicateTemplate(
                        sourcePlanId = id,
                        currentUserId = templateViewModel.currentUserId ?: "",
                        onSuccess = {
                            Toasty.custom(context, templateAddedMsg, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                        },
                        onError = { error ->
                            Toasty.custom(context, error, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                        }
                    )
                },
                emptyMessage = if (query.isBlank()) stringResource(R.string.empty_no_community_templates) else stringResource(R.string.empty_no_templates_match)
            )
        }
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

    val shareText = stringResource(R.string.msg_share_my_template_text)
    val shareTitle = stringResource(R.string.title_share_template)

    fun shareTemplate(id: String) {
        val shareUrl = "https://tzh652.github.io/template?id=$id"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, String.format(shareText, shareUrl))
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, shareTitle)
        context.startActivity(shareIntent)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0,0,0,0), // Reset window insets to prevent automatic top padding
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTemplateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 120.dp) //  Lift the FAB up so it's not blocked
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_add),
                    contentDescription = stringResource(R.string.desc_add_template)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding).padding(bottom = 50.dp)
        ) {
            val userPlans by templateViewModel.userWeeklyPlans.collectAsStateWithLifecycle()

            val templateDeletedMsg = stringResource(R.string.msg_template_deleted)
            CategorizedTemplatesScreen(
                weeklyPlans = userPlans,
                onDelete = { id ->
                    templateViewModel.deleteWeeklyPlan(
                        planId = id,
                        onSuccess = {
                            Toasty.custom(context, templateDeletedMsg, R.drawable.foodieheallogo_removebg_and_word, R.color.black, Toast.LENGTH_SHORT, true, true).show()
                        }
                    )
                },
                onPlanDetails = { id -> onPlanDetails(id,true) },
                onEdit =  onEdit,
                onShare = { id -> shareTemplate(id) },
                isMyTemplate = true,
                emptyMessage = stringResource(R.string.empty_no_my_templates)
            )
        }
    }
}

@Composable
fun CategorizedTemplatesScreen(
    weeklyPlans: List<WeeklyPlan>,
    onPlanDetails:(String)->Unit,
    isMyTemplate: Boolean = false,
    searchQuery: String = "",
    onEdit: (String) -> Unit = {},
    onDelete:(String)-> Unit = {},
    onShare: (String) -> Unit = {},
    onAdd: (String) -> Unit = {},
    emptyMessage: String = stringResource(R.string.not_available)
) {
    if (weeklyPlans.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

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
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(
            items = activeCategories,
            key = { it.dbKey }
        ) { category ->
            val plans = categorizedPlans[category].orEmpty()

            Column(modifier = Modifier.fillMaxWidth()) {
                val categoryName = stringResource(id = category.displayNameRes)
                Text(
                    text = getHighlightedText(categoryName, searchQuery),
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
                            searchQuery = searchQuery,
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
    searchQuery: String = "",
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
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = getHighlightedText(plan.planName, searchQuery),
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

            //  Extract unique recipe images to display a preview
            val recipeImages = remember(plan) {
                plan.dailyPlans.values
                    .asSequence()
                    .flatten()
                    .flatMap { it.recipes }
                    .mapNotNull { it.recipeImageUrl }
                    .distinct()
                    .take(4)
                    .toList()
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
                text = stringResource(R.string.meals_scheduled_count, totalMeals),
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
                    text = { Text(stringResource(R.string.menu_edit)) },
                    onClick = {
                        onShowMenuChange(false)
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_delete)) },
                    onClick = {
                        onShowMenuChange(false)
                        onDelete()
                    }
                )
            }else{
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_to_my_template)) },
                    onClick = {
                        onShowMenuChange(false)
                        onAdd()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_share)) },
                onClick = {
                    onShowMenuChange(false)
                    onShare()
                }
            )
        }
    }
}