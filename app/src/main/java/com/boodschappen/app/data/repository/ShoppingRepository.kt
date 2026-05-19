package com.boodschappen.app.data.repository

import com.boodschappen.app.data.local.PriceRecord
import com.boodschappen.app.data.local.PriceRecordDao
import com.boodschappen.app.data.local.ShoppingDao
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.data.local.ShoppingList
import com.boodschappen.app.data.local.ShoppingListDao
import com.boodschappen.app.data.remote.OpenFoodFactsApi
import com.boodschappen.app.data.remote.ProductDto
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val dao: ShoppingDao,
    private val listDao: ShoppingListDao,
    private val api: OpenFoodFactsApi,
    private val priceDao: PriceRecordDao? = null
) {
    fun getItems(listId: Long): Flow<List<ShoppingItem>> = dao.getAllItems(listId)
    fun getAllLists(): Flow<List<ShoppingList>> = listDao.getAllLists()

    suspend fun addItem(item: ShoppingItem) {
        dao.insertItem(item)
        if (item.price != null) priceDao?.insert(PriceRecord(itemName = item.name.trim(), price = item.price))
    }

    suspend fun updateItem(item: ShoppingItem) {
        dao.updateItem(item)
        if (item.price != null) priceDao?.insert(PriceRecord(itemName = item.name.trim(), price = item.price))
    }

    suspend fun getLastPrice(name: String): PriceRecord? = priceDao?.getLatest(name.trim())
    suspend fun deleteItem(item: ShoppingItem) = dao.deleteItem(item)
    suspend fun getItemById(id: Long) = dao.getItemById(id)
    suspend fun getOverigItems() = dao.getOverigItems()

    suspend fun deleteCheckedItems(listId: Long) {
        dao.deleteCheckedNonRecurringItems(listId)
        dao.resetRecurringCheckedItems(listId)
    }

    suspend fun deleteAllItems(listId: Long) = dao.deleteAllItems(listId)

    suspend fun createList(list: ShoppingList): Long = listDao.insertList(list)
    suspend fun updateList(list: ShoppingList) = listDao.updateList(list)
    suspend fun deleteList(listId: Long) {
        listDao.deleteItemsInList(listId)
        listDao.deleteListById(listId)
    }

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
            val product = response.products.firstOrNull { it.getBestImage() != null }
                ?: response.products.firstOrNull()
            if (product != null) Result.success(product)
            else Result.failure(Exception("Geen resultaat"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
