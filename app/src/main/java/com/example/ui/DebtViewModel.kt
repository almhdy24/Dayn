package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Customers : Screen()
    object Reports : Screen()
    object Settings : Screen()
    data class CustomerDetails(val customerId: Int) : Screen()
    object AddCustomer : Screen()
    data class AddDebt(val preselectedCustomerId: Int? = null) : Screen()
    data class RecordPayment(val preselectedCustomerId: Int? = null) : Screen()
    data class Statement(val customerId: Int) : Screen()
}

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DebtRepository(database)

    // Navigation Stack
    val navigationStack = mutableStateListOf<Screen>(Screen.Home)

    // App State Lists
    val customers = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactionsToday = repository.getTransactionsToday().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search Query for Customers
    val customerSearchQuery = mutableStateOf("")

    // Settings States
    private val sharedPrefs = application.getSharedPreferences("debt_book_prefs", Context.MODE_PRIVATE)
    val allowNegativeBalance = mutableStateOf(sharedPrefs.getBoolean("allow_negative_balance", false))
    val darkModeEnabled = mutableStateOf(sharedPrefs.getBoolean("dark_mode_enabled", false))

    // UI Message state for toast-like notifications
    val uiMessage = mutableStateOf<String?>(null)
    val uiError = mutableStateOf<String?>(null)

    fun navigateTo(screen: Screen) {
        navigationStack.add(screen)
    }

    fun navigateBack(): Boolean {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
            return true
        }
        return false
    }

    fun navigateToMainTab(screen: Screen) {
        navigationStack.clear()
        navigationStack.add(screen)
    }

    fun setAllowNegativeBalance(allowed: Boolean) {
        allowNegativeBalance.value = allowed
        sharedPrefs.edit().putBoolean("allow_negative_balance", allowed).apply()
    }

    fun toggleDarkMode() {
        val newVal = !darkModeEnabled.value
        darkModeEnabled.value = newVal
        sharedPrefs.edit().putBoolean("dark_mode_enabled", newVal).apply()
    }

    // Customer Actions
    fun addCustomer(name: String, phone: String?, address: String?, onSuccess: () -> Unit) {
        if (name.isBlank()) {
            uiError.value = "الرجاء إدخال اسم العميل"
            return
        }
        viewModelScope.launch {
            try {
                repository.addCustomer(name, phone, address)
                uiMessage.value = "تم إضافه العميل بنجاح"
                onSuccess()
            } catch (e: Exception) {
                uiError.value = "فشل إضافة العميل: ${e.message}"
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer)
                uiMessage.value = "تم حذف العميل بنجاح"
                // If deleted customer is open, pop it
                val current = navigationStack.lastOrNull()
                if (current is Screen.CustomerDetails && current.customerId == customer.id) {
                    navigateBack()
                }
            } catch (e: Exception) {
                uiError.value = "فشل حذف العميل: ${e.message}"
            }
        }
    }

    // Transaction Actions
    fun recordDebt(customerId: Int, amount: Double, note: String?, createdAt: Long = System.currentTimeMillis(), onSuccess: () -> Unit) {
        if (amount <= 0) {
            uiError.value = "الرجاء إدخال مبلغ صحيح أكبر من الصفر"
            return
        }
        viewModelScope.launch {
            try {
                repository.recordDebt(customerId, amount, note, createdAt)
                uiMessage.value = "تم تسجيل الدين بنجاح"
                onSuccess()
            } catch (e: Exception) {
                uiError.value = "فشل تسجيل الدين: ${e.message}"
            }
        }
    }

    fun recordPayment(customerId: Int, amount: Double, note: String?, createdAt: Long = System.currentTimeMillis(), onSuccess: () -> Unit) {
        if (amount <= 0) {
            uiError.value = "الرجاء إدخال مبلغ صحيح أكبر من الصفر"
            return
        }
        viewModelScope.launch {
            try {
                repository.recordPayment(customerId, amount, note, allowNegativeBalance.value, createdAt)
                uiMessage.value = "تم تسجيل السداد بنجاح"
                onSuccess()
            } catch (e: Exception) {
                uiError.value = e.message ?: "فشل تسجيل السداد"
            }
        }
    }

    fun deleteTransaction(tx: CustomerTransaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(tx)
                uiMessage.value = "تم حذف العملية وتحديث رصيد العميل"
            } catch (e: Exception) {
                uiError.value = "فشل الحذف: ${e.message}"
            }
        }
    }

    // Backup & Restore
    fun exportBackupToClipboard(context: Context) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackup()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("DebtBookBackup", json)
                clipboard.setPrimaryClip(clip)
                uiMessage.value = "تم نسخ النسخة الاحتياطية إلى الحافظة بنجاح! يمكنك حفظها في مكان آمن."
            } catch (e: Exception) {
                uiError.value = "فشل تصدير البيانات: ${e.message}"
            }
        }
    }

    fun importBackupFromClipboard(json: String) {
        if (json.isBlank()) {
            uiError.value = "الحافظة فارغة، يرجى نسخ كود النسخة احتياطية أولاً"
            return
        }
        viewModelScope.launch {
            try {
                repository.importBackup(json.trim())
                uiMessage.value = "تم استعادة البيانات بنجاح!"
            } catch (e: Exception) {
                uiError.value = e.message ?: "فشل استيراد البيانات"
            }
        }
    }

    fun getTransactionsForCustomer(customerId: Int): Flow<List<CustomerTransaction>> {
        return repository.getTransactionsForCustomer(customerId)
    }

    fun getCustomerById(customerId: Int): Flow<Customer?> {
        return repository.getCustomer(customerId)
    }
}
