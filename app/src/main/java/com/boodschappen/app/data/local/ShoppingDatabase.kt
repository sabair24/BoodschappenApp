package com.boodschappen.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ShoppingItem::class, ShoppingList::class, PriceRecord::class], version = 3, exportSchema = false)
abstract class ShoppingDatabase : RoomDatabase() {
    abstract fun shoppingDao(): ShoppingDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun priceRecordDao(): PriceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: ShoppingDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE shopping_items ADD COLUMN price REAL")
                database.execSQL("ALTER TABLE shopping_items ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE shopping_items ADD COLUMN listId INTEGER NOT NULL DEFAULT 1")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                database.execSQL(
                    "INSERT INTO shopping_lists (id, name, emoji, createdAt) VALUES (1, 'Mijn lijst', '🛒', ${System.currentTimeMillis()})"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS price_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemName TEXT NOT NULL,
                        price REAL NOT NULL,
                        date INTEGER NOT NULL
                    )"""
                )
            }
        }

        fun getDatabase(context: Context): ShoppingDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingDatabase::class.java,
                    "shopping_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { INSTANCE = it }
            }
        }
    }
}
