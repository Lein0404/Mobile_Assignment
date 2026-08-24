package com.example.foodieheal.hiring.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.Chef.States
import com.example.foodieheal.R
import com.example.foodieheal.hiring.viewmodel.BookmarkViewModel
import com.example.foodieheal.ui.components.ActiveFiltersRow
import com.example.foodieheal.ui.components.ChefFilterBottomSheet
import com.example.foodieheal.ui.components.ChefFilterState
import com.example.foodieheal.ui.components.ChefSearchBar
import com.example.foodieheal.ui.components.filterAndSortChefs
import com.example.mobileassignmentloginpart.Model.Chef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkedChefsTabContent(
    viewModel: BookmarkViewModel,
    onChefClick: (Chef) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarkedChefs = viewModel.bookmarkedChefsList
    var filterState by remember { mutableStateOf(ChefFilterState()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val filteredBookmarkedChefs = remember(bookmarkedChefs, filterState) {
        filterAndSortChefs(bookmarkedChefs, filterState)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (bookmarkedChefs.isNotEmpty()) {
            // Search & Filter Bar
            ChefSearchBar(
                query = filterState.searchQuery,
                onQueryChange = { filterState = filterState.copy(searchQuery = it) },
                onFilterClick = { showFilterSheet = true },
                activeFilterCount = filterState.activeFilterCount,
                placeholder = "Search bookmarks..."
            )

            // Active Filters Row
            ActiveFiltersRow(
                filterState = filterState,
                onFilterChange = { filterState = it },
                onResetAll = { filterState = ChefFilterState(searchQuery = filterState.searchQuery) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            val isLoading = viewModel.isLoadingBookmarks
            if (isLoading && bookmarkedChefs.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (bookmarkedChefs.isNotEmpty()) {
                if (filteredBookmarkedChefs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.filter),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No bookmarks match your filters",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search criteria or resetting filters.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { filterState = ChefFilterState() }) {
                            Text("Reset Filters")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.header_bookmarked_chefs, filteredBookmarkedChefs.size),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = "${filteredBookmarkedChefs.size} saved",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            items = filteredBookmarkedChefs,
                            key = { it.chefId.ifEmpty { it.id } }
                        ) { chef ->
                            ChefHireItem(
                                chef = chef,
                                onClick = { onChefClick(chef) }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bookmark),
                        contentDescription = stringResource(R.string.no_bookmarks),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.empty_no_bookmarked_chefs),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.empty_bookmarked_chefs_sub),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ChefFilterBottomSheet(
                filterState = filterState,
                availableStates = States,
                onApply = { updated ->
                    filterState = updated
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}
