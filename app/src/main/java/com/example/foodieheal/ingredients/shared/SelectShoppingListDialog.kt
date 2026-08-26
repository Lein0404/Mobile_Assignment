package com.example.foodieheal.ingredients.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodieheal.R
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import com.example.foodieheal.ui.components.DropDownList

/**
 * Dialog prompting user to select which shopping list to add an ingredient to
 * when multiple shopping lists exist and no default list is set.
 */
@Composable
fun SelectShoppingListDialog(
    shoppingLists: List<ShoppingListEntity>,
    onDismissRequest: () -> Unit,
    onConfirm: (ShoppingListEntity) -> Unit
) {
    if (shoppingLists.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(0) }
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
                    .padding(top = 8.dp)
            ) {
                DropDownList(
                    labelId = R.string.shopping_list_title,
                    placeholderId = R.string.select_shopping_list_placeholder,
                    selectedValue = currentSelectedTitle,
                    options = options,
                    onOptionSelected = { chosenTitle ->
                        val idx = options.indexOf(chosenTitle)
                        if (idx >= 0) {
                            selectedIndex = idx
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val chosen = shoppingLists.getOrElse(selectedIndex) { shoppingLists.first() }
                    onConfirm(chosen)
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
