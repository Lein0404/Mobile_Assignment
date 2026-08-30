package com.example.foodieheal.ingredients.view

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.model.ShoppingList
import com.example.foodieheal.ingredients.shared.ShoppingListShareHelper
import com.example.foodieheal.ingredients.viewModel.IngredientsViewModelFactory
import com.example.foodieheal.ingredients.viewModel.ShoppingListViewModel
import com.example.foodieheal.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListHomeScreen(
    navController: NavController,
    viewModel: ShoppingListViewModel = viewModel(
        factory = IngredientsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val homeState = uiState.homeState
    val context = LocalContext.current
    val application = context.applicationContext as Application

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shopping_list_home_title),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onShowCreateListDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.icon_xlarge_size))
                    .offset(y = (-dimensionResource(id = R.dimen.padding_xxl)))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_add),
                    contentDescription = stringResource(R.string.shopping_list_new_title),
                    modifier = Modifier.size(dimensionResource(id = R.dimen.padding_xxl))
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = dimensionResource(id = R.dimen.padding_l))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

            // Search Bar
            OutlinedTextField(
                value = homeState.searchQuery,
                onValueChange = { viewModel.onListSearchQueryChange(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.shopping_list_home_search_placeholder),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.search),
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_l)))

            // ──────────────── Content ────────────────
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (homeState.shoppingLists.isEmpty()) {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_l)),
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_xxl))
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_list_home_empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.onShowCreateListDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_outline_add),
                                contentDescription = stringResource(R.string.shopping_list_new_title)
                            )
                            Spacer(Modifier.width(dimensionResource(id = R.dimen.padding_smd)))
                            Text(stringResource(R.string.shopping_list_create_button))
                        }
                    }
                }
            } else if (homeState.filteredShoppingLists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.shopping_list_home_no_match),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_md)),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(homeState.filteredShoppingLists, key = { it.shoppingListId }) { list ->
                        ShoppingListCard(
                            shoppingList = list,
                            onClick = {
                                viewModel.selectShoppingList(list.shoppingListId)
                                navController.navigate(Screen.ShoppingList.createRoute(list.shoppingListId))
                            },
                            onSetDefault = {
                                val currentDefault = homeState.shoppingLists.find { it.isDefault }
                                if (currentDefault != null && currentDefault.shoppingListId != list.shoppingListId) {
                                    viewModel.onShowHomeChangeDefaultDialog(true, list)
                                } else {
                                    viewModel.setDefaultShoppingList(list.shoppingListId)
                                    val title = list.title.ifEmpty { list.shoppingListId }
                                    Toast.makeText(context, application.getString(R.string.shopping_list_set_default_toast, title), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeselectDefault = {
                                viewModel.deselectDefaultShoppingList(list.shoppingListId)
                                val title = list.title.ifEmpty { list.shoppingListId }
                                Toast.makeText(context, application.getString(R.string.shopping_list_deselect_default_toast, title), Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.onShowHomeDeleteListDialog(true, list.shoppingListId)
                            }
                        )
                    }
                }
            }
        }
    }

    // New Shopping List Dialog
    if (homeState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onShowCreateListDialog(false)
            },
            title = {
                Text(
                    text = stringResource(R.string.shopping_list_new_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = homeState.newListNameInput,
                        onValueChange = { viewModel.updateNewListName(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.label_name),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_sm)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createNewShoppingList(homeState.newListNameInput) { newListId ->
                        navController.navigate(Screen.ShoppingList.createRoute(newListId))
                    }
                }) {
                    Text(
                        text = stringResource(R.string.btn_add),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onShowCreateListDialog(false)
                }) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.padding_xl)),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Change Default Confirmation Dialog
    if (homeState.showChangeDefaultDialog && homeState.targetListForDefault != null) {
        val currentDefault = homeState.shoppingLists.find { it.isDefault }
        val currentDefaultName = currentDefault?.title?.ifEmpty { currentDefault.shoppingListId } ?: ""
        AlertDialog(
            onDismissRequest = {
                viewModel.onShowHomeChangeDefaultDialog(false)
            },
            title = { Text(stringResource(R.string.shopping_list_change_default_title)) },
            text = { Text(stringResource(R.string.shopping_list_change_default_message, currentDefaultName)) },
            confirmButton = {
                TextButton(onClick = {
                    homeState.targetListForDefault?.let {
                        viewModel.setDefaultShoppingList(it.shoppingListId)
                        val title = it.title.ifEmpty { it.shoppingListId }
                        Toast.makeText(context, application.getString(R.string.shopping_list_set_default_toast, title), Toast.LENGTH_SHORT).show()
                    }
                    viewModel.onShowHomeChangeDefaultDialog(false)
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onShowHomeChangeDefaultDialog(false)
                }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (homeState.showDeleteDialog) {
        val targetId = homeState.listToDeleteId ?: ""
        val targetList = homeState.shoppingLists.find { it.shoppingListId == targetId }
        val targetName = targetList?.title?.ifEmpty { targetId } ?: targetId
        AlertDialog(
            onDismissRequest = { viewModel.onShowHomeDeleteListDialog(false) },
            title = { Text(stringResource(R.string.shopping_list_delete_title)) },
            text = { Text(stringResource(R.string.shopping_list_delete_message, targetName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteShoppingList(targetId)
                    Toast.makeText(context, application.getString(R.string.shopping_list_deleted_toast, targetName), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.dialog_yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowHomeDeleteListDialog(false) }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun ShoppingListCard(
    shoppingList: ShoppingList,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onDeselectDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(shoppingList.lastUpdated) { dateFormat.format(Date(shoppingList.lastUpdated)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_md))
                ) {
                    Text(
                        text = shoppingList.title.ifEmpty { shoppingList.shoppingListId },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (shoppingList.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(dimensionResource(id = R.dimen.padding_xsm))
                        ) {
                            Text(
                                text = stringResource(R.string.label_default),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(id = R.dimen.padding_sm),
                                    vertical = dimensionResource(id = R.dimen.padding_xxsm)
                                )
                            )
                        }
                    }
                }

                // Date Pill Tag
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(dimensionResource(id = R.dimen.padding_xsm))
                        )
                        .padding(
                            horizontal = dimensionResource(id = R.dimen.padding_sm),
                            vertical = dimensionResource(id = R.dimen.padding_xxsm)
                        )
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // 3 dots vertical menu
            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vertical_more),
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
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
                            showMenu = false
                            ShoppingListShareHelper.shareShoppingList(context, shoppingList)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text =
                                    if (shoppingList.isDefault) stringResource(R.string.shopping_list_deselect_default)
                                    else stringResource(R.string.shopping_list_set_default),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_medium_size)),
                                tint =
                                    if (shoppingList.isDefault) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            if (shoppingList.isDefault) {
                                onDeselectDefault()
                            } else {
                                onSetDefault()
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
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
