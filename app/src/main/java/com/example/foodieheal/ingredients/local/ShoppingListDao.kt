package com.example.foodieheal.ingredients.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the local multi-list Shopping List module.
 * All operations are local-only — no Supabase sync.
 */
@Dao
interface ShoppingListDao {

    // ──────────────── Shopping Lists ────────────────

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE user_id = :userId ORDER BY created_at DESC")
    fun getAllShoppingListsWithItems(userId: String): Flow<List<ShoppingListWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE user_id = :userId AND shopping_list_id = :shoppingListId LIMIT 1")
    fun getShoppingListWithItems(userId: String, shoppingListId: String): Flow<ShoppingListWithItemsEntity?>

    @Query("SELECT * FROM shopping_lists WHERE user_id = :userId ORDER BY shopping_list_id DESC")
    suspend fun getShoppingListsForUser(userId: String): List<ShoppingListEntity>

    @Query("SELECT * FROM shopping_lists WHERE user_id = :userId ORDER BY last_updated DESC LIMIT 1")
    suspend fun getLatestShoppingList(userId: String): ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity)

    @Query("SELECT * FROM shopping_lists WHERE user_id = :userId AND is_default = 1 LIMIT 1")
    suspend fun getDefaultShoppingList(userId: String): ShoppingListEntity?

    @Query("UPDATE shopping_lists SET is_default = 0 WHERE user_id = :userId")
    suspend fun clearAllDefaults(userId: String)

    @Query("UPDATE shopping_lists SET is_default = 1, last_updated = :lastUpdated WHERE shopping_list_id = :shoppingListId AND user_id = :userId")
    suspend fun setListAsDefault(shoppingListId: String, userId: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_lists SET is_default = 0, last_updated = :lastUpdated WHERE shopping_list_id = :shoppingListId AND user_id = :userId")
    suspend fun deselectListAsDefault(shoppingListId: String, userId: String, lastUpdated: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setDefaultShoppingList(shoppingListId: String, userId: String) {
        clearAllDefaults(userId)
        setListAsDefault(shoppingListId, userId)
    }

    @Query("UPDATE shopping_lists SET last_updated = :lastUpdated WHERE shopping_list_id = :shoppingListId AND user_id = :userId")
    suspend fun updateLastUpdated(shoppingListId: String, userId: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_lists SET title = :title, last_updated = :lastUpdated WHERE shopping_list_id = :shoppingListId AND user_id = :userId")
    suspend fun updateShoppingListTitle(shoppingListId: String, userId: String, title: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("DELETE FROM shopping_lists WHERE shopping_list_id = :shoppingListId AND user_id = :userId")
    suspend fun deleteShoppingList(shoppingListId: String, userId: String)

    @Query("DELETE FROM shopping_lists WHERE user_id = :userId")
    suspend fun deleteAllShoppingLists(userId: String)

    // ──────────────── Shopping List Items ────────────────

    @Query("SELECT * FROM shopping_list_items WHERE shopping_list_id = :shoppingListId AND user_id = :userId")
    fun getItemsForList(shoppingListId: String, userId: String): Flow<List<ShoppingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingListItemEntity>)

    @Query("UPDATE shopping_list_items SET is_checked = :isChecked WHERE id = :id")
    suspend fun updateItemChecked(id: Long, isChecked: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM shopping_list_items WHERE shopping_list_id = :shoppingListId AND user_id = :userId AND is_checked = 1")
    suspend fun clearCheckedItems(shoppingListId: String, userId: String)

    @Query("DELETE FROM shopping_list_items WHERE shopping_list_id = :shoppingListId AND user_id = :userId")
    suspend fun clearAllItemsInList(shoppingListId: String, userId: String)

    @Query("DELETE FROM shopping_list_items WHERE user_id = :userId")
    suspend fun deleteAllItemsForUser(userId: String)
}
