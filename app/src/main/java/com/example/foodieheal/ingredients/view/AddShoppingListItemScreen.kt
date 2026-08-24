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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { ingredientsViewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            stringResource(R.string.shopping_list_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.padding_l)),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
                    trailingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))
                Text(
                    text = stringResource(R.string.shopping_list_categories),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_l))
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_smd)))

                // Categories chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = dimensionResource(id = R.dimen.padding_l)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_smd))
                ) {
                    items(IngredientCategory.entries) { category ->
                        FilterChip(
                            selected = uiState.selectedCategories.contains(category),
                            onClick = { ingredientsViewModel.toggleCategory(category) },
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
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_l)),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = dimensionResource(id = R.dimen.padding_l),
                            end = dimensionResource(id = R.dimen.padding_l),
                            bottom = 100.dp // Space for bottom button
                        )
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
                    }
                }
            }

            // Floating Bottom "Add n Item(s) to Shopping List" button aligned to the bottom of the Box
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = dimensionResource(id = R.dimen.padding_l)),
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
                        .padding(horizontal = dimensionResource(id = R.dimen.padding_l), vertical = dimensionResource(id = R.dimen.padding_l)),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = Color(0XFFC2C2C2) // TODO: add this as disabledColor to Theme.kt?
                    ),
                    enabled = selectedIngredients.isNotEmpty()
                ) {
                    Text(
                        text = stringResource(R.string.add_shopping_item_button, selectedIngredients.size),
                        style = MaterialTheme.typography.labelLarge,
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
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.padding_xsm)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)
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
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.ingredient.ingredientDesc,
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
