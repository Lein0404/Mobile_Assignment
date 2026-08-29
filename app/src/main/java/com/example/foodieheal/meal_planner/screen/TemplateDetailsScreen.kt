package com.example.foodieheal.meal_planner.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import es.dmoral.toasty.Toasty
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val shareTitle = stringResource(R.string.menu_share)
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
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showMoreButton by remember { mutableStateOf(false) }

    fun copyIdToClipboard(id: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.label_plan_id), id)
        clipboard.setPrimaryClip(clip)
        Toasty.custom(context, context.getString(R.string.msg_plan_id_copied), R.drawable.foodieheallogo_removebg_preview, R.color.black, Toast.LENGTH_SHORT, true, true).show()
    }

    fun shareTemplate(id: String) {
        val shareUrl = "https://tzh652.github.io/template?id=$id"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.msg_share_template_text, shareUrl))
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, shareTitle)
        context.startActivity(shareIntent)
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
                                contentDescription = if (plan.public) stringResource(R.string.label_public_template) else stringResource(R.string.label_private_template),
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
                                contentDescription = stringResource(R.string.desc_copy_plan_id),
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
                        onAdd = { onAdd() },
                        onShare = { shareTemplate(plan.planId) }
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
                Text(stringResource(R.string.btn_apply_template))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 🌟 Plan Description Section
            if (plan.planDescription.isNotBlank()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.tab_details),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = plan.planDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                onTextLayout = { textLayoutResult ->
                                    if (!isDescriptionExpanded) {
                                        showMoreButton = textLayoutResult.hasVisualOverflow
                                    }
                                }
                            )
                            
                            if (showMoreButton || isDescriptionExpanded) {
                                Text(
                                    text = if (isDescriptionExpanded) stringResource(R.string.msg_show_less) else stringResource(R.string.msg_more),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                            text = stringResource(R.string.placeholder_no_meals_scheduled),
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
                title = stringResource(R.string.dialog_title_apply_template),
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
