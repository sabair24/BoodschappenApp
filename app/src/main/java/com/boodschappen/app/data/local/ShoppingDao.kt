package com.boodschappen.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY isChecked ASC, createdAt DESC")
    fun getAllItems(listId: Long): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE category = 'Overig'")
    suspend fun getOverigItems(): List<ShoppingItem>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getItemById(id: Long): ShoppingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1 AND isRecurring = 0 AND listId = :listId")
    suspend fun deleteCheckedNonRecurringItems(listId: Long)

    @Query("UPDATE shopping_items SET isChecked = 0 WHERE isChecked = 1 AND isRecurring = 1 AND listId = :listId")
    suspend fun resetRecurringCheckedItems(listId: Long)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteAllItems(listId: Long)
}
