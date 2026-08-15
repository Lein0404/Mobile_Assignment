package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.foodieheal.ingredients.model.*
import com.example.foodieheal.model.Status
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.navigation.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.viewModel.IngredientItem
import com.example.foodieheal.ingredients.viewModel.IngredientRequestItem
import com.example.foodieheal.ingredients.viewModel.IngredientRequestUiState
import com.example.foodieheal.ingredients.viewModel.IngredientsUiState
import com.example.foodieheal.ui.components.StatusBadge
import com.example.foodieheal.ui.theme.FoodieHealTheme

@Preview(showBackground = true)
@Composable
fun IngredientsScreenPreview() {
    FoodieHealTheme {
        IngredientsMainScreen(rememberNavController())
    }
}

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

    // Sync tab state only if initialTab is explicitly provided (0 or 1)
    LaunchedEffect(initialTab) {
        if (initialTab != -1) {
            viewModel.onTabChange(initialTab)
        }
    }

    val tabs = listOf("Existing", "Requests")
    
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
                contentColor = Color.White
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Ingredients",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )

                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (uiState.selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                                    height = 3.dp,
                                    color = Color.White
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
                                        fontSize = 15.sp,
                                        fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (uiState.selectedTab == index) Color.White else Color.White.copy(alpha = 0.8f)
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
                    contentColor = Color.White,
                    /*shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.padding(bottom = 16.dp),*/
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-32).dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_add),
                        contentDescription = "New Request",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        containerColor = Color(0xFFF8F8F8)
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
                    onAddToCart = { ingredient ->
                        viewModel.addToShoppingList(ingredient)
                        Toast.makeText(context, "${ingredient.ingredientName} added to Shopping List", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                IngredientRequestsScreen(
                    viewModel = requestViewModel,
                    uiState = requestUiState,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun IngredientRequestsScreen(
    viewModel: IngredientRequestViewModel,
    uiState: IngredientRequestUiState,
    navController: NavController
) {
    var showStatusFilterDialog by remember { mutableStateOf(false) }
    var tempSelectedStatus by remember { mutableStateOf(uiState.selectedStatus) }

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
                    modifier = Modifier.size(70.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.title_no_internet),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please connect to the internet to view ingredient requests",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text(
                text = "Search ingredient requests here",
                style = MaterialTheme.typography.labelLarge
            ) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp)
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
                    Spacer(modifier = Modifier.width(12.dp))
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
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Categories", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IngredientCategory.entries) { category ->
                FilterChip(
                    selected = uiState.selectedCategories.contains(category),
                    onClick = { viewModel.toggleCategory(category) },
                    label = { Text(category.categoryName) },
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.fetchRequests() }) {
                        Text("Retry")
                    }
                }
            }
        } else if (uiState.filteredRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No requests found.", color = Color.Gray)
            }
        } else {
            val grouped = uiState.filteredRequests.groupBy { it.request.ingredientCategory ?: IngredientCategory.OTHERS }
            
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
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
                    item { Spacer(modifier = Modifier.height(80.dp)) } // FAB space
                }
            }
        }
    }

    if (showStatusFilterDialog) {
        AlertDialog(
            onDismissRequest = { showStatusFilterDialog = false },
            title = { Text("Filter by Status", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        "All" to null,
                        "Pending" to Status.PENDING,
                        "Approved" to Status.APPROVED,
                        "Rejected" to Status.REJECTED
                    )
                    options.forEach { (label, status) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempSelectedStatus = status }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = tempSelectedStatus == status,
                                onClick = { tempSelectedStatus = status }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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
                    Text("Apply Filter", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusFilterDialog = false }) {
                    Text("Cancel", color = Color.Gray)
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
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.request.ingredientName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(item.calorieSummary, color = Color.Gray, fontSize = 14.sp)
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
    onAddToCart: (Ingredients) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text(
                text = "Search ingredient requests here",
                style = MaterialTheme.typography.labelLarge
            ) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null
                ) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Categories", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IngredientCategory.entries) { category ->
                FilterChip(
                    selected = uiState.selectedCategories.contains(category),
                    onClick = { viewModel.toggleCategory(category) },
                    label = { Text(category.categoryName) },
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.fetchIngredients() }) {
                        Text("Retry")
                    }
                }
            }
        }
        if (uiState.filteredIngredients.isEmpty() && !uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No ingredients match your search.")
            }
        }

        val grouped = uiState.filteredIngredients.groupBy { it.ingredient.ingredientCategory ?: IngredientCategory.OTHERS }
        
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                grouped.forEach { (category, items) ->
                    item {
                        Text(category.categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    // TODO: when logic is all done, improve the UI!
                    items(items) { item ->
                        IngredientCard(
                            item = item,
                            onClick = {
                                navController.navigate(Screen.IngredientDetail.createRoute(item.ingredient.ingredientId))
                            },
                            onAddToCart = { onAddToCart(item.ingredient) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun IngredientCard(
    item: IngredientItem,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(item.ingredient.ingredientName, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = item.calorieSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { onAddToCart() }) {
                Icon(painter = painterResource(R.drawable.ic_add_to_shopping_cart), contentDescription = "Add to shopping list", tint = Color.Black)
            }
        }
    }
}
