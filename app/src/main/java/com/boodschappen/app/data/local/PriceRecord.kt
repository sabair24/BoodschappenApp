package com.boodschappen.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_records")
data class PriceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val price: Double,
    val date: Long = System.currentTimeMillis()
)
