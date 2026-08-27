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
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.local.toEntity
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModel
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ingredients.viewModel.ShoppingListViewModel
import com.example.foodieheal.ui.components.DropDownList

/**
 * Screen for pasting and adding ingredients parsed from the device's clipboard.
 * Filters out unrecognized lines and only displays valid, registered ingredients.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListAddFromScreen(
    navController: NavController,
    targetShoppingListId: String? = null,
    ingredientsViewModel: IngredientsViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    shoppingListViewModel: ShoppingListViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val shoppingUiState by shoppingListViewModel.uiState.collectAsState()
    val addFromState = shoppingUiState.addFromState
    val ingredientsUiState by ingredientsViewModel.uiState.collectAsState()

    // Initialize selectedListId in VM if not set
    LaunchedEffect(targetShoppingListId, shoppingUiState.homeState.shoppingLists) {
        if (addFromState.selectedListId.isEmpty()) {
            val initialId = targetShoppingListId
                ?: shoppingUiState.detailState.activeShoppingList?.shoppingListId
                ?: shoppingUiState.homeState.shoppingLists.find { it.isDefault }?.shoppingListId
                ?: shoppingUiState.homeState.shoppingLists.firstOrNull()?.shoppingListId
                ?: ""
            shoppingListViewModel.updateAddFromSelectedListId(initialId)
        }
    }

    val selectedShoppingList = remember(addFromState.selectedListId, shoppingUiState.homeState.shoppingLists) {
        shoppingUiState.homeState.shoppingLists.find { it.shoppingListId == addFromState.selectedListId }
            ?: shoppingUiState.homeState.shoppingLists.firstOrNull()
    }

    val shoppingListOptions = remember(shoppingUiState.homeState.shoppingLists) {
        shoppingUiState.homeState.shoppingLists.map { it.title.ifEmpty { it.shoppingListId } }
    }

    LaunchedEffect(ingredientsUiState.ingredients) {
        if (ingredientsUiState.ingredients.isNotEmpty() && !addFromState.isParsed) {
            val allEntities = ingredientsUiState.ingredients.map { it.ingredient.toEntity() }
            shoppingListViewModel.refreshFromClipboard(context, allEntities)
        }
    }

    val allSelected = addFromState.parsedIngredients.isNotEmpty() && 
        addFromState.selectedIngredientIds.size == addFromState.parsedIngredients.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shopping_list_paste_clipboard),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dimensionResource(R.dimen.padding_l))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_l)))

            // ── Target Shopping List Selector ──
            if (shoppingUiState.homeState.shoppingLists.isNotEmpty()) {
                DropDownList(
                    labelId = R.string.shopping_list_title,
                    placeholderId = R.string.select_shopping_list_placeholder,
                    selectedValue = selectedShoppingList?.title?.ifEmpty { selectedShoppingList.shoppingListId } ?: "",
                    options = shoppingListOptions,
                    onOptionSelected = { chosenTitle ->
                        val found = shoppingUiState.homeState.shoppingLists.find {
                            (it.title.ifEmpty { it.shoppingListId }) == chosenTitle
                        }
                        if (found != null) {
                            shoppingListViewModel.updateAddFromSelectedListId(found.shoppingListId)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_md)))

            // ── Content ──
            if (ingredientsUiState.isLoading && !addFromState.isParsed) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (addFromState.parsedIngredients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_l)),
                        modifier = Modifier.padding(dimensionResource(R.dimen.padding_l))
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_list_no_ingredients_clipboard),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { 
                                val allEntities = ingredientsUiState.ingredients.map { it.ingredient.toEntity() }
                                shoppingListViewModel.refreshFromClipboard(context, allEntities) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_sm))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_paste),
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_size))
                            )
                            Spacer(Modifier.width(dimensionResource(R.dimen.padding_smd)))
                            Text(stringResource(R.string.shopping_list_refresh_clipboard))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xsm)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.padding_smd))
                ) {
                    items(
                        items = addFromState.parsedIngredients,
                        key = { it.ingredientId }
                    ) { ingredient ->
                        val isSelected = addFromState.selectedIngredientIds.contains(ingredient.ingredientId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    shoppingListViewModel.toggleAddFromIngredientSelection(ingredient.ingredientId)
                                }
                                .padding(vertical = dimensionResource(R.dimen.padding_smd)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { 
                                    shoppingListViewModel.toggleAddFromIngredientSelection(ingredient.ingredientId)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_smd)))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ingredient.ingredientName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!ingredient.ingredientCategory.isNullOrBlank()) {
                                    Text(
                                        text = ingredient.ingredientCategory,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }

                // ── Bottom Action Row ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.padding_l)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            shoppingListViewModel.setAddFromAllSelected(!allSelected)
                        }
                    ) {
                        Text(
                            text = if (allSelected) stringResource(R.string.shopping_list_unselect_all) else stringResource(R.string.shopping_list_select_all),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val ingredientsToAdd = addFromState.parsedIngredients.filter {
                                addFromState.selectedIngredientIds.contains(it.ingredientId)
                            }
                            val targetId = addFromState.selectedListId.ifEmpty {
                                selectedShoppingList?.shoppingListId ?: ""
                            }
                            if (targetId.isNotEmpty() && ingredientsToAdd.isNotEmpty()) {
                                shoppingListViewModel.addIngredientsToShoppingList(
                                    shoppingListId = targetId,
                                    ingredients = ingredientsToAdd
                                ) { count ->
                                    val targetListName = selectedShoppingList?.title?.ifEmpty { targetId } ?: targetId
                                    Toast.makeText(context, application.getString(R.string.shopping_list_items_added_to_list, count, targetListName), Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = addFromState.selectedIngredientIds.isNotEmpty() && (addFromState.selectedListId.isNotEmpty() || selectedShoppingList != null),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_sm))
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_list_add_selected),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
