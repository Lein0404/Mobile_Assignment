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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.ShoppingListItem
import com.example.foodieheal.ingredients.shared.IngredientSearchAndFilter
import com.example.foodieheal.ingredients.shared.ShoppingListShareHelper
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
    val detailState = uiState.detailState
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val focusManager = LocalFocusManager.current

    LaunchedEffect(shoppingListId) {
        if (shoppingListId != null) {
            viewModel.selectShoppingList(shoppingListId)
        }
    }

    var showTopMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val activeList = detailState.activeShoppingList
    val checkedCount = detailState.items.count { it.isChecked }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedUpdated = remember(activeList?.lastUpdated) {
        if (activeList != null) dateFormat.format(Date(activeList.lastUpdated)) else ""
    }

    val currentSavedTitle = activeList?.title?.ifEmpty { activeList.shoppingListId } ?: ""
    val hasUnsavedChanges = activeList != null &&
        detailState.editableTitle.trim().isNotEmpty() &&
        detailState.editableTitle.trim() != currentSavedTitle

    // Functions to handle Shopping List title change
    fun saveTitleIfChanged() {
        val trimmed = detailState.editableTitle.trim()
        if (activeList != null && trimmed.isNotEmpty() && trimmed != currentSavedTitle) {
            viewModel.updateShoppingListTitle(activeList.shoppingListId, trimmed)
        }
    }

    fun handleBack() {
        if (hasUnsavedChanges) {
            viewModel.onShowUnsavedChangesDialog(true)
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
                            value = detailState.editableTitle,
                            onValueChange = { viewModel.updateEditableTitle(it) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimary,
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
                                text = stringResource(R.string.shopping_list_last_updated, formattedUpdated),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrowback),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTopMenu = !showTopMenu }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_vertical_more),
                                contentDescription = stringResource(R.string.shopping_list_options),
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
                                        text = stringResource(R.string.shopping_list_share),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_share),
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_medium_size)),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    activeList?.let {
                                        val currentShoppingList = it.copy(
                                            title = detailState.editableTitle.trim().ifEmpty { it.title },
                                            items = detailState.items
                                        )
                                        ShoppingListShareHelper.shareShoppingList(context, currentShoppingList)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.shopping_list_copy_clipboard),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_content_copy),
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_medium_size)),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    activeList?.let {
                                        val currentShoppingList = it.copy(
                                            title = detailState.editableTitle.trim().ifEmpty { it.title },
                                            items = detailState.items
                                        )
                                        ShoppingListShareHelper.copyShoppingListToClipboard(context, currentShoppingList)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.shopping_list_paste_clipboard),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_content_paste),
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_medium_size)),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    navController.navigate(
                                        Screen.ShoppingListAddFrom.createRoute(activeList?.shoppingListId)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (activeList?.isDefault == true) stringResource(R.string.shopping_list_deselect_default) else stringResource(R.string.shopping_list_set_default),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check_circle),
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_medium_size)),
                                        tint = if (activeList?.isDefault == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    activeList?.let { currentList ->
                                        val title = currentList.title.ifEmpty { currentList.shoppingListId }
                                        if (currentList.isDefault) {
                                            viewModel.deselectDefaultShoppingList(currentList.shoppingListId)
                                            Toast.makeText(context, application.getString(R.string.shopping_list_deselect_default_toast, title), Toast.LENGTH_SHORT).show()
                                        } else {
                                            val currentDefault = uiState.homeState.shoppingLists.find { it.isDefault }
                                            if (currentDefault != null && currentDefault.shoppingListId != currentList.shoppingListId) {
                                                viewModel.onShowDetailChangeDefaultDialog(true)
                                            } else {
                                                viewModel.setDefaultShoppingList(currentList.shoppingListId)
                                                Toast.makeText(context, application.getString(R.string.shopping_list_set_default_toast, title), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.shopping_list_delete),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = null,
                                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_medium_size)),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.onShowDetailDeleteDialog(true)
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
                                    text = stringResource(R.string.shopping_list_add_item),
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
                                if (detailState.items.isNotEmpty()) {
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
                searchQuery = detailState.searchQuery,
                onSearchQueryChange = { viewModel.onItemSearchQueryChange(it) },
                searchPlaceholder = stringResource(R.string.shopping_list_search_items_placeholder),
                selectedCategories = detailState.selectedCategories,
                onToggleCategory = { viewModel.toggleCategory(it) },
                isExpanded = detailState.isCategoriesExpanded,
                onExpandedChange = { viewModel.toggleCategoriesExpanded() }
            )

            // ──────────────── Items Content ────────────────
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (detailState.items.isEmpty()) {
                // Empty state for items
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_l)),
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_xxl))
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_list_empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                activeList?.let {
                                    navController.navigate(Screen.AddShoppingListItem.createRoute(it.shoppingListId))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm))
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_outline_add), contentDescription = null)
                            Spacer(Modifier.width(dimensionResource(id = R.dimen.padding_smd)))
                            Text(stringResource(R.string.shopping_list_add_items_button))
                        }
                    }
                }
            } else if (detailState.filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.shopping_list_no_match),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val grouped = detailState.filteredItems.groupBy { it.category ?: IngredientCategory.OTHERS }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_xxsm)),
                    contentPadding = PaddingValues(
                        start = dimensionResource(id = R.dimen.padding_l),
                        end = dimensionResource(id = R.dimen.padding_l),
                        top = dimensionResource(id = R.dimen.padding_smd),
                        bottom = 100.dp
                    )
                ) {
                    grouped.entries.forEachIndexed { index, entry ->
                        val category = entry.key
                        val items = entry.value

                        item {
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_xl)))
                            }
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(
                                    bottom = dimensionResource(R.dimen.padding_xsm)
                                )
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
    if (detailState.showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowUnsavedChangesDialog(false) },
            title = { Text(stringResource(R.string.shopping_list_save_changes_title)) },
            text = { Text(stringResource(R.string.shopping_list_save_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    saveTitleIfChanged()
                    viewModel.onShowUnsavedChangesDialog(false)
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.btn_save), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.updateEditableTitle(currentSavedTitle)
                    viewModel.onShowUnsavedChangesDialog(false)
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.btn_discard), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // ──────────────── Change Default Confirmation Dialog ────────────────
    if (detailState.showChangeDefaultDialog && activeList != null) {
        val currentDefault = uiState.homeState.shoppingLists.find { it.isDefault }
        val currentDefaultName = currentDefault?.title?.ifEmpty { currentDefault.shoppingListId } ?: ""
        AlertDialog(
            onDismissRequest = { viewModel.onShowDetailChangeDefaultDialog(false) },
            title = { Text(stringResource(R.string.shopping_list_change_default_title)) },
            text = { Text(stringResource(R.string.shopping_list_change_default_message, currentDefaultName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDefaultShoppingList(activeList.shoppingListId)
                    val title = activeList.title.ifEmpty { activeList.shoppingListId }
                    Toast.makeText(context, application.getString(R.string.shopping_list_set_default_toast, title), Toast.LENGTH_SHORT).show()
                    viewModel.onShowDetailChangeDefaultDialog(false)
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowDetailChangeDefaultDialog(false) }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // ──────────────── Delete Confirmation Dialog ────────────────
    if (detailState.showDeleteDialog) {
        val targetId = detailState.selectedShoppingListId ?: ""
        val targetList = activeList
        val targetName = targetList?.title?.ifEmpty { targetId } ?: targetId
        AlertDialog(
            onDismissRequest = { viewModel.onShowDetailDeleteDialog(false) },
            title = { Text(stringResource(R.string.shopping_list_delete_title)) },
            text = { Text(stringResource(R.string.shopping_list_delete_message, targetName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteShoppingList(targetId)
                    Toast.makeText(context, application.getString(R.string.shopping_list_deleted_toast, targetName), Toast.LENGTH_SHORT).show()
                    viewModel.onShowDetailDeleteDialog(false)
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowDetailDeleteDialog(false) }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // ──────────────── Clear Confirmation Dialogs ────────────────
    if (detailState.showClearCheckedDialog) {
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

    if (detailState.showClearAllDialog) {
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

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItem,
    onCheckedChange: () -> Unit
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_smd))
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onCheckedChange() },
                modifier = Modifier.offset(x = (-3).dp), // Align visual box with header text
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
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
