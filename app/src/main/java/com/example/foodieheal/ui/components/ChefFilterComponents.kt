package com.example.foodieheal.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.Chef.States
import com.example.foodieheal.R
import com.example.foodieheal.Chef.model.Chef

enum class RateSortOrder(val label: String) {
    NONE("None"),
    ASCENDING("Rating: Low to High"),
    DESCENDING("Rating: High to Low")
}

enum class PriceSortOrder(val label: String) {
    NONE("None"),
    ASCENDING("Price: Low to High"),
    DESCENDING("Price: High to Low")
}

enum class AgeRange(val label: String) {
    YOUNG("18 - 30"),
    MID("31 - 45"),
    SENIOR("45+")
}

data class ChefFilterState(
    val searchQuery: String = "",
    val rateSortOrder: RateSortOrder = RateSortOrder.NONE,
    val priceSortOrder: PriceSortOrder = PriceSortOrder.NONE,
    val selectedState: String? = null,
    val selectedAgeRange: AgeRange? = null,
    val selectedGender: String? = null
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (rateSortOrder != RateSortOrder.NONE) count++
            if (priceSortOrder != PriceSortOrder.NONE) count++
            if (!selectedState.isNullOrBlank()) count++
            if (selectedAgeRange != null) count++
            if (!selectedGender.isNullOrBlank()) count++
            return count
        }

    val isFilterActive: Boolean
        get() = activeFilterCount > 0 || searchQuery.isNotBlank()
}

fun filterAndSortChefs(chefs: List<Chef>, filter: ChefFilterState): List<Chef> {
    return chefs.filter { chef ->
        // Search Query (name, state, or description)
        val matchesQuery = filter.searchQuery.isBlank() ||
                chef.name.contains(filter.searchQuery.trim(), ignoreCase = true) ||
                (chef.state?.contains(filter.searchQuery.trim(), ignoreCase = true) == true) ||
                (chef.description?.contains(filter.searchQuery.trim(), ignoreCase = true) == true)

        // State Filter
        val matchesState = filter.selectedState == null ||
                chef.state.equals(filter.selectedState, ignoreCase = true)

        // Age Filter
        val chefAge = chef.age ?: 0
        val matchesAge = filter.selectedAgeRange == null || when (filter.selectedAgeRange) {
            AgeRange.YOUNG -> chefAge in 18..30
            AgeRange.MID -> chefAge in 31..45
            AgeRange.SENIOR -> chefAge > 45
        }

        // Gender Filter
        val matchesGender = filter.selectedGender == null ||
                chef.gender.equals(filter.selectedGender, ignoreCase = true)

        matchesQuery && matchesState && matchesAge && matchesGender
    }.let { list ->
        var result = list

        // Sort by Rating
        result = when (filter.rateSortOrder) {
            RateSortOrder.ASCENDING -> result.sortedBy { it.averagerating ?: 0.0 }
            RateSortOrder.DESCENDING -> result.sortedByDescending { it.averagerating ?: 0.0 }
            RateSortOrder.NONE -> result
        }

        // Sort by Price (if specified)
        result = when (filter.priceSortOrder) {
            PriceSortOrder.ASCENDING -> result.sortedBy { it.Pricing ?: 0.0 }
            PriceSortOrder.DESCENDING -> result.sortedByDescending { it.Pricing ?: 0.0 }
            PriceSortOrder.NONE -> result
        }

        result
    }
}

@Composable
fun ChefSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    activeFilterCount: Int,
    modifier: Modifier = Modifier,
    placeholder: String = "Search chef by name, state..."
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Filter Action Button with Badge
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(text = activeFilterCount.toString())
                    }
                }
            }
        ) {
            FilledTonalIconButton(
                onClick = onFilterClick,
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (activeFilterCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filter),
                    contentDescription = "Open Filters",
                    tint = if (activeFilterCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveFiltersRow(
    filterState: ChefFilterState,
    onFilterChange: (ChefFilterState) -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (filterState.activeFilterCount == 0) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset All Button
        AssistChip(
            onClick = onResetAll,
            label = { Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.error
            )
        )

        // Rate Sort Chip
        if (filterState.rateSortOrder != RateSortOrder.NONE) {
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(rateSortOrder = RateSortOrder.NONE)) },
                label = { Text(filterState.rateSortOrder.label, fontSize = 12.sp) },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // Price Sort Chip
        if (filterState.priceSortOrder != PriceSortOrder.NONE) {
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(priceSortOrder = PriceSortOrder.NONE)) },
                label = { Text(filterState.priceSortOrder.label, fontSize = 12.sp) },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // State Chip
        filterState.selectedState?.let { state ->
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(selectedState = null)) },
                label = { Text("State: $state", fontSize = 12.sp) },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // Gender Chip
        filterState.selectedGender?.let { gender ->
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(selectedGender = null)) },
                label = { Text("Gender: $gender", fontSize = 12.sp) },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // Age Range Chip
        filterState.selectedAgeRange?.let { age ->
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(selectedAgeRange = null)) },
                label = { Text("Age: ${age.label}", fontSize = 12.sp) },
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }
    }
}

@Composable
fun ChefFilterBottomSheet(
    filterState: ChefFilterState,
    availableStates: List<String> = States,
    onApply: (ChefFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var tempState by remember { mutableStateOf(filterState) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter & Sort Chefs",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = { tempState = ChefFilterState(searchQuery = tempState.searchQuery) }) {
                Text("Reset All", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Sort by Rating
        FilterSectionHeader(title = "Sort by Rating")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tempState.rateSortOrder == RateSortOrder.DESCENDING,
                onClick = {
                    tempState = tempState.copy(
                        rateSortOrder = if (tempState.rateSortOrder == RateSortOrder.DESCENDING) RateSortOrder.NONE else RateSortOrder.DESCENDING
                    )
                },
                label = { Text("High to Low") }
            )
            FilterChip(
                selected = tempState.rateSortOrder == RateSortOrder.ASCENDING,
                onClick = {
                    tempState = tempState.copy(
                        rateSortOrder = if (tempState.rateSortOrder == RateSortOrder.ASCENDING) RateSortOrder.NONE else RateSortOrder.ASCENDING
                    )
                },
                label = { Text("Low to High") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Sort by Hourly Rate / Price
        FilterSectionHeader(title = "Sort by Hourly Rate")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tempState.priceSortOrder == PriceSortOrder.ASCENDING,
                onClick = {
                    tempState = tempState.copy(
                        priceSortOrder = if (tempState.priceSortOrder == PriceSortOrder.ASCENDING) PriceSortOrder.NONE else PriceSortOrder.ASCENDING
                    )
                },
                label = { Text("Price: Low to High") }
            )
            FilterChip(
                selected = tempState.priceSortOrder == PriceSortOrder.DESCENDING,
                onClick = {
                    tempState = tempState.copy(
                        priceSortOrder = if (tempState.priceSortOrder == PriceSortOrder.DESCENDING) PriceSortOrder.NONE else PriceSortOrder.DESCENDING
                    )
                },
                label = { Text("Price: High to Low") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Gender
        FilterSectionHeader(title = "Chef Gender")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Male", "Female").forEach { gender ->
                FilterChip(
                    selected = tempState.selectedGender.equals(gender, ignoreCase = true),
                    onClick = {
                        tempState = tempState.copy(
                            selectedGender = if (tempState.selectedGender.equals(gender, ignoreCase = true)) null else gender
                        )
                    },
                    label = { Text(gender) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Age Range
        FilterSectionHeader(title = "Chef Age")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgeRange.entries.forEach { range ->
                FilterChip(
                    selected = tempState.selectedAgeRange == range,
                    onClick = {
                        tempState = tempState.copy(
                            selectedAgeRange = if (tempState.selectedAgeRange == range) null else range
                        )
                    },
                    label = { Text(range.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. State / Location (from ReusableList.kt States)
        FilterSectionHeader(title = "State / Location")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableStates.forEach { state ->
                FilterChip(
                    selected = tempState.selectedState.equals(state, ignoreCase = true),
                    onClick = {
                        tempState = tempState.copy(
                            selectedState = if (tempState.selectedState.equals(state, ignoreCase = true)) null else state
                        )
                    },
                    label = { Text(state) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Apply Button
        Button(
            onClick = {
                onApply(tempState)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Apply Filters",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
