package com.example.foodieheal.hiring.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.Chef.States
import com.example.foodieheal.Chef.getStateResId
import com.example.foodieheal.R
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.ui.components.getHighlightedText

enum class RateSortOrder(val labelRes: Int) {
    NONE(R.string.filter_all_statuses),
    ASCENDING(R.string.chef_filter_rating_low_to_high),
    DESCENDING(R.string.chef_filter_rating_high_to_low)
}

enum class PriceSortOrder(val labelRes: Int) {
    NONE(R.string.filter_all_statuses),
    ASCENDING(R.string.chef_filter_price_low_to_high),
    DESCENDING(R.string.chef_filter_price_high_to_low)
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
    placeholder: String = stringResource(R.string.chef_filter_search_placeholder)
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
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.chef_filter_cd_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.cancel),
                            contentDescription = stringResource(R.string.chef_filter_cd_clear_search),
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
                    contentDescription = stringResource(R.string.chef_filter_cd_open_filters),
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
            label = { Text(stringResource(R.string.chef_filter_clear_all), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.error
            )
        )

        // Rate Sort Chip
        if (filterState.rateSortOrder != RateSortOrder.NONE) {
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(rateSortOrder = RateSortOrder.NONE)) },
                label = { Text(stringResource(filterState.rateSortOrder.labelRes), fontSize = 12.sp) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // Price Sort Chip
        if (filterState.priceSortOrder != PriceSortOrder.NONE) {
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(priceSortOrder = PriceSortOrder.NONE)) },
                label = { Text(stringResource(filterState.priceSortOrder.labelRes), fontSize = 12.sp) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // State Chip
        filterState.selectedState?.let { state ->
            val stateDisplay = getStateResId(state)?.let { stringResource(it) } ?: state
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(selectedState = null)) },
                label = { Text(stringResource(R.string.chef_filter_chip_state, stateDisplay), fontSize = 12.sp) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // Gender Chip
        filterState.selectedGender?.let { gender ->
            val genderDisplay = if (gender.equals("Male", ignoreCase = true)) {
                stringResource(R.string.chef_filter_gender_male)
            } else if (gender.equals("Female", ignoreCase = true)) {
                stringResource(R.string.chef_filter_gender_female)
            } else {
                gender
            }
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(selectedGender = null)) },
                label = { Text(stringResource(R.string.chef_filter_chip_gender, genderDisplay), fontSize = 12.sp) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // Age Range Chip
        filterState.selectedAgeRange?.let { age ->
            InputChip(
                selected = true,
                onClick = { onFilterChange(filterState.copy(selectedAgeRange = null)) },
                label = { Text(stringResource(R.string.chef_filter_chip_age, age.label), fontSize = 12.sp) },
                trailingIcon = {
                    Icon(painter = painterResource(R.drawable.cancel), contentDescription = null, modifier = Modifier.size(14.dp))
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

    val localizedStateMap = distinctStates.associateWith { state ->
        getStateResId(state)?.let { stringResource(it) } ?: state
    }

    val filteredStates = remember(stateSearchQuery, distinctStates, localizedStateMap) {
        if (stateSearchQuery.isBlank()) distinctStates
        else distinctStates.filter { state ->
            val localizedName = localizedStateMap[state] ?: state
            state.contains(stateSearchQuery.trim(), ignoreCase = true) ||
            localizedName.contains(stateSearchQuery.trim(), ignoreCase = true)
        }
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
                text = stringResource(R.string.chef_filter_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = {
                    tempState = ChefFilterState(searchQuery = tempState.searchQuery)
                    stateSearchQuery = ""
                }
            ) {
                Text(stringResource(R.string.chef_filter_reset_all), color = MaterialTheme.colorScheme.error)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Sort by Rating
            FilterSectionHeader(icon = R.drawable.ic_star, title = stringResource(R.string.chef_filter_sort_by_rating))
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
                    label = { Text(stringResource(R.string.chef_filter_rating_high_to_low)) },
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
                    label = { Text(stringResource(R.string.chef_filter_rating_low_to_high)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionHeader(icon = R.drawable.dollar_symbol, title = stringResource(R.string.chef_filter_sort_by_rate))
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
                    label = { Text(stringResource(R.string.chef_filter_price_low_to_high)) },
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
                    label = { Text(stringResource(R.string.chef_filter_price_high_to_low)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionHeader(icon = null, title = stringResource(R.string.chef_filter_chef_gender))
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
                        label = {
                            Text(
                                if (gender.equals("Male", ignoreCase = true)) stringResource(R.string.chef_filter_gender_male)
                                else stringResource(R.string.chef_filter_gender_female)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilterSectionHeader(icon = R.drawable.ic_clock, title = stringResource(R.string.chef_filter_chef_age))
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

            FilterSectionHeader(icon = R.drawable.location, title = stringResource(R.string.chef_filter_state_location))

            OutlinedTextField(
                value = stateSearchQuery,
                onValueChange = { stateSearchQuery = it },
                placeholder = { Text(stringResource(R.string.chef_filter_search_state_placeholder), fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(24.dp))
                },
                trailingIcon = {
                    if (stateSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { stateSearchQuery = "" }) {
                            Icon(painter = painterResource(R.drawable.cancel), contentDescription = stringResource(R.string.chef_filter_cd_clear_search))
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
                        val displayName = localizedStateMap[state] ?: (getStateResId(state)?.let { stringResource(it) } ?: state)
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
                                        fullText = displayName,
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
                    val selectedStateDisplay = getStateResId(selected)?.let { stringResource(it) } ?: selected
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.chef_filter_selected_location),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InputChip(
                        selected = true,
                        onClick = { tempState = tempState.copy(selectedState = null) },
                        label = { Text(selectedStateDisplay, fontSize = 11.sp) },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.cancel),
                                contentDescription = stringResource(R.string.chef_filter_remove),
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
                text = stringResource(R.string.chef_filter_apply_filters),
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
