package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.ingredients.shared.IngredientSearchAndFilter
import com.example.foodieheal.model.Status
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.viewModel.IngredientItem
import com.example.foodieheal.ingredients.viewModel.IngredientRequestItem
import com.example.foodieheal.ingredients.viewModel.IngredientRequestUiState
import com.example.foodieheal.ingredients.viewModel.IngredientsUiState
import com.example.foodieheal.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsMainScreen(
    navController: NavController,
    initialTab: Int = -1
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    /**
     * Calls the `IngredientsViewModelFactory`
     * which initializes:
     * - `IngredientsViewModel` with an `Application` argument passed from here,
     *   an `IngredientsRepository` instance and a `ShoppingListRepository` instance created and passed from IngredientsViewModelFactory
     *
     * - `IngredientRequestViewModel` with an `Application` argument passed from here,
     *   and a `IngredientRequestRepository` instance created and passed from IngredientsViewModelFactory
     */
    val factory = IngredientsViewModelFactory(application)
    val viewModel: IngredientsViewModel = viewModel(factory = factory)
    val requestViewModel: IngredientRequestViewModel = viewModel(factory = factory)
    
    val uiState by viewModel.uiState.collectAsState()
    val requestUiState by requestViewModel.uiState.collectAsState()

    // Separate scroll states for each tab
    val existingCategoryScrollState = rememberLazyListState()
    val requestsCategoryScrollState = rememberLazyListState()

    // Sync tab state only if initialTab is explicitly provided (0 or 1)
    LaunchedEffect(initialTab) {
        if (initialTab != -1) {
            viewModel.onTabChange(initialTab)
        }
    }

    val tabs = listOf(
        stringResource(R.string.ingredients_tab_existing),
        stringResource(R.string.ingredients_tab_requests)
    )
    
    // Refresh data when screen becomes active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route == Screen.Ingredients.route) {
            viewModel.fetchIngredients()
            requestViewModel.fetchRequests()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.ingredients_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.shopping_list_back)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        indicator = { tabPositions ->
                            if (uiState.selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                                    height = 3.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = uiState.selectedTab == index,
                                onClick = { viewModel.onTabChange(index) },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (uiState.selectedTab == index) {
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
        },
        floatingActionButton = {
            // Floating Action Button for Requests tab (only when online)
            if (uiState.selectedTab == 1 && requestUiState.isNetworkAvailable) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.IngredientRequestForm.createRoute()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_xlarge_size))
                        .offset(y = (-dimensionResource(id = R.dimen.padding_xxl)))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_add),
                        contentDescription = stringResource(R.string.ingredients_fab_new_request),
                        modifier = Modifier.size(dimensionResource(id = R.dimen.padding_xxl))
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.selectedTab == 0) {
                IngredientsExistingScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    navController = navController,
                    categoryScrollState = existingCategoryScrollState,
                    onAddToCart = { ingredient ->
                        viewModel.addToShoppingList(ingredient)
                        Toast.makeText(
                            context,
                            application.getString(R.string.ingredients_toast_added, ingredient.ingredientName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } else {
                IngredientRequestsScreen(
                    viewModel = requestViewModel,
                    uiState = requestUiState,
                    navController = navController,
                    categoryScrollState = requestsCategoryScrollState
                )
            }
        }
    }
}

@Composable
fun IngredientRequestsScreen(
    viewModel: IngredientRequestViewModel,
    uiState: IngredientRequestUiState,
    navController: NavController,
    categoryScrollState: LazyListState = rememberLazyListState()
) {
    // Gate: show offline message when not connected
    if (!uiState.isNetworkAvailable) {
        Box(
            modifier = Modifier.fillMaxSize(), 
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                    text = stringResource(R.string.no_internet_connection),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xsm)))
                Text(
                    text = stringResource(R.string.ingredients_no_internet_requests),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        IngredientSearchAndFilter(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            searchPlaceholder = stringResource(R.string.ingredients_requests_search_placeholder),
            selectedCategories = uiState.selectedCategories,
            onToggleCategory = { viewModel.toggleCategory(it) },
            showFilterIcon = true,
            isFilterActive = uiState.selectedStatus != null,
            onFilterClick = { viewModel.onShowStatusFilterDialog(true) },
            lazyRowState = categoryScrollState,
            isExpanded = uiState.isCategoriesExpanded,
            onExpandedChange = { viewModel.toggleCategoriesExpanded() }
        )

        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(uiState.errorMessage),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_md)))
                    Button(onClick = { viewModel.fetchRequests() }) {
                        Text(stringResource(R.string.btn_retry))
                    }
                }
            }
        } else if (uiState.filteredRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.ingredients_requests_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        bottom = 80.dp // Keep FAB space
                    )
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(category.categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        items(items) { item ->
                            IngredientRequestCard(item) {
                                navController.navigate(Screen.IngredientDetail.createRoute(item.request.ingredientRequestId, true))
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showStatusFilterDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowStatusFilterDialog(false) },
            title = {
                Text(
                    text = stringResource(R.string.ingredients_requests_filter_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        stringResource(R.string.ingredients_requests_filter_all) to null,
                        stringResource(R.string.ingredients_requests_filter_pending) to Status.PENDING,
                        stringResource(R.string.ingredients_requests_filter_approved) to Status.APPROVED,
                        stringResource(R.string.ingredients_requests_filter_rejected) to Status.REJECTED
                    )
                    options.forEach { (label, status) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateTempStatus(status) }
                                .padding(vertical = dimensionResource(id = R.dimen.padding_xsm))
                        ) {
                            RadioButton(
                                selected = uiState.tempSelectedStatus == status,
                                onClick = { viewModel.updateTempStatus(status) }
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
                        viewModel.onStatusFilterChange(uiState.tempSelectedStatus)
                        viewModel.onShowStatusFilterDialog(false)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ingredients_requests_apply_filter),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowStatusFilterDialog(false) }) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
fun IngredientRequestCard(
    item: IngredientRequestItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.padding_xsm)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_md))
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
                    text = item.request.ingredientDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StatusBadge(status = item.request.requestStatus)
        }
    }
}

@Composable
fun IngredientsExistingScreen(
    viewModel: IngredientsViewModel,
    uiState: IngredientsUiState,
    navController: NavController,
    categoryScrollState: LazyListState = rememberLazyListState(),
    onAddToCart: (Ingredients) -> Unit = {},
    showAddToCart: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        IngredientSearchAndFilter(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            searchPlaceholder = stringResource(R.string.ingredients_existing_search_placeholder),
            selectedCategories = uiState.selectedCategories,
            onToggleCategory = { viewModel.toggleCategory(it) },
            lazyRowState = categoryScrollState,
            isExpanded = uiState.isCategoriesExpanded,
            onExpandedChange = { viewModel.toggleCategoriesExpanded() }
        )

        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(uiState.errorMessage),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_md)))
                    Button(onClick = { viewModel.fetchIngredients() }) {
                        Text(text = stringResource(R.string.btn_retry))
                    }
                }
            }
        }
        if (uiState.filteredIngredients.isEmpty() && !uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.ingredients_existing_empty))
            }
        }

        val grouped = uiState.filteredIngredients.groupBy { it.ingredient.ingredientCategory ?: IngredientCategory.OTHERS }

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
                    bottom = dimensionResource(id = R.dimen.padding_l)
                )
            ) {
                grouped.forEach { (category, items) ->
                    item {
                        Text(category.categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    items(items) { item ->
                        IngredientCard(
                            item = item,
                            onClick = {
                                navController.navigate(Screen.IngredientDetail.createRoute(item.ingredient.ingredientId, showAddToCart = showAddToCart))
                            },
                            onAddToCart = { onAddToCart(item.ingredient) },
                            showAddToCart = showAddToCart
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IngredientCard(
    item: IngredientItem,
    onClick: () -> Unit,
    onAddToCart: () -> Unit = {},
    showAddToCart: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.padding_xsm)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_md))
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_sm))
            ) {
                Text(
                    text = item.ingredient.ingredientName,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = item.ingredient.ingredientDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showAddToCart) {
                IconButton(onClick = { onAddToCart() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_to_shopping_cart),
                        contentDescription = stringResource(R.string.desc_add_recipe),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
