package com.example.foodieheal.Chef.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.Chef.model.ChefPrepAggregator
import com.example.foodieheal.Chef.model.ChefPrepIngredient
import com.example.foodieheal.R
import com.example.foodieheal.hiring.model.AppointmentRecipeWithDetails

private enum class PrepFilterCategory {
    ALL,
    TO_PREP,
    COMPLETED,
    CHEF_PROVIDED,
    CLIENT_PROVIDED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChefPrepChecklistSection(
    appointmentId: String,
    attachedRecipes: List<AppointmentRecipeWithDetails>,
    checkedItemKeys: Set<String>,
    onToggleItem: (itemKey: String) -> Unit,
    onSetAllItems: (itemKeys: List<String>, isChecked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allIngredients = remember(attachedRecipes, checkedItemKeys) {
        ChefPrepAggregator.aggregate(attachedRecipes, checkedItemKeys)
    }

    if (allIngredients.isEmpty()) return

    var isExpanded by rememberSaveable { mutableStateOf(initialExpanded) }
    var selectedFilter by rememberSaveable { mutableStateOf(PrepFilterCategory.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val chefIngredients = remember(allIngredients) {
        allIngredients.filter { it.isChefProvided }
    }
    val totalChefCount = chefIngredients.size
    val completedChefCount = chefIngredients.count { it.isChecked }
    val progressFraction = if (totalChefCount > 0) completedChefCount.toFloat() / totalChefCount.toFloat() else 1f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "prep_progress")
    val progressPercent = (progressFraction * 100).toInt()

    // Filtered list based on search and selected category
    val filteredIngredients = remember(allIngredients, selectedFilter, searchQuery) {
        allIngredients.filter { item ->
            val matchesCategory = when (selectedFilter) {
                PrepFilterCategory.ALL -> true
                PrepFilterCategory.TO_PREP -> item.isChefProvided && !item.isChecked
                PrepFilterCategory.COMPLETED -> item.isChefProvided && item.isChecked
                PrepFilterCategory.CHEF_PROVIDED -> item.isChefProvided
                PrepFilterCategory.CLIENT_PROVIDED -> !item.isChefProvided
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.recipeNames.any { it.contains(searchQuery, ignoreCase = true) } ||
                    item.displayQuantity.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (completedChefCount == totalChefCount && totalChefCount > 0) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_recipe),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.prep_checklist_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val subtitle = if (attachedRecipes.size > 1) {
                            stringResource(R.string.prep_checklist_subtitle, attachedRecipes.size)
                        } else {
                            stringResource(R.string.prep_checklist_subtitle_single)
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Completion status chip (for chef provided ingredients)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (completedChefCount == totalChefCount && totalChefCount > 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = "$completedChefCount/$totalChefCount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (completedChefCount == totalChefCount && totalChefCount > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        painter = if (isExpanded) painterResource(R.drawable.ic_arrow_drop_up) else painterResource(R.drawable.ic_arrow_drop_down),
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Visual Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.prep_progress_format, completedChefCount, totalChefCount, progressPercent),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (completedChefCount == totalChefCount && totalChefCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check_circle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.prep_all_completed_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Action Buttons (Check All, Reset, Copy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (totalChefCount > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        onSetAllItems(chefIngredients.map { it.id }, true)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.check_circle),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.prep_mark_all),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        onSetAllItems(chefIngredients.map { it.id }, false)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.refresh),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.prep_reset),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        IconButton(
                            onClick = {
                                val summaryText = buildString {
                                    appendLine("Chef Ingredient Prep Checklist")
                                    appendLine("Appointment: #$appointmentId")
                                    appendLine("Chef Items Progress: $completedChefCount/$totalChefCount items prepared ($progressPercent%)")
                                    appendLine("--------------------------------")
                                    allIngredients.forEach { item ->
                                        val checkbox = if (item.isChefProvided) {
                                            if (item.isChecked) "[x]" else "[ ]"
                                        } else {
                                            "[Client]"
                                        }
                                        val provider = if (item.isChefProvided) "(Chef Provides)" else "(Client Supplies)"
                                        appendLine("$checkbox ${item.name}: ${item.displayQuantity} $provider")
                                        if (item.recipeNames.isNotEmpty()) {
                                            appendLine("    For: ${item.recipeNames.joinToString(", ")}")
                                        }
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(summaryText))
                                Toast.makeText(context, "Prep list copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy),
                                contentDescription = "Copy Checklist Summary",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.prep_search_placeholder),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        painter = painterResource(R.drawable.cancel),
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )

                    // Filter Chips Row
                    val pendingChefCount = chefIngredients.count { !it.isChecked }
                    val clientCount = allIngredients.count { !it.isChefProvided }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == PrepFilterCategory.ALL,
                                onClick = { selectedFilter = PrepFilterCategory.ALL },
                                label = { Text(stringResource(R.string.prep_filter_all, allIngredients.size), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == PrepFilterCategory.TO_PREP,
                                onClick = { selectedFilter = PrepFilterCategory.TO_PREP },
                                label = { Text(stringResource(R.string.prep_filter_pending, pendingChefCount), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == PrepFilterCategory.COMPLETED,
                                onClick = { selectedFilter = PrepFilterCategory.COMPLETED },
                                label = { Text(stringResource(R.string.prep_filter_completed, completedChefCount), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == PrepFilterCategory.CHEF_PROVIDED,
                                onClick = { selectedFilter = PrepFilterCategory.CHEF_PROVIDED },
                                label = { Text(stringResource(R.string.prep_filter_chef_provided, totalChefCount), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        if (clientCount > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilter == PrepFilterCategory.CLIENT_PROVIDED,
                                    onClick = { selectedFilter = PrepFilterCategory.CLIENT_PROVIDED },
                                    label = { Text(stringResource(R.string.prep_filter_client_provided, clientCount), fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    // Checklist Item List
                    if (filteredIngredients.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.prep_no_ingredients_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredIngredients.forEach { item ->
                                PrepIngredientItemRow(
                                    item = item,
                                    onToggle = { onToggleItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrepIngredientItemRow(
    item: ChefPrepIngredient,
    onToggle: () -> Unit
) {
    val isInteractive = item.isChefProvided

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isInteractive) {
                    Modifier.clickable { onToggle() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (item.isChecked) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        } else if (!item.isChefProvided) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            1.dp,
            if (item.isChecked) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (item.isChefProvided) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // Client provides: No checkbox. Show a clean indicator badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(top = 3.dp, start = 2.dp)
                        .size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_account_circle),
                            contentDescription = "Client Provides",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Ingredient Name + Quantity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.Bold,
                            textDecoration = if (item.isChecked && item.isChefProvided) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.isChecked) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quantity pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.isChecked) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        } else if (!item.isChefProvided) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = item.displayQuantity,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isChecked) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else if (!item.isChefProvided) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Provider Badge & Dishes breakdown
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Provider Pill (Chef vs Client)
                    if (item.isChefProvided) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = stringResource(R.string.prep_tag_chef_brings),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = stringResource(R.string.prep_tag_client_supplies),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Dishes summary
                    if (item.recipeNames.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.prep_used_in_dishes, item.recipeNames.joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
