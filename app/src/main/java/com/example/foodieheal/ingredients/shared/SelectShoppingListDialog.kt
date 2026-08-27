package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                text = "Select shopping list to add",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Option 1: Existing Shopping List ──
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Existing shopping list",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!isNewList) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!isNewList) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
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

                // ── Option 2: New Shopping List ──
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New shopping list",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isNewList) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isNewList) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Shopping List Name",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = newListNameInput,
                            onValueChange = onNewListNameChange,
                            placeholder = {
                                Text(
                                    text = "Name",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
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
                    text = "Add",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
