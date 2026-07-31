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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.Ingredients
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.navigation.IngredientDetail
import androidx.compose.ui.tooling.preview.Preview
import com.example.foodieheal.ui.theme.MobileAssignmentTheme
import androidx.navigation.compose.rememberNavController

@Preview(showBackground = true)
@Composable
fun IngredientsScreenPreview() {
    MobileAssignmentTheme {
        IngredientsScreen(rememberNavController())
    }
}

@Composable
fun IngredientsScreen(navController: NavController) {
    val viewModel: IngredientsViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Existing", "Requests")

    val filteredIngredients by viewModel.filteredIngredients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
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
                ExistingTabContent(viewModel, filteredIngredients, isLoading, navController)
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
    ingredients: List<Ingredients>,
    isLoading: Boolean,
    navController: NavController
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("Search community ingredients here") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Categories", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IngredientCategory.entries) { category ->
                FilterChip(
                    selected = viewModel.selectedCategories.contains(category),
                    onClick = { viewModel.toggleCategory(category) },
                    label = { Text(category.categoryName) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val grouped = ingredients.groupBy { it.ingredientCategory ?: IngredientCategory.OTHERS }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                grouped.forEach { (category, items) ->
                    item {
                        Text(category.categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    items(items) { ingredient ->
                        IngredientCard(ingredient) {
                            navController.navigate(IngredientDetail(ingredient.ingredientId))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun IngredientCard(ingredient: Ingredients, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(ingredient.ingredientName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text("230kcal/100g", color = Color.Gray, fontSize = 14.sp)
            }
            IconButton(onClick = { /* Add to cart action */ }) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = "Add to cart", tint = Color.Black)
            }
        }
    }
}
