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
                Result.failure(Exception("OFF status=${response.status}, product=${response.product != null}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByName(query: String): Result<ProductDto> {
        return try {
            val response = api.searchByName(query)
            // Kies het eerste resultaat met een afbeelding
            val product = response.products.firstOrNull { it.getBestImage() != null }
                ?: response.products.firstOrNull()
            if (product != null) Result.success(product)
            else Result.failure(Exception("Geen resultaat"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
