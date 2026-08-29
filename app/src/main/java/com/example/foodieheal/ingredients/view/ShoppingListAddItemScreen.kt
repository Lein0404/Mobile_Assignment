package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.foodieheal.ingredients.local.ShoppingListItemEntity
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.shared.IngredientSearchAndFilter
import com.example.foodieheal.ingredients.viewModel.IngredientItem
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ingredients.viewModel.ShoppingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListAddItemScreen(
    navController: NavController,
    targetShoppingListId: String? = null,
    ingredientsViewModel: IngredientsViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    shoppingListViewModel: ShoppingListViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by ingredientsViewModel.uiState.collectAsState()
    val shoppingUiState by shoppingListViewModel.uiState.collectAsState()
    val addItemState = shoppingUiState.addItemState
    val context = LocalContext.current

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
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                IngredientSearchAndFilter(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { ingredientsViewModel.onSearchQueryChange(it) },
                    searchPlaceholder = stringResource(R.string.shopping_list_search_placeholder),
                    selectedCategories = uiState.selectedCategories,
                    onToggleCategory = { ingredientsViewModel.toggleCategory(it) },
                    isExpanded = uiState.isCategoriesExpanded,
                    onExpandedChange = { ingredientsViewModel.toggleCategoriesExpanded() }
                )

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
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            items(items) { item ->
                                val isSelected = addItemState.selectedIngredients.any { it.ingredient.ingredientId == item.ingredient.ingredientId }
                                SelectableIngredientCard(
                                    item = item,
                                    isSelected = isSelected,
                                    onToggleSelection = {
                                        shoppingListViewModel.toggleIngredientSelection(item)
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
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val selectedCount = addItemState.selectedIngredients.size
                val toastMessage = stringResource(R.string.add_shopping_item_toast_added, selectedCount)
                Button(
                    onClick = {
                        if (addItemState.selectedIngredients.isNotEmpty()) {
                            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
                            if (userId.isEmpty()) return@Button

                            val resolvedListId = targetShoppingListId
                                ?: shoppingUiState.detailState.selectedShoppingListId
                                ?: shoppingUiState.homeState.shoppingLists.firstOrNull()?.shoppingListId

                            if (resolvedListId != null) {
                                val entities = addItemState.selectedIngredients.map { item ->
                                    ShoppingListItemEntity(
                                        shoppingListId = resolvedListId,
                                        userId = userId,
                                        ingredientId = item.ingredient.ingredientId,
                                        ingredientName = item.ingredient.ingredientName,
                                        ingredientCategory = item.ingredient.ingredientCategory?.name,
                                        isChecked = false
                                    )
                                }
                                shoppingListViewModel.addItems(resolvedListId, entities)
                                shoppingListViewModel.clearAddItemSelection()
                                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                // Create new shopping list first, then add items
                                shoppingListViewModel.createNewShoppingList { newListId ->
                                    val entities = addItemState.selectedIngredients.map { item ->
                                        ShoppingListItemEntity(
                                            shoppingListId = newListId,
                                            userId = userId,
                                            ingredientId = item.ingredient.ingredientId,
                                            ingredientName = item.ingredient.ingredientName,
                                            ingredientCategory = item.ingredient.ingredientCategory?.name,
                                            isChecked = false
                                        )
                                    }
                                    shoppingListViewModel.addItems(newListId, entities)
                                    shoppingListViewModel.clearAddItemSelection()
                                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.padding_l), vertical = dimensionResource(id = R.dimen.padding_l)),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    enabled = addItemState.selectedIngredients.isNotEmpty()
                ) {
                    Text(
                        text = stringResource(R.string.add_shopping_item_button, selectedCount),
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
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.ingredient.ingredientDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
