package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Reusable filter bottom sheet for ingredients and ingredient requests across Admin and User portals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientRequestFilterBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    selectedCategories: Set<IngredientCategory>,
    onToggleCategory: (IngredientCategory) -> Unit,
    showDateFilters: Boolean = true,
    createdStartDate: LocalDate? = null,
    createdEndDate: LocalDate? = null,
    onCreatedStartDateChange: (LocalDate?) -> Unit = {},
    onCreatedEndDateChange: (LocalDate?) -> Unit = {},
    processedStartDate: LocalDate? = null,
    processedEndDate: LocalDate? = null,
    onProcessedStartDateChange: (LocalDate?) -> Unit = {},
    onProcessedEndDateChange: (LocalDate?) -> Unit = {},
    title: String = if (showDateFilters) stringResource(R.string.admin_filter_sheet_title) else stringResource(R.string.ingredients_filter_sheet_title),
    categoryScrollState: LazyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
    onResetAll: () -> Unit,
    onApply: () -> Unit
) {
    if (!show) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onResetAll) {
                    Text(
                        text = stringResource(R.string.admin_filter_reset_all),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Category Filter Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_sm))
            ) {
                Text(
                    text = stringResource(R.string.categories_filter_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedCategories.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = selectedCategories.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.padding_sm),
                                vertical = dimensionResource(R.dimen.padding_xxsm)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Chips Row
            LazyRow(
                state = categoryScrollState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp), // chips aren't clipped while scrolling
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_smd))
            ) {
                items(IngredientCategory.entries.toTypedArray()) { category ->
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
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // 2. Date Filters (only for request screens)
            if (showDateFilters) {
                Spacer(modifier = Modifier.height(20.dp))

                // Requested Date Filter
                Text(
                    text = stringResource(R.string.admin_filter_section_created_date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    DateRangeSelector(
                        startDate = createdStartDate,
                        endDate = createdEndDate,
                        onStartDateChange = onCreatedStartDateChange,
                        onEndDateChange = onCreatedEndDateChange
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Processed Date Filter
                Text(
                    text = stringResource(R.string.admin_filter_section_processed_date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    DateRangeSelector(
                        startDate = processedStartDate,
                        endDate = processedEndDate,
                        onStartDateChange = onProcessedStartDateChange,
                        onEndDateChange = onProcessedEndDateChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Apply Button
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.admin_filter_apply_btn),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartDateChange: (LocalDate?) -> Unit,
    onEndDateChange: (LocalDate?) -> Unit
) {
    var pickingForStart by remember { mutableStateOf<Boolean?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateBox(
            label = stringResource(R.string.admin_filter_from),
            date = startDate,
            onClick = { pickingForStart = true },
            onClear = { onStartDateChange(null) },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "—",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DateBox(
            label = stringResource(R.string.admin_filter_to),
            date = endDate,
            onClick = { pickingForStart = false },
            onClear = { onEndDateChange(null) },
            modifier = Modifier.weight(1f)
        )
    }

    if (pickingForStart != null) {
        val isStart = pickingForStart == true
        val initialLocalDate = if (isStart) startDate ?: LocalDate.now() else endDate ?: LocalDate.now()
        val initialMillis = initialLocalDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { pickingForStart = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            if (isStart) {
                                onStartDateChange(selectedLocalDate)
                            } else {
                                onEndDateChange(selectedLocalDate)
                            }
                        }
                        pickingForStart = null
                    }
                ) {
                    Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingForStart = null }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DateBox(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (date != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = date?.format(formatter) ?: stringResource(R.string.admin_filter_select_date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (date != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (date != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
            if (date != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cancel),
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
