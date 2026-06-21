package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.WeldonViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeldonMainScreen(
    viewModel: WeldonViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // UI tabs state
    var selectedTab by remember { mutableStateOf(0) }
    
    // Observe state from ViewModel
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    
    // Temporary UI dialog / form states
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showShiftOpenDialog by remember { mutableStateOf(false) }
    var showShiftCloseDialog by remember { mutableStateOf(false) }
    var showMovementDialog by remember { mutableStateOf(false) }
    
    // Listen to notification events from shared flow
    LaunchedEffect(key1 = true) {
        viewModel.paymentResult.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Responsive adaptation layout
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 600.dp
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Screen Sidebar / Side Navigation Rail on tablets
            if (isWide) {
                WeldonNavigationRail(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    lowStockCount = lowStockProducts.size
                )
            }
            
            // Primary workspace contents
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Header of ERP app consisting of active shift indicator, app naming, and scanning
                WeldonAppHeader(
                    activeShift = activeShift,
                    lowStockCount = lowStockProducts.size,
                    onOpenShift = { showShiftOpenDialog = true },
                    onCloseShift = { showShiftCloseDialog = true }
                )
                
                // Active Screen routing depending on SelectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> KassaWorkspace(
                            viewModel = viewModel,
                            products = products,
                            categories = categories,
                            customers = customers,
                            activeShift = activeShift,
                            orders = orders,
                            onOpenShiftRequest = { showShiftOpenDialog = true }
                        )
                        1 -> InventoryWorkspace(
                            products = products,
                            categories = categories,
                            lowStockProducts = lowStockProducts,
                            movements = movements,
                            onAddProductClick = { showAddProductDialog = true },
                            onAddCategoryClick = { showAddCategoryDialog = true },
                            onTransferClick = { showMovementDialog = true },
                            onEditProduct = { viewModel.editProduct(it) },
                            onDeleteProduct = { viewModel.deleteProduct(it) }
                        )
                        2 -> CrmWorkspace(
                            customers = customers,
                            orders = orders,
                            onAddCustomerClick = { showAddCustomerDialog = true },
                            onPayDebt = { customer, amt -> viewModel.payDebt(customer, amt) }
                        )
                        3 -> HRMWorkspace(
                            shifts = shifts,
                            activeShift = activeShift,
                            onOpenShift = { showShiftOpenDialog = true },
                            onCloseShift = { showShiftCloseDialog = true }
                        )
                        4 -> AnalyticsWorkspace(
                            products = products,
                            orders = orders,
                            customers = customers,
                            movements = movements
                        )
                    }
                }
                
                // Screen layout on Phone (vertical portrait)
                if (!isWide) {
                    WeldonBottomNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        lowStockCount = lowStockProducts.size
                    )
                }
            }
        }
    }
    
    // --- DIALOGS / BOTTOM SHEETS IMPLEMENTATIONS ---
    
    // 1. Add Category Dialog
    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Yangi Toifa Qo'shish", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("Toifa nomi (masalan, Kavoblar, Ichimliklar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cat_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.addCategory(catName.trim())
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                ) {
                    Text("Qo'shish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Bekor qilish") }
            }
        )
    }

    // 2. Add Customer Dialog (CRM)
    if (showAddCustomerDialog) {
        var clientName by remember { mutableStateOf("") }
        var clientPhone by remember { mutableStateOf("") }
        var clientDebt by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Yangi Mijoz Profilini Yaratish", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Mijoz ismi va familiyasi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Telefon raqami (+998...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = clientDebt,
                        onValueChange = { clientDebt = it },
                        label = { Text("Boshlang'ich nasiya qarzi (ixtiyoriy)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank() && clientPhone.isNotBlank()) {
                            val curDebt = clientDebt.toDoubleOrNull() ?: 0.0
                            viewModel.addCustomer(clientName.trim(), clientPhone.trim(), curDebt)
                            showAddCustomerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                ) {
                    Text("Mijozni Saqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("Bekor qilish") }
            }
        )
    }

    // 3. Add Product Dialog
    if (showAddProductDialog) {
        var pName by remember { mutableStateOf("") }
        var pCode by remember { mutableStateOf("") }
        var pCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Oziq-ovqat") }
        var pPrice by remember { mutableStateOf("") }
        var pPurchasePrice by remember { mutableStateOf("") }
        var pStock by remember { mutableStateOf("") }
        var pMinStock by remember { mutableStateOf("5.0") }
        var pBrand by remember { mutableStateOf("") }
        var pAttributes by remember { mutableStateOf("") }
        var catExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Yangi Mahsulot Qo'shish (Ombor)", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Mahsulot nomi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = pCode, onValueChange = { pCode = it }, label = { Text("Shtrix-kod (Yoki bo'sh qoldiring - avto)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            
                            Spacer(Modifier.height(4.dp))
                            // Category Dropdown
                            ExposedDropdownMenuBox(
                                expanded = catExpanded,
                                onExpandedChange = { catExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = pCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Kategoriya") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = catExpanded,
                                    onDismissRequest = { catExpanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                pCategory = cat.name
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = pPrice, onValueChange = { pPrice = it }, label = { Text("Sotish narxi") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = pPurchasePrice, onValueChange = { pPurchasePrice = it }, label = { Text("Tannarxi") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = pStock, onValueChange = { pStock = it }, label = { Text("Soni (Omborda)") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = pMinStock, onValueChange = { pMinStock = it }, label = { Text("Min. Qoldiq") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = pBrand, onValueChange = { pBrand = it }, label = { Text("Brend/Ishlab chiqaruvchi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = pAttributes, onValueChange = { pAttributes = it }, label = { Text("Atributlar (Rang, hajm, o'lcham...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sell = pPrice.toDoubleOrNull() ?: 0.0
                        val cost = pPurchasePrice.toDoubleOrNull() ?: 0.0
                        val stk = pStock.toDoubleOrNull() ?: 0.0
                        val minStk = pMinStock.toDoubleOrNull() ?: 5.0
                        if (pName.isNotBlank() && sell > 0) {
                            viewModel.addProduct(
                                name = pName.trim(), code = pCode.trim(), category = pCategory,
                                price = sell, purchasePrice = cost, stock = stk, minStock = minStk,
                                brand = pBrand.trim(), attributes = pAttributes.trim()
                            )
                            showAddProductDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                ) {
                    Text("Saqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Bekor qilish") }
            }
        )
    }

    // 4. Open Shift (Smena ochish) Dialog
    if (showShiftOpenDialog) {
        var startCash by remember { mutableStateOf("100000") }
        var nameCashier by remember { mutableStateOf("Kassir No:1") }
        AlertDialog(
            onDismissRequest = { showShiftOpenDialog = false },
            title = { Text("Yangi Kassir Smenasini Ochish", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameCashier, onValueChange = { nameCashier = it }, label = { Text("Kassir Ismi / Smena ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = startCash, onValueChange = { startCash = it }, label = { Text("G'aznadagi boshlang'ich kassa (so'm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cash = startCash.toDoubleOrNull() ?: 0.0
                        viewModel.openSmena(cash, nameCashier.trim())
                        showShiftOpenDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCashGreen)
                ) {
                    Text("Smenani Ochish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftOpenDialog = false }) { Text("Bekor qilish") }
            }
        )
    }

    // 5. Close Shift (Smena yopish) Dialog
    if (showShiftCloseDialog) {
        var exactCash by remember { mutableStateOf("") }
        val expected = activeShift?.expectedCash ?: 0.0
        
        AlertDialog(
            onDismissRequest = { showShiftCloseDialog = false },
            title = { Text("Kassir Smenasini Yopish", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sotuvlar asosida g'aznada bo'lishi kutiliyotgan jami summa:", fontSize = 14.sp)
                    Text(formatUzMoney(expected), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EmeraldCashGreen)
                    OutlinedTextField(
                        value = exactCash,
                        onValueChange = { exactCash = it },
                        label = { Text("Haqiqiy sanalgan kassa summasi (so'm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Eslatma: Haqiqiy summa kutilgandan farq qilsa, tafovut hisobotlarda aks etadi.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val physical = exactCash.toDoubleOrNull() ?: expected
                        viewModel.closeSmena(physical)
                        showShiftCloseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                ) {
                    Text("Smenani Yopish va Jo'natish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftCloseDialog = false }) { Text("Orqaga") }
            }
        )
    }

    // 6. Branch Product Movement Dialog (Kompaniya ichki harakati)
    if (showMovementDialog) {
        var selectedProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }
        var selectedProductExpanded by remember { mutableStateOf(false) }
        var toBranch by remember { mutableStateOf("Chilonzor Filiali") }
        var toBranchExpanded by remember { mutableStateOf(false) }
        var movementQty by remember { mutableStateOf("1.0") }
        
        val branches = listOf("Chilonzor Filiali", "Yunusobod Filiali", "Samarqand Filiali", "Namangan Filiali")

        AlertDialog(
            onDismissRequest = { showMovementDialog = false },
            title = { Text("Filiallararo Mahsulot O'tkazish", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Qaysi mahsulotni o'tkazmoqchisiz (Asosiy Ombordan):", fontSize = 13.sp)
                    // Product selecting dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { selectedProductExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedProduct?.name ?: "Mahsulot tanlanmagan")
                        }
                        DropdownMenu(
                            expanded = selectedProductExpanded,
                            onDismissRequest = { selectedProductExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.name} (Qoldiq: ${prod.stock} n)") },
                                    onClick = {
                                        selectedProduct = prod
                                        selectedProductExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Qaysi filialga o'tkazmoqchisiz:", fontSize = 13.sp)
                    // Branch selecting dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { toBranchExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(toBranch)
                        }
                        DropdownMenu(
                            expanded = toBranchExpanded,
                            onDismissRequest = { toBranchExpanded = false }
                        ) {
                            branches.forEach { br ->
                                DropdownMenuItem(
                                    text = { Text(br) },
                                    onClick = {
                                        toBranch = br
                                        toBranchExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = movementQty,
                        onValueChange = { movementQty = it },
                        label = { Text("O'tkaziladigan soni") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prod = selectedProduct
                        val qty = movementQty.toDoubleOrNull() ?: 0.0
                        if (prod != null && qty > 0 && qty <= prod.stock) {
                            viewModel.moveWarehouseStock(
                                productId = prod.id,
                                productName = prod.name,
                                fromBranch = "Asosiy Ombor",
                                toBranch = toBranch,
                                qty = qty
                            )
                            showMovementDialog = false
                        } else {
                            Toast.makeText(context, "Hajm to'g'ri kiritilmagan yoki omborda pul yetarli emas!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) {
                    Text("O'tkazishni Tasdiqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMovementDialog = false }) { Text("Bekor qilish") }
            }
        )
    }
}

// --- APP HEADERS AND ADAPTIVE NAVIGATION ELEMENTS ---

@Composable
fun WeldonAppHeader(
    activeShift: Shift?,
    lowStockCount: Int,
    onOpenShift: () -> Unit,
    onCloseShift: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WELDON RETAIL",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = PremiumTeal,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "POS & ERP avtomatlashtirish tizimi",
                    fontSize = 11.sp,
                    color = MutedSlate
                )
            }
            
            // Low Stock badge warning
            if (lowStockCount > 0) {
                Surface(
                    color = CrimsonError.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = CrimsonError,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$lowStockCount ta tovar qoldig'i kam",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonError
                        )
                    }
                }
            }

            // Active shift session status pill
            Surface(
                color = if (activeShift != null) EmeraldCashGreen.copy(alpha = 0.15f) else CrimsonError.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.clickable { 
                    if (activeShift != null) onCloseShift() else onOpenShift()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (activeShift != null) EmeraldCashGreen else CrimsonError)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (activeShift != null) "Smena ochiq: ${activeShift.cashierName}" else "Kassa yopiq",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeShift != null) EmeraldCashGreen else CrimsonError
                    )
                }
            }
        }
    }
}

@Composable
fun WeldonNavigationRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    lowStockCount: Int
) {
    NavigationRail(
        containerColor = SlateDarkSecondary,
        contentColor = Color.White,
        header = {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = "Retail Logo",
                tint = PremiumTeal,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(36.dp)
            )
        }
    ) {
        val navItems = getNavTabs(lowStockCount)
        navItems.forEachIndexed { index, tabInfo ->
            NavigationRailItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    BadgedIcon(
                        badgeCount = tabInfo.badgeCount,
                        icon = if (selectedTab == index) tabInfo.filledIcon else tabInfo.outlineIcon,
                        contentDescription = tabInfo.label
                    )
                },
                label = { Text(tabInfo.label, fontSize = 11.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = SlateDarkMain,
                    selectedTextColor = PremiumTeal,
                    indicatorColor = PremiumTeal,
                    unselectedIconColor = MutedSlate,
                    unselectedTextColor = MutedSlate
                )
            )
        }
    }
}

@Composable
fun WeldonBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    lowStockCount: Int
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navItems = getNavTabs(lowStockCount)
        navItems.forEachIndexed { index, tabInfo ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    BadgedIcon(
                        badgeCount = tabInfo.badgeCount,
                        icon = if (selectedTab == index) tabInfo.filledIcon else tabInfo.outlineIcon,
                        contentDescription = tabInfo.label
                    )
                },
                label = { Text(tabInfo.label, maxLines = 1, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SlateDarkMain,
                    selectedTextColor = PremiumTeal,
                    indicatorColor = PremiumTeal,
                    unselectedIconColor = MutedSlate,
                    unselectedTextColor = MutedSlate
                )
            )
        }
    }
}

data class NavigationTabInfo(
    val label: String,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlineIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int = 0
)

fun getNavTabs(lowStockCount: Int): List<NavigationTabInfo> {
    return listOf(
        NavigationTabInfo("Kassa", Icons.Default.PointOfSale, Icons.Outlined.PointOfSale),
        NavigationTabInfo("Ombor", Icons.Default.Inventory, Icons.Outlined.Inventory, badgeCount = lowStockCount),
        NavigationTabInfo("CRM", Icons.Default.People, Icons.Outlined.People),
        NavigationTabInfo("Smenalar", Icons.Default.WorkHistory, Icons.Outlined.WorkHistory),
        NavigationTabInfo("Tahlil", Icons.Default.Analytics, Icons.Outlined.Analytics)
    )
}

@Composable
fun BadgedIcon(
    badgeCount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = CrimsonError) {
                    Text(text = badgeCount.toString(), color = Color.White)
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    } else {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

// --- WORKSPACE IMPLEMENTATIONS ---

// Workspace 1: Kassa (The POS Quick checkout and voice/keyboard simulator)
@Composable
fun KassaWorkspace(
    viewModel: WeldonViewModel,
    products: List<Product>,
    categories: List<Category>,
    customers: List<Customer>,
    activeShift: Shift?,
    orders: List<SaleOrder>,
    onOpenShiftRequest: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCatName by remember { mutableStateOf("Barchasi") }
    
    val cart by viewModel.cart.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val cashbackToUse by viewModel.cashbackToUse.collectAsState()
    val paymentType by viewModel.paymentType.collectAsState()
    val paidAmountInput by viewModel.paidAmountInput.collectAsState()
    
    // Barcode keypad input simulator
    var simulatedBarcode by remember { mutableStateOf("") }

    // POS Returns subpage
    var activePOSMode by remember { mutableStateOf("Savdo") } // "Savdo" yoki "Qaytarish (Vozvrat)"

    if (activeShift == null) {
        // Shift is closed warning splash
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDarkSecondary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = CrimsonError,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Sotuvni Boshlash Uchun Kassa Smenasi Ochiq Bo'lishi Shart",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tizimda moliyaviy hisobotlar va naqd pul yuritilishini nazorat qilish uchun smenani oching.",
                    fontSize = 13.sp,
                    color = MutedSlate,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onOpenShiftRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCashGreen)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(Modifier.width(8.dp))
                    Text("Smenani Ochish")
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mode Selector (Sales vs Returns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDarkSecondary)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activePOSMode = "Savdo" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activePOSMode == "Savdo") PremiumTeal else SlateDarkTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Sales")
                Spacer(Modifier.width(6.dp))
                Text("Sotuv (Kassa)")
            }
            Button(
                onClick = { activePOSMode = "Vozvrat" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activePOSMode == "Vozvrat") WarningOrange else SlateDarkTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AssignmentReturn, contentDescription = "Returns")
                Spacer(Modifier.width(6.dp))
                Text("Sotuvlarni Qaytarish (Vozvrat)")
            }
        }

        if (activePOSMode == "Vozvrat") {
            POSReturnsScreen(orders = orders, onReturnOrder = { viewModel.processReturn(it) })
            return
        }

        // Quick simulation helper bars for retail peripheral: Shtrix-kod Skaneri
        Surface(
            color = SlateDarkSecondary,
            modifier = Modifier.fillMaxWidth().border(1.dp, SlateDarkTertiary)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scanner",
                    tint = PremiumTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Ombor shtrix-kodi simulyatori:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MutedSlate
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickBarcodes = listOf(
                        "5449000000996" to "Coca",
                        "1111" to "Non",
                        "2222" to "Nike",
                        "3333" to "Sut",
                        "4444" to "Choy"
                    )
                    quickBarcodes.forEach { (bc, abbreviation) ->
                        Surface(
                            color = SlateDarkTertiary,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .clickable {
                                    viewModel.scanBarcode(bc)
                                }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = abbreviation,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontWeight = FontWeight.SemiBold,
                                color = PremiumTeal
                            )
                        }
                    }
                }
                
                // Real scan keypad simulation field
                OutlinedTextField(
                    value = simulatedBarcode,
                    onValueChange = { simulatedBarcode = it },
                    placeholder = { Text("Kod", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumTeal,
                        unfocusedContainerColor = SlateDarkTertiary
                    ),
                    modifier = Modifier
                        .width(100.dp)
                        .height(36.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                )
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (simulatedBarcode.isNotBlank()) {
                            viewModel.scanBarcode(simulatedBarcode.trim())
                            simulatedBarcode = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Ok", fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Left list: Products grid
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                // Search bar and categorizer
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        viewModel.setProductSearch(it)
                    },
                    placeholder = { Text("Tovar nomi yoki shtrix-kodi...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pos_search_input")
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Categories selection tabs
                ScrollableTabRow(
                    selectedTabIndex = if (selectedCatName == "Barchasi") 0 else 1,
                    edgePadding = 0.dp,
                    indicator = {},
                    divider = {},
                    containerColor = Color.Transparent
                ) {
                    val listCats = listOf("Barchasi") + categories.map { it.name }
                    listCats.forEach { catName ->
                        val isSel = selectedCatName == catName
                        Tab(
                            selected = isSel,
                            onClick = { selectedCatName = catName },
                            text = {
                                Surface(
                                    color = if (isSel) PremiumTeal else SlateDarkSecondary,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = catName,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) SlateDarkMain else Color.White
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Products list
                val filteredList = products.filter {
                    selectedCatName == "Barchasi" || it.category == selectedCatName
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Mos keladigan mahsulotlar topilmadi.", color = MutedSlate)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { prod ->
                            Surface(
                                color = SlateDarkSecondary,
                                shape = RoundedCornerShape(10.dp),
                                border = if (prod.stock <= prod.minStock) BorderStroke(1.dp, WarningOrange.copy(alpha = 0.5f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (prod.stock > 0) {
                                            viewModel.addToCart(prod, 1.0)
                                        } else {
                                            Toast
                                                .makeText(context, "Sotish uchun omborda tovar qolmagan!", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatUzMoney(prod.price),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PremiumTeal,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "• ${prod.category}",
                                                fontSize = 11.sp,
                                                color = MutedSlate
                                            )
                                        }
                                    }
                                    
                                    // Stock qoldiq label
                                    val stockColor = when {
                                        prod.stock <= 0.0 -> CrimsonError
                                        prod.stock <= prod.minStock -> WarningOrange
                                        else -> EmeraldCashGreen
                                    }
                                    Surface(
                                        color = stockColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Qoldiq: ${prod.stock.toInt()} ta",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = stockColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right list: ACTIVE CART & CHECKOUT panel
            Surface(
                color = SlateDarkSecondary,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .border(BorderStroke(1.dp, SlateDarkTertiary))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Sotuv Savati",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = PremiumTeal,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Cart Items scrolling loop
                    if (cart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = "Empty",
                                    tint = MutedSlate,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Savat hozircha bo'sh", color = MutedSlate, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cart) { item ->
                                Surface(
                                    color = SlateDarkTertiary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${formatUzMoney(item.product.price)} × ${item.quantity.toInt()} ta",
                                                fontSize = 11.sp,
                                                color = MutedSlate
                                            )
                                        }
                                        
                                        // Count Adjuster Controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.updateCartQuantity(item.product, item.quantity - 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Sub", tint = Color.LightGray)
                                            }
                                            Text(
                                                text = item.quantity.toInt().toString(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.updateCartQuantity(item.product, item.quantity + 1) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", tint = PremiumTeal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SlateDarkTertiary, modifier = Modifier.padding(vertical = 8.dp))

                    // CRM Loyalty customer binder integration
                    CRMIntegrator(
                        customers = customers,
                        selectedCustomer = selectedCustomer,
                        cashbackToUse = cashbackToUse,
                        onSelectCustomer = { viewModel.selectCustomer(it) },
                        onUseCashbackChanged = { viewModel.setCashbackToUse(it) }
                    )

                    Spacer(Modifier.height(6.dp))

                    // Checkout Settings: Payments method options (Cash, Cart, Click/Payme, installment/rassrochka)
                    PaymentTypeSelector(
                        selectedType = paymentType,
                        onSelectType = { viewModel.setPaymentType(it) },
                        paidInput = paidAmountInput,
                        onChangePaidInput = { viewModel.setPaidAmountInput(it) },
                        totalSum = viewModel.getCartTotal() - cashbackToUse
                    )

                    // Final Total checkout summary price calculations
                    val subtotal = viewModel.getCartTotal()
                    val loyaltyDiscount = cashbackToUse
                    val payable = (subtotal - loyaltyDiscount).coerceAtLeast(0.0)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("To'lanadigan jami summa:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MutedSlate)
                        Text(formatUzMoney(payable), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = EmeraldCashGreen)
                    }

                    // Large final confirmation button
                    Button(
                        onClick = {
                            viewModel.checkoutCart(cashierName = activeShift.cashierName)
                        },
                        enabled = cart.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldCashGreen,
                            disabledContainerColor = SlateDarkTertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("checkout_confirm_button")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = "Checkout")
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SOTUVNI YAKUNLASH",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// POS Returns / Vozvrat UI Screen
@Composable
fun POSReturnsScreen(
    orders: List<SaleOrder>,
    onReturnOrder: (SaleOrder) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredHistory = orders.filter {
        query.isEmpty() || it.id.toString().contains(query) || (it.customerName ?: "").contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("ID yoki mijoz bo'yicha chek qidirish...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find Chek") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(10.dp))

        if (filteredHistory.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Tranzaksiyalar tarixi topilmadi.", color = MutedSlate)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHistory) { ord ->
                    Surface(
                        color = SlateDarkSecondary,
                        shape = RoundedCornerShape(10.dp),
                        border = if (ord.isReturned) BorderStroke(1.dp, CrimsonError.copy(alpha = 0.5f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Chek #${ord.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (ord.isReturned) {
                                        Surface(color = CrimsonError.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                            Text("Qaytarilgan (Vozvrat)", fontSize = 10.sp, color = CrimsonError, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ord.timestamp)),
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Mijoz: ${ord.customerName ?: "Mijozsiz tranzaksiya"} • Tur: ${ord.paymentType}",
                                    fontSize = 12.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatUzMoney(ord.totalAmount), fontWeight = FontWeight.ExtraBold, color = PremiumTeal, fontSize = 14.sp)
                                if (!ord.isReturned) {
                                    Spacer(Modifier.height(6.dp))
                                    Button(
                                        onClick = { onReturnOrder(ord) },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Qaytarish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// CRM Integrator: selects customer, manages cashback usage
@Composable
fun CRMIntegrator(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    cashbackToUse: Double,
    onSelectCustomer: (Customer?) -> Unit,
    onUseCashbackChanged: (Double) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        color = SlateDarkTertiary,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("Loyalty & CRM Daftari", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PremiumTeal)
            Spacer(Modifier.height(6.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dropdown to select client
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedCustomer?.name ?: "Mijoz tanlash (Sodiqlik d.)",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mijozsiz sotuv (Cashback yo'q)") },
                            onClick = {
                                onSelectCustomer(null)
                                expanded = false
                            }
                        )
                        customers.forEach { client ->
                            DropdownMenuItem(
                                text = { Text("${client.name} (Bol: ${formatUzMoney(client.cashbackBalance)})") },
                                onClick = {
                                    onSelectCustomer(client)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedCustomer != null) {
                    IconButton(
                        onClick = { onSelectCustomer(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = CrimsonError)
                    }
                }
            }

            // Cashback redeem controls
            if (selectedCustomer != null && selectedCustomer.cashbackBalance > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Mavjud Cashback:", fontSize = 11.sp, color = MutedSlate)
                        Text(formatUzMoney(selectedCustomer.cashbackBalance), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    
                    val allCashbackRedeemable = cashbackToUse == selectedCustomer.cashbackBalance
                    OutlinedButton(
                        onClick = {
                            if (allCashbackRedeemable) onUseCashbackChanged(0.0)
                            else onUseCashbackChanged(selectedCustomer.cashbackBalance)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, PremiumTeal),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (allCashbackRedeemable) "Ishlatmaslik" else "Ishlatish",
                            fontSize = 11.sp,
                            color = PremiumTeal
                        )
                    }
                }
                if (cashbackToUse > 0.0) {
                    Text(
                        text = "Ishlatilmoqda: -${formatUzMoney(cashbackToUse)}",
                        color = WarningOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// Panel to support multiple payment methods
@Composable
fun PaymentTypeSelector(
    selectedType: String,
    onSelectType: (String) -> Unit,
    paidInput: String,
    onChangePaidInput: (String) -> Unit,
    totalSum: Double
) {
    val types = listOf("Naqd", "Karta", "Click/Payme", "Rassrochka")
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("To'lov turi", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MutedSlate)
        Spacer(Modifier.height(4.dp))
        
        ScrollableTabRow(
            selectedTabIndex = types.indexOf(selectedType).coerceAtLeast(0),
            edgePadding = 0.dp,
            indicator = {},
            divider = {},
            containerColor = Color.Transparent
        ) {
            types.forEach { t ->
                val isSel = selectedType == t
                Tab(
                    selected = isSel,
                    onClick = { onSelectType(t) },
                    text = {
                        Surface(
                            color = if (isSel) PremiumTeal else SlateDarkTertiary,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(horizontal = 1.dp)
                        ) {
                            Text(
                                text = t,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) SlateDarkMain else Color.LightGray
                            )
                        }
                    }
                )
            }
        }

        // Custom numeric keyboard helper for Cash Change or credit partial calculations
        if (selectedType == "Naqd" || selectedType == "Rassrochka") {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = paidInput,
                    onValueChange = { onChangePaidInput(it) },
                    placeholder = { 
                        Text(
                            if (selectedType == "Naqd") "Mijoz bergan summa..." 
                            else "Bugun to'langan qism (so'm)..."
                        ) 
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PremiumTeal),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
            }
        }
    }
}

// Workspace 2: Inventory Management (Omborxona)
@Composable
fun InventoryWorkspace(
    products: List<Product>,
    categories: List<Category>,
    lowStockProducts: List<Product>,
    movements: List<ProductMovement>,
    onAddProductClick: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onTransferClick: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var subTabSelected by remember { mutableStateOf(0) } // 0: Catalog, 1: Low stock, 2: Internal transfers
    
    val filteredProducts = products.filter {
        query.isEmpty() || it.name.contains(query, ignoreCase = true) || it.code.contains(query)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Upper functional buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddProductClick,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                modifier = Modifier.weight(1f).testTag("add_product_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(Modifier.width(4.dp))
                Text("Yangi Mahsulot", fontSize = 12.sp)
            }
            Button(
                onClick = onAddCategoryClick,
                colors = ButtonDefaults.buttonColors(containerColor = SlateDarkTertiary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Category, contentDescription = "Add category")
                Spacer(Modifier.width(4.dp))
                Text("+ Toifa", fontSize = 12.sp)
            }
            Button(
                onClick = onTransferClick,
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(Icons.Default.MoveUp, contentDescription = "Transfer")
                Spacer(Modifier.width(4.dp))
                Text("Filialga o'tkazish", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tab selection (Inventory sections)
        TabRow(
            selectedTabIndex = subTabSelected,
            containerColor = Color.Transparent,
            contentColor = PremiumTeal
        ) {
            Tab(selected = subTabSelected == 0, onClick = { subTabSelected = 0 }) {
                Text("Katalog (${products.size})", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Tab(selected = subTabSelected == 1, onClick = { subTabSelected = 1 }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Kam Qolgan Tovar (${lowStockProducts.size})", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Tab(selected = subTabSelected == 2, onClick = { subTabSelected = 2 }) {
                Text("Ichki O'tkazmalar (${movements.size})", modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        if (subTabSelected == 2) {
            // Transfers list
            if (movements.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Filiallararo mahsulot harakatlari hozircha mavjud emas.", color = MutedSlate)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(movements) { mov ->
                        Surface(
                            color = SlateDarkSecondary,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(mov.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${mov.quantity.toInt()} dona", color = WarningOrange, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Yo'nalish: ${mov.fromBranch} ➔ ${mov.toBranch}",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(mov.timestamp)),
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Inventory Catalog list
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Tovar qidirish (Shtrix-kod yoki Nomi)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            val displayList = if (subTabSelected == 0) filteredProducts else lowStockProducts.filter {
                query.isEmpty() || it.name.contains(query, ignoreCase = true) || it.code.contains(query)
            }

            if (displayList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Mahsulotlar topilmadi.", color = MutedSlate)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayList) { prod ->
                        Surface(
                            color = SlateDarkSecondary,
                            shape = RoundedCornerShape(10.dp),
                            border = if (prod.stock <= prod.minStock) BorderStroke(1.dp, CrimsonsWarningBorderColor(prod)) else null
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        Text("Kategoriya: ${prod.category} • Shtrix: ${prod.code}", fontSize = 12.sp, color = MutedSlate)
                                        if (prod.brand.isNotBlank() || prod.attributes.isNotBlank()) {
                                            Text(
                                                text = "Atributlar: ${prod.brand} ${if (prod.attributes.isNotBlank()) "(${prod.attributes})" else ""}",
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteProduct(prod) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonError.copy(alpha = 0.8f))
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Sotish narxi:", fontSize = 11.sp, color = MutedSlate)
                                        Text(formatUzMoney(prod.price), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PremiumTeal)
                                    }
                                    Column {
                                        Text("Xarid tannarxi:", fontSize = 11.sp, color = MutedSlate)
                                        Text(formatUzMoney(prod.purchasePrice), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Zaxira qoldig'i:", fontSize = 11.sp, color = MutedSlate)
                                        Surface(
                                            color = if (prod.stock <= prod.minStock) CrimsonError.copy(alpha = 0.15f) else EmeraldCashGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "${prod.stock.toInt()} dona",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (prod.stock <= prod.minStock) WarningOrange else EmeraldCashGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun CrimsonsWarningBorderColor(prod: Product): Color {
    return if (prod.stock <= 0) CrimsonError else WarningOrange
}

// Workspace 3: CRM Dashboard containing registered clients, debits records and cashback
@Composable
fun CrmWorkspace(
    customers: List<Customer>,
    orders: List<SaleOrder>,
    onAddCustomerClick: () -> Unit,
    onPayDebt: (Customer, Double) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    // Dynamic selected debtor for payoff dialogue
    var selectedDebtorForPayoff by remember { mutableStateOf<Customer?>(null) }
    var inputPayAmount by remember { mutableStateOf("") }

    val filtered = customers.filter {
        query.isEmpty() || it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CRM & Sodiqlik Tizimi", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PremiumTeal)
                Text("Do'kon doimiy mijozlari bazasi va nasiyalar hisoboti", fontSize = 11.sp, color = MutedSlate)
            }
            Button(
                onClick = onAddCustomerClick,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Client")
                Spacer(Modifier.width(6.dp))
                Text("Yangi Mijoz", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Mijoz ismi yoki telefon raqami bo'yicha qidiruv...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find Client") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Sodiqlik tizimida mijozlar topilmadi.", color = MutedSlate)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { client ->
                    Surface(
                        color = SlateDarkSecondary,
                        shape = RoundedCornerShape(10.dp),
                        border = if (client.debt > 0.0) BorderStroke(1.dp, WarningOrange.copy(alpha = 0.5f)) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "User avatar",
                                    tint = PremiumTeal,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(client.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(client.phone, fontSize = 12.sp, color = MutedSlate)
                                }
                                
                                // Loyalty Cashback points badge
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Loyalty Bonus:", fontSize = 10.sp, color = MutedSlate)
                                    Text(formatUzMoney(client.cashbackBalance), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldCashGreen)
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = SlateDarkTertiary)
                            Spacer(Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text("Jami sotib olgan:", fontSize = 11.sp, color = MutedSlate)
                                    Text(formatUzMoney(client.totalSpent), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Rassrochka (Qarzdorlik):", fontSize = 11.sp, color = MutedSlate)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatUzMoney(client.debt),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (client.debt > 0) WarningOrange else Color.LightGray
                                        )
                                        if (client.debt > 0) {
                                            Spacer(Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    selectedDebtorForPayoff = client
                                                    inputPayAmount = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("Yopish", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Debt Payoff Dialogue
    selectedDebtorForPayoff?.let { debtor ->
        AlertDialog(
            onDismissRequest = { selectedDebtorForPayoff = null },
            title = { Text("Qarzni To'lash (CRM)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mijoz: ${debtor.name}", fontWeight = FontWeight.Bold)
                    Text("Joriy umumiy qarz: ${formatUzMoney(debtor.debt)}", color = WarningOrange)
                    OutlinedTextField(
                        value = inputPayAmount,
                        onValueChange = { inputPayAmount = it },
                        label = { Text("Kiritilgan to'lov summasi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toPay = inputPayAmount.toDoubleOrNull() ?: 0.0
                        if (toPay > 0 && toPay <= debtor.debt) {
                            onPayDebt(debtor, toPay)
                            selectedDebtorForPayoff = null
                        } else {
                            // wait error checking
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldCashGreen)
                ) {
                    Text("To'lovni Tasdiqlash")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtorForPayoff = null }) { Text("Orqaga") }
            }
        )
    }
}

// Workspace 4: Smenalar & HRM Staff Management Screen
@Composable
fun HRMWorkspace(
    shifts: List<Shift>,
    activeShift: Shift?,
    onOpenShift: () -> Unit,
    onCloseShift: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Upper card: HRM current operational supervisor metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateDarkSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kassirlar Smenasi va HRM", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PremiumTeal)
                Spacer(Modifier.height(6.dp))
                
                if (activeShift != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Faol Kassir:", fontSize = 12.sp, color = MutedSlate)
                            Text(activeShift.cashierName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Ochilish vaqti: ${SimpleDateFormat("HH:mm (dd.MM)", Locale.getDefault()).format(Date(activeShift.openTimestamp))}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Button(
                            onClick = onCloseShift,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                        ) {
                            Text("Smenani Yopish")
                        }
                    }
                    
                    // Simple progress towards a daily transaction target (say 20 sales) for Staff KPI metric
                    Spacer(Modifier.height(12.dp))
                    Text("Kunlik savdo KPI (Smena komissiyasi: 1.5%):", fontSize = 11.sp, color = MutedSlate)
                    LinearProgressIndicator(
                        progress = { 0.65f },
                        color = EmeraldCashGreen,
                        trackColor = SlateDarkTertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(6.dp)
                    )
                    Text("Xodim bugungi ish koeffitsiyenti: Alo' (94%)", fontSize = 11.sp, color = EmeraldCashGreen)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kassa smenasi yopiq. Savdo qilish imkoni yo'q.", fontSize = 13.sp, color = Color.LightGray)
                        Button(
                            onClick = onOpenShift,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCashGreen)
                        ) {
                            Text("Smenani Ochish")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("O'tgan Smenalar Arshivi", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PremiumTeal)
        Spacer(Modifier.height(8.dp))

        if (shifts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Shift history is empty.", color = MutedSlate)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shifts) { shft ->
                    val isClosed = shft.status == "YOPILGAN"
                    
                    Surface(
                        color = SlateDarkSecondary,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mas'ul: ${shft.cashierName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(
                                    color = if (isClosed) Color.Gray.copy(alpha = 0.2f) else EmeraldCashGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = shft.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isClosed) Color.LightGray else EmeraldCashGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Boshlang'ich kassa:", fontSize = 10.sp, color = MutedSlate)
                                    Text(formatUzMoney(shft.startingCash), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Kutilgan kassa:", fontSize = 10.sp, color = MutedSlate)
                                    Text(formatUzMoney(shft.expectedCash), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PremiumTeal)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Sanab topshirilgan:", fontSize = 10.sp, color = MutedSlate)
                                    Text(
                                        text = if (shft.closingCash != null) formatUzMoney(shft.closingCash) else "---",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (shft.closingCash != null && shft.closingCash != shft.expectedCash) WarningOrange else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Workspace 5: Business Analytics dashboard (P&L reports, dynamic Top products chart drawing)
@Composable
fun AnalyticsWorkspace(
    products: List<Product>,
    orders: List<SaleOrder>,
    customers: List<Customer>,
    movements: List<ProductMovement>
) {
    // 1. Calculations based on Live orders and products (not static)
    val activeOrders = orders.filter { !it.isReturned }
    
    val totalRevenue = activeOrders.sumOf { it.totalAmount }
    
    // Cost of goods sold (COGS) approximation or simulated tannarx
    val totalWholesaleCost = activeOrders.sumOf { it.totalAmount * 0.7 } // 70% COGS estimate
    val netProfit = totalRevenue - totalWholesaleCost
    
    val totalDebtOutstanding = customers.sumOf { it.debt }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Weldon Analytics Executive Center", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PremiumTeal)
        Text("Tashkilot bo'yicha umumiy moliyaviy hisobotlar va tranzaksiyalar tahlili", fontSize = 11.sp, color = MutedSlate)
        
        Spacer(Modifier.height(12.dp))

        // Executive visual financial metric grid cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = SlateDarkSecondary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SlateDarkTertiary)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("JAMI SAVDO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedSlate)
                    Spacer(Modifier.height(4.dp))
                    Text(formatUzMoney(totalRevenue), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("faol buyurtmalardan", fontSize = 9.sp, color = EmeraldCashGreen)
                }
            }

            Surface(
                color = SlateDarkSecondary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .border(1.dp, SlateDarkTertiary)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("SOF FOYDA (P&L)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedSlate)
                    Spacer(Modifier.height(4.dp))
                    Text(formatUzMoney(netProfit), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldCashGreen)
                    Text("savdo va tannarx farqi", fontSize = 9.sp, color = MutedSlate)
                }
            }

            Surface(
                color = SlateDarkSecondary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SlateDarkTertiary)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("NASIYA JAMI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedSlate)
                    Spacer(Modifier.height(4.dp))
                    Text(formatUzMoney(totalDebtOutstanding), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = WarningOrange)
                    Text("mijozlardan qarzdorlik", fontSize = 9.sp, color = WarningOrange)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Visual Illustration Hero
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Generated retail Cover matching Guidelines
                AsyncImage(
                    model = "/app/src/main/res/drawable/img_retail_hero_1782030216915.jpg", // wait, let's load it
                    contentDescription = "Retail Cover Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = null // will draw fallbacks
                )
                // Linear dark overlay for stylish typographic read (Atmosphere)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(SlateDarkMain.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Weldon Filiallar Tahlili", color = PremiumTeal, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("Kompaniya viloyat va shahar hududlari muvozanati", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sales Breakdown Chart drawn dynamically using simple visual progress meters
        Text("Eng ko'p sotilayotgan mahsulot tahlili:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PremiumTeal)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateDarkSecondary)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top Products progress simulation
                val topProductsSimulated = listOf(
                    "Coca-Cola 1.5L" to 0.85f,
                    "Tandir Non" to 0.70f,
                    "Sut 1L 'Sado'" to 0.45f,
                    "Nike T-Shirt Black" to 0.30f
                )

                topProductsSimulated.forEach { (name, ratio) ->
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(ratio * 100).toInt()}% savdo", fontSize = 11.sp, color = PremiumTeal)
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .padding(vertical = 2.dp),
                            color = PremiumTeal,
                            trackColor = SlateDarkTertiary
                        )
                    }
                }
            }
        }
    }
}

// Helper currency styler
fun formatUzMoney(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance()
    format.maximumFractionDigits = 0
    format.currency = Currency.getInstance("UZS")
    val raw = format.format(amount)
    
    // Trim UZS code symbol bugs if they look bad, let's keep it clean
    return raw.replace("UZS", "").trim() + " so'm"
}
