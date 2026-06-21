package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE code = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE :query OR code LIKE :query ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stock <= minStock")
    fun getLowStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stock = stock + :qty WHERE id = :productId")
    suspend fun adjustStock(productId: Int, qty: Double)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sale_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<SaleOrder>>

    @Query("SELECT * FROM sale_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: Int): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SaleOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("UPDATE sale_orders SET isReturned = 1 WHERE id = :orderId")
    suspend fun markOrderAsReturned(orderId: Int)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE name LIKE :query OR phone LIKE :query ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET cashbackBalance = cashbackBalance + :delta WHERE id = :customerId")
    suspend fun updateCashback(customerId: Int, delta: Double)

    @Query("UPDATE customers SET debt = debt + :delta, totalSpent = totalSpent + :spentDelta WHERE id = :customerId")
    suspend fun updateDebtAndSpending(customerId: Int, delta: Double, spentDelta: Double)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY openTimestamp DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE status = 'OCHIQ' LIMIT 1")
    suspend fun getActiveShift(): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun startShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)
}

@Dao
interface ProductMovementDao {
    @Query("SELECT * FROM product_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<ProductMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordMovement(movement: ProductMovement)
}
