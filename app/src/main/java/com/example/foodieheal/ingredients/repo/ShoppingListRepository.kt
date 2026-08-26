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
     * Generates the next sequential ID for shopping list (starts from "SPL0001" for each user).
     */
    suspend fun getNextShoppingListId(userId: String): String = withContext(Dispatchers.IO) {
        val existingLists = dao.getShoppingListsForUser(userId)
        val maxNum = existingLists
            .mapNotNull { it.shoppingListId.removePrefix("SPL").toIntOrNull() }
            .maxOrNull() ?: 0
        "SPL${(maxNum + 1).toString().padStart(4, '0')}"
    }

    /**
     * Creates a new shopping list for the user with an initial empty list.
     */
    suspend fun createShoppingList(userId: String, title: String? = null): ShoppingListEntity = withContext(Dispatchers.IO) {
        val nextId = getNextShoppingListId(userId)
        val now = System.currentTimeMillis()
        val defaultTitle = title?.takeIf { it.isNotBlank() } ?: "Shopping List ${nextId.removePrefix("SPL").trimStart('0')}"
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

    suspend fun insertItem(item: ShoppingListItemEntity) = withContext(Dispatchers.IO) {
        dao.insertItem(item)
        dao.updateLastUpdated(item.shoppingListId, item.userId)
    }

    suspend fun insertItems(items: List<ShoppingListItemEntity>) = withContext(Dispatchers.IO) {
        if (items.isNotEmpty()) {
            dao.insertItems(items)
            val first = items.first()
            dao.updateLastUpdated(first.shoppingListId, first.userId)
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
