package com.example.foodieheal.ingredients.shared

import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.Units

/**
 * Shared state for a single unit-calorie row in the ingredient form.
 * Used across Admin and User ingredient form screens.
 */
data class UnitRowState(
    val selectedUnit: Units? = null,
    val calories: String = "",
    val unitError: Int? = null,
    val caloriesError: Int? = null,
)

/**
 * Shared form state for ingredient creation/editing/review screens.
 * Contains all fields common to AdminAddIngredientScreen,
 * AdminIngredientRequestFormScreen, and IngredientRequestFormScreen.
 */
data class IngredientFormState(
    val requestId: String? = null,
    val ingredientName: String = "",
    val category: IngredientCategory? = null,
    val description: String = "",
    val imageUrl: String? = null,
    val unitRows: List<UnitRowState> = listOf(UnitRowState()),
    val isSubmitting: Boolean = false,
    val isStatusConflict: Boolean = false,
    val errorMessage: Int? = null,

    // Per-field validation errors
    val nameError: Int? = null,
    val categoryError: Int? = null,
    val descriptionError: Int? = null,
    val unitRowsError: Int? = null,
)

/**
 * Stateless helper that provides shared form manipulation and validation logic
 * for ingredient forms. Each ViewModel delegates to these pure functions
 * instead of duplicating the logic.
 */
object IngredientFormHelper {

    // ──────────────────────────────────────────────
    // Field update helpers (clear error on edit)
    // ──────────────────────────────────────────────

    fun updateName(state: IngredientFormState, name: String): IngredientFormState =
        state.copy(ingredientName = name, nameError = null)

    fun updateCategory(state: IngredientFormState, category: IngredientCategory): IngredientFormState =
        state.copy(category = category, categoryError = null)

    fun updateDescription(state: IngredientFormState, desc: String): IngredientFormState =
        state.copy(description = desc, descriptionError = null)

    // ──────────────────────────────────────────────
    // Unit row helpers
    // ──────────────────────────────────────────────

    fun addUnitRow(state: IngredientFormState): IngredientFormState =
        state.copy(unitRows = state.unitRows + UnitRowState(), unitRowsError = null)

    fun updateUnitRow(state: IngredientFormState, index: Int, unit: Units?, calories: String): IngredientFormState {
        val newList = state.unitRows.toMutableList()
        newList[index] = newList[index].copy(
            selectedUnit = unit,
            calories = calories,
            unitError = null,
            caloriesError = null
        )
        return state.copy(unitRows = newList, unitRowsError = null)
    }

    fun removeUnitRow(state: IngredientFormState, index: Int): IngredientFormState {
        if (state.unitRows.size <= 1) return state
        val newList = state.unitRows.toMutableList()
        newList.removeAt(index)
        return state.copy(unitRows = newList)
    }

    // ──────────────────────────────────────────────
    // Validation
    // ──────────────────────────────────────────────

    /**
     * Validates all form fields.
     * @return a [Pair] of (isValid, updatedStateWithErrors).
     * The caller should apply the updated state to the StateFlow.
     */
    fun validateForm(state: IngredientFormState): Pair<Boolean, IngredientFormState> {
        var isValid = true

        val nameError = if (state.ingredientName.isBlank()) {
            isValid = false
            R.string.error_name_required
        } else null

        val categoryError = if (state.category == null) {
            isValid = false
            R.string.error_category_required
        } else null

        val descriptionError = if (state.description.isBlank()) {
            isValid = false
            R.string.error_description_required
        } else null

        // At least one unit row must be fully filled
        val hasAtLeastOneFilledRow = state.unitRows.any {
            it.selectedUnit != null && it.calories.isNotBlank()
        }
        val unitRowsError = if (!hasAtLeastOneFilledRow) {
            isValid = false
            R.string.error_at_least_one_unit_required
        } else null

        // Per-row validation for partially filled rows
        val validatedRows = state.unitRows.map { row ->
            val unitError = if (row.selectedUnit == null && row.calories.isNotBlank()) {
                isValid = false
                R.string.error_unit_required
            } else null

            val caloriesError = if (row.selectedUnit != null && row.calories.isBlank()) {
                isValid = false
                R.string.error_calories_required
            } else if (row.calories.contains(".")) {
                isValid = false
                R.string.error_no_decimal_allowed
            } else if (row.calories.isNotBlank() && row.calories.toIntOrNull() == null) {
                isValid = false
                R.string.error_invalid_number
            } else null

            row.copy(unitError = unitError, caloriesError = caloriesError)
        }

        val updatedState = state.copy(
            nameError = nameError,
            categoryError = categoryError,
            descriptionError = descriptionError,
            unitRowsError = unitRowsError,
            unitRows = validatedRows
        )

        return Pair(isValid, updatedState)
    }

    // ──────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────

    fun clearError(state: IngredientFormState): IngredientFormState =
        state.copy(errorMessage = null)
}
