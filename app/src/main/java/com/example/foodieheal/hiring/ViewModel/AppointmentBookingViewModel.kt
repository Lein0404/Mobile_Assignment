package com.example.foodieheal.hiring.viewmodel

import android.util.Log
import com.example.foodieheal.R
import com.example.foodieheal.MainActivity
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
import com.example.foodieheal.Chef.model.WeeklyAvailability
import com.example.foodieheal.Recipe.Model.Recipe
import com.example.foodieheal.Recipe.Repo.RecipeRepository
import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown
import com.example.foodieheal.hiring.model.AppointmentPricingBreakdown.Companion.parseTimeSlotToCalendars
import com.example.foodieheal.hiring.model.SelectedAppointmentRecipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.foodieheal.hiring.util.HiringNetworkHelper
import java.time.LocalDate
import java.util.Calendar

class AppointmentBookingViewModel(
    private val repository: HiringRepository = HiringRepository(),
    private val networkMonitor: NetworkMonitor? = null,
    private val recipeRepository: RecipeRepository = RecipeRepository()
) : ViewModel() {

    private fun resString(resId: Int, vararg args: Any): String? {
        return MainActivity.appContext?.getString(resId, *args)
    }

    private val _selectedChef = MutableStateFlow<Chef?>(null)
    val selectedChef: StateFlow<Chef?> = _selectedChef.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    private val _chefAppointmentsState = MutableStateFlow(ChefAppointmentsUiState())
    val chefAppointmentsState: StateFlow<ChefAppointmentsUiState> = _chefAppointmentsState.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(HiringNetworkHelper.isDeviceOnline())
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    // Bookmarked recipes for selection
    private val _bookmarkedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val bookmarkedRecipes: StateFlow<List<Recipe>> = _bookmarkedRecipes.asStateFlow()

    private val _isLoadingBookmarks = MutableStateFlow(false)
    val isLoadingBookmarks: StateFlow<Boolean> = _isLoadingBookmarks.asStateFlow()

    // Attached recipes to appointment
    private val _selectedRecipes = MutableStateFlow<List<SelectedAppointmentRecipe>>(emptyList())
    val selectedRecipes: StateFlow<List<SelectedAppointmentRecipe>> = _selectedRecipes.asStateFlow()

    // Rebooking progress state (holds the appointmentId being loaded)
    private val _isRebooking = MutableStateFlow<String?>(null)
    val isRebooking: StateFlow<String?> = _isRebooking.asStateFlow()

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
        HiringNetworkHelper.observeHiringNetwork(
            networkMonitor = networkMonitor,
            coroutineScope = viewModelScope,
            stateFlow = _isNetworkAvailable,
            onReconnected = {
                if (currentChefId.isNotBlank()) {
                    fetchAppointmentsForChef(currentChefId)
                }
            }
        )
    }

    fun selectChef(chef: Chef) {
        _selectedChef.value = chef
    }

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun prepareRebook(
        appointment: Appointment,
        onSuccess: (chefId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val appointmentId = appointment.AppointmentID.orEmpty()
        val chefId = appointment.chefId

        if (chefId.isBlank()) {
            onError("Chef details are missing.")
            return
        }

        viewModelScope.launch {
            _isRebooking.value = appointmentId
            try {
                val chef = repository.fetchChefById(chefId)
                if (chef == null) {
                    onError("Chef profile could not be found.")
                    return@launch
                }
                _selectedChef.value = chef

                val attachedWithDetails = if (appointmentId.isNotBlank()) {
                    repository.fetchAppointmentRecipes(appointmentId)
                } else emptyList()

                val selected = attachedWithDetails.mapNotNull { item ->
                    item.recipe?.let { recipe ->
                        SelectedAppointmentRecipe(
                            recipe = recipe,
                            serviceCount = item.service_count.toInt().coerceAtLeast(1),
                            customNote = item.custom_note.orEmpty(),
                            chefProvidesIngredients = item.chef_provide_ingredient
                        )
                    }
                }
                _selectedRecipes.value = selected

                _uiState.update { current ->
                    current.copy(
                        address = appointment.Address,
                        postcode = appointment.Postcode,
                        state = appointment.State,
                        healthPreference = appointment.Health_Preference,
                        servingSize = appointment.Serving_Size.toString(),
                        description = appointment.Note,
                        appointmentTime = "",
                        startTime = "",
                        endTime = "",
                        hasAttemptedSubmit = false,
                        errors = emptySet()
                    )
                }

                val targetChefId = chef.chefId.ifEmpty { chef.id }
                onSuccess(targetChefId)
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error preparing rebook", e)
                onError(e.localizedMessage ?: "Failed to prepare rebook.")
            } finally {
                _isRebooking.value = null
            }
        }
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
                    customNote = "",
                    chefProvidesIngredients = true
                )
            )
        }
        _selectedRecipes.value = currentList
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

    fun updateChefProvidesIngredients(recipeId: String, provides: Boolean) {
        _selectedRecipes.update { list ->
            list.map { item ->
                if (item.recipe.recipe_id == recipeId) {
                    item.copy(chefProvidesIngredients = provides)
                } else item
            }
        }
    }

    fun removeSelectedRecipe(recipeId: String) {
        _selectedRecipes.update { list ->
            list.filterNot { it.recipe.recipe_id == recipeId }
        }
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
                val parsedTimes = parseTimeSlotToCalendars(parts[0], parts[1])
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
                            val weeklyAvail = WeeklyAvailability.fromJsonElement(chef.availability_hours)
                            val parsedDate = try {
                                LocalDate.parse(targetDate)
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

        val parsedTimes = parseTimeSlotToCalendars(parts[0], parts[1]) ?: return
        val (startCal, endCal) = parsedTimes

        val targetDate = selectedDate.value.toString()
        val isOccupied = isSlotOverlapping(startCal, endCal, targetDate)

        var isUnavailable = false
        var unavailableReason: String? = null
        val chef = selectedChef.value
        if (chef?.availability_hours != null) {
            val weeklyAvail = WeeklyAvailability.fromJsonElement(chef.availability_hours)
            val parsedDate = try {
                LocalDate.parse(targetDate)
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

    private fun isMatchingDate(dateStr: String, targetDateStr: String): Boolean {
        val d1 = dateStr.trim()
        val d2 = targetDateStr.trim()
        if (d1.equals(d2, ignoreCase = true)) return true

        val formats = listOf(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )

        var parsedD1: LocalDate? = null
        for (fmt in formats) {
            try {
                parsedD1 = LocalDate.parse(d1, fmt)
                break
            } catch (_: Exception) {}
        }

        var parsedD2: LocalDate? = null
        for (fmt in formats) {
            try {
                parsedD2 = LocalDate.parse(d2, fmt)
                break
            } catch (_: Exception) {}
        }

        return if (parsedD1 != null && parsedD2 != null) {
            parsedD1 == parsedD2
        } else {
            d1.contains(d2) || d2.contains(d1)
        }
    }

    private fun isSlotOverlapping(
        startCal: Calendar,
        endCal: Calendar,
        targetDate: String,
        currentAppointmentId: String? = null
    ): Boolean {
        val appointments = chefAppointmentsState.value.appointments.filter {
            isMatchingDate(it.Date, targetDate) &&
                    !it.Status.equals("cancelled", ignoreCase = true) &&
                    !it.Status.equals("rejected", ignoreCase = true) &&
                    (currentAppointmentId == null || it.AppointmentID != currentAppointmentId)
        }

        for (appt in appointments) {
            try {
                val (apptStartCal, apptEndCal) = parseTimeSlotToCalendars(appt.Start_Time, appt.End_Time) ?: continue

                // Overlap occurs if new Start < existing End AND new End > existing Start
                val isOverlap = startCal.before(apptEndCal) && endCal.after(apptStartCal)
                if (isOverlap) return true
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error parsing appointment times for overlap check", e)
            }
        }
        return false
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
            onError(resString(R.string.error_booking_no_internet) ?: "No internet connection. Please connect to the internet to complete booking.")
            return
        }

        if (userId.isBlank() || chefId.isBlank()) {
            onError(resString(R.string.error_booking_invalid_parameters) ?: "Invalid booking parameters. Please select a valid chef.")
            return
        }

        if (selectedDate.isBlank() || startTime.isBlank() || endTime.isBlank()) {
            onError(resString(R.string.error_booking_select_date_time) ?: "Please select a valid appointment date and time slot.")
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
                    resString(R.string.error_booking_time_slot_occupied) ?: "The selected time slot is already occupied by another booking. Please choose a different time."
                validationErrors.contains(AppointmentValidationError.InvalidTime) ->
                    resString(R.string.error_booking_invalid_time_range) ?: "Invalid time range selected. End time must be after start time."
                validationErrors.contains(AppointmentValidationError.InvalidAddress) ->
                    resString(R.string.error_booking_invalid_address) ?: "Please enter a valid appointment address."
                validationErrors.contains(AppointmentValidationError.InvalidPostcode) ->
                    resString(R.string.error_booking_invalid_postcode) ?: "Please enter a valid 5-digit Malaysian postcode."
                validationErrors.contains(AppointmentValidationError.InvalidState) ->
                    resString(R.string.error_booking_invalid_state) ?: "Please select a state from the dropdown."
                validationErrors.contains(AppointmentValidationError.InvalidServingSize) ->
                    resString(R.string.error_booking_invalid_serving_size) ?: "Please specify a valid party / serving size greater than 0."
                validationErrors.contains(AppointmentValidationError.InvalidDescription) ->
                    resString(R.string.error_booking_invalid_description) ?: "Please provide event notes or cooking details for your chef."
                else -> resString(R.string.error_booking_fix_errors) ?: "Please check the form and fix the highlighted errors."
            }
            onError(firstErrorMessage)
            return
        }

        if (totalPrice <= 0.0) {
            onError(resString(R.string.error_booking_invalid_total_price) ?: "Invalid total price calculation. Total price must be greater than zero.")
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
                onError(e.message ?: (resString(R.string.error_booking_time_already_booked) ?: "This time slot is already booked. Please choose a different time."))
            } catch (e: Exception) {
                Log.e("AppointmentBookingVM", "Error creating appointment in repository", e)
                _uiState.update { it.copy(isSubmitting = false) }
                val unknownErr = resString(R.string.error_booking_unknown) ?: "Unknown error occurred. Please try again."
                val errorDetail = e.localizedMessage ?: unknownErr
                val errMsg = resString(R.string.error_booking_create_failed, errorDetail) ?: "Failed to create appointment: $errorDetail"
                onError(errMsg)
            }
        }
    }
}
