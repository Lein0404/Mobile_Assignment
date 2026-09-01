package com.example.foodieheal.Admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.foodieheal.Admin.ViewModel1.AdminApprovalViewModel
import com.example.foodieheal.Chef.model.Chef
import com.example.foodieheal.R
import com.example.foodieheal.ui.components.getHighlightedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChefApprovalContent(
    viewModel: AdminApprovalViewModel,
    navController: NavController
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Text(
                    text = stringResource(R.string.chef_approval),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                )
            }
        }

        // Search Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.admin_chef_search_hint),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                painter = painterResource(R.drawable.cancel),
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Status Filter Tabs
        val tabs = listOf(
            Triple(0, stringResource(R.string.chef_approval_tab_all), viewModel.totalCount),
            Triple(1, stringResource(R.string.chef_approval_tab_pending), viewModel.pendingCount),
            Triple(2, stringResource(R.string.chef_approval_tab_approved), viewModel.approvedCount),
            Triple(3, stringResource(R.string.chef_approval_tab_rejected), viewModel.rejectedCount)
        )

        ScrollableTabRow(
            selectedTabIndex = viewModel.selectedStatusTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (viewModel.selectedStatusTab in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedStatusTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            tabs.forEach { (index, title, count) ->
                val isSelected = viewModel.selectedStatusTab == index
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.onStatusTabSelected(index) },
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

        val displayedChefs = viewModel.displayedChefs

        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refreshChefs() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (displayedChefs.isEmpty()) {
                val isSearching = viewModel.searchQuery.isNotBlank()
                val emptyMessage = if (isSearching) {
                    stringResource(R.string.admin_chef_search_no_results, viewModel.searchQuery)
                } else {
                    when (viewModel.selectedStatusTab) {
                        0 -> stringResource(R.string.no_chefs_found)
                        1 -> stringResource(R.string.no_pending_applications)
                        2 -> stringResource(R.string.no_approved_chefs)
                        3 -> stringResource(R.string.no_rejected_chefs)
                        else -> stringResource(R.string.no_chefs_found)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_outline_account_circle),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        if (isSearching) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Text(stringResource(R.string.clear_search))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(displayedChefs, key = { it.chefId }) { chef ->
                        ChefApprovalCard(
                            chef = chef,
                            searchQuery = viewModel.searchQuery,
                            onViewClick = {
                                navController.navigate("chefDetail/${chef.chefId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChefApprovalCard(
    chef: Chef,
    searchQuery: String = "",
    onViewClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chef.profilePictureUrl.isNullOrEmpty()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_account_circle),
                        contentDescription = stringResource(R.string.default_profile),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                } else {
                    AsyncImage(
                        model = chef.profilePictureUrl,
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            getHighlightedText(fullText = chef.name, query = searchQuery)
                        } else {
                            AnnotatedString(chef.name)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    StatusChip(status = chef.status)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // Reused unified DetailRow with search match highlighting
            DetailRow(
                painter = painterResource(R.drawable.mail),
                label = stringResource(R.string.label_email),
                value = chef.email,
                highlightQuery = searchQuery
            )

            Spacer(modifier = Modifier.height(8.dp))

            DetailRow(
                painter = painterResource(R.drawable.telephone),
                label = stringResource(R.string.label_phone),
                value = chef.phoneNumber,
                highlightQuery = searchQuery
            )

            Spacer(modifier = Modifier.height(8.dp))

            DetailRow(
                painter = painterResource(R.drawable.ic_clock),
                label = stringResource(R.string.label_experience),
                value = stringResource(R.string.experience_years_format, chef.experience)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onViewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_view),
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.review_application),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    painter: Painter,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightQuery: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val highlightedText = if (highlightQuery.isNotBlank()) {
                getHighlightedText(fullText = value, query = highlightQuery)
            } else {
                AnnotatedString(value)
            }
            Text(
                text = highlightedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val isApproved = status.equals("Approved", ignoreCase = true)
    val isRejected = status.equals("Rejected", ignoreCase = true)

    val bgColor = when {
        isApproved -> Color(0xFFE8F5E9)
        isRejected -> Color(0xFFFFEBEE)
        else -> Color(0xFFFFF3E0)
    }
    val textColor = when {
        isApproved -> Color(0xFF2E7D32)
        isRejected -> Color(0xFFC62828)
        else -> Color(0xFFE65100)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = textColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}