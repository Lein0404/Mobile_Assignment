package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.SupabaseClient
import io.github.jan.supabase.auth.auth
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.viewModel.IngredientItem
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ingredients.viewModel.ShoppingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingListItemScreen(
    navController: NavController,
    ingredientsViewModel: IngredientsViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    shoppingListViewModel: ShoppingListViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by ingredientsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Track selected ingredients
    val selectedIngredients = remember { mutableStateListOf<IngredientItem>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_shopping_item_title),
                        fontSize = 24.sp,
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
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { ingredientsViewModel.onSearchQueryChange(it) },
                    placeholder = { Text(stringResource(R.string.add_shopping_item_search_placeholder), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.shopping_list_categories), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Categories chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IngredientCategory.entries) { category ->
                        FilterChip(
                            selected = uiState.selectedCategories.contains(category),
                            onClick = { ingredientsViewModel.toggleCategory(category) },
                            label = { Text(category.categoryName, fontSize = 12.sp) },
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredIngredients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.add_shopping_item_no_ingredients))
                    }
                } else {
                    val grouped = uiState.filteredIngredients.groupBy { it.ingredient.ingredientCategory ?: IngredientCategory.OTHERS }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        grouped.forEach { (category, items) ->
                            item {
                                Text(
                                    text = category.categoryName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            items(items) { item ->
                                val isSelected = selectedIngredients.any { it.ingredient.ingredientId == item.ingredient.ingredientId }
                                SelectableIngredientCard(
                                    item = item,
                                    isSelected = isSelected,
                                    onToggleSelection = {
                                        if (isSelected) {
                                            selectedIngredients.removeAll { it.ingredient.ingredientId == item.ingredient.ingredientId }
                                        } else {
                                            selectedIngredients.add(item)
                                        }
                                    }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) } // Space for bottom button
                    }
                }
            }

            // Floating Bottom "Add n Item(s) to Shopping List" button aligned to the bottom of the Box
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.Bottom, // push the button down to the bottom
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                val toastMessage = stringResource(R.string.add_shopping_item_toast_added, selectedIngredients.size)
                Button(
                    onClick = {
                        if (selectedIngredients.isNotEmpty()) {
                            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
                            val entities = selectedIngredients.map { item ->
                                ShoppingListEntity(
                                    userId = userId,
                                    ingredientId = item.ingredient.ingredientId,
                                    ingredientName = item.ingredient.ingredientName,
                                    isChecked = false
                                )
                            }
                            shoppingListViewModel.addItems(entities)
                            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.Gray
                    ),
                    enabled = selectedIngredients.isNotEmpty()
                ) {
                    Text(
                        text = stringResource(R.string.add_shopping_item_button, selectedIngredients.size),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableIngredientCard(
    item: IngredientItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() },
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.ingredient.ingredientName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.calorieSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
