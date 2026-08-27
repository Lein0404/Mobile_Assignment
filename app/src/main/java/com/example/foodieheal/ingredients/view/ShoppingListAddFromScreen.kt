package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        text = "Paste from clipboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.shopping_list_back),
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

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
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "No valid ingredients found on clipboard.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { 
                                val allEntities = ingredientsUiState.ingredients.map { it.ingredient.toEntity() }
                                shoppingListViewModel.refreshFromClipboard(context, allEntities) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_paste),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh from Clipboard")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
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
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { 
                                    shoppingListViewModel.toggleAddFromIngredientSelection(ingredient.ingredientId)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ingredient.ingredientName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!ingredient.ingredientCategory.isNullOrBlank()) {
                                    Text(
                                        text = ingredient.ingredientCategory ?: "",
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
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            shoppingListViewModel.setAddFromAllSelected(!allSelected)
                        }
                    ) {
                        Text(
                            text = if (allSelected) "Unselect all" else "Select all",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
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
                                    Toast.makeText(context, "$count items added to $targetListName", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = addFromState.selectedIngredientIds.isNotEmpty() && (addFromState.selectedListId.isNotEmpty() || selectedShoppingList != null),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Add selected items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
