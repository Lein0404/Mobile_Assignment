package com.example.foodieheal.ingredients.shared

import android.content.Context
import android.content.Intent
import com.example.foodieheal.ingredients.model.ShoppingList

/**
 * Utility functions for sharing a shopping list as plain text via Android Share Sheet.
 */
object ShoppingListShareHelper {

    fun formatShoppingListForSharing(shoppingList: ShoppingList): String {
        val title = shoppingList.title.ifEmpty { shoppingList.shoppingListId }
        if (shoppingList.items.isEmpty()) {
            return "$title\n\n(No items in shopping list)"
        }

        val grouped = shoppingList.items.groupBy { it.category?.categoryName ?: "Others" }
        val body = grouped.entries.joinToString("\n\n") { (categoryName, items) ->
            val itemsText = items.joinToString("\n") { item ->
                if (item.isChecked) "${item.ingredientName} ✔" else item.ingredientName
            }
            "[$categoryName]\n$itemsText"
        }

        return "$title\n\n$body"
    }

    fun shareShoppingList(context: Context, shoppingList: ShoppingList) {
        val shareText = formatShoppingListForSharing(shoppingList)
        val title = shoppingList.title.ifEmpty { shoppingList.shoppingListId }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share shopping list")
        context.startActivity(shareIntent)
    }
}
