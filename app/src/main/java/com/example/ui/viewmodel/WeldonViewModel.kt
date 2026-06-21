package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.WeldonDatabase
import com.example.data.model.*
import com.example.data.repository.WeldonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeldonViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: WeldonRepository
    
    // UI state for search queries
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()
    
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    init {
        val database = WeldonDatabase.getDatabase(application)
        repository = WeldonRepository(
            database.productDao(),
            database.categoryDao(),
            database.saleDao(),
            database.customerDao(),
            database.shiftDao(),
            database.productMovementDao()
        )
        
        // Seed database if empty
        viewModelScope.launch {
            repository.allCategories.first().let { currentCats ->
                if (currentCats.isEmpty()) {
                    seedDatabase()
                }
            }
            // Load active shift
            checkActiveShift()
        }
    }

    // --- State Streams ---
    val products: StateFlow<List<Product>> = _productSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allProducts
            else repository.searchProducts(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<SaleOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = _customerSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allCustomers
            else repository.searchCustomers(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shifts: StateFlow<List<Shift>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movements: StateFlow<List<ProductMovement>> = repository.allMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeShift = MutableStateFlow<Shift?>(null)
    val activeShift: StateFlow<Shift?> = _activeShift.asStateFlow()

    // --- POS Cart State ---
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _cashbackToUse = MutableStateFlow(0.0)
    val cashbackToUse: StateFlow<Double> = _cashbackToUse.asStateFlow()

    private val _paymentType = MutableStateFlow("Naqd") // "Naqd", "Karta", "Rassrochka", "Click/Payme"
    val paymentType: StateFlow<String> = _paymentType.asStateFlow()

    private val _paidAmountInput = MutableStateFlow("")
    val paidAmountInput: StateFlow<String> = _paidAmountInput.asStateFlow()

    // Status notifications for UI Toast/Alerts
    private val _paymentResult = MutableSharedFlow<String>()
    val paymentResult: SharedFlow<String> = _paymentResult.asSharedFlow()

    // --- Seed Utility ---
    private suspend fun seedDatabase() {
        val cats = listOf("Oziq-ovqat", "Kiyim-kechak", "Elektronika", "Kimyoviy moddalar")
        cats.forEach { repository.insertCategory(Category(name = it)) }

        val initialProducts = listOf(
            Product(code = "5449000000996", name = "Coca-Cola 1.5L", category = "Oziq-ovqat", price = 12500.0, purchasePrice = 8500.0, stock = 42.0, minStock = 10.0, brand = "Coca-Cola", attributes = "Shisha idish"),
            Product(code = "1111", name = "Tandir Non", category = "Oziq-ovqat", price = 4000.0, purchasePrice = 2200.0, stock = 60.0, minStock = 15.0, brand = "Milliy", attributes = "Issiq"),
            Product(code = "2222", name = "Nike T-Shirt Black", category = "Kiyim-kechak", price = 180000.0, purchasePrice = 110000.0, stock = 15.0, minStock = 5.0, brand = "Nike", attributes = "O'lcham: L, Rang: Qora"),
            Product(code = "3333", name = "Sut 1L 'Sado' 3.2%", category = "Oziq-ovqat", price = 11000.0, purchasePrice = 7800.0, stock = 24.0, minStock = 8.0, brand = "Sado", attributes = "Paket qadoq"),
            Product(code = "4444", name = "Lipton Choy Qutisi", category = "Oziq-ovqat", price = 14000.0, purchasePrice = 9000.0, stock = 3.0, minStock = 6.0, brand = "Lipton", attributes = "25 dona paketcha"), // Triggers Low Stock
            Product(code = "5555", name = "Samsung Galaxy A54", category = "Elektronika", price = 4200000.0, purchasePrice = 3600000.0, stock = 8.0, minStock = 2.0, brand = "Samsung", attributes = "8/256GB, Black")
        )
        initialProducts.forEach { repository.insertProduct(it) }

        val initialCustomers = listOf(
            Customer(name = "Valijon Aliyev", phone = "+998901234567", cashbackBalance = 24000.0, totalSpent = 850000.0, debt = 0.0),
            Customer(name = "Kamola Sobirova", phone = "+998935559988", cashbackBalance = 15000.0, totalSpent = 1200000.0, debt = 250000.0),
            Customer(name = "Sardor Umarov", phone = "+998977771122", cashbackBalance = 0.0, totalSpent = 0.0, debt = 0.0)
        )
        initialCustomers.forEach { repository.insertCustomer(it) }
    }

    // --- Search query operations ---
    fun setProductSearch(query: String) {
        _productSearchQuery.value = query
    }

    fun setCustomerSearch(query: String) {
        _customerSearchQuery.value = query
    }

    // --- POS Cart Operations ---
    data class CartItem(val product: Product, val quantity: Double)

    fun addToCart(product: Product, quantity: Double = 1.0) {
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val updatedItem = currentList[existingIndex].copy(quantity = currentList[existingIndex].quantity + quantity)
            currentList[existingIndex] = updatedItem
        } else {
            currentList.add(CartItem(product, quantity))
        }
        _cart.value = currentList
    }

    fun updateCartQuantity(product: Product, quantity: Double) {
        if (quantity <= 0) {
            removeFromCart(product)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = quantity)
            _cart.value = currentList
        }
    }

    fun removeFromCart(product: Product) {
        _cart.value = _cart.value.filter { it.product.id != product.id }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _selectedCustomer.value = null
        _cashbackToUse.value = 0.0
        _paidAmountInput.value = ""
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        _cashbackToUse.value = 0.0
    }

    fun setCashbackToUse(amount: Double) {
        val maxAvailable = _selectedCustomer.value?.cashbackBalance ?: 0.0
        _cashbackToUse.value = amount.coerceIn(0.0, maxAvailable)
    }

    fun setPaymentType(type: String) {
        _paymentType.value = type
        if (type == "Naqd") {
            _paidAmountInput.value = ""
        }
    }

    fun setPaidAmountInput(input: String) {
        _paidAmountInput.value = input
    }

    // --- Checkout ---
    fun checkoutCart(cashierName: String) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return
        
        viewModelScope.launch {
            val total = getCartTotal()
            val cashbackUsed = _cashbackToUse.value
            val finalTotal = total - cashbackUsed
            
            val paidAmount = when (_paymentType.value) {
                "Naqd" -> {
                    val customPaidVal = _paidAmountInput.value.toDoubleOrNull()
                    if (customPaidVal != null && customPaidVal >= finalTotal) customPaidVal else finalTotal
                }
                "Karta", "Click/Payme" -> finalTotal
                "Rassrochka" -> {
                    // Part payment or zero payment initially in credit/nasiya
                    _paidAmountInput.value.toDoubleOrNull() ?: 0.0
                }
                else -> finalTotal
            }
            
            // Calculate 1% Cashback earned
            val cashbackEarned = if (_selectedCustomer.value != null && _paymentType.value != "Rassrochka") {
                finalTotal * 0.01 // 1% callback loyalty rewards
            } else {
                0.0
            }

            val order = SaleOrder(
                totalAmount = finalTotal,
                paidAmount = paidAmount,
                paymentType = _paymentType.value,
                cashbackEarned = cashbackEarned,
                cashbackUsed = cashbackUsed,
                customerId = _selectedCustomer.value?.id,
                customerName = _selectedCustomer.value?.name,
                cashierName = cashierName
            )

            val saleItems = cartItems.map { cItem ->
                SaleItem(
                    orderId = 0, // Assigned by Repository
                    productId = cItem.product.id,
                    productName = cItem.product.name,
                    code = cItem.product.code,
                    quantity = cItem.quantity,
                    price = cItem.product.price,
                    purchasePrice = cItem.product.purchasePrice
                )
            }

            try {
                // Checkout transaction in repository
                val orderId = repository.checkout(order, saleItems)
                
                // If there's an active shift, increment expectedCash
                _activeShift.value?.let { shift ->
                    val cashFlow = if (_paymentType.value == "Naqd") paidAmount else 0.0
                    val updatedShift = shift.copy(expectedCash = shift.expectedCash + cashFlow)
                    repository.closeShift(updatedShift)
                    _activeShift.value = updatedShift
                }

                _paymentResult.emit("Sotuv omadli amalga oshirildi! Chek ID: #$orderId")
                clearCart()
            } catch (e: Exception) {
                _paymentResult.emit("Xatolik yuz berdi: ${e.localizedMessage}")
            }
        }
    }

    fun getCartTotal(): Double {
        return _cart.value.sumOf { it.product.price * it.quantity }
    }

    // --- Search with Shtrix-kod immediately to add ---
    fun scanBarcode(barcode: String): Boolean {
        var found = false
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                addToCart(product, 1.0)
                _paymentResult.emit("${product.name} savatga qo'shildi!")
                found = true
            } else {
                _paymentResult.emit("Ushbu shtrix-kodga ega mahsulot topilmadi: $barcode")
            }
        }
        return found
    }

    // --- Product Operations ---
    fun addProduct(name: String, code: String, category: String, price: Double, purchasePrice: Double, stock: Double, minStock: Double, brand: String, attributes: String) {
        viewModelScope.launch {
            val cleanCode = if (code.isBlank()) (100000..999999).random().toString() else code
            val newProduct = Product(
                name = name,
                code = cleanCode,
                category = category,
                price = price,
                purchasePrice = purchasePrice,
                stock = stock,
                minStock = minStock,
                brand = brand,
                attributes = attributes
            )
            repository.insertProduct(newProduct)
            _paymentResult.emit("Yangi mahsulot saqlandi: $name")
        }
    }

    fun editProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            _paymentResult.emit("Mahsulot tahrirlandi: ${product.name}")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _paymentResult.emit("Mahsulot o'chirildi: ${product.name}")
        }
    }

    // --- Category Operations ---
    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name))
            _paymentResult.emit("Yangi toifa qo'shildi: $name")
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _paymentResult.emit("Toifa o'chirildi: ${category.name}")
        }
    }

    // --- Customer CRM Operations ---
    fun addCustomer(name: String, phone: String, initialDebt: Double = 0.0) {
        viewModelScope.launch {
            val newCustomer = Customer(
                name = name,
                phone = phone,
                cashbackBalance = 0.0,
                totalSpent = 0.0,
                debt = initialDebt
            )
            repository.insertCustomer(newCustomer)
            _paymentResult.emit("CRM: Yangi mijoz qo'shildi: $name")
        }
    }

    fun editCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            _paymentResult.emit("Mijoz ma'lumotlari yangilandi!")
        }
    }

    fun payDebt(customer: Customer, amount: Double) {
        viewModelScope.launch {
            repository.payDebt(customer.id, amount)
            _paymentResult.emit("${customer.name} qarzi ${amount} so'mga kamaytirildi.")
        }
    }

    // --- Shift Operations (HRM) ---
    private fun checkActiveShift() {
        viewModelScope.launch {
            val shift = repository.getActiveShift()
            _activeShift.value = shift
        }
    }

    fun openSmena(startingCash: Double, cashierName: String) {
        viewModelScope.launch {
            val currentActive = repository.getActiveShift()
            if (currentActive != null) {
                _paymentResult.emit("Xatolik: Avval ochilgan smena yopilishi shart!")
                return@launch
            }
            val newShift = Shift(
                cashierName = cashierName,
                startingCash = startingCash,
                expectedCash = startingCash, // Initially expected is starting cash
                status = "OCHIQ"
            )
            val id = repository.openShift(newShift)
            _activeShift.value = newShift.copy(id = id.toInt())
            _paymentResult.emit("Yangi kassa smenasi ochildi. Kassir: $cashierName")
        }
    }

    fun closeSmena(closingCash: Double) {
        val current = _activeShift.value ?: return
        viewModelScope.launch {
            val finalShift = current.copy(
                closeTimestamp = System.currentTimeMillis(),
                closingCash = closingCash,
                status = "YOPILGAN"
            )
            repository.closeShift(finalShift)
            _activeShift.value = null
            _paymentResult.emit("Smena muvaffaqiyatli yopildi. KPI va hisobotlar yangilandi.")
        }
    }

    // --- Warehouse Movements ---
    fun moveWarehouseStock(productId: Int, productName: String, fromBranch: String, toBranch: String, qty: Double) {
        viewModelScope.launch {
            val movement = ProductMovement(
                productId = productId,
                productName = productName,
                fromBranch = fromBranch,
                toBranch = toBranch,
                quantity = qty
            )
            repository.recordMovement(movement)
            _paymentResult.emit("Ichki harakat: $qty dona $productName -> $toBranch")
        }
    }

    // --- Returns (Vozvrat) ---
    fun processReturn(order: SaleOrder) {
        viewModelScope.launch {
            val items = repository.getOrderItems(order.id)
            repository.returnOrder(order, items)
            _paymentResult.emit("Vozvrat: Chek #${order.id} qaytarildi va ombor qoldig'i tiklandi.")
        }
    }
}
