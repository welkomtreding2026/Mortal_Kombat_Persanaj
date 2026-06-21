package com.example.data.repository

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class WeldonRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao,
    private val shiftDao: ShiftDao,
    private val productMovementDao: ProductMovementDao
) {
    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    
    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts("%$query%")
    }
    
    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)
    }
    
    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }
    
    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }
    
    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }
    
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    // Sales & Orders
    val allOrders: Flow<List<SaleOrder>> = saleDao.getAllOrders()
    
    suspend fun getOrderItems(orderId: Int): List<SaleItem> {
        return saleDao.getItemsForOrder(orderId)
    }
    
    suspend fun checkout(
        order: SaleOrder,
        items: List<SaleItem>
    ): Long {
        // 1. Insert order and get ID
        val orderId = saleDao.insertOrder(order).toInt()
        
        // 2. Map items to order ID and insert
        val itemsWithId = items.map { it.copy(orderId = orderId) }
        saleDao.insertSaleItems(itemsWithId)
        
        // 3. Deduct stock and update database
        for (item in items) {
            productDao.adjustStock(item.productId, -item.quantity)
        }
        
        // 4. Update customer CRM info if applicable
        if (order.customerId != null) {
            val netCashbackDelta = order.cashbackEarned - order.cashbackUsed
            customerDao.updateCashback(order.customerId, netCashbackDelta)
            
            val remainingDebt = if (order.paymentType == "Rassrochka") {
                order.totalAmount - order.paidAmount
            } else {
                0.0
            }
            customerDao.updateDebtAndSpending(order.customerId, remainingDebt, order.totalAmount)
        }
        
        return orderId.toLong()
    }
    
    suspend fun returnOrder(order: SaleOrder, items: List<SaleItem>) {
        if (order.isReturned) return
        
        // Mark order as returned
        saleDao.markOrderAsReturned(order.id)
        
        // Restore stock
        for (item in items) {
            productDao.adjustStock(item.productId, item.quantity)
        }
        
        // Reverse customer updates
        if (order.customerId != null) {
            val reverseCashbackDelta = order.cashbackUsed - order.cashbackEarned
            customerDao.updateCashback(order.customerId, reverseCashbackDelta)
            
            val reverseDebt = if (order.paymentType == "Rassrochka") {
                -(order.totalAmount - order.paidAmount)
            } else {
                0.0
            }
            customerDao.updateDebtAndSpending(order.customerId, reverseDebt, -order.totalAmount)
        }
    }

    // Customers / CRM
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    
    fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.searchCustomers("%$query%")
    }
    
    suspend fun insertCustomer(customer: Customer) {
        customerDao.insertCustomer(customer)
    }
    
    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }
    
    suspend fun payDebt(customerId: Int, payment: Double) {
        customerDao.updateDebtAndSpending(customerId, -payment, 0.0)
    }

    // Shifts / HRM
    val allShifts: Flow<List<Shift>> = shiftDao.getAllShifts()
    
    suspend fun getActiveShift(): Shift? {
        return shiftDao.getActiveShift()
    }
    
    suspend fun openShift(shift: Shift): Long {
        return shiftDao.startShift(shift)
    }
    
    suspend fun closeShift(shift: Shift) {
        shiftDao.updateShift(shift)
    }

    // Product Movements
    val allMovements: Flow<List<ProductMovement>> = productMovementDao.getAllMovements()
    
    suspend fun recordMovement(movement: ProductMovement) {
        productMovementDao.recordMovement(movement)
        // Adjust stock in the local product
        productDao.adjustStock(movement.productId, -movement.quantity)
    }
}
