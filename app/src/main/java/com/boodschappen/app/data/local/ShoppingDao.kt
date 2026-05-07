package com.boodschappen.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, createdAt DESC")
    fun getAllItems(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getItemById(id: Long): ShoppingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteCheckedItems()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllItems()

    @Query("SELECT * FROM shopping_items WHERE isChecked = 0 ORDER BY category ASC, name ASC")
    fun getUncheckedItems(): Flow<List<ShoppingItem>>
}
