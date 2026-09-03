package com.example.foodieheal.ingredients.repo

import com.example.foodieheal.ingredients.local.*
import com.example.foodieheal.ingredients.model.IngredientCategory
import com.example.foodieheal.ingredients.model.ShoppingList
import com.example.foodieheal.ingredients.model.ShoppingListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ShoppingListRepository(private val dao: ShoppingListDao) {

    /**
     * Observe all shopping lists (with their items) for a user.
     */
    fun getShoppingLists(userId: String): Flow<List<ShoppingList>> {
        return dao.getAllShoppingListsWithItems(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Observe a specific shopping list (with its items) by ID.
     */
    fun getShoppingList(userId: String, shoppingListId: String): Flow<ShoppingList?> {
        return dao.getShoppingListWithItems(userId, shoppingListId).map { it?.toDomain() }
    }

    /**
     * Generates a unique sequential ID for shopping list (e.g. "SPL_${userId}_0001").
     * Prefixing with userId ensures global uniqueness and prevents Room @Relation data leakage across users.
     */
    suspend fun getNextShoppingListId(userId: String): String = withContext(Dispatchers.IO) {
        val existingLists = dao.getShoppingListsForUser(userId)
        val maxNum = existingLists
            .mapNotNull {
                it.shoppingListId.substringAfterLast("_").removePrefix("SPL").toIntOrNull()
            }
            .maxOrNull() ?: 0
        "SPL_${userId}_${(maxNum + 1).toString().padStart(4, '0')}"
    }

    /**
     * Creates a new shopping list for the user with an initial empty list.
     */
    suspend fun createShoppingList(userId: String, title: String? = null): ShoppingListEntity = withContext(Dispatchers.IO) {
        val nextId = getNextShoppingListId(userId)
        val now = System.currentTimeMillis()
        val listNumber = nextId.substringAfterLast("_").removePrefix("SPL").trimStart('0').ifEmpty { "1" }
        val defaultTitle = title?.takeIf { it.isNotBlank() } ?: "Shopping List $listNumber"
        val entity = ShoppingListEntity(
            shoppingListId = nextId,
            userId = userId,
            title = defaultTitle,
            createdAt = now,
            lastUpdated = now
        )
        dao.insertShoppingList(entity)
        entity
    }

    suspend fun updateShoppingListTitle(shoppingListId: String, userId: String, title: String) = withContext(Dispatchers.IO) {
        dao.updateShoppingListTitle(shoppingListId, userId, title)
    }

    suspend fun getDefaultShoppingList(userId: String): ShoppingListEntity? = withContext(Dispatchers.IO) {
        dao.getDefaultShoppingList(userId)
    }

    suspend fun setDefaultShoppingList(shoppingListId: String, userId: String) = withContext(Dispatchers.IO) {
        dao.setDefaultShoppingList(shoppingListId, userId)
    }

    suspend fun deselectDefaultShoppingList(shoppingListId: String, userId: String) = withContext(Dispatchers.IO) {
        dao.deselectListAsDefault(shoppingListId, userId)
    }

    suspend fun getShoppingListsForUser(userId: String): List<ShoppingListEntity> = withContext(Dispatchers.IO) {
        dao.getShoppingListsForUser(userId)
    }

    suspend fun insertItem(item: ShoppingListItemEntity) = withContext(Dispatchers.IO) {
        val existingItems = dao.getItemsForListSync(item.shoppingListId, item.userId)
        val itemBase = extractBaseName(item.ingredientName)
        val incomingCount = extractCount(item.ingredientName)
        val matchingExisting = existingItems.find {
            !it.isChecked && extractBaseName(it.ingredientName).equals(itemBase, ignoreCase = true)
        }

        if (matchingExisting != null) {
            val newName = aggregateIngredientName(matchingExisting.ingredientName, incomingCount)
            dao.updateItemName(matchingExisting.id, newName)
        } else {
            dao.insertItem(item)
        }
        dao.updateLastUpdated(item.shoppingListId, item.userId)
    }

    suspend fun insertItems(items: List<ShoppingListItemEntity>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val shoppingListId = items.first().shoppingListId
        val userId = items.first().userId
        val currentItems = dao.getItemsForListSync(shoppingListId, userId).toMutableList()

        for (item in items) {
            val itemBase = extractBaseName(item.ingredientName)
            val incomingCount = extractCount(item.ingredientName)
            val matchingIndex = currentItems.indexOfFirst {
                !it.isChecked && extractBaseName(it.ingredientName).equals(itemBase, ignoreCase = true)
            }

            if (matchingIndex >= 0) {
                val matching = currentItems[matchingIndex]
                val newName = aggregateIngredientName(matching.ingredientName, incomingCount)
                dao.updateItemName(matching.id, newName)
                currentItems[matchingIndex] = matching.copy(ingredientName = newName)
            } else {
                dao.insertItem(item)
                currentItems.add(item)
            }
        }
        dao.updateLastUpdated(shoppingListId, userId)
    }

    companion object {
        fun extractCount(name: String): Int {
            val regex = Regex("\\(x(\\d+)\\)$")
            val match = regex.find(name.trim())
            return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        }

        fun extractBaseName(name: String): String =
            name.replace(Regex("\\s*\\(x\\d+\\)$"), "").trim()

        fun aggregateIngredientName(existingName: String, countToAdd: Int = 1): String {
            val base = extractBaseName(existingName)
            val currentCount = extractCount(existingName)
            val newCount = currentCount + countToAdd
            return if (newCount > 1) "$base (x$newCount)" else base
        }
    }

    suspend fun updateChecked(id: Long, shoppingListId: String, userId: String, isChecked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateItemChecked(id, isChecked)
        dao.updateLastUpdated(shoppingListId, userId)
    }

    suspend fun deleteItem(id: Long, shoppingListId: String, userId: String) = withContext(Dispatchers.IO) {
        dao.deleteItem(id)
        dao.updateLastUpdated(shoppingListId, userId)
    }

    suspend fun clearChecked(shoppingListId: String, userId: String) = withContext(Dispatchers.IO) {
        dao.clearCheckedItems(shoppingListId, userId)
        dao.updateLastUpdated(shoppingListId, userId)
    }

    suspend fun clearAll(shoppingListId: String, userId: String) = withContext(Dispatchers.IO) {
        dao.clearAllItemsInList(shoppingListId, userId)
        dao.updateLastUpdated(shoppingListId, userId)
    }

    suspend fun deleteShoppingList(shoppingListId: String, userId: String) = withContext(Dispatchers.IO) {
        dao.clearAllItemsInList(shoppingListId, userId)
        dao.deleteShoppingList(shoppingListId, userId)
    }

    /**
     * Adds an ingredient item to the user's active/latest shopping list,
     * or creates a new "SPL0001" list first if the user has no shopping lists yet.
     */
    suspend fun addItemToActiveOrCreateShoppingList(
        userId: String,
        ingredientId: String,
        ingredientName: String,
        category: IngredientCategory? = null
    ): ShoppingListItemEntity = withContext(Dispatchers.IO) {
        val latestList = dao.getLatestShoppingList(userId) ?: createShoppingList(userId)
        val item = ShoppingListItemEntity(
            shoppingListId = latestList.shoppingListId,
            userId = userId,
            ingredientId = ingredientId,
            ingredientName = ingredientName,
            ingredientCategory = category?.name,
            isChecked = false
        )
        insertItem(item)
        item
    }
}
