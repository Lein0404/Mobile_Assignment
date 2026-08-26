package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.ShoppingListItem
import com.example.foodieheal.ingredients.shared.IngredientSearchAndFilter
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ingredients.viewModel.ShoppingListViewModel
import com.example.foodieheal.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    navController: NavController,
    shoppingListId: String? = null,
    viewModel: ShoppingListViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(shoppingListId) {
        if (shoppingListId != null) {
            viewModel.selectShoppingList(shoppingListId)
        }
    }

    var showTopMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val activeList = uiState.activeShoppingList
    val checkedCount = uiState.items.count { it.isChecked }
    val totalCount = uiState.items.size

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedUpdated = remember(activeList?.lastUpdated) {
        if (activeList != null) dateFormat.format(Date(activeList.lastUpdated)) else ""
    }

    // ──────────────── Editable Title State ────────────────
    val currentSavedTitle = activeList?.title?.ifEmpty { activeList.shoppingListId } ?: ""
    var editableTitle by remember(activeList?.shoppingListId, activeList?.title) {
        mutableStateOf(currentSavedTitle)
    }

    val hasUnsavedChanges = activeList != null &&
        editableTitle.trim().isNotEmpty() &&
        editableTitle.trim() != currentSavedTitle

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showChangeDefaultDialog by remember { mutableStateOf(false) }

    fun saveTitleIfChanged() {
        val trimmed = editableTitle.trim()
        if (activeList != null && trimmed.isNotEmpty() && trimmed != currentSavedTitle) {
            viewModel.updateShoppingListTitle(activeList.shoppingListId, trimmed)
        }
    }

    fun handleBack() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
            navController.popBackStack()
        }
    }

    // Handle physical / system back button press
    BackHandler(enabled = true) {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    saveTitleIfChanged()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (formattedUpdated.isNotEmpty()) {
                            Text(
                                text = "Last updated: $formattedUpdated",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.shopping_list_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTopMenu = !showTopMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Share shopping list",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    // Empty for now (will be implemented later)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (activeList?.isDefault == true) "Deselect as default" else "Set as default",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (activeList?.isDefault == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    activeList?.let { currentList ->
                                        if (currentList.isDefault) {
                                            viewModel.deselectDefaultShoppingList(currentList.shoppingListId)
                                            Toast.makeText(context, "Shopping list deselected as default", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val currentDefault = uiState.shoppingLists.find { it.isDefault }
                                            if (currentDefault != null && currentDefault.shoppingListId != currentList.shoppingListId) {
                                                showChangeDefaultDialog = true
                                            } else {
                                                viewModel.setDefaultShoppingList(currentList.shoppingListId)
                                                Toast.makeText(context, "Set as default shopping list", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete shopping list",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    activeList?.let {
                                        viewModel.deleteShoppingList(it.shoppingListId)
                                        Toast.makeText(context, "Shopping list deleted", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (activeList != null) {
                Box {
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
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
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false },
                        modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Add item",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showFabMenu = false
                                navController.navigate(Screen.AddShoppingListItem.createRoute(activeList.shoppingListId))
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
                                showFabMenu = false
                                if (checkedCount > 0) {
                                    viewModel.onShowClearCheckedDialog(true)
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
                                showFabMenu = false
                                if (uiState.items.isNotEmpty()) {
                                    viewModel.onShowClearAllDialog(true)
                                } else {
                                    Toast.makeText(context, R.string.shopping_list_already_empty, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    saveTitleIfChanged()
                }
        ) {
            // ──────────────── Search & Category Filters ────────────────
            IngredientSearchAndFilter(
                searchQuery = uiState.itemSearchQuery,
                onSearchQueryChange = { viewModel.onItemSearchQueryChange(it) },
                searchPlaceholder = "Search items here",
                selectedCategories = uiState.selectedCategories,
                onToggleCategory = { viewModel.toggleCategory(it) },
                isExpanded = uiState.isCategoriesExpanded,
                onExpandedChange = { viewModel.toggleCategoriesExpanded() }
            )

            // ──────────────── Items Content ────────────────
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                // Empty state for items
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_list_empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Button(
                            onClick = {
                                activeList?.let {
                                    navController.navigate(Screen.AddShoppingListItem.createRoute(it.shoppingListId))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_outline_add), contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Items")
                        }
                    }
                }
            } else if (uiState.filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.shopping_list_no_match),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                val grouped = uiState.filteredItems.groupBy { it.category ?: IngredientCategory.OTHERS }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 100.dp
                    )
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(items, key = { it.id }) { item ->
                            ShoppingListItemRow(
                                item = item,
                                onCheckedChange = { viewModel.toggleChecked(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ──────────────── Unsaved Changes Dialog ────────────────
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Save Changes?") },
            text = { Text("Do you want to save changes to the shopping list title before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    saveTitleIfChanged()
                    showUnsavedChangesDialog = false
                    navController.popBackStack()
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editableTitle = currentSavedTitle
                    showUnsavedChangesDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // ──────────────── Change Default Confirmation Dialog ────────────────
    if (showChangeDefaultDialog && activeList != null) {
        val currentDefault = uiState.shoppingLists.find { it.isDefault }
        val currentDefaultName = currentDefault?.title?.ifEmpty { currentDefault.shoppingListId } ?: ""
        AlertDialog(
            onDismissRequest = { showChangeDefaultDialog = false },
            title = { Text("Change Default Shopping List") },
            text = { Text("A shopping list ($currentDefaultName) has already been set as default. Change to this shopping list instead?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDefaultShoppingList(activeList.shoppingListId)
                    Toast.makeText(context, "Set as default shopping list", Toast.LENGTH_SHORT).show()
                    showChangeDefaultDialog = false
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeDefaultDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // ──────────────── Clear Confirmation Dialogs ────────────────
    if (uiState.showClearCheckedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowClearCheckedDialog(false) },
            title = { Text(stringResource(R.string.shopping_list_clear_checked_dialog_title)) },
            text = { Text(stringResource(R.string.shopping_list_clear_checked_dialog_text, checkedCount)) },
            confirmButton = {
                val toastMessage = stringResource(R.string.shopping_list_clear_checked_toast, checkedCount)
                TextButton(onClick = {
                    viewModel.clearChecked()
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                    viewModel.onShowClearCheckedDialog(false)
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowClearCheckedDialog(false) }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (uiState.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowClearAllDialog(false) },
            title = { Text(stringResource(R.string.shopping_list_clear_all_dialog_title)) },
            text = { Text(stringResource(R.string.shopping_list_clear_all_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    Toast.makeText(context, R.string.shopping_list_clear_all_toast, Toast.LENGTH_SHORT).show()
                    viewModel.onShowClearAllDialog(false)
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowClearAllDialog(false) }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

/**
 * Clean ingredient item row matching Image 3: Checkbox on left, Item name on right with line-through when checked.
 */
@Composable
fun ShoppingListItemRow(
    item: ShoppingListItem,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Text(
            text = item.ingredientName,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
