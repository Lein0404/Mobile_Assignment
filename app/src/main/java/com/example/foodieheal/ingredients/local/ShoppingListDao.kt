package com.example.foodieheal.ingredients.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the local Shopping List.
 * All operations are local-only — no Supabase sync.
 */
@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_list WHERE user_id = :userId ORDER BY added_at DESC")
    fun getAllItems(userId: String): Flow<List<ShoppingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListEntity)

    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE shopping_list SET is_checked = :isChecked WHERE id = :id")
    suspend fun updateChecked(id: Long, isChecked: Boolean)

    @Query("DELETE FROM shopping_list WHERE user_id = :userId")
    suspend fun clearAll(userId: String)

    @Query("DELETE FROM shopping_list WHERE user_id = :userId AND is_checked = 1")
    suspend fun clearChecked(userId: String)
}
