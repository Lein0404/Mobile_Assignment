package com.example.foodieheal.ingredients.shared

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.foodieheal.ingredients.local.IngredientsEntity
import com.example.foodieheal.ingredients.model.ShoppingList

/**
 * Utility functions for sharing, copying, and parsing shopping lists.
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

    fun copyShoppingListToClipboard(context: Context, shoppingList: ShoppingList) {
        val shareText = formatShoppingListForSharing(shoppingList)
        val title = shoppingList.title.ifEmpty { shoppingList.shoppingListId }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(title, shareText)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Shopping list copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun getClipboardText(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text
            return text?.toString() ?: ""
        }
        return ""
    }

    /**
     * Parses raw clipboard text line by line and matches lines against existing registered ingredients.
     * Unrecognized lines (non-ingredient text) are filtered out and excluded.
     */
    fun parseAndValidateClipboardText(
        rawText: String,
        allIngredients: List<IngredientsEntity>
    ): List<IngredientsEntity> {
        if (rawText.isBlank() || allIngredients.isEmpty()) return emptyList()

        val lines = rawText.lines()
        val matchedList = mutableListOf<IngredientsEntity>()

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Skip category headers like "[Beverages]"
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) continue

            // Clean line: remove checkmark unicode/emojis and bullet points / numbering
            val cleanLine = trimmed
                .replace("✔", "")
                .replace("✅", "")
                .replace("[x]", "", ignoreCase = true)
                .replace("[v]", "", ignoreCase = true)
                .replace(Regex("^[•\\-*\\d+.\\s]+"), "")
                .trim()

            if (cleanLine.isEmpty()) continue

            // Extract base name without (xN) or general (quantity unit) parenthesis for matching
            val baseName = cleanLine
                .replace(Regex("\\s*\\(x\\d+\\)$"), "")
                .replace(Regex("\\s*\\([^)]+\\)$"), "")
                .trim()

            // 1. Exact match (case-insensitive) on full line or base name
            var matched = allIngredients.find { it.ingredientName.equals(cleanLine, ignoreCase = true) }
            if (matched == null && baseName.isNotEmpty()) {
                matched = allIngredients.find { it.ingredientName.equals(baseName, ignoreCase = true) }
            }

            // 2. Substring / contains match (find the most specific match)
            if (matched == null) {
                matched = allIngredients
                    .filter { ing ->
                        cleanLine.contains(ing.ingredientName, ignoreCase = true) ||
                        ing.ingredientName.contains(cleanLine, ignoreCase = true) ||
                        (baseName.isNotEmpty() && (baseName.contains(ing.ingredientName, ignoreCase = true) || ing.ingredientName.contains(baseName, ignoreCase = true)))
                    }
                    .maxByOrNull { it.ingredientName.length }
            }

            if (matched != null) {
                val existingIndex = matchedList.indexOfFirst { it.ingredientId == matched.ingredientId }
                if (existingIndex >= 0) {
                    val existing = matchedList[existingIndex]
                    val countToAdd = com.example.foodieheal.ingredients.repo.ShoppingListRepository.extractCount(cleanLine)
                    val newName = com.example.foodieheal.ingredients.repo.ShoppingListRepository.aggregateIngredientName(existing.ingredientName, countToAdd)
                    matchedList[existingIndex] = existing.copy(ingredientName = newName)
                } else {
                    matchedList.add(matched.copy(ingredientName = cleanLine))
                }
            }
        }

        return matchedList
    }
}
