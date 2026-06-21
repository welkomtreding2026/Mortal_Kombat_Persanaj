package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        Product::class,
        Category::class,
        SaleOrder::class,
        SaleItem::class,
        Customer::class,
        Shift::class,
        ProductMovement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WeldonDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
    abstract fun shiftDao(): ShiftDao
    abstract fun productMovementDao(): ProductMovementDao

    companion object {
        @Volatile
        private var INSTANCE: WeldonDatabase? = null

        fun getDatabase(context: Context): WeldonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeldonDatabase::class.java,
                    "weldon_retail_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
