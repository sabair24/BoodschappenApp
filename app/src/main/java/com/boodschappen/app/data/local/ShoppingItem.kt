package com.boodschappen.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: String = "1",
    val unit: String = "",
    val category: String = "Overig",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val brand: String? = null,
    val isChecked: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val price: Double? = null,
    val isRecurring: Boolean = false,
    val listId: Long = 1
)

enum class Category(val displayName: String, val emoji: String) {
    GROENTE_FRUIT("Groente & Fruit", "🥦"),
    ZUIVEL("Zuivel & Eieren", "🥛"),
    VLEES_VIS("Vlees & Vis", "🥩"),
    BAKKERIJ("Bakkerij", "🍞"),
    DRANKEN("Dranken", "🧃"),
    DIEPVRIES("Diepvries", "❄️"),
    SNOEP_KOEK("Snoep & Koek", "🍪"),
    HUISHOUDEN("Huishouden", "🧹"),
    VERZORGING("Verzorging", "🧴"),
    OVERIG("Overig", "🛒");

    companion object {
        fun fromName(name: String) = entries.find { it.displayName == name } ?: OVERIG
    }
}
