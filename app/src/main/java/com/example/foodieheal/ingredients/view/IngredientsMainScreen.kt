package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientRequestViewModelFactory
import com.example.foodieheal.navigation.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.R
import com.example.foodieheal.model.Status
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
fun IngredientsMainScreen(navController: NavController, initialTab: Int = -1) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val viewModel: IngredientsViewModel = viewModel()
    val requestViewModel: IngredientRequestViewModel = viewModel(
        factory = IngredientRequestViewModelFactory(application)
    )
    
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
        ) {
            // Top Bar & Tabs Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Text(
                        text = "Ingredients",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
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

            // Tab Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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

        // Floating Action Button for Requests tab (only when online)
        if (uiState.selectedTab == 1 && requestUiState.isNetworkAvailable) {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.IngredientRequestForm.createRoute()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 16.dp) // Adjust for bottom nav if needed
            ) {
                Icon(painter = painterResource(R.drawable.ic_outline_add), contentDescription = "New Request")
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
            placeholder = { Text("Search requested ingredients here") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        } else if (uiState.filteredRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No requests found.", color = Color.Gray)
            }
        } else {
            val grouped = uiState.filteredRequests.groupBy { it.request.ingredientCategory ?: IngredientCategory.OTHERS }
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

@Composable
fun IngredientRequestCard(item: IngredientRequestItem, onClick: () -> Unit) {
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
            placeholder = { Text("Search community ingredients here") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
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

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
        if (uiState.filteredIngredients.isEmpty() && !uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No ingredients match your search.")
            }
        }

        val grouped = uiState.filteredIngredients.groupBy { it.ingredient.ingredientCategory ?: IngredientCategory.OTHERS }
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
