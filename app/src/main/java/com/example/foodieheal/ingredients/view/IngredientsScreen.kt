package com.example.foodieheal.ingredients.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.IngredientItem
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.navigation.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.foodieheal.R
import com.example.foodieheal.ui.theme.FoodieHealTheme

@Preview(showBackground = true)
@Composable
fun IngredientsScreenPreview() {
    FoodieHealTheme {
        IngredientsScreen(rememberNavController())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsScreen(navController: NavController) {
    val viewModel: IngredientsViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Existing", "Requests")

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary)) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTab, matchContentSize = true),
                            color = Color.White
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.7f)
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedTab == 0) {
                ExistingTabContent(viewModel, uiState, navController)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Requests feature coming soon")
                }
            }
        }
    }
}

@Composable
fun ExistingTabContent(
    viewModel: IngredientsViewModel,
    uiState: com.example.foodieheal.ingredients.model.IngredientsUiState,
    navController: NavController
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text("Search community ingredients here") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
        if (uiState.filteredIngredients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No ingredients found.")
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
                    IngredientCard(item) {
                        navController.navigate(Screen.IngredientDetail.createRoute(item.ingredient.ingredientId))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun IngredientCard(
    item: IngredientItem,
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
            IconButton(onClick = { /* Add to cart action */ }) {
                Icon(painter = painterResource(R.drawable.ic_add_to_shopping_cart), contentDescription = "Add to cart", tint = Color.Black)
            }
        }
    }
}
