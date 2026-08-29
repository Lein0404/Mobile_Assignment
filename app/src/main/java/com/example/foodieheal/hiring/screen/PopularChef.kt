package com.example.foodieheal.hiring.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.foodieheal.Chef.States
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.Chef.model.WeeklyAvailability
import com.example.foodieheal.R
import com.example.foodieheal.hiring.components.ActiveFiltersRow
import com.example.foodieheal.hiring.components.ChefFilterBottomSheet
import com.example.foodieheal.hiring.components.ChefFilterState
import com.example.foodieheal.hiring.components.ChefSearchBar
import com.example.foodieheal.hiring.components.filterAndSortChefs
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
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
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(12.dp),
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
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
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
                            contentPadding = PaddingValues(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.header_chef),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                        Text(
                                            text = stringResource(R.string.chefs_found_count, filteredChefs.size),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                if (chef.profilePictureUrl.isNullOrEmpty()) {
                    Text(
                        text = chef.name.take(1).uppercase(Locale.ROOT).ifBlank {
                            stringResource(R.string.default_initial_chef)
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
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

            // Availability Badge
            val isAvailableToday = remember(chef.availability_hours) {
                WeeklyAvailability.fromJsonElement(chef.availability_hours).isAvailableToday()
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isAvailableToday) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isAvailableToday) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Text(
                        text = if (isAvailableToday) {
                            stringResource(R.string.chef_available_today)
                        } else {
                            stringResource(R.string.chef_off_today)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailableToday) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chef Name
            Text(
                text = chef.name.ifEmpty { stringResource(R.string.unknown_chef) },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Experience & Location Details
            val expText = stringResource(R.string.experience_years_short, chef.experience ?: 0)
            val locationText = chef.state?.takeIf { it.isNotBlank() } ?: stringResource(R.string.none_selected)

            Text(
                text = "$expText • $locationText",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            // Rating and Pricing Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rating = chef.averagerating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = stringResource(R.string.rating_star),
                        tint = Color(0xFFFFB300), // Gold Star
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = if (rating != null && rating > 0.0) {
                            String.format(Locale.US, "%.1f", rating)
                        } else {
                            stringResource(R.string.chef_new_rating)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                chef.Pricing?.let { price ->
                    Text(
                        text = stringResource(R.string.rate_per_hour, price.toInt()),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
