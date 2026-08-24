package com.example.foodieheal.meal_planner.viewModel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.meal_planner.model.WeeklyPlan
import com.example.foodieheal.model.Recipe
import com.example.foodieheal.navigation.Screen
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.time.Duration.Companion.milliseconds

class MealPlannerViewModel(
    application: Application,
    private val repository: MealPlannerRepository
) : AndroidViewModel(application) {

    private companion object {
        private const val TAG = "ColdStartDebug"
    }

    var mealPlansCache = mutableStateMapOf<LocalDate, DailyPlan?>()
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val networkMonitor = NetworkMonitor(application)

    var isNetworkAvailable by mutableStateOf(true)
        private set

    private var lastActiveDate: LocalDate = LocalDate.now()
    private var currentMaxCalories: Int = 2000

    var deepLinkSourceDays by mutableStateOf<List<LocalDate>?>(null)
        private set

    var selectedTabRoute by mutableStateOf(Screen.Home.route)
        private set

    // Explicit flag to protect deep-link navigation transactions
    var isProcessingDeepLink by mutableStateOf(false)
        private set

    init {
        Log.d(TAG, "MealPlannerViewModel Initialized")
        observeNetworkStatus()
        observeAuthState()
    }

    /**
     * MUST be called ONLY AFTER NavController has finished executing navigate(route).
     * We add a delay to ensure the target screen has completed its first composition
     * and collected the deep link state before we clear it.
     */
    fun consumeDeepLinkProcessed() {
        viewModelScope.launch {
            delay(500.milliseconds)
            isProcessingDeepLink = false
            deepLinkSourceDays = null
        }
    }

    fun prepareSharedWeeklyPlan(sourceWeekStart: LocalDate) {
        isProcessingDeepLink = true
        val days = getCurrentWeekDays(sourceWeekStart)
        deepLinkSourceDays = days
        selectedTabRoute = Screen.Home.route

        Log.d(TAG, "prepareSharedWeeklyPlan called with start date: $sourceWeekStart | DeepLinkDays set: ${days.size} days")
    }

    fun onTabSelected(route: String) {
        Log.d(TAG, "onTabSelected: $route")
        selectedTabRoute = route
    }

    fun clearDeepLinkState() {
        Log.d(TAG, "clearDeepLinkState called")
        isProcessingDeepLink = false
        deepLinkSourceDays = null
    }

    fun generateShareLink(currentWeekStart: LocalDate): String {
        return "https://tzh652.github.io/share?sourceStart=$currentWeekStart"
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                isNetworkAvailable = connected
                Log.d(TAG, "Network connection state updated: connected=$connected")
                if (connected) {
                    loadPlanForDate(lastActiveDate, forceRefresh = true)
                    loadPlanForDate(lastActiveDate.minusDays(1), forceRefresh = true)
                    loadPlanForDate(lastActiveDate.plusDays(1), forceRefresh = true)
                }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            SupabaseClient.client.auth.sessionStatus.collect { status ->
                Log.d(TAG, "observeAuthState collected status: $status | deepLinkSourceDays: $deepLinkSourceDays")
                if (status is SessionStatus.Authenticated) {
                    Log.d(TAG, "Auth session restored. Reloading plan for: $lastActiveDate")
                    loadPlanForDate(lastActiveDate, forceRefresh = true)
                }
            }
        }
    }

    fun loadPlanForDate(date: LocalDate, forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id

            if (currentUserId.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    mealPlansCache[date] = null
                }
                return@launch
            }

            lastActiveDate = date

            withContext(Dispatchers.Main) { isLoading = true }

            val result = repository.getDailyPlan(date)

            withContext(Dispatchers.Main) {
                result.onSuccess { plan ->
                    if (plan != null && plan.user_id == currentUserId) {
                        mealPlansCache[date] = plan
                    } else {
                        mealPlansCache[date] = null
                    }
                }.onFailure {
                    mealPlansCache[date] = null
                }
                isLoading = false
            }
        }
    }

    fun addRecipeToMeal(date: LocalDate, mealType: MealType, recipe: Recipe) {
        viewModelScope.launch {
            if (!isNetworkAvailable) return@launch

            isLoading = true

            val currentPlan = mealPlansCache[date] ?: DailyPlan(
                user_id = "",
                date = date.toString(),
                meals = emptyList()
            )

            val slotExists = currentPlan.meals.any { it.mealType == mealType }

            val updatedMeals = if (slotExists) {
                currentPlan.meals.map { slot ->
                    if (slot.mealType == mealType) {
                        slot.copy(recipes = slot.recipes + recipe)
                    } else {
                        slot
                    }
                }
            } else {
                val newSlot = RealMealSlot(mealType = mealType, recipes = listOf(recipe))
                currentPlan.meals + newSlot
            }

            val updatedPlan = currentPlan.copy(meals = updatedMeals)
            mealPlansCache[date] = updatedPlan

            invalidateConditionsCacheAndReload(date)

            val result = withContext(Dispatchers.IO) { repository.saveDailyPlan(updatedPlan) }
            result.onFailure { exception ->
                Log.e(TAG, "Failed to save updated meal plan to Supabase", exception)
            }

            isLoading = false
        }
    }

    fun deleteRecipeFromMeal(date: LocalDate, mealType: MealType, recipeToDelete: Recipe) {
        viewModelScope.launch {
            if (!isNetworkAvailable) return@launch

            val currentPlan = mealPlansCache[date] ?: return@launch

            val updatedMeals = currentPlan.meals.map { slot ->
                if (slot.mealType == mealType) {
                    val targetIndex = slot.recipes.indexOfFirst { it.recipe_id == recipeToDelete.recipe_id }
                    if (targetIndex != -1) {
                        val remainingRecipes = slot.recipes.toMutableList().apply {
                            removeAt(targetIndex)
                        }
                        slot.copy(recipes = remainingRecipes)
                    } else {
                        slot
                    }
                } else {
                    slot
                }
            }

            val updatedPlan = currentPlan.copy(meals = updatedMeals)
            mealPlansCache[date] = updatedPlan

            invalidateConditionsCacheAndReload(date)

            withContext(Dispatchers.IO) { repository.saveDailyPlan(updatedPlan) }
        }
    }

    fun copyDailyPlanToDate(sourcePlan: DailyPlan, targetDate: LocalDate) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _uiEvent.emit("Cannot copy plans while offline.")
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                repository.saveDailyPlan(
                    sourcePlan.copy(
                        date = targetDate.toString(),
                        meals = sourcePlan.meals.map { it.copy(recipes = it.recipes.toList()) }
                    )
                )
            }
            result.onSuccess {
                mealPlansCache[targetDate] = sourcePlan.copy(date = targetDate.toString())
                invalidateConditionsCacheAndReload(targetDate)
                _uiEvent.emit("Successfully copied plan to $targetDate!")
            }.onFailure {
                _uiEvent.emit("Failed to copy meal plan.")
            }
        }
    }

    fun copyWeeklyPlanToDate(sourceWeekDays: List<LocalDate>, targetWeekStart: LocalDate) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _uiEvent.emit("Cannot duplicate calendar schedules while offline.")
                return@launch
            }
            withContext(Dispatchers.IO) {
                sourceWeekDays.forEachIndexed { index, sourceDate ->
                    val targetDate = targetWeekStart.plusDays(index.toLong())
                    val sourcePlan = repository.getDailyPlan(sourceDate).getOrNull()

                    if (sourcePlan != null && sourcePlan.meals.any { it.recipes.isNotEmpty() }) {
                        val clonedPlan = sourcePlan.copy(
                            date = targetDate.toString(),
                            meals = sourcePlan.meals.map { slot -> slot.copy(recipes = slot.recipes.toList()) }
                        )
                        repository.saveDailyPlan(clonedPlan)
                        withContext(Dispatchers.Main) {
                            mealPlansCache[targetDate] = clonedPlan
                            invalidateConditionsCacheAndReload(targetDate)
                        }
                    }
                }
            }
            _uiEvent.emit("Successfully duplicated the entire week schedule!")
        }
    }

    fun applyTemplateToDate(template: WeeklyPlan, startDate: LocalDate) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _uiEvent.emit("Cannot apply template while offline.")
                return@launch
            }

            isLoading = true
            val currentUserId = withContext(Dispatchers.IO) { SupabaseClient.client.auth.currentUserOrNull()?.id.orEmpty() }

            if (currentUserId.isBlank()) {
                _uiEvent.emit("User is not authenticated.")
                isLoading = false
                return@launch
            }

            withContext(Dispatchers.IO) {
                template.dailyPlans.forEach { (dayOfWeek, mealSlots) ->
                    val validSlots = mealSlots.filter { it.recipes.isNotEmpty() }
                    if (validSlots.isEmpty()) return@forEach

                    val daysOffset = (dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
                    val targetDate = startDate.plusDays(daysOffset)

                    val dailyPlan = DailyPlan(
                        user_id = currentUserId,
                        date = targetDate.toString(),
                        meals = validSlots
                    )

                    withContext(Dispatchers.Main) {
                        mealPlansCache[targetDate] = dailyPlan
                        invalidateConditionsCacheAndReload(targetDate)
                    }

                    val result = repository.saveDailyPlan(dailyPlan)
                    result.onFailure { error ->
                        Log.e(TAG, "Failed to save template day for $targetDate", error)
                    }
                }
            }

            isLoading = false
            _uiEvent.emit("Successfully applied '${template.planName}' template!")
        }
    }

    fun getCurrentWeekDays(referenceDate: LocalDate): List<LocalDate> {
        val monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { dayOffset ->
            monday.plusDays(dayOffset.toLong())
        }
    }

    enum class DayCondition {
        UNDER_INTAKE,
        SLIGHTLY_LOW,
        IDEAL,
        SLIGHTLY_HIGH,
        EXCESS_INTAKE
    }

    private val conditionsCache = mutableMapOf<YearMonth, Map<LocalDate, DayCondition>>()

    var monthConditions by mutableStateOf<Map<LocalDate, DayCondition>>(emptyMap())
        private set

    fun loadMonthConditions(month: YearMonth, maxCalories: Int = currentMaxCalories) {
        if (maxCalories > 0) {
            currentMaxCalories = maxCalories
        }

        if (conditionsCache.containsKey(month)) {
            monthConditions = conditionsCache[month].orEmpty()
            return
        }

        viewModelScope.launch {
            val conditions = fetchConditionsForMonth(month, currentMaxCalories)
            conditionsCache[month] = conditions
            monthConditions = conditions.toMap()
        }
    }

    fun prefetchAdjacentMonths(currentMonth: YearMonth, maxCalories: Int = currentMaxCalories) {
        if (maxCalories > 0) {
            currentMaxCalories = maxCalories
        }
        val prevMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)

        viewModelScope.launch {
            if (!conditionsCache.containsKey(prevMonth)) {
                conditionsCache[prevMonth] = fetchConditionsForMonth(prevMonth, currentMaxCalories)
            }
            if (!conditionsCache.containsKey(nextMonth)) {
                conditionsCache[nextMonth] = fetchConditionsForMonth(nextMonth, currentMaxCalories)
            }
        }
    }

    fun invalidateConditionsCacheAndReload(currentDate: LocalDate, maxCalories: Int = currentMaxCalories) {
        if (maxCalories > 0) {
            currentMaxCalories = maxCalories
        }
        val month = YearMonth.from(currentDate)
        conditionsCache.remove(month)
        loadMonthConditions(month, currentMaxCalories)
    }

    private suspend fun fetchConditionsForMonth(
        month: YearMonth,
        maxCalories: Int
    ): Map<LocalDate, DayCondition> = withContext(Dispatchers.IO) {
        if (maxCalories <= 0) return@withContext emptyMap()

        val resultMap = mutableMapOf<LocalDate, DayCondition>()
        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val date = currentDate

            val dailyPlan = mealPlansCache[date] ?: repository.getDailyPlan(date).getOrNull()

            if (dailyPlan != null) {
                withContext(Dispatchers.Main) {
                    mealPlansCache[date] = dailyPlan
                }

                val totalCalories = dailyPlan.meals.sumOf { slot ->
                    slot.recipes.sumOf { recipe -> recipe.calories ?: 0 }
                }

                if (totalCalories > 0) {
                    val ratio = totalCalories.toDouble() / maxCalories.toDouble()
                    val condition = when {
                        ratio < 0.80 -> DayCondition.UNDER_INTAKE
                        ratio in 0.80..0.94 -> DayCondition.SLIGHTLY_LOW
                        ratio in 0.95..1.05 -> DayCondition.IDEAL
                        ratio in 1.06..1.20 -> DayCondition.SLIGHTLY_HIGH
                        else -> DayCondition.EXCESS_INTAKE
                    }
                    resultMap[date] = condition
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        resultMap
    }
}