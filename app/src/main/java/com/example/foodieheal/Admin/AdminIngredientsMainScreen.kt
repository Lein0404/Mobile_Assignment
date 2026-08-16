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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.foodieheal.Admin.ViewModel.AdminIngredientRequestItem
import com.example.foodieheal.Admin.ViewModel.AdminIngredientsUiState
import com.example.foodieheal.Admin.ViewModel.AdminIngredientsViewModel
import com.example.foodieheal.Admin.ViewModel.AdminViewModelFactory
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.Ingredients
import com.example.foodieheal.ingredients.view.IngredientsExistingScreen
import com.example.foodieheal.ingredients.viewModel.IngredientsUiState
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ui.components.StatusBadge
import com.example.foodieheal.model.Status
import com.example.foodieheal.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIngredientsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val adminFactory = AdminViewModelFactory(application)
    val ingredientsFactory = IngredientsViewModelFactory(application)
    
    val adminViewModel: AdminIngredientsViewModel = viewModel(factory = adminFactory)
    val ingredientsViewModel: IngredientsViewModel = viewModel(factory = ingredientsFactory)
    
    val adminUiState by adminViewModel.uiState.collectAsState()
    val ingredientsUiState by ingredientsViewModel.uiState.collectAsState()

    // Refresh data when screen becomes active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route == Screen.AdminIngredient.route) {
            adminViewModel.fetchRequests()
            ingredientsViewModel.fetchIngredients()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // Default to Community tab
    val tabs = listOf("Community", "Requests")

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
                contentColor = Color.White
            ) {
                Column {
                    Text(
                        text = "Admin Ingredients",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                    )
                    
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    height = 3.dp,
                                    color = Color.White
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Network check for both tabs
                val isNetworkAvailable = if (selectedTab == 0) ingredientsUiState.isNetworkAvailable else adminUiState.isNetworkAvailable
                
                if (!isNetworkAvailable) {
                    AdminOfflineMessage()
                } else {
                    if (selectedTab == 0) {
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
        if (selectedTab == 0 && ingredientsUiState.isNetworkAvailable) {
            FloatingActionButton(
                onClick = { 
                    // TODO: navController.navigate(Screen.AdminAddIngredient.createRoute())
                    // Just a placeholder for now
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_add),
                    contentDescription = "Add New Ingredient",
                    modifier = Modifier.size(32.dp)
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
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
                text = "Please connect to the internet to manage ingredients and requests",
                fontSize = 15.sp,
                color = Color.Gray,
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Search Bar
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
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Categories", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        // 2. Category Chips
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
                            AdminIngredientRequestCard(item) {
                                navController.navigate(Screen.AdminIngredientDetail.createRoute(item.request.ingredientRequestId))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
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
fun AdminIngredientRequestCard(item: AdminIngredientRequestItem, onClick: () -> Unit) {
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
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(item.request.ingredientName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(item.calorieSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Text("Requested By: ${item.requesterName}", style = MaterialTheme.typography.bodySmall)
            }
            
            StatusBadge(status = item.request.requestStatus)
        }
    }
}
