package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String, // Barcode / Shtrix-kod
    val name: String, // Mahsulot nomi
    val category: String, // Toifa
    val price: Double, // Sotish narxi
    val purchasePrice: Double, // Sotib olish narxi (P&L tahlil uchun)
    val stock: Double, // Ombordagi qoldiq
    val minStock: Double = 5.0, // Minimal qoldiq ogohlantirishi
    val brand: String = "", // Brend
    val attributes: String = "" // O'lcham, rang va hokazo
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "sale_orders")
data class SaleOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val paidAmount: Double,
    val paymentType: String, // "Naqd", "Karta", "Rassrochka", "Click/Payme"
    val cashbackEarned: Double = 0.0,
    val cashbackUsed: Double = 0.0,
    val customerId: Int? = null,
    val customerName: String? = null,
    val cashierName: String = "Kassir",
    val isReturned: Boolean = false // Vozvrat qilinganmi
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val productId: Int,
    val productName: String,
    val code: String,
    val quantity: Double,
    val price: Double,
    val purchasePrice: Double // Sotish vaqtidagi tannarxi
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val cashbackBalance: Double = 0.0,
    val totalSpent: Double = 0.0,
    val debt: Double = 0.0 // Nasiya miqdori
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cashierName: String,
    val openTimestamp: Long = System.currentTimeMillis(),
    val closeTimestamp: Long? = null,
    val startingCash: Double,
    val closingCash: Double? = null,
    val expectedCash: Double = 0.0,
    val status: String = "OCHIQ" // "OCHIQ", "YOPILGAN"
)

@Entity(tableName = "product_movements")
data class ProductMovement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val fromBranch: String,
    val toBranch: String,
    val quantity: Double,
    val timestamp: Long = System.currentTimeMillis()
)
