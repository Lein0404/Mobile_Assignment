package com.example.foodieheal.hiring.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodieheal.hiring.data.AppointmentConflictException
import com.example.foodieheal.hiring.data.HiringRepository
import com.example.foodieheal.hiring.model.AppointmentUiState
import com.example.foodieheal.hiring.model.AppointmentValidationError
import com.example.foodieheal.hiring.model.ChefAppointmentsUiState
import com.example.foodieheal.meal_planner.viewModel.NetworkMonitor
import com.example.foodieheal.hiring.model.Appointment
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown
import com.example.foodieheal.hiring.model.SelectedAppointmentRecipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppointmentBookingViewModel(
    private val repository: HiringRepository = HiringRepository(),
    private val networkMonitor: NetworkMonitor? = null,
    private val recipeRepository: RecipeRepository = RecipeRepository()
) : ViewModel() {

    private val _selectedChef = MutableStateFlow<Chef?>(null)
    val selectedChef: StateFlow<Chef?> = _selectedChef.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    private val _chefAppointmentsState = MutableStateFlow(ChefAppointmentsUiState())
    val chefAppointmentsState: StateFlow<ChefAppointmentsUiState> = _chefAppointmentsState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    // Bookmarked recipes for selection
    private val _bookmarkedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val bookmarkedRecipes: StateFlow<List<Recipe>> = _bookmarkedRecipes.asStateFlow()

    private val _isLoadingBookmarks = MutableStateFlow(false)
    val isLoadingBookmarks: StateFlow<Boolean> = _isLoadingBookmarks.asStateFlow()

    // Attached recipes to appointment
    private val _selectedRecipes = MutableStateFlow<List<SelectedAppointmentRecipe>>(emptyList())
    val selectedRecipes: StateFlow<List<SelectedAppointmentRecipe>> = _selectedRecipes.asStateFlow()

    val pricingBreakdown: StateFlow<AppointmentPricingBreakdown> = combine(
        _selectedChef,
        _uiState,
        _selectedRecipes
    ) { chef, state, recipes ->
        AppointmentPricingBreakdown.calculate(
            chefHourlyRate = chef?.Pricing ?: 0.0,
            appointmentTime = state.appointmentTime,
            selectedRecipes = recipes,
            userState = state.state,
            chefState = chef?.state.orEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppointmentPricingBreakdown()
    )

    val currentChefId: String
        get() = selectedChef.value?.let { it.chefId.ifEmpty { it.id } }.orEmpty()

    init {
        observeNetworkStatus()
    }

    private fun observeNetworkStatus() {
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.isConnected.collect { connected ->
                    _isNetworkAvailable.value = connected
                    if (connected && currentChefId.isNotBlank()) {
                        fetchAppointmentsForChef(currentChefId)
                    }
                }
            }
        }
    }

    fun selectChef(chef: Chef) {
        _selectedChef.value = chef
    }

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun clearSelectedChef() {
        _selectedChef.value = null
    }

    fun onAppointmentTimeSlotChanged(startTime: String, endTime: String) {
        val combined = "$startTime - $endTime"
        _uiState.update {
            it.copy(
                startTime = startTime,
                endTime = endTime,
                appointmentTime = combined
            )
        }
        validateTimeSlot(combined)
        revalidateIfSubmitted()
    }

    fun onAppointmentTimeChanged(time: String) {
        if (time.contains("-")) {
            val parts = time.split("-").map { it.trim() }
            if (parts.size == 2) {
                _uiState.update {
                    it.copy(
                        startTime = parts[0],
                        endTime = parts[1],
                        appointmentTime = time
                    )
                }
            } else {
                _uiState.update { it.copy(appointmentTime = time) }
            }
        } else {
            _uiState.update { it.copy(appointmentTime = time) }
        }
        validateTimeSlot(time)
        revalidateIfSubmitted()
    }

    fun onAddressChanged(address: String) {
        _uiState.update { it.copy(address = address) }
        revalidateIfSubmitted()
    }

    fun onPostcodeChanged(postcode: String) {
        if (postcode.all { it.isDigit() }) {
            _uiState.update { it.copy(postcode = postcode) }
            revalidateIfSubmitted()
        }
    }

    fun onStateChanged(state: String) {
        _uiState.update { it.copy(state = state) }
        revalidateIfSubmitted()
    }

    fun onHealthPreferenceChanged(healthPreference: String) {
        _uiState.update { it.copy(healthPreference = healthPreference) }
        revalidateIfSubmitted()
    }

    fun onServingSizeChanged(servingSize: String) {
        if (servingSize.all { it.isDigit() }) {
            _uiState.update { it.copy(servingSize = servingSize) }
            revalidateIfSubmitted()
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
        revalidateIfSubmitted()
    }

    fun clearAppointmentForm() {
        _uiState.value = AppointmentUiState()
        _selectedRecipes.value = emptyList()
    }

    fun fetchUserBookmarks(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoadingBookmarks.value = true
            try {
                val bookmarks = recipeRepository.getBookmarkedRecipes(userId).getOrDefault(emptyList())
                _bookmarkedRecipes.value = bookmarks
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error fetching bookmarked recipes", e)
            } finally {
                _isLoadingBookmarks.value = false
            }
        }
    }

    fun toggleRecipeSelection(recipe: Recipe) {
        val recipeId = recipe.recipe_id ?: return
        val currentList = _selectedRecipes.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.recipe.recipe_id == recipeId }

        if (existingIndex >= 0) {
            currentList.removeAt(existingIndex)
        } else {
            val defaultServings = uiState.value.servingSize.toIntOrNull()?.takeIf { it > 0 } ?: 1
            currentList.add(
                SelectedAppointmentRecipe(
                    recipe = recipe,
                    serviceCount = defaultServings,
                    customNote = ""
                )
            )
        }
        _selectedRecipes.value = currentList
    }

    fun isRecipeSelected(recipeId: String?): Boolean {
        if (recipeId == null) return false
        return _selectedRecipes.value.any { it.recipe.recipe_id == recipeId }
    }

    fun updateRecipeServings(recipeId: String, servings: Int) {
        val clampedServings = servings.coerceIn(1, 99)
        _selectedRecipes.update { list ->
            list.map { item ->
                if (item.recipe.recipe_id == recipeId) {
                    item.copy(serviceCount = clampedServings)
                } else item
            }
        }
    }

    fun updateRecipeCustomNote(recipeId: String, note: String) {
        _selectedRecipes.update { list ->
            list.map { item ->
                if (item.recipe.recipe_id == recipeId) {
                    item.copy(customNote = note)
                } else item
            }
        }
    }

    fun removeSelectedRecipe(recipeId: String) {
        _selectedRecipes.update { list ->
            list.filterNot { it.recipe.recipe_id == recipeId }
        }
    }

    fun clearSelectedRecipes() {
        _selectedRecipes.value = emptyList()
    }

    private fun revalidateIfSubmitted() {
        if (_uiState.value.hasAttemptedSubmit) {
            val errors = validateForm()
            _uiState.update { it.copy(errors = errors) }
        }
    }

    fun validateAndSubmit(onValid: () -> Unit) {
        _uiState.update { it.copy(hasAttemptedSubmit = true) }
        val errors = validateForm()
        _uiState.update { it.copy(errors = errors) }
        if (errors.isEmpty()) {
            onValid()
        }
    }

    fun fetchAppointmentsForChef(chefId: String) {
        if (chefId.isBlank()) return

        viewModelScope.launch {
            _chefAppointmentsState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (appointments, chefUser) = repository.fetchChefAppointments(chefId)
                _chefAppointmentsState.update {
                    it.copy(
                        appointments = appointments,
                        chefUser = chefUser,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error fetching chef schedule", e)
                _chefAppointmentsState.update {
                    it.copy(
                        errorMessage = e.localizedMessage ?: "Failed to fetch chef schedule",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun validateForm(): Set<AppointmentValidationError> {
        val state = uiState.value
        return validateFormValues(
            appointmentTime = state.appointmentTime,
            address = state.address,
            postcode = state.postcode,
            state = state.state,
            servingSize = state.servingSize,
            description = state.description,
            targetDate = selectedDate.value.toString()
        )
    }

    fun validateFormValues(
        appointmentTime: String,
        address: String,
        postcode: String,
        state: String,
        servingSize: String,
        description: String,
        targetDate: String,
        currentAppointmentId: String? = null
    ): Set<AppointmentValidationError> {
        val errors = mutableSetOf<AppointmentValidationError>()

        if (appointmentTime.isBlank() || !appointmentTime.contains("-")) {
            errors.add(AppointmentValidationError.InvalidTime)
        } else {
            val parts = appointmentTime.split("-").map { it.trim() }
            if (parts.size == 2) {
                val parsedTimes = parseTimeSlot(parts[0], parts[1])
                if (parsedTimes == null) {
                    errors.add(AppointmentValidationError.InvalidTime)
                } else {
                    val (startCal, endCal) = parsedTimes
                    if (!endCal.after(startCal)) {
                        errors.add(AppointmentValidationError.InvalidTime)
                    } else if (isSlotOverlapping(startCal, endCal, targetDate, currentAppointmentId)) {
                        errors.add(AppointmentValidationError.TimeSlotOccupied)
                    } else {
                        val chef = selectedChef.value
                        if (chef?.availability_hours != null) {
                            val weeklyAvail = com.example.foodieheal.Chef.model.WeeklyAvailability.fromJsonElement(chef.availability_hours)
                            val parsedDate = try {
                                java.time.LocalDate.parse(targetDate)
                            } catch (_: Exception) {
                                selectedDate.value
                            }
                            val startHour = startCal.get(Calendar.HOUR_OF_DAY)
                            val endHour = endCal.get(Calendar.HOUR_OF_DAY)
                            val (isAvail, reason) = weeklyAvail.validateTimeSlotForDate(parsedDate, startHour, endHour)
                            if (!isAvail) {
                                errors.add(AppointmentValidationError.ChefUnavailableSlot)
                                _uiState.update { it.copy(chefUnavailableReason = reason) }
                            }
                        }
                    }
                }
            } else {
                errors.add(AppointmentValidationError.InvalidTime)
            }
        }

        if (address.isBlank()) errors.add(AppointmentValidationError.InvalidAddress)
        if (postcode.isBlank() || postcode.length != 5) {
            errors.add(AppointmentValidationError.InvalidPostcode)
        }
        if (state.isBlank()) errors.add(AppointmentValidationError.InvalidState)
        if (servingSize.isBlank() || servingSize.toIntOrNull() == null || (servingSize.toIntOrNull() ?: 0) <= 0) {
            errors.add(AppointmentValidationError.InvalidServingSize)
        }
        if (description.isBlank()) errors.add(AppointmentValidationError.InvalidDescription)

        return errors
    }

    fun validateTimeSlot(time: String) {
        if (!time.contains("-")) return
        val parts = time.split("-").map { it.trim() }
        if (parts.size != 2) return

        val parsedTimes = parseTimeSlot(parts[0], parts[1]) ?: return
        val (startCal, endCal) = parsedTimes

        val targetDate = selectedDate.value.toString()
        val isOccupied = isSlotOverlapping(startCal, endCal, targetDate)

        var isUnavailable = false
        var unavailableReason: String? = null
        val chef = selectedChef.value
        if (chef?.availability_hours != null) {
            val weeklyAvail = com.example.foodieheal.Chef.model.WeeklyAvailability.fromJsonElement(chef.availability_hours)
            val parsedDate = try {
                java.time.LocalDate.parse(targetDate)
            } catch (_: Exception) {
                selectedDate.value
            }
            val startHour = startCal.get(Calendar.HOUR_OF_DAY)
            val endHour = endCal.get(Calendar.HOUR_OF_DAY)
            val (isAvail, reason) = weeklyAvail.validateTimeSlotForDate(parsedDate, startHour, endHour)
            if (!isAvail) {
                isUnavailable = true
                unavailableReason = reason
            }
        }

        _uiState.update { currentState ->
            val updatedErrors = currentState.errors.toMutableSet()
            if (isOccupied) {
                updatedErrors.add(AppointmentValidationError.TimeSlotOccupied)
            } else {
                updatedErrors.remove(AppointmentValidationError.TimeSlotOccupied)
            }

            if (isUnavailable) {
                updatedErrors.add(AppointmentValidationError.ChefUnavailableSlot)
            } else {
                updatedErrors.remove(AppointmentValidationError.ChefUnavailableSlot)
            }

            currentState.copy(
                errors = updatedErrors,
                isTimeSlotOccupied = isOccupied,
                chefUnavailableReason = unavailableReason
            )
        }
    }

    private fun parseTime(timeStr: String): Date? {
        val trimmed = timeStr.trim()
        val patterns = listOf("hh:mm a", "h:mm a", "HH:mm:ss", "HH:mm", "H:mm:ss", "H:mm")
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                val date = format.parse(trimmed)
                if (date != null) return date
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseTimeSlot(startTimeStr: String, endTimeStr: String): Pair<Calendar, Calendar>? {
        return try {
            val startDate = parseTime(startTimeStr) ?: return null
            val endDate = parseTime(endTimeStr) ?: return null

            val startCal = Calendar.getInstance().apply { time = startDate }
            val endCal = Calendar.getInstance().apply { time = endDate }

            Pair(startCal, endCal)
        } catch (e: Exception) {
            null
        }
    }

    private fun isSlotOverlapping(
        startCal: Calendar,
        endCal: Calendar,
        targetDate: String,
        currentAppointmentId: String? = null
    ): Boolean {
        val appointments = chefAppointmentsState.value.appointments.filter {
            it.Date == targetDate &&
                    !it.Status.equals("cancelled", ignoreCase = true) &&
                    !it.Status.equals("rejected", ignoreCase = true) &&
                    (currentAppointmentId == null || it.AppointmentID != currentAppointmentId)
        }

        for (appt in appointments) {
            try {
                val apptStart = parseTime(appt.Start_Time) ?: continue
                val apptEnd = parseTime(appt.End_Time) ?: continue

                val apptStartCal = Calendar.getInstance().apply { time = apptStart }
                val apptEndCal = Calendar.getInstance().apply { time = apptEnd }

                // Overlap occurs if new Start < existing End AND new End > existing Start
                val isOverlap = startCal.before(apptEndCal) && endCal.after(apptStartCal)
                if (isOverlap) return true
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error parsing appointment times for overlap check", e)
            }
        }
        return false
    }

    fun calculateTotalPrice(
        hourlyRate: Double = _selectedChef.value?.Pricing ?: 0.0,
        appointmentTime: String = _uiState.value.appointmentTime,
        selectedRecipes: List<SelectedAppointmentRecipe> = _selectedRecipes.value,
        userState: String = _uiState.value.state,
        chefState: String = _selectedChef.value?.state.orEmpty()
    ): Double {
        return AppointmentPricingBreakdown.calculate(
            chefHourlyRate = hourlyRate,
            appointmentTime = appointmentTime,
            selectedRecipes = selectedRecipes,
            userState = userState,
            chefState = chefState
        ).finalTotalPrice
    }

    fun getPricingBreakdown(
        hourlyRate: Double = _selectedChef.value?.Pricing ?: 0.0,
        appointmentTime: String = _uiState.value.appointmentTime,
        selectedRecipes: List<SelectedAppointmentRecipe> = _selectedRecipes.value,
        userState: String = _uiState.value.state,
        chefState: String = _selectedChef.value?.state.orEmpty()
    ): AppointmentPricingBreakdown {
        return AppointmentPricingBreakdown.calculate(
            chefHourlyRate = hourlyRate,
            appointmentTime = appointmentTime,
            selectedRecipes = selectedRecipes,
            userState = userState,
            chefState = chefState
        )
    }

    fun createAppointment(
        userId: String,
        chefId: String,
        selectedDate: String,
        startTime: String,
        endTime: String,
        totalPrice: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isNetworkAvailable.value) {
            onError("No internet connection. Please connect to the internet to complete booking.")
            return
        }

        if (userId.isBlank() || chefId.isBlank()) {
            onError("Invalid booking parameters. Please select a valid chef.")
            return
        }

        if (selectedDate.isBlank() || startTime.isBlank() || endTime.isBlank()) {
            onError("Please select a valid appointment date and time slot.")
            return
        }

        val state = uiState.value
        val appointmentTimeSlot = "$startTime - $endTime"

        // Comprehensive Pre-Validation Check before touching database
        val validationErrors = validateFormValues(
            appointmentTime = appointmentTimeSlot,
            address = state.address,
            postcode = state.postcode,
            state = state.state,
            servingSize = state.servingSize,
            description = state.description,
            targetDate = selectedDate
        )

        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(errors = validationErrors, hasAttemptedSubmit = true) }
            val firstErrorMessage = when {
                validationErrors.contains(AppointmentValidationError.TimeSlotOccupied) ->
                    "The selected time slot is already occupied by another booking. Please choose a different time."
                validationErrors.contains(AppointmentValidationError.InvalidTime) ->
                    "Invalid time range selected. End time must be after start time."
                validationErrors.contains(AppointmentValidationError.InvalidAddress) ->
                    "Please enter a valid appointment address."
                validationErrors.contains(AppointmentValidationError.InvalidPostcode) ->
                    "Please enter a valid 5-digit Malaysian postcode."
                validationErrors.contains(AppointmentValidationError.InvalidState) ->
                    "Please select a state from the dropdown."
                validationErrors.contains(AppointmentValidationError.InvalidServingSize) ->
                    "Please specify a valid party / serving size greater than 0."
                validationErrors.contains(AppointmentValidationError.InvalidDescription) ->
                    "Please provide event notes or cooking details for your chef."
                else -> "Please check the form and fix the highlighted errors."
            }
            onError(firstErrorMessage)
            return
        }

        if (totalPrice <= 0.0) {
            onError("Invalid total price calculation. Total price must be greater than zero.")
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        val newAppointment = Appointment(
            Date = selectedDate,
            Start_Time = startTime,
            End_Time = endTime,
            Address = state.address.trim(),
            Postcode = state.postcode.trim(),
            State = state.state,
            Note = state.description.trim(),
            Serving_Size = state.servingSize.toIntOrNull() ?: 1,
            Health_Preference = state.healthPreference,
            Total_Price = totalPrice,
            Status = "Pending",
            chefId = chefId,
            userId = userId
        )

        viewModelScope.launch {
            try {
                repository.createAppointment(newAppointment, _selectedRecipes.value)
                _uiState.update { it.copy(isSubmitting = false) }
                clearAppointmentForm()
                onSuccess()
            } catch (e: AppointmentConflictException) {
                // Server confirmed a time-slot clash then show the precise conflict message
                Log.w("AppointmentBookingVM", "Booking conflict detected: ${e.message}")
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errors = it.errors + AppointmentValidationError.TimeSlotOccupied
                    )
                }
                onError(e.message ?: "This time slot is already booked. Please choose a different time.")
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error creating appointment in repository", e)
                _uiState.update { it.copy(isSubmitting = false) }
                onError("Failed to create appointment: ${e.localizedMessage ?: "Unknown error occurred. Please try again."}")
            }
        }
    }
}
