package com.example.foodieheal.hiring.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodieheal.Chef.States
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.ActiveFiltersRow
import com.example.foodieheal.ui.components.ChefFilterBottomSheet
import com.example.foodieheal.ui.components.ChefFilterState
import com.example.foodieheal.ui.components.ChefSearchBar
import com.example.foodieheal.ui.components.filterAndSortChefs
import com.example.foodieheal.Chef.model.Chef
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularChefsTabContent(
    chefs: List<Chef>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onChefClick: (Chef) -> Unit,
    onRefresh: () -> Unit = onRetry,
    modifier: Modifier = Modifier
) {
    var filterState by remember { mutableStateOf(ChefFilterState()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filteredChefs = remember(chefs, filterState) {
        filterAndSortChefs(chefs, filterState)
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        // Search & Filter Trigger Bar
        ChefSearchBar(
            query = filterState.searchQuery,
            onQueryChange = { filterState = filterState.copy(searchQuery = it) },
            onFilterClick = { showFilterSheet = true },
            activeFilterCount = filterState.activeFilterCount,
            placeholder = stringResource(R.string.search_chef_placeholder)
        )

        // Active filter chips row
        ActiveFiltersRow(
            filterState = filterState,
            onFilterChange = { filterState = it },
            onResetAll = { filterState = ChefFilterState(searchQuery = filterState.searchQuery) }
        )

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading && chefs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                !errorMessage.isNullOrEmpty() && chefs.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(R.string.btn_retry))
                            }
                        }
                    }
                }
                chefs.isNotEmpty() -> {
                    if (filteredChefs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
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
                                    text = stringResource(R.string.no_chefs_match_filters),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.adjust_search_criteria_or_reset),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = { filterState = ChefFilterState() }
                                ) {
                                    Text(stringResource(R.string.reset_all_filters))
                                }
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
                                        text = stringResource(R.string.header_chef),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    Text(
                                        text = stringResource(R.string.chefs_found_count, filteredChefs.size),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(filteredChefs, key = { it.chefId.ifEmpty { it.id } }) { chef ->
                                ChefHireItem(
                                    chef = chef,
                                    onClick = { onChefClick(chef) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_no_chefs_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

@Composable
fun ChefHireItem(
    chef: Chef,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                if (chef.profilePictureUrl.isNullOrEmpty()) {
                    Text(
                        text = chef.name?.take(1)?.uppercase() ?: stringResource(R.string.default_initial_chef),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chef Name
            Text(
                text = chef.name.ifEmpty { stringResource(R.string.unknown_chef) },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Experience & Location
            val expText = stringResource(R.string.experience_years_short, chef.experience ?: 0)
            val locationText = chef.state?.takeIf { it.isNotBlank() }.orEmpty()

            Text(
                text = expText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = locationText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating and Pricing Row at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rating = chef.averagerating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = stringResource(R.string.rating_star),
                        tint = Color(0xFFFFB300), // Gold Star Color
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = if (rating != null && rating > 0.0) String.format(Locale.US, "%.1f", rating) else stringResource(R.string.not_available),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                chef.Pricing?.let { price ->
                    Text(
                        text = stringResource(R.string.rate_per_hour, price.toInt()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
