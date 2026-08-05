package com.example.foodieheal.ingredients.repo

import com.example.foodieheal.ingredients.local.ShoppingListDao
import com.example.foodieheal.ingredients.local.ShoppingListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ShoppingListRepository(private val dao: ShoppingListDao) {

    fun getShoppingList(userId: String): Flow<List<ShoppingListEntity>> {
        return dao.getAllItems(userId)
    }

    suspend fun insertItem(item: ShoppingListEntity) = withContext(Dispatchers.IO) {
        dao.insertItem(item)
    }

    suspend fun updateChecked(id: Long, isChecked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateChecked(id, isChecked)
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteItem(id)
    }

    suspend fun clearChecked(userId: String) = withContext(Dispatchers.IO) {
        dao.clearChecked(userId)
    }

    suspend fun clearAll(userId: String) = withContext(Dispatchers.IO) {
        dao.clearAll(userId)
    }
}
