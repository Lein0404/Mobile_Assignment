package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import com.example.foodieheal.ui.components.DropDownList

/**
 * Dialog prompting user to select which shopping list to add an ingredient to
 * or create a new shopping list for the ingredient.
 */
@Composable
fun SelectShoppingListDialog(
    shoppingLists: List<ShoppingListEntity>,
    isNewList: Boolean,
    newListNameInput: String,
    selectedIndex: Int,
    onIsNewListChange: (Boolean) -> Unit,
    onNewListNameChange: (String) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onConfirmNewList: () -> Unit
) {
    val options = remember(shoppingLists) {
        shoppingLists.map { it.title.ifEmpty { it.shoppingListId } }
    }
    val currentSelectedTitle = options.getOrElse(selectedIndex) { options.firstOrNull() ?: "" }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.select_shopping_list_dialog_title),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.padding_xsm)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_md))
            ) {
                // Option 1: Existing Shopping List
                if (shoppingLists.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIsNewListChange(false) }
                    ) {
                        RadioButton(
                            selected = !isNewList,
                            onClick = { onIsNewListChange(false) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_smd)))
                        Text(
                            text = stringResource(R.string.existing_shopping_list_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!isNewList) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!isNewList) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = dimensionResource(R.dimen.padding_md))
                        ) {
                            DropDownList(
                                labelId = R.string.shopping_list_title,
                                placeholderId = R.string.select_shopping_list_placeholder,
                                selectedValue = currentSelectedTitle,
                                options = options,
                                onOptionSelected = { chosenTitle ->
                                    val index = options.indexOf(chosenTitle)
                                    if (index >= 0) {
                                        onSelectedIndexChange(index)
                                    }
                                }
                            )
                        }
                    }
                }

                // Option 2: New Shopping List
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIsNewListChange(true) }
                ) {
                    RadioButton(
                        selected = isNewList,
                        onClick = { onIsNewListChange(true) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding_smd)))
                    Text(
                        text = stringResource(R.string.shopping_list_new_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNewList) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isNewList) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = dimensionResource(R.dimen.padding_md)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xsm))
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_list_name_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = newListNameInput,
                            onValueChange = onNewListNameChange,
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
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isNewList) {
                        onConfirmNewList()
                    } else {
                        onConfirm()
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.btn_add),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
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
