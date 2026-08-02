package com.example.foodieheal.meal_planner.viewModel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.Recipe
import com.example.foodieheal.meal_planner.data.MealPlannerRepository
import com.example.foodieheal.meal_planner.model.DailyPlan
import com.example.foodieheal.meal_planner.model.MealType
import com.example.foodieheal.meal_planner.model.RealMealSlot
import com.example.foodieheal.navigation.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MealPlannerViewModel(
    application: Application,
    private val repository: MealPlannerRepository
) : AndroidViewModel(application) {

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

    init {
        observeNetworkStatus()
    }
    var deepLinkSourceDays by mutableStateOf<List<LocalDate>?>(null)
        private set

    // Channels ensure one-time UI event delivery (like navigation) without missing signals
    // 1. Change SharedFlow to a Channel to prevent dropping cold-start events
    private val _navigationChannel = Channel<String>(Channel.BUFFERED)
    val navigationEvent = _navigationChannel.receiveAsFlow()

    var selectedTabRoute by mutableStateOf(Screen.Home.route)
        private set

    fun prepareSharedWeeklyPlan(sourceWeekStart: LocalDate) {
        deepLinkSourceDays = getCurrentWeekDays(sourceWeekStart)

        // Set the active tab to Planner when deep link is processed
        selectedTabRoute = Screen.Planner.route

        viewModelScope.launch {
            _navigationChannel.send(Screen.Main.route)
        }
    }

    // Function to update current tab manually when user taps bottom bar items
    fun onTabSelected(route: String) {
        selectedTabRoute = route
    }

    fun clearDeepLinkState() {
        deepLinkSourceDays = null
    }

    fun generateShareLink(currentWeekStart: LocalDate): String {
        return "https://tzh652.github.io/share?sourceStart=$currentWeekStart"
    }
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                isNetworkAvailable = connected
                if (connected) {
                    // 🌟 FIX: Force a fresh database check upon reconnection to recover offline deviations
                    loadPlanForDate(lastActiveDate, forceRefresh = true)
                    loadPlanForDate(lastActiveDate.minusDays(1), forceRefresh = true)
                    loadPlanForDate(lastActiveDate.plusDays(1), forceRefresh = true)
                }
            }
        }
    }

    fun loadPlanForDate(date: LocalDate, forceRefresh: Boolean = false) {
        lastActiveDate = date

        if (!forceRefresh && mealPlansCache.containsKey(date) && mealPlansCache[date] != null) return

        viewModelScope.launch {
            isLoading = true
            val result = repository.getDailyPlan(date)

            result.onSuccess { plan ->
                mealPlansCache[date] = plan
            }.onFailure {
                mealPlansCache[date] = null
            }
            isLoading = false
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

            val result = repository.saveDailyPlan(updatedPlan)
            result.onFailure { exception ->
                Log.e("MealPlannerVM", "Failed to save updated meal plan to Supabase", exception)
            }

            isLoading = false
        }
    }

    fun deleteRecipeFromMeal(date: LocalDate, mealType: MealType, recipeToDelete: Recipe) {
        viewModelScope.launch {
            if (!isNetworkAvailable) return@launch

            Log.d("MealPlannerDelete", "=== 🛑 START DELETE PROCESS ===")
            val currentPlan = mealPlansCache[date]
            if (currentPlan == null) {
                Log.w("MealPlannerDelete", "❌ Cancelled: Plan cache node for $date is NULL.")
                return@launch
            }

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

            val result = repository.saveDailyPlan(updatedPlan)

            result.onSuccess {
                Log.d("MealPlannerDelete", "✅ SUCCESS: Supabase storage updated successfully!")
            }
            result.onFailure { error ->
                Log.e("MealPlannerDelete", "💥 FAILURE: Supabase update transaction failed!", error)
            }
            Log.d("MealPlannerDelete", "=== 🛑 END DELETE PROCESS ===")
        }
    }

    fun copyDailyPlanToDate(sourcePlan: DailyPlan, targetDate: LocalDate) {
        viewModelScope.launch {
            if (!isNetworkAvailable) {
                _uiEvent.emit("Cannot copy plans while offline.")
                return@launch
            }
            val result = repository.saveDailyPlan(
                sourcePlan.copy(
                    date = targetDate.toString(),
                    meals = sourcePlan.meals.map { it.copy(recipes = it.recipes.toList()) }
                )
            )
            result.onSuccess {
                // 🌟 FIXED: Force-update the local cache map container seamlessly
                mealPlansCache[targetDate] = sourcePlan.copy(date = targetDate.toString())
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
            sourceWeekDays.forEachIndexed { index, sourceDate ->
                val targetDate = targetWeekStart.plusDays(index.toLong())
                val sourcePlan = repository.getDailyPlan(sourceDate).getOrNull()

                if (sourcePlan != null && sourcePlan.meals.any { it.recipes.isNotEmpty() }) {
                    val clonedPlan = sourcePlan.copy(
                        date = targetDate.toString(),
                        meals = sourcePlan.meals.map { slot -> slot.copy(recipes = slot.recipes.toList()) }
                    )
                    repository.saveDailyPlan(clonedPlan)

                    // 🌟 FIXED: Force immediate allocation updates inside state variables
                    mealPlansCache[targetDate] = clonedPlan
                }
            }
            _uiEvent.emit("Successfully duplicated the entire week schedule!")
        }
    }

    fun getCurrentWeekDays(baseDate: LocalDate = LocalDate.now()): List<LocalDate> {
        val sunday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return (0..6).map { sunday.plusDays(it.toLong()) }
    }
}