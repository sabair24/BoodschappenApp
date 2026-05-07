package com.boodschappen.app.data.repository

import com.boodschappen.app.data.local.ShoppingDao
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.data.remote.OpenFoodFactsApi
import com.boodschappen.app.data.remote.ProductDto
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val dao: ShoppingDao,
    private val api: OpenFoodFactsApi
) {
    val allItems: Flow<List<ShoppingItem>> = dao.getAllItems()

    suspend fun addItem(item: ShoppingItem) = dao.insertItem(item)
    suspend fun updateItem(item: ShoppingItem) = dao.updateItem(item)
    suspend fun deleteItem(item: ShoppingItem) = dao.deleteItem(item)
    suspend fun deleteCheckedItems() = dao.deleteCheckedItems()
    suspend fun deleteAllItems() = dao.deleteAllItems()
    suspend fun getItemById(id: Long) = dao.getItemById(id)

    suspend fun lookupBarcode(barcode: String): Result<ProductDto> {
        return try {
            val response = api.getProduct(barcode)
            if (response.status == 1 && response.product != null) {
                Result.success(response.product)
            } else {
                Result.failure(Exception("Product niet gevonden"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
