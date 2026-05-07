package com.boodschappen.app.data.remote

import com.boodschappen.app.data.local.ShoppingItem
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.abs

class FirestoreRepository {

    private val db = Firebase.firestore

    companion object {
        fun generateCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return (1..6).map { chars.random() }.joinToString("")
        }

        fun newItemId(): Long =
            abs(UUID.randomUUID().hashCode().toLong()).let { if (it == 0L) 1L else it }
    }

    private fun listDoc(code: String) =
        db.collection("gedeeldelijsten").document(code)

    private fun ShoppingItem.toMap(): Map<String, Any?> = mapOf(
        "id"        to id,
        "name"      to name,
        "quantity"  to quantity,
        "unit"      to unit,
        "category"  to category,
        "barcode"   to barcode,
        "imageUrl"  to imageUrl,
        "brand"     to brand,
        "isChecked" to isChecked,
        "note"      to note,
        "createdAt" to createdAt
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toShoppingItem() = ShoppingItem(
        id        = (get("id") as? Long) ?: (get("id") as? Number)?.toLong() ?: 0L,
        name      = get("name") as? String ?: "",
        quantity  = get("quantity") as? String ?: "1",
        unit      = get("unit") as? String ?: "",
        category  = get("category") as? String ?: "Overig",
        barcode   = get("barcode") as? String,
        imageUrl  = get("imageUrl") as? String,
        brand     = get("brand") as? String,
        isChecked = get("isChecked") as? Boolean ?: false,
        note      = get("note") as? String ?: "",
        createdAt = (get("createdAt") as? Long) ?: (get("createdAt") as? Number)?.toLong()
                    ?: System.currentTimeMillis()
    )

    /** Maak een nieuwe gedeelde lijst aan en geef de code terug */
    suspend fun createList(code: String, items: List<ShoppingItem>) {
        val data = mapOf(
            "items"     to items.map { it.toMap() },
            "createdAt" to System.currentTimeMillis()
        )
        listDoc(code).set(data).await()
    }

    /** Controleer of een lijst met deze code bestaat */
    suspend fun listExists(code: String): Boolean =
        listDoc(code).get().await().exists()

    /** Publiceer de huidige itemslijst naar Firestore */
    suspend fun publishList(code: String, items: List<ShoppingItem>) {
        listDoc(code).update("items", items.map { it.toMap() }).await()
    }

    /** Luister realtime naar wijzigingen in de gedeelde lijst */
    fun getItemsFlow(code: String): Flow<List<ShoppingItem>> = callbackFlow {
        val listener = listDoc(code).addSnapshotListener { snap, error ->
            if (error != null || snap == null) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val rawItems = snap.get("items") as? List<Map<String, Any?>> ?: emptyList()
            val items = rawItems
                .map { it.toShoppingItem() }
                .sortedWith(compareBy({ it.isChecked }, { -it.createdAt }))
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    /** Verwijder de gedeelde lijst */
    suspend fun deleteList(code: String) {
        listDoc(code).delete().await()
    }
}
