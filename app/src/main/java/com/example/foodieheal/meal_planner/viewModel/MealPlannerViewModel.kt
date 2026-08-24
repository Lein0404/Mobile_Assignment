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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

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

    var sharerId by mutableStateOf<String?>(null)
        private set

    var deepLinkSourceDays by mutableStateOf<List<LocalDate>?>(null)
        private set

    var selectedTabRoute by mutableStateOf(Screen.Home.route)
        private set

    private val conditionsCache = mutableMapOf<YearMonth, Map<LocalDate, DayCondition>>()
    var monthConditions by mutableStateOf<Map<LocalDate, DayCondition>>(emptyMap())
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
     * Resets the deep link processing flag.
     * The actual data (deepLinkSourceDays) is preserved until explicitly cleared by the UI.
     */
    fun consumeDeepLinkProcessed() {
        isProcessingDeepLink = false
    }

    fun prepareSharedWeeklyPlan(sourceWeekStart: LocalDate, sourceSharerId: String?) {
        isProcessingDeepLink = true
        val days = getCurrentWeekDays(sourceWeekStart)
        deepLinkSourceDays = days
        sharerId = sourceSharerId
        selectedTabRoute = Screen.Home.route

        Log.d(TAG, "prepareSharedWeeklyPlan called with start date: $sourceWeekStart | Sharer: $sourceSharerId | DeepLinkDays set: ${days.size} days")
    }

    fun onTabSelected(route: String) {
        Log.d(TAG, "onTabSelected: $route")
        selectedTabRoute = route
    }

    fun clearDeepLinkState() {
        Log.d(TAG, "clearDeepLinkState called")
        isProcessingDeepLink = false
        val wasShared = deepLinkSourceDays != null
        deepLinkSourceDays = null
        sharerId = null
        
        // 🌟 If we were previewing a shared plan, refresh the current month to show our own data dots again
        if (wasShared) {
            invalidateConditionsCacheAndReload(lastActiveDate)
        }
    }

    fun generateShareLink(currentWeekStart: LocalDate): String {
        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
        return "https://tzh652.github.io/share?sourceStart=$currentWeekStart&sharerId=$currentUserId"
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
                    Log.d(TAG, "Auth session restored. Clearing cache and reloading data.")
                    clearAllCache()
                    
                    // 1. Reload the current focused date
                    loadPlanForDate(lastActiveDate, forceRefresh = true)
                    
                    // 2. Reload the calendar dots for the current month
                    loadMonthConditions(YearMonth.from(lastActiveDate))
                } else {
                    // Clear data immediately on logout to prevent data leaking between users
                    clearAllCache()
                }
            }
        }
    }

    private fun clearAllCache() {
        Log.d(TAG, "clearAllCache() called")
        // Defensive check against early auth status emissions before property initialization completes
        runCatching { mealPlansCache.clear() }
        runCatching { conditionsCache.clear() }
        updateMonthConditionsState()
    }

    fun loadPlanForDate(date: LocalDate, forceRefresh: Boolean = false) {
        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
        if (currentUserId.isNullOrEmpty()) return

        // 🌟 Determine who we are targeting: Me or the Sharer
        val isSharedDate = deepLinkSourceDays?.contains(date) == true
        val targetUserId = if (isSharedDate && sharerId != null) sharerId!! else currentUserId

        // 🌟 OPTIMIZATION: Only skip if the cached plan belongs to the correct target user
        val cachedPlan = mealPlansCache[date]
        if (!forceRefresh && cachedPlan != null && cachedPlan.user_id == targetUserId) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            lastActiveDate = date
            withContext(Dispatchers.Main) { isLoading = true }

            val result = repository.getDailyPlan(date, targetUserId)

            withContext(Dispatchers.Main) {
                result.onSuccess { plan ->
                    if (plan != null) {
                        // 🌟 Ensure we only cache plans that belong to the intended target
                        if (plan.user_id == targetUserId) {
                            mealPlansCache[date] = plan
                        }
                    } else if (forceRefresh || !mealPlansCache.containsKey(date)) {
                        // Only wipe cache if we're sure there's nothing on the server for this user/date
                        mealPlansCache[date] = null
                    }
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
        val capturedSharerId = sharerId
        val myId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""

        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _uiEvent.emit("Cannot duplicate calendar schedules while offline.")
                return@launch
            }

            isLoading = true
            var successCount = 0

            // 🌟 STEP 1: Immediately clear the preview state
            // This ensures swiping during the save process doesn't revert to sharer data
            clearDeepLinkState()

            withContext(Dispatchers.IO) {
                sourceWeekDays.forEachIndexed { index, sourceDate ->
                    val targetDate = targetWeekStart.plusDays(index.toLong())
                    
                    // 🌟 STEP 2: Fetch the "pattern" from User B (the sharer)
                    val sourcePlan = repository.getDailyPlan(sourceDate, capturedSharerId).getOrNull()

                    if (sourcePlan != null && sourcePlan.meals.any { it.recipes.isNotEmpty() }) {
                        // 🌟 STEP 3: Create a NEW plan record owned by User A (me)
                        val clonedPlan = sourcePlan.copy(
                            user_id = myId,
                            date = targetDate.toString(),
                            meals = sourcePlan.meals.map { slot -> slot.copy(recipes = slot.recipes.toList()) }
                        )
                        
                        // Save to my personal planner in the DB
                        val saveResult = repository.saveDailyPlan(clonedPlan)
                        
                        if (saveResult.isSuccess) {
                            successCount++
                            withContext(Dispatchers.Main) {
                                // Update local memory immediately
                                mealPlansCache[targetDate] = clonedPlan
                            }
                        }
                    }
                }

                // STEP 4: Refresh nutritional indicators for the target week
                val affectedMonths = targetWeekStart.let { start ->
                    setOf(YearMonth.from(start), YearMonth.from(start.plusDays(6)))
                }
                withContext(Dispatchers.Main) {
                    affectedMonths.forEach { conditionsCache.remove(it) }
                    loadMonthConditions(YearMonth.from(targetWeekStart))
                }
            }

            isLoading = false
            
            if (successCount > 0) {
                _uiEvent.emit("Successfully saved $successCount days to your planner!")
                // 🌟 Final check: Ensure UI displays my new data correctly
                sourceWeekDays.forEachIndexed { index, _ ->
                    val targetDate = targetWeekStart.plusDays(index.toLong())
                    loadPlanForDate(targetDate, forceRefresh = true)
                }
            } else {
                _uiEvent.emit("No recipes found to copy.")
            }
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

    fun loadMonthConditions(month: YearMonth, maxCalories: Int = currentMaxCalories) {
        if (maxCalories > 0) {
            currentMaxCalories = maxCalories
        }

        if (conditionsCache.containsKey(month)) {
            updateMonthConditionsState()
            return
        }

        viewModelScope.launch {
            val conditions = fetchConditionsForMonth(month, currentMaxCalories)
            conditionsCache[month] = conditions
            updateMonthConditionsState()
        }
    }

    private fun updateMonthConditionsState() {
        // Combine all cached months into a single flat map for the UI to consume easily
        monthConditions = conditionsCache.values.reduceOrNull { acc, map -> acc + map } ?: emptyMap()
    }

    fun prefetchAdjacentMonths(currentMonth: YearMonth, maxCalories: Int = currentMaxCalories) {
        if (maxCalories > 0) {
            currentMaxCalories = maxCalories
        }
        val prevMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)

        viewModelScope.launch {
            var changed = false
            if (!conditionsCache.containsKey(prevMonth)) {
                conditionsCache[prevMonth] = fetchConditionsForMonth(prevMonth, currentMaxCalories)
                changed = true
            }
            if (!conditionsCache.containsKey(nextMonth)) {
                conditionsCache[nextMonth] = fetchConditionsForMonth(nextMonth, currentMaxCalories)
                changed = true
            }
            if (changed) updateMonthConditionsState()
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

        // 🌟 DECIDE WHO TO FETCH: ME or SHARER
        // If the month contains any shared days, we prioritize the sharer's context for preview
        val targetUserId = if (deepLinkSourceDays?.any { YearMonth.from(it) == month } == true && sharerId != null) {
            sharerId
        } else {
            SupabaseClient.client.auth.currentUserOrNull()?.id
        }

        // 1. Batch fetch all plans AND recipes for the whole month in TWO network requests total
        val plansResult = repository.getDailyPlansInRange(startDate, endDate, targetUserId)
        val plans = plansResult.getOrDefault(emptyList())

        // 2. Update memory cache so swiping is instant
        withContext(Dispatchers.Main) {
            plans.forEach { plan ->
                val date = LocalDate.parse(plan.date)
                // Only update cache if it's our own data or we are actively in sharing mode
                if (plan.user_id == targetUserId) {
                    mealPlansCache[date] = plan
                }
            }
        }

        // 3. Compute local condition logic
        plans.forEach { dailyPlan ->
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
                val date = LocalDate.parse(dailyPlan.date)
                resultMap[date] = condition
            }
        }

        resultMap
    }
}