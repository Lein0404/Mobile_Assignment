package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.model.Status

/**
 * Shared composable for Search Bar with Filter Icon + optional ScrollableTabRow for status filters.
 */
@Composable
fun IngredientSearchAndFilter(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    showFilterIcon: Boolean = true,
    isFilterActive: Boolean = false,
    onFilterClick: () -> Unit = {},
    showStatusTabs: Boolean = false,
    selectedStatus: Status? = null,
    onStatusSelected: (Status?) -> Unit = {},
    totalCount: Int = 0,
    pendingCount: Int = 0,
    approvedCount: Int = 0,
    rejectedCount: Int = 0
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = searchPlaceholder,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = dimensionResource(id = R.dimen.padding_xsm))
                    ) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.cancel),
                                    contentDescription = stringResource(R.string.clear_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_small_size))
                                )
                            }
                        }
                        if (showFilterIcon) {
                            IconButton(onClick = onFilterClick) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter_alt),
                                    contentDescription = stringResource(R.string.ingredients_requests_apply_filter),
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.padding_l)),
                singleLine = true,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(if (showStatusTabs) dimensionResource(id = R.dimen.padding_sm) else dimensionResource(id = R.dimen.padding_l)))

        // 2. Status Filter Tabs
        if (showStatusTabs) {
            val tabs = listOf(
                Triple(null, stringResource(R.string.admin_filter_all), totalCount),
                Triple(Status.PENDING, stringResource(R.string.admin_filter_pending), pendingCount),
                Triple(Status.APPROVED, stringResource(R.string.admin_filter_approved), approvedCount),
                Triple(Status.REJECTED, stringResource(R.string.admin_filter_rejected), rejectedCount)
            )

            val selectedIndex = when (selectedStatus) {
                null -> 0
                Status.PENDING -> 1
                Status.APPROVED -> 2
                Status.REJECTED -> 3
            }

            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = dimensionResource(id = R.dimen.padding_l),
                indicator = { tabPositions ->
                    if (selectedIndex in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                tabs.forEachIndexed { index, (status, title, count) ->
                    val isSelected = selectedIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { onStatusSelected(status) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
}
