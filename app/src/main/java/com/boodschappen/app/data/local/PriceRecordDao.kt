package com.boodschappen.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PriceRecordDao {
    @Insert
    suspend fun insert(record: PriceRecord)

    @Query("SELECT * FROM price_records WHERE itemName = :name ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(name: String): PriceRecord?
}
