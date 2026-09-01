package com.example.foodieheal.ingredients.shared

import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.IngredientRequest
import com.example.foodieheal.model.Status
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Stateless helper for filtering ingredient request items across Admin and User portals.
 */
object IngredientRequestFilterHelper {

    fun parseToLocalDate(isoString: String?): LocalDate? {
        if (isoString.isNullOrBlank()) return null
        return try {
            ZonedDateTime.parse(isoString)
                .withZoneSameInstant(ZoneId.of("Asia/Kuala_Lumpur"))
                .toLocalDate()
        } catch (_: Exception) {
            try {
                Instant.parse(isoString)
                    .atZone(ZoneId.of("Asia/Kuala_Lumpur"))
                    .toLocalDate()
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Filters a list of request items based on search query, categories, status, and date ranges.
     */
    fun <T> filterRequests(
        items: List<T>,
        searchQuery: String,
        selectedCategories: Set<IngredientCategory>,
        selectedStatus: Status?,
        createdDateStart: LocalDate?,
        createdDateEnd: LocalDate?,
        processedDateStart: LocalDate?,
        processedDateEnd: LocalDate?,
        getRequest: (T) -> IngredientRequest
    ): List<T> {
        val query = searchQuery.trim()
        return items.filter { item ->
            val request = getRequest(item)
            val matchesQuery = query.isEmpty() ||
                request.ingredientName.contains(query, ignoreCase = true) ||
                request.ingredientDesc.contains(query, ignoreCase = true)

            val matchesCategory = selectedCategories.isEmpty() ||
                request.ingredientCategory == null ||
                selectedCategories.contains(request.ingredientCategory)

            val matchesStatus = selectedStatus == null || request.requestStatus == selectedStatus

            val createdDate = parseToLocalDate(request.datetimeCreated)
            val matchesCreatedDate = when {
                createdDateStart != null && createdDateEnd != null ->
                    createdDate != null && !createdDate.isBefore(createdDateStart) && !createdDate.isAfter(createdDateEnd)
                createdDateStart != null ->
                    createdDate != null && !createdDate.isBefore(createdDateStart)
                createdDateEnd != null ->
                    createdDate != null && !createdDate.isAfter(createdDateEnd)
                else -> true
            }

            val processedDate = parseToLocalDate(request.datetimeProcessed)
            val matchesProcessedDate = when {
                processedDateStart != null && processedDateEnd != null ->
                    processedDate != null && !processedDate.isBefore(processedDateStart) && !processedDate.isAfter(processedDateEnd)
                processedDateStart != null ->
                    processedDate != null && !processedDate.isBefore(processedDateStart)
                processedDateEnd != null ->
                    processedDate != null && !processedDate.isAfter(processedDateEnd)
                else -> true
            }

            matchesQuery && matchesCategory && matchesStatus && matchesCreatedDate && matchesProcessedDate
        }
    }
}
