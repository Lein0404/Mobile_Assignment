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
 *
 * @param searchQuery          Current search text
 * @param onSearchQueryChange  Callback when the search text changes
 * @param searchPlaceholder    Placeholder string shown in the search field
 * @param selectedCategories   Currently selected category chips
 * @param onToggleCategory     Callback when a chip is toggled
 * @param categoriesLabel      Header text above the chip row (e.g. "Categories")
 * @param showFilterIcon       Whether to show the status-filter icon (request screens only)
 * @param isFilterActive       Highlights the filter icon when a status filter is applied
 * @param onFilterClick        Callback when the filter icon is tapped
 * @param unselectedLabelColor Label color for unselected chips (defaults to onSecondary)
 */
@Composable
fun IngredientSearchAndFilter(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    selectedCategories: Set<IngredientCategory>,
    onToggleCategory: (IngredientCategory) -> Unit,
    categoriesLabel: String = stringResource(R.string.shopping_list_categories),
    showFilterIcon: Boolean = false,
    isFilterActive: Boolean = false,
    onFilterClick: () -> Unit = {},
    lazyRowState: LazyListState = rememberLazyListState()
) {
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
    Text(
        text = categoriesLabel,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_l))
    )
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_sm)))

    // 2. Category Chips
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

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))
}
