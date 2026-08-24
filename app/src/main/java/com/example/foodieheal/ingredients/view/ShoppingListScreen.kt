package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.foodieheal.R
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.ShoppingListItem
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ingredients.viewModel.ShoppingListViewModel
import com.example.foodieheal.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    navController: NavController,
    viewModel: ShoppingListViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showMenu by remember { mutableStateOf(false) }
    var showClearCheckedDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val checkedCount = uiState.items.count { it.entity.isChecked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shopping_list_title),
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
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showMenu = !showMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_xlarge_size))
                        .offset(y = (-dimensionResource(id = R.dimen.padding_xxl)))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_horiz_more),
                        contentDescription = stringResource(R.string.shopping_list_options),
                        modifier = Modifier.size(dimensionResource(R.dimen.padding_xxl))
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surface
                    )
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.shopping_list_add_items),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            navController.navigate(Screen.AddShoppingListItem.route)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.shopping_list_clear_checked),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            if (checkedCount > 0) {
                                showClearCheckedDialog = true
                            } else {
                                Toast.makeText(context, R.string.shopping_list_no_checked_items, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.shopping_list_clear_all),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            if (uiState.items.isNotEmpty()) {
                                showClearAllDialog = true
                            } else {
                                Toast.makeText(context, R.string.shopping_list_already_empty, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        },
        containerColor = Color(0xFFF8F8F8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_l)))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text(stringResource(R.string.shopping_list_search_placeholder), style = MaterialTheme.typography.bodyMedium) },
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
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge,
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
                        onClick = { viewModel.toggleCategory(category) },
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

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_l)))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) 
                            stringResource(R.string.shopping_list_empty_state) 
                        else 
                            stringResource(R.string.shopping_list_no_match)
                    )
                }
            } else {
                val grouped = uiState.filteredItems.groupBy { it.category ?: IngredientCategory.OTHERS }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_l)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = dimensionResource(id = R.dimen.padding_l),
                        end = dimensionResource(id = R.dimen.padding_l),
                        bottom = dimensionResource(id = R.dimen.padding_xxl)
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
                            ShoppingListItemCard(item) {
                                viewModel.toggleChecked(item)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showClearCheckedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCheckedDialog = false },
            title = { Text(stringResource(R.string.shopping_list_clear_checked_dialog_title)) },
            text = { Text(stringResource(R.string.shopping_list_clear_checked_dialog_text, checkedCount)) },
            confirmButton = {
                val toastMessage = stringResource(R.string.shopping_list_clear_checked_toast, checkedCount)
                TextButton(onClick = {
                    viewModel.clearChecked()
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                    showClearCheckedDialog = false
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCheckedDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.shopping_list_clear_all_dialog_title)) },
            text = { Text(stringResource(R.string.shopping_list_clear_all_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    Toast.makeText(context, R.string.shopping_list_clear_all_toast, Toast.LENGTH_SHORT).show()
                    showClearAllDialog = false
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onCheckedChange: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange() },
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_sm)),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.padding_xsm)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.padding_md))
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_sm))
            ) {
                Text(
                    text = item.entity.ingredientName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        textDecoration = if (item.entity.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.ingredientDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Checkbox(
                checked = item.entity.isChecked,
                onCheckedChange = { onCheckedChange() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
