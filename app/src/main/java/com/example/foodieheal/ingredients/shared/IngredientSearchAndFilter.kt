package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory

/**
 * Shared composable for the search bar + "Categories" label + category filter chips
 * used across IngredientsExistingScreen, IngredientRequestsScreen,
 * and AdminIngredientRequestsScreen.
 */
@Composable
fun IngredientSearchAndFilter(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    selectedCategories: Set<IngredientCategory>,
    onToggleCategory: (IngredientCategory) -> Unit,
    categoriesLabel: String = stringResource(R.string.categories_filter_header),
    showFilterIcon: Boolean = false,
    isFilterActive: Boolean = false,
    onFilterClick: () -> Unit = {},
    lazyRowState: LazyListState = rememberLazyListState(),
    isExpanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    val headerColor =
        if (selectedCategories.isNotEmpty()) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

    // 1. Search Bar
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = searchPlaceholder,
                style = MaterialTheme.typography.labelLarge
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.padding_l)),
        singleLine = true,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
        trailingIcon = {
            if (showFilterIcon) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = dimensionResource(id = R.dimen.padding_md))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_alt),
                        contentDescription = stringResource(R.string.ingredients_requests_apply_filter),
                        tint = if (isFilterActive) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        modifier = Modifier.clickable { onFilterClick() }
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_md)))
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.clickable { onFilterClick() }
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.search)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

    // 2. Category Header with Toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!isExpanded) }
            .padding(horizontal = dimensionResource(id = R.dimen.padding_l)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = categoriesLabel,
            fontWeight = FontWeight.Bold,
            color = headerColor
        )
        Icon(
            painter =
                if (isExpanded) painterResource(R.drawable.ic_arrow_drop_up)
                else painterResource(R.drawable.ic_arrow_drop_down),
            contentDescription =
                if (isExpanded) stringResource(R.string.ingredients_collapse_categories)
                else stringResource(R.string.ingredients_expand_categories),
            tint = headerColor
        )
    }

    if (isExpanded) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_sm)))

        // 3. Category Chips
        LazyRow(
            state = lazyRowState,
            contentPadding = PaddingValues(horizontal = dimensionResource(id = R.dimen.padding_l)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_smd))
        ) {
            items(IngredientCategory.entries) { category ->
                val isSelected = selectedCategories.contains(category)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleCategory(category) },
                    label = {
                        Text(
                            text = category.categoryName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_md)),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        labelColor = MaterialTheme.colorScheme.onSecondary,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = null
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))
}
