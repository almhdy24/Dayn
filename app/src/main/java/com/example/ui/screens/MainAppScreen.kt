package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Customer
import com.example.data.CustomerTransaction
import com.example.ui.DebtViewModel
import com.example.ui.Screen
import com.example.ui.theme.DangerColor
import com.example.ui.theme.SuccessColor
import java.text.SimpleDateFormat
import java.util.*

// Helper function to format timestamp
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar"))
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: DebtViewModel) {
    val context = LocalContext.current
    val currentScreen by remember { derivedStateOf { viewModel.navigationStack.lastOrNull() ?: Screen.Home } }

    // Observe DB lists
    val customersList by viewModel.customers.collectAsState()
    val transactionsList by viewModel.transactions.collectAsState()
    val transactionsToday by viewModel.transactionsToday.collectAsState()

    // Status snackbars / banner states
    val message by viewModel.uiMessage
    val errorMessage by viewModel.uiError

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.uiMessage.value = null
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.uiError.value = null
        }
    }

    // Wrap entire app in RTL Layout direction for consistent premium Arabic experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Display Bottom navigation bar only on basic main views
                if (currentScreen in listOf(Screen.Home, Screen.Customers, Screen.Reports, Screen.Settings)) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen is Screen.Home,
                            onClick = { viewModel.navigateToMainTab(Screen.Home) },
                            icon = { Icon(if (currentScreen is Screen.Home) Icons.Default.Home else Icons.Outlined.Home, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Customers,
                            onClick = { viewModel.navigateToMainTab(Screen.Customers) },
                            icon = { Icon(if (currentScreen is Screen.Customers) Icons.Default.People else Icons.Outlined.People, contentDescription = "العملاء") },
                            label = { Text("العملاء", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Reports,
                            onClick = { viewModel.navigateToMainTab(Screen.Reports) },
                            icon = { Icon(if (currentScreen is Screen.Reports) Icons.Default.Analytics else Icons.Outlined.Analytics, contentDescription = "التقارير") },
                            label = { Text("التقارير", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Settings,
                            onClick = { viewModel.navigateToMainTab(Screen.Settings) },
                            icon = { Icon(if (currentScreen is Screen.Settings) Icons.Default.Settings else Icons.Outlined.Settings, contentDescription = "الإعدادات") },
                            label = { Text("الإعدادات", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        is Screen.Home -> HomeScreen(
                            viewModel = viewModel,
                            customers = customersList,
                            transactionsToday = transactionsToday
                        )
                        is Screen.Customers -> CustomersScreen(
                            viewModel = viewModel,
                            customers = customersList
                        )
                        is Screen.Reports -> ReportsScreen(
                            viewModel = viewModel,
                            customers = customersList,
                            transactions = transactionsList
                        )
                        is Screen.Settings -> SettingsScreen(
                            viewModel = viewModel
                        )
                        is Screen.CustomerDetails -> CustomerDetailsScreen(
                            viewModel = viewModel,
                            customerId = screen.customerId
                        )
                        is Screen.AddCustomer -> AddCustomerScreen(
                            viewModel = viewModel
                        )
                        is Screen.AddDebt -> AddDebtScreen(
                            viewModel = viewModel,
                            preselectedCustomerId = screen.preselectedCustomerId,
                            customers = customersList
                        )
                        is Screen.RecordPayment -> RecordPaymentScreen(
                            viewModel = viewModel,
                            preselectedCustomerId = screen.preselectedCustomerId,
                            customers = customersList
                        )
                        is Screen.Statement -> StatementScreen(
                            viewModel = viewModel,
                            customerId = screen.customerId
                        )
                    }
                }
            }
        }
    }
}

// ------------------- HOME SCREEN -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DebtViewModel,
    customers: List<Customer>,
    transactionsToday: List<CustomerTransaction>
) {
    val totalCustomers = customers.size
    val totalOutstandingDebt = customers.filter { it.balance > 0 }.sumOf { it.balance }
    
    // Payments today count
    val totalPaymentsToday = transactionsToday.filter { it.type == "payment" }.sumOf { it.amount }

    // Retrieve all transactions for the recent activity feed
    val transactionsList by viewModel.transactions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sleek Arabic Styled Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "دفتر الديون",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "مرحباً، متجر الخير",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B) // Slate-500
                )
            }
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { viewModel.navigateToMainTab(Screen.Settings) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Outstandings Summary Card (Material 3 Style, shape = 32.dp)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("total_debt_card"),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Decorative background canvas for premium subtle circular glow blur
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.04f),
                                radius = 160.dp.toPx(),
                                center = Offset(size.width * 0.9f, size.height * 1.1f)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "إجمالي المديونية المستحقة",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = String.format(Locale("ar"), "%,.0f", totalOutstandingDebt),
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "ج.س",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Sub-card 1: تحصيل اليوم
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "تحصيل اليوم",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale("ar"), "%,.0f ج.س", totalPaymentsToday),
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Sub-card 2: عملاء مدينون
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "عملاء مدينون",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val activeDebtorsCount = customers.filter { it.balance > 0 }.size
                                        Text(
                                            text = "$activeDebtorsCount عملاء",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Header
            item {
                Text(
                    text = "الإجراءات السريعة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF94A3B8), // slate-400
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                )
            }

            // Quick Actions Cards Rows
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "إضافة دين",
                        icon = Icons.Default.Add,
                        color = DangerColor,
                        bgColor = Color(0xFFFFEBEE), // Red-50
                        modifier = Modifier.weight(1f).testTag("quick_add_debt"),
                        onClick = { viewModel.navigateTo(Screen.AddDebt()) }
                    )
                    ActionCard(
                        title = "سجل سداد",
                        icon = Icons.Default.Check,
                        color = SuccessColor,
                        bgColor = Color(0xFFE8F5E9), // Green-50
                        modifier = Modifier.weight(1f).testTag("quick_record_payment"),
                        onClick = { viewModel.navigateTo(Screen.RecordPayment()) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "عميل جديد",
                        icon = Icons.Default.PersonAdd,
                        color = MaterialTheme.colorScheme.primary,
                        bgColor = Color(0xFFE3F2FD), // Blue-50
                        modifier = Modifier.weight(1f).testTag("quick_add_customer"),
                        onClick = { viewModel.navigateTo(Screen.AddCustomer) }
                    )
                    ActionCard(
                        title = "التقارير",
                        icon = Icons.Default.Analytics,
                        color = Color(0xFFB45309), // Amber-700
                        bgColor = Color(0xFFFFF8E1), // Amber-50
                        modifier = Modifier.weight(1f).testTag("quick_reports"),
                        onClick = { viewModel.navigateToMainTab(Screen.Reports) }
                    )
                }
            }

            // Recent Activity Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر المعاملات",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF94A3B8) // slate-400
                    )
                    Text(
                        text = "عرض الكل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { viewModel.navigateToMainTab(Screen.Reports) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Recent Activity Feed Items
            val recentTxs = transactionsList.take(3)
            if (recentTxs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا يوجد معاملة مدرجة حالياً.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentTxs) { tx ->
                    val customer = customers.find { it.id == tx.customerId }
                    val nameStr = customer?.name ?: "عميل مجهول"
                    val avatarChar = if (nameStr.isNotEmpty()) nameStr.first().toString() else "ع"
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.CustomerDetails(tx.customerId)) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rounded avatar character
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarChar,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nameStr,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B) // slate-800
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (tx.type == "debt") "سحب دين جديد" else "سداد واستلام قيد",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (tx.type == "debt") DangerColor else SuccessColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "• ${formatTimestamp(tx.createdAt)}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Text(
                                text = if (tx.type == "debt") {
                                    String.format(Locale("ar"), "-%,.0f ج.س", tx.amount)
                                } else {
                                    String.format(Locale("ar"), "+%,.0f ج.س", tx.amount)
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "debt") DangerColor else SuccessColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp), // custom modern 3xl corner radius like HTML
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // sleek flat style with border/shadow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(bgColor, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155), // Slate-700
                textAlign = TextAlign.Center
            )
        }
    }
}

// ------------------- CUSTOMERS SCREEN -------------------
@Composable
fun CustomersScreen(
    viewModel: DebtViewModel,
    customers: List<Customer>
) {
    var searchQuery by viewModel.customerSearchQuery

    // Reactive filter of customer database List
    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.phone ?: "").contains(searchQuery)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.AddCustomer) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة عميل")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "سجل العملاء والحسابات",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic Real-time search by name/phone
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث باسم العميل أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "لا يوجد عملاء حالياً، أضف عميلاً لبدء المحاسبة." else "لا توجد نتائج تطابق بحثك.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCustomers) { cust ->
                        CustomerCard(
                            customer = cust,
                            onClick = { viewModel.navigateTo(Screen.CustomerDetails(cust.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCard(customer: Customer, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular initial Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (customer.name.isNotEmpty()) customer.name.first().toString() else "👤",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = customer.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!customer.phone.isNullOrBlank()) {
                        Text(
                            text = customer.phone,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Highlighting based on Debt color values
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "الحساب القائم",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale("ar"), "%,.2f ج.س", customer.balance),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (customer.balance > 0) DangerColor else SuccessColor
                )
            }
        }
    }
}

// ------------------- ADD CUSTOMER SCREEN -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(viewModel: DebtViewModel) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إقامة حساب عميل جديد", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم العميل كامل (مطلوب)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف (اختياري)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("مكان السكن / المتجر / العنوان (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.addCustomer(name, phone, address) {
                        viewModel.navigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تثبيت وحفظ الحساب الجديد", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------- CUSTOMER DETAILS SCREEN -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(viewModel: DebtViewModel, customerId: Int) {
    val context = LocalContext.current
    val customerState = viewModel.getCustomerById(customerId).collectAsState(initial = null)
    val transactionsList by viewModel.getTransactionsForCustomer(customerId).collectAsState(initial = emptyList())
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val customer = customerState.value

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حساب العميل تفصيلاً", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف العميل", tint = DangerColor)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Customer Header Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = customer.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!customer.phone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(dialIntent)
                                    }
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        customer.phone,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (!customer.address.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        customer.address,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Large customer balance indicator
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (customer.balance > 0) DangerColor.copy(alpha = 0.1f) else SuccessColor.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("الرصيد القائم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale("ar"), "%,.2f ج.س", customer.balance),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (customer.balance > 0) DangerColor else SuccessColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic action triggers for this specific customer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.AddDebt(customer.id)) },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دين جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.navigateTo(Screen.RecordPayment(customer.id)) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("سداد مبلغ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.Statement(customer.id)) },
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الكشف", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "تاريخ حركة المعاملات",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (transactionsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد قيود أو حركات سابقة لهذا العميل.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(transactionsList) { tx ->
                        TransactionRowItem(tx = tx) {
                            viewModel.deleteTransaction(tx)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("تأكيد حذف الحساب") },
            text = { Text("هل أنت متأكد من حذف حساب العميل '${customer.name}' بالكامل؟ سيؤدي ذلك أيضاً لحذف جميع حركات الدين والسداد السابقة الخاصة به بشكل نهائي!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteCustomer(customer)
                    }
                ) {
                    Text("نعم، احذف الحساب", color = DangerColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء المطلب", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}

@Composable
fun TransactionRowItem(tx: CustomerTransaction, onDelete: () -> Unit) {
    var showConfirmDeleteTx by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (tx.type == "debt") Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (tx.type == "debt") DangerColor else SuccessColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (tx.type == "debt") "دين جديد" else "سداد واستلام",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (tx.type == "debt") DangerColor else SuccessColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestamp(tx.createdAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (!tx.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tx.note,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale("ar"), "%,.2f ج.س", tx.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (tx.type == "debt") DangerColor else SuccessColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { showConfirmDeleteTx = true }) {
                    Icon(Icons.Default.Close, contentDescription = "حذف قيد", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (showConfirmDeleteTx) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteTx = false },
            title = { Text("حذف العملية") },
            text = { Text("هل تريد حذف هذه العملية حقاً؟ سيقوم النظام بإعادة تحديث رصيد العميل بناء على ذلك.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteTx = false
                        onDelete()
                    }
                ) {
                    Text("نعم، احذف العملية", color = DangerColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteTx = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}

// ------------------- RECORD DEBT SCREEN -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    viewModel: DebtViewModel,
    preselectedCustomerId: Int?,
    customers: List<Customer>
) {
    var selectedCustomer by remember {
        mutableStateOf<Customer?>(customers.find { it.id == preselectedCustomerId })
    }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل دين جديد آجل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown Customer Select
            if (preselectedCustomerId == null) {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedCustomer?.name ?: "الرجاء اختيار العميل...",
                        onValueChange = {},
                        label = { Text("اختر العميل المشتري آجل") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        if (customers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("لا يوجد عملاء، أضف عميل أولاً") },
                                onClick = {
                                    expandedDropdown = false
                                    viewModel.navigateTo(Screen.AddCustomer)
                                }
                            )
                        } else {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = { Text(cust.name) },
                                    onClick = {
                                        selectedCustomer = cust
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Readonly label representation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("العميل المدين", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text(selectedCustomer?.name ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("المبلغ التراكمي (جنية سوداني)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = DangerColor) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("بيان السلع / تفصيل أو ملاحظة (مثال: سكر وبصل، حليب)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val customer = selectedCustomer
                    if (customer == null) {
                        viewModel.uiError.value = "الرجاء اختيار العميل المستفيد"
                        return@Button
                    }
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.recordDebt(customer.id, amount, note) {
                        viewModel.navigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DangerColor)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل وحفظ الدين", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------- RECORD PAYMENT SCREEN -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    viewModel: DebtViewModel,
    preselectedCustomerId: Int?,
    customers: List<Customer>
) {
    var selectedCustomer by remember {
        mutableStateOf<Customer?>(customers.find { it.id == preselectedCustomerId })
    }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل سداد واستلام نقدي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown Customer Select
            if (preselectedCustomerId == null) {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedCustomer?.name ?: "الرجاء اختيار العميل دفع الحساب...",
                        onValueChange = {},
                        label = { Text("اختر العميل المسدد") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        if (customers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("لا يوجد عملاء، أضف عميل أولاً") },
                                onClick = {
                                    expandedDropdown = false
                                    viewModel.navigateTo(Screen.AddCustomer)
                                }
                            )
                        } else {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = { Text(cust.name) },
                                    onClick = {
                                        selectedCustomer = cust
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Readonly label representation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("العميل المسلح أو المسدد", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text(selectedCustomer?.name ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale("ar"), "الرصيد المستحق الحالي: %,.2f ج.س", selectedCustomer?.balance ?: 0.0),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("المبلغ المستلم (جنية سوداني)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = SuccessColor) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("أي ملاحظات إضافية (أقساط، كاش، تحويل بنكك، إلخ)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val customer = selectedCustomer
                    if (customer == null) {
                        viewModel.uiError.value = "الرجاء اختيار العميل"
                        return@Button
                    }
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    viewModel.recordPayment(customer.id, amount, note) {
                        viewModel.navigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تثبيت السداد وحسم الرصيد", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------- COMFY STATEMENT (كشف الحساب) SCREEN -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(viewModel: DebtViewModel, customerId: Int) {
    val context = LocalContext.current
    val customerState = viewModel.getCustomerById(customerId).collectAsState(initial = null)
    val transactionsStream by viewModel.getTransactionsForCustomer(customerId).collectAsState(initial = emptyList())

    val customer = customerState.value

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Chronologic sorted (Oldest to Newest) to calculate accurate cumulative balance running totals
    val chronologicTxs = remember(transactionsStream) {
        transactionsStream.sortedBy { it.createdAt }
    }

    // Prepare running total lists
    val txsWithRunningBalance = remember(chronologicTxs) {
        var currentSum = 0.0
        chronologicTxs.map { tx ->
            if (tx.type == "debt") {
                currentSum += tx.amount
            } else {
                currentSum -= tx.amount
            }
            Pair(tx, currentSum)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("كشف حساب العميل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Customer Meta
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(customer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!customer.phone.isNullOrBlank()) {
                        Text("رقم الهاتف: ${customer.phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "رصيد الختام المطلوب القائم حالياً: ${String.format(Locale("ar"), "%,.2f ج.س", customer.balance)}",
                        fontWeight = FontWeight.Bold,
                        color = if (customer.balance > 0) DangerColor else SuccessColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions for statement sharing/printing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val shareText = buildShareStatementText(customer, txsWithRunningBalance)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة واتساب", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        printStatementPdf(context, customer, txsWithRunningBalance)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ / طباعة PDF", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Headers for columns
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("التاريخ والبيان", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("المبلغ", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("الرصيد الناتج", modifier = Modifier.weight(1.3f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (txsWithRunningBalance.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد حركات سابقة كشفا لحساب هذا العميل.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    items(txsWithRunningBalance) { pair ->
                        val tx = pair.first
                        val sum = pair.second
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(formatTimestamp(tx.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = if (tx.type == "debt") "دين: ${tx.note ?: "شراء آجل"}" else "سداد: ${tx.note ?: "دفعة كاش"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = String.format(Locale("ar"), "%,.1f", tx.amount),
                                color = if (tx.type == "debt") DangerColor else SuccessColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End,
                                fontSize = 14.sp
                            )
                            Text(
                                text = String.format(Locale("ar"), "%,.1f", sum),
                                modifier = Modifier.weight(1.3f),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Bold,
                                color = if (sum > 0) DangerColor else SuccessColor,
                                fontSize = 14.sp
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

// Share statement helper textual compilation
fun buildShareStatementText(customer: Customer, txs: List<Pair<CustomerTransaction, Double>>): String {
    val sb = java.lang.StringBuilder()
    sb.append("⚠️ *كشف حساب مالي - دفتر الديون* ⚠️\n")
    sb.append("------------------------------------\n")
    sb.append("👤 *العميل:* ${customer.name}\n")
    if (!customer.phone.isNullOrBlank()) {
        sb.append("📞 *الهاتف:* ${customer.phone}\n")
    }
    sb.append("📅 *تاريخ الاستخراج:* ${SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())}\n")
    sb.append("------------------------------------\n")
    sb.append("📜 *حركة القيود المالية (الأحدث أخيراً):*\n\n")

    txs.forEach { pair ->
        val tx = pair.first
        val balance = pair.second
        val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale("ar")).format(Date(tx.createdAt))
        val typeLabel = if (tx.type == "debt") "دين آجل 🔴" else "سداد حسم 🟢"
        val noteStr = if (tx.note.isNullOrBlank()) "" else " (${tx.note})"
        sb.append("$dateStr - $typeLabel: ${tx.amount} ج.س$noteStr | الرصيد: $balance ج.س\n")
    }

    sb.append("------------------------------------\n")
    sb.append("💰 *إجمالي الرصيد القائم المستحق:* \n")
    sb.append("*${String.format(Locale("ar"), "%,.2f", customer.balance)} جنيه سوداني*\n\n")
    sb.append("سائلين الله لكم البركة والتوفيق لخدمتكم.")
    return sb.toString()
}

// Printable PDF HTML Document helper
fun printStatementPdf(context: Context, customer: Customer, txs: List<Pair<CustomerTransaction, Double>>) {
    val rowsHtml = java.lang.StringBuilder()
    var orderNo = 1
    txs.forEach { pair ->
        val tx = pair.first
        val balance = pair.second
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(tx.createdAt))
        val typeLabel = if (tx.type == "debt") "دين (دين جديدة)" else "سداد وحسم"
        val color = if (tx.type == "debt") "#C62828" else "#2E7D32"
        rowsHtml.append(
            """
            <tr>
                <td style="text-align: center;">$orderNo</td>
                <td>$dateStr</td>
                <td style="color: $color; font-weight: bold;">$typeLabel</td>
                <td>${tx.note ?: "-"}</td>
                <td style="color: $color; font-weight: bold; text-align: left;">${String.format(Locale("ar"), "%,.2f", tx.amount)}</td>
                <td style="font-weight: bold; text-align: left;">${String.format(Locale("ar"), "%,.2f", balance)}</td>
            </tr>
            """
        )
        orderNo++
    }

    val htmlDocument = """
        <!DOCTYPE html>
        <html dir="rtl">
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: 'Cairo', sans-serif; padding: 20px; color: #111; }
                .header { text-align: center; margin-bottom: 30px; border-bottom: 3px double #0B2D4D; padding-bottom: 10px; }
                .header h1 { color: #0B2D4D; margin: 0; font-size: 26px; }
                .meta-table { width: 100%; margin-bottom: 25px; border-collapse: collapse; }
                .meta-table td { padding: 8px; font-size: 15px; }
                .tx-table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                .tx-table th, .tx-table td { border: 1px solid #ddd; padding: 10px; font-size: 14px; text-align: right; }
                .tx-table th { background-color: #0B2D4D; color: white; }
                .tx-table tr:nth-child(even) { background-color: #f9f9f9; }
                .total-card { margin-top: 30px; padding: 15px; background-color: #f5f7fa; border-right: 5px solid #0B2D4D; text-align: left; }
                .total-card h2 { margin: 0; color: #0B2D4D; font-size: 20px; }
                .footer { text-align: center; margin-top: 40px; font-size: 12px; color: #777; border-top: 1px dashed #ccc; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>دفتر الديون الإلكتروني - كشف حساب مالي</h1>
                <p>تطبيق السداد المنزلي والمتاجر لخدمة المحلات التجارية والبيع بالأجل</p>
            </div>
            
            <table class="meta-table">
                <tr>
                    <td><strong>اسم العميل:</strong> ${customer.name}</td>
                    <td><strong>تاريخ الاستخراج:</strong> ${SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())}</td>
                </tr>
                <tr>
                    <td><strong>رقم الهاتف:</strong> ${customer.phone ?: "غير متوفر"}</td>
                    <td><strong>العنوان والحي:</strong> ${customer.address ?: "غير متوفر"}</td>
                </tr>
            </table>
            
            <table class="tx-table">
                <thead>
                    <tr>
                        <th style="text-align: center; width: 45px;">#</th>
                        <th style="width: 140px;">التاريخ والوقت</th>
                        <th style="width: 100px;">النوع</th>
                        <th>الملاحظات والبيان</th>
                        <th style="text-align: left; width: 110px;">المبلغ (ج.س)</th>
                        <th style="text-align: left; width: 110px;">الرصيد التراكمي</th>
                    </tr>
                </thead>
                <tbody>
                    ${rowsHtml}
                </tbody>
            </table>
            
            <div class="total-card">
                <h2>إجمالي الرصيد القائم المستحق المطلوب حالياً:</h2>
                <h1 style="color: #C62828; margin: 5px 0 0 0; text-align: left;">${String.format(Locale("ar"), "%,.2f", customer.balance)} جنيه سوداني</h1>
            </div>
            
            <div class="footer">
                <p>تم سحب هذا البيان تلقائياً ويُعتبر مستنداً محاسبياً صالحاً للمراجعة لدفتر الديون الإلكتروني.</p>
            </div>
        </body>
        </html>
    """.trimIndent()

    // Render Webview in background and print
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "كشف حساب - ${customer.name}"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlDocument, "text/html; charset=utf-8", "utf-8", null)
}

// ------------------- REPORTS & GRAPHS SCREEN -------------------
@Composable
fun ReportsScreen(
    viewModel: DebtViewModel,
    customers: List<Customer>,
    transactions: List<CustomerTransaction>
) {
    val totalCustomers = customers.size
    val totalOutstandingDebt = customers.filter { it.balance > 0 }.sumOf { it.balance }
    
    val totalDebtsSum = transactions.filter { it.type == "debt" }.sumOf { it.amount }
    val totalPaymentsSum = transactions.filter { it.type == "payment" }.sumOf { it.amount }

    // Top Debtors List (ordered by highest balance desc)
    val topDebtors = remember(customers) {
        customers.filter { it.balance > 0 }.sortedByDescending { it.balance }.take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "التقارير والإحصائيات الحية",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Dashboard Metrics Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("نظرة محاسبية شاملة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي الديون المسجلة تاريخياً:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale("ar"), "%,.1f ج.س", totalDebtsSum), fontWeight = FontWeight.Bold, color = DangerColor)
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي السدادات المستلمة تاريخياً:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale("ar"), "%,.1f ج.س", totalPaymentsSum), fontWeight = FontWeight.Bold, color = SuccessColor)
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("صافي المبالغ القائمة عند العملاء الباقية للتحصيل:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale("ar"), "%,.1f ج.س", totalOutstandingDebt), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    }
                }
            }
        }

        // Custom canvas visual bar chart (Drawn beautifully in Jetpack Compose)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "التوزيع المادي للمعامِلات (جنية سوداني)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val successColor = SuccessColor
                    val dangerColor = DangerColor

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val maxVal = maxOf(totalDebtsSum, totalPaymentsSum, totalOutstandingDebt).coerceAtLeast(1.0)
                        val totalWidth = size.width
                        val totalHeight = size.height

                        // Calculate bars coordinates
                        val barSpacing = totalWidth / 6f
                        val barWidth = totalWidth / 10f

                        // Available height minus space for label details
                        val chartHeight = totalHeight - 40f

                        val debtHeight = (totalDebtsSum / maxVal * chartHeight).toFloat().coerceAtLeast(10f)
                        val paymentHeight = (totalPaymentsSum / maxVal * chartHeight).toFloat().coerceAtLeast(10f)
                        val outstandingHeight = (totalOutstandingDebt / maxVal * chartHeight).toFloat().coerceAtLeast(10f)

                        // 1. Debt Bar
                        drawRoundRect(
                            color = dangerColor,
                            topLeft = Offset(barSpacing, chartHeight - debtHeight),
                            size = Size(barWidth, debtHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // 2. Payment Bar
                        drawRoundRect(
                            color = successColor,
                            topLeft = Offset(barSpacing * 2.5f, chartHeight - paymentHeight),
                            size = Size(barWidth, paymentHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // 3. Outstanding Bar
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(barSpacing * 4f, chartHeight - outstandingHeight),
                            size = Size(barWidth, outstandingHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Drwan guidelines or values above bars in native Canvas
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 26f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            // Debts Value text
                            drawText(
                                String.format(Locale("ar"), "%,.0f", totalDebtsSum),
                                barSpacing + barWidth / 2f,
                                chartHeight - debtHeight - 10f,
                                paint
                            )
                            // Payments Value text
                            drawText(
                                String.format(Locale("ar"), "%,.0f", totalPaymentsSum),
                                barSpacing * 2.5f + barWidth / 2f,
                                chartHeight - paymentHeight - 10f,
                                paint
                            )
                            // Outstanding Value text
                            drawText(
                                String.format(Locale("ar"), "%,.0f", totalOutstandingDebt),
                                barSpacing * 4f + barWidth / 2f,
                                chartHeight - outstandingHeight - 10f,
                                paint
                            )

                            // Labels at bottom
                            paint.color = android.graphics.Color.DKGRAY
                            drawText("إجمالي الديون", barSpacing + barWidth / 2f, totalHeight - 5f, paint)
                            drawText("إجمالي السداد", barSpacing * 2.5f + barWidth / 2f, totalHeight - 5f, paint)
                            drawText("الديون القائمة", barSpacing * 4f + barWidth / 2f, totalHeight - 5f, paint)
                        }
                    }
                }
            }
        }

        // Top Debtors List Section
        item {
            Text(
                text = "قائمة أكبر ٥ عملاء مدينين",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (topDebtors.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "لا توجد ديون معلقة على أي عميل حالياً! ممتاز.",
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(topDebtors) { debtor ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.CustomerDetails(debtor.id)) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(DangerColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(debtor.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = String.format(Locale("ar"), "%,.1f ج.س", debtor.balance),
                            color = DangerColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ------------------- SETTINGS SCREEN -------------------
@Composable
fun SettingsScreen(viewModel: DebtViewModel) {
    val context = LocalContext.current
    val allowNegState by viewModel.allowNegativeBalance
    val darkModeState by viewModel.darkModeEnabled

    var showImportDialog by remember { mutableStateOf(false) }
    var importTextCode by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "الإعدادات وإدارة قواعد البيانات",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Feature Switches Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("إعدادات النظام المحاسبي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Allow negative balance toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("السماح بالرصيد المدين السالب", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("يتيح تسجيل سدادات محاسبية بقيم أكبر من الدين القائم القائم حالياً للعميل.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = allowNegState,
                            onCheckedChange = { viewModel.setAllowNegativeBalance(it) }
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Dark mode toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المظهر والوضع الداكن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("تبديل واجهات التطبيق إلى الوضع الليلي لحماية العين عند استخدامه ليلاً.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = darkModeState,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                }
            }
        }

        // Backups Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("النسخ الاحتياطي وحماية البيانات (أوفلاين)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "يعمل التطبيق بالكامل بدون إنترنت لحفظ بياناتك محلياً بشكل فوري. لتجنب فقد البيانات، يرجى استخراج نسخة احتياطية بشكل دوري وحفظها.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { viewModel.exportBackupToClipboard(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير ونسخ كود الاحتياط للحافظة", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            importTextCode = ""
                            showImportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استيراد واستعادة من كود الحافظة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // App Info Statement Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("دفتر الديون (Debt Book) - النسخة المحاسبية", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("مصمم ومطور خصيصاً لخدمة البقالات والمتاجر والمقاهي وصغار التجار السودانيين.", fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("الإصدار الحالي: v1.0.0 (يعمل بالكامل دون إنترنت)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد واسترجاع قاعدة البيانات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى لصق الرمز البرمجي الطويل للنسخة الاحتياطية الذي قمت بنسخه مسبقاً:")
                    OutlinedTextField(
                        value = importTextCode,
                        onValueChange = { importTextCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("ألصق الكود هنا...") },
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 10
                    )
                    Text("⚠️ تحذير: هذا الإجراء سيقوم بحذف جميع البيانات الحالية واستبدالها بالكامل بالبيانات المستوردة!", color = DangerColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        viewModel.importBackupFromClipboard(importTextCode)
                    }
                ) {
                    Text("تأكيد و استبدال قواعد البيانات", color = DangerColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء المطلب", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}
