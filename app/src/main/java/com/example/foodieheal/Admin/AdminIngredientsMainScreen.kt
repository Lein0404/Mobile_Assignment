package com.example.foodieheal.Admin

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.foodieheal.Admin.ViewModel.AdminIngredientRequestItem
import com.example.foodieheal.Admin.ViewModel.AdminIngredientsUiState
import com.example.foodieheal.Admin.ViewModel.AdminIngredientsViewModel
import com.example.foodieheal.Admin.ViewModel.AdminViewModelFactory
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.view.IngredientsExistingScreen
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ui.components.StatusBadge
import com.example.foodieheal.model.Status
import com.example.foodieheal.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIngredientsScreen(
    navController: NavController,
    initialTab: Int = -1
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val adminFactory = AdminViewModelFactory(application)
    val ingredientsFactory = IngredientsViewModelFactory(application)
    
    val adminViewModel: AdminIngredientsViewModel = viewModel(factory = adminFactory)
    val ingredientsViewModel: IngredientsViewModel = viewModel(factory = ingredientsFactory)
    
    val adminUiState by adminViewModel.uiState.collectAsState()
    val ingredientsUiState by ingredientsViewModel.uiState.collectAsState()

    // Sync tab state only if initialTab is explicitly provided (0 or 1)
    LaunchedEffect(initialTab) {
        if (initialTab != -1) {
            adminViewModel.onTabChange(initialTab)
        }
    }

    // Refresh data when screen becomes active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route == Screen.AdminIngredient.route) {
            adminViewModel.fetchRequests()
            ingredientsViewModel.fetchIngredients()
        }
    }

    val tabs = listOf(
        stringResource(R.string.admin_tab_community),
        stringResource(R.string.admin_tab_requests)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
        ) {
            // Header with Title and Tabs
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.admin_ingredients_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(
                                start = dimensionResource(id = R.dimen.corner_radius_md),
                                top = dimensionResource(id = R.dimen.padding_l),
                                bottom = dimensionResource(id = R.dimen.padding_md)
                            )
                    )
                    
                    TabRow(
                        selectedTabIndex = adminUiState.selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        indicator = { tabPositions ->
                            if (adminUiState.selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[adminUiState.selectedTab]),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = adminUiState.selectedTab == index,
                                onClick = { adminViewModel.onTabChange(index) },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (adminUiState.selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (adminUiState.selectedTab == index) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Network check for both tabs
                val isNetworkAvailable = if (adminUiState.selectedTab == 0) ingredientsUiState.isNetworkAvailable else adminUiState.isNetworkAvailable
                
                if (!isNetworkAvailable) {
                    AdminOfflineMessage()
                } else {
                    if (adminUiState.selectedTab == 0) {
                        IngredientsExistingScreen(
                            viewModel = ingredientsViewModel,
                            uiState = ingredientsUiState,
                            navController = navController,
                            showAddToCart = false // Admins don't have shopping list
                        )
                    } else {
                        AdminIngredientRequestsScreen(
                            viewModel = adminViewModel,
                            uiState = adminUiState,
                            navController = navController
                        )
                    }
                }
            }
        }

        // Floating Action Button for Community tab (only when online)
        if (adminUiState.selectedTab == 0 && ingredientsUiState.isNetworkAvailable) {
            FloatingActionButton(
                onClick = { 
                    navController.navigate(Screen.AdminAddIngredient.route)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.padding_l))
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_add),
                    contentDescription = stringResource(R.string.admin_fab_add_ingredient),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.padding_xxl))
                )
            }
        }
    }
}

@Composable
fun AdminOfflineMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimensionResource(id = R.dimen.padding_xxl)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.wifi_off),
                contentDescription = stringResource(R.string.desc_no_network),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_xlarge_size))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_md)))
            Text(
                text = stringResource(R.string.title_no_internet),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsm)))
            Text(
                text = stringResource(R.string.admin_offline_manage_message),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIngredientRequestsScreen(
    viewModel: AdminIngredientsViewModel,
    uiState: AdminIngredientsUiState,
    navController: NavController
) {
    var showStatusFilterDialog by remember { mutableStateOf(false) }
    var tempSelectedStatus by remember { mutableStateOf(uiState.selectedStatus) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

        // 1. Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text(
                text = stringResource(R.string.admin_requests_search_placeholder),
                style = MaterialTheme.typography.labelLarge
            ) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.padding_l)),
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = dimensionResource(id = R.dimen.padding_md))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_alt),
                        contentDescription = "Filter",
                        tint = if (uiState.selectedStatus != null) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        modifier = Modifier
                            .clickable {
                                tempSelectedStatus = uiState.selectedStatus
                                showStatusFilterDialog = true
                            }
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_md)))
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier
                            .clickable {
                                tempSelectedStatus = uiState.selectedStatus
                                showStatusFilterDialog = true
                            }
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))
        Text(
            text = stringResource(R.string.admin_categories_header),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_l))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_sm)))

        // 2. Category Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(id = R.dimen.padding_l)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_smd))
        ) {
            items(IngredientCategory.entries) { category ->
                val isSelected = uiState.selectedCategories.contains(category)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.toggleCategory(category) },
                    label = { 
                        Text(
                            text = category.categoryName,
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_md)),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        labelColor = MaterialTheme.colorScheme.primary,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(uiState.errorMessage!!), color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_md)))
                    Button(onClick = { viewModel.fetchRequests() }) {
                        Text(stringResource(R.string.btn_retry))
                    }
                }
            }
        } else if (uiState.filteredRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.admin_no_requests_found), color = Color.Gray)
            }
        } else {
            val grouped = uiState.filteredRequests.groupBy { it.request.ingredientCategory ?: IngredientCategory.OTHERS }
            
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_l)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = dimensionResource(id = R.dimen.padding_l),
                        end = dimensionResource(id = R.dimen.padding_l),
                        bottom = dimensionResource(id = R.dimen.padding_xxl)
                    )
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(category.categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        items(items) { item ->
                            AdminIngredientRequestCard(item) {
                                navController.navigate(Screen.AdminIngredientDetail.createRoute(item.request.ingredientRequestId))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStatusFilterDialog) {
        AlertDialog(
            onDismissRequest = { showStatusFilterDialog = false },
            title = { Text(stringResource(R.string.admin_filter_status_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        stringResource(R.string.admin_filter_all) to null,
                        stringResource(R.string.admin_filter_pending) to Status.PENDING,
                        stringResource(R.string.admin_filter_approved) to Status.APPROVED,
                        stringResource(R.string.admin_filter_rejected) to Status.REJECTED
                    )
                    options.forEach { (label, status) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempSelectedStatus = status }
                                .padding(vertical = dimensionResource(id = R.dimen.padding_xsm))
                        ) {
                            RadioButton(
                                selected = tempSelectedStatus == status,
                                onClick = { tempSelectedStatus = status }
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_smd)))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onStatusFilterChange(tempSelectedStatus)
                        showStatusFilterDialog = false
                    }
                ) {
                    Text(stringResource(R.string.admin_apply_filter), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusFilterDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AdminIngredientRequestCard(item: AdminIngredientRequestItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.padding_xsm)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_l))
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_sm)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_sm))
            ) {
                Text(
                    text = item.request.ingredientName,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.admin_requested_by, item.requesterName),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            StatusBadge(status = item.request.requestStatus)
        }
    }
}
