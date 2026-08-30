package com.example.foodieheal.hiring.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.Chef.States
import com.example.foodieheal.R
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.ui.components.getHighlightedText

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChefFilterBottomSheet(
    filterState: ChefFilterState,
    availableStates: List<String> = States,
    onApply: (ChefFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var tempState by remember { mutableStateOf(filterState) }
    var stateSearchQuery by remember { mutableStateOf("") }

    val distinctStates = remember(availableStates) {
        availableStates.distinct().sorted()
    }

    val filteredStates = remember(stateSearchQuery, distinctStates) {
        if (stateSearchQuery.isBlank()) distinctStates
        else distinctStates.filter { it.contains(stateSearchQuery.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter & Sort Chefs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = {
                    tempState = ChefFilterState(searchQuery = tempState.searchQuery)
                    stateSearchQuery = ""
                }
            ) {
                Text("Reset All", color = MaterialTheme.colorScheme.error)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Sort by Rating
            FilterSectionHeader(icon = R.drawable.ic_star, title = "Sort by Rating")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempState.rateSortOrder == RateSortOrder.DESCENDING,
                    onClick = {
                        tempState = tempState.copy(
                            rateSortOrder = if (tempState.rateSortOrder == RateSortOrder.DESCENDING) RateSortOrder.NONE else RateSortOrder.DESCENDING
                        )
                    },
                    label = { Text("High to Low") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = tempState.rateSortOrder == RateSortOrder.ASCENDING,
                    onClick = {
                        tempState = tempState.copy(
                            rateSortOrder = if (tempState.rateSortOrder == RateSortOrder.ASCENDING) RateSortOrder.NONE else RateSortOrder.ASCENDING
                        )
                    },
                    label = { Text("Low to High") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionHeader(icon = R.drawable.dollar_symbol, title = "Sort by Hourly Rate")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempState.priceSortOrder == PriceSortOrder.ASCENDING,
                    onClick = {
                        tempState = tempState.copy(
                            priceSortOrder = if (tempState.priceSortOrder == PriceSortOrder.ASCENDING) PriceSortOrder.NONE else PriceSortOrder.ASCENDING
                        )
                    },
                    label = { Text("Price: Low to High") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = tempState.priceSortOrder == PriceSortOrder.DESCENDING,
                    onClick = {
                        tempState = tempState.copy(
                            priceSortOrder = if (tempState.priceSortOrder == PriceSortOrder.DESCENDING) PriceSortOrder.NONE else PriceSortOrder.DESCENDING
                        )
                    },
                    label = { Text("Price: High to Low") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionHeader(icon = null, title = "Chef Gender")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Male", "Female").forEach { gender ->
                    FilterChip(
                        selected = tempState.selectedGender.equals(gender, ignoreCase = true),
                        onClick = {
                            tempState = tempState.copy(
                                selectedGender = if (tempState.selectedGender.equals(gender, ignoreCase = true)) null else gender
                            )
                        },
                        label = { Text(gender) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionHeader(icon = R.drawable.ic_clock, title = "Chef Age")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
                        label = { Text(range.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionHeader(icon = R.drawable.location, title = "State / Location")

            OutlinedTextField(
                value = stateSearchQuery,
                onValueChange = { stateSearchQuery = it },
                placeholder = { Text("Search location / state...", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp))
                },
                trailingIcon = {
                    if (stateSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { stateSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (distinctStates.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredStates) { state ->
                        val isSelected = tempState.selectedState.equals(state, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tempState = tempState.copy(
                                    selectedState = if (isSelected) null else state
                                )
                            },
                            label = {
                                Text(
                                    text = getHighlightedText(
                                        fullText = state,
                                        query = stateSearchQuery
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Show Selected State Chip
                tempState.selectedState?.let { selected ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selected Location:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InputChip(
                        selected = true,
                        onClick = { tempState = tempState.copy(selectedState = null) },
                        label = { Text(selected, fontSize = 11.sp) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        Button(
            onClick = {
                onApply(tempState)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Apply Filters",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FilterSectionHeader(
    title: String,
    @DrawableRes icon: Int? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
