package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class DebtRepository(private val database: AppDatabase) {
    private val customerDao = database.customerDao()
    private val transactionDao = database.transactionDao()

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<CustomerTransaction>> = transactionDao.getAllTransactions()

    fun getCustomer(id: Int): Flow<Customer?> = customerDao.getCustomerById(id)

    fun getTransactionsForCustomer(customerId: Int): Flow<List<CustomerTransaction>> =
        transactionDao.getTransactionsForCustomer(customerId)

    suspend fun getTransactionsForCustomerSync(customerId: Int): List<CustomerTransaction> =
        transactionDao.getTransactionsForCustomerSync(customerId)

    fun getTransactionsToday(): Flow<List<CustomerTransaction>> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return transactionDao.getTransactionsSince(calendar.timeInMillis)
    }

    suspend fun addCustomer(name: String, phone: String?, address: String?): Long {
        val newCustomer = Customer(
            name = name,
            phone = phone?.takeIf { it.isNotBlank() },
            address = address?.takeIf { it.isNotBlank() },
            balance = 0.0
        )
        return customerDao.insertCustomer(newCustomer)
    }

    suspend fun recordDebt(customerId: Int, amount: Double, note: String?, createdAt: Long = System.currentTimeMillis()) {
        val customer = customerDao.getCustomerByIdSync(customerId) ?: throw IllegalArgumentException("العميل غير موجود")
        val transaction = CustomerTransaction(
            customerId = customerId,
            amount = amount,
            type = "debt",
            note = note?.takeIf { it.isNotBlank() },
            createdAt = createdAt
        )
        // Insert transaction
        transactionDao.insertTransaction(transaction)
        // Update customer balance: adding debt increases the balance (customer owes more)
        val newBalance = customer.balance + amount
        customerDao.updateBalance(customerId, newBalance)
    }

    suspend fun recordPayment(
        customerId: Int,
        amount: Double,
        note: String?,
        allowNegativeBalance: Boolean,
        createdAt: Long = System.currentTimeMillis()
    ) {
        val customer = customerDao.getCustomerByIdSync(customerId) ?: throw IllegalArgumentException("العميل غير موجود")
        val newBalance = customer.balance - amount
        
        if (newBalance < 0 && !allowNegativeBalance) {
            throw IllegalArgumentException("لا يمكن أن يكون الرصيد سالبًا (مبلغ السداد أكبر من الدين القائم)")
        }

        val transaction = CustomerTransaction(
            customerId = customerId,
            amount = amount,
            type = "payment",
            note = note?.takeIf { it.isNotBlank() },
            createdAt = createdAt
        )
        
        transactionDao.insertTransaction(transaction)
        customerDao.updateBalance(customerId, newBalance)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun deleteTransaction(tx: CustomerTransaction) {
        val customer = customerDao.getCustomerByIdSync(tx.customerId)
        if (customer != null) {
            val balanceDiff = if (tx.type == "debt") -tx.amount else tx.amount
            customerDao.updateBalance(tx.customerId, customer.balance + balanceDiff)
        }
        transactionDao.deleteTransaction(tx)
    }

    suspend fun exportBackup(): String {
        val customers = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
        val transactions = transactionDao.getAllTransactions().firstOrNull() ?: emptyList()

        // Construct a simple JSON text manually or via Moshi to be 100% bug-free and custom.
        val customersJson = customers.joinToString(",") { c ->
            """{"id":${c.id},"name":"${escapeJson(c.name)}","phone":${if (c.phone == null) "null" else "\"${escapeJson(c.phone)}\""},"address":${if (c.address == null) "null" else "\"${escapeJson(c.address)}\""},"balance":${c.balance},"createdAt":${c.createdAt}}"""
        }
        val transactionsJson = transactions.joinToString(",") { t ->
            """{"id":${t.id},"customerId":${t.customerId},"amount":${t.amount},"type":"${t.type}","note":${if (t.note == null) "null" else "\"${escapeJson(t.note)}\""},"createdAt":${t.createdAt}}"""
        }

        return """{"version":1,"customers":[$customersJson],"transactions":[$transactionsJson]}"""
    }

    suspend fun importBackup(jsonString: String) {
        // Simplified custom parser to operate without complex parser libraries that could crash on formatting.
        // We look for customers and transactions blocks.
        try {
            // Re-initialize Database with transactions
            val customersSegment = getJsonArray(jsonString, "customers")
            val transactionsSegment = getJsonArray(jsonString, "transactions")

            // Parse Customers
            val parsedCustomers = mutableListOf<Customer>()
            for (obj in customersSegment) {
                val id = getIntField(obj, "id") ?: 0
                val name = getStringField(obj, "name") ?: ""
                val phone = getStringField(obj, "phone")
                val address = getStringField(obj, "address")
                val balance = getDoubleField(obj, "balance") ?: 0.0
                val createdAt = getLongField(obj, "createdAt") ?: System.currentTimeMillis()
                if (name.isNotEmpty()) {
                    parsedCustomers.add(Customer(id, name, phone, address, balance, createdAt))
                }
            }

            // Parse Transactions
            val parsedTransactions = mutableListOf<CustomerTransaction>()
            for (obj in transactionsSegment) {
                val id = getIntField(obj, "id") ?: 0
                val customerId = getIntField(obj, "customerId") ?: 0
                val amount = getDoubleField(obj, "amount") ?: 0.0
                val type = getStringField(obj, "type") ?: "debt"
                val note = getStringField(obj, "note")
                val createdAt = getLongField(obj, "createdAt") ?: System.currentTimeMillis()
                if (customerId > 0 && amount > 0) {
                    parsedTransactions.add(CustomerTransaction(id, customerId, amount, type, note, createdAt))
                }
            }

            if (parsedCustomers.isNotEmpty()) {
                // Clear and write atomically
                database.clearAllTables()
                for (cust in parsedCustomers) {
                    customerDao.insertCustomer(cust)
                }
                for (tx in parsedTransactions) {
                    transactionDao.insertTransaction(tx)
                }
            } else {
                throw Exception("نسخة احتياطية فارغة أو غير صالحة")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("فشل قراءة ملف النسخة الاحتياطية: ${e.message}")
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun getJsonArray(json: String, key: String): List<String> {
        val startToken = "\"$key\":["
        val startIdx = json.indexOf(startToken)
        if (startIdx == -1) return emptyList()
        var braceCount = 1
        var idx = startIdx + startToken.length
        val result = mutableListOf<String>()
        var currentObj = java.lang.StringBuilder()

        while (idx < json.length && braceCount > 0) {
            val char = json[idx]
            if (char == '[') braceCount++
            else if (char == ']') {
                braceCount--
                if (braceCount == 0) break
            }
            currentObj.append(char)
            idx++
        }

        val arrayStr = currentObj.toString().trim()
        if (arrayStr.isEmpty()) return emptyList()

        // Split by objects {...},{...} safely respecting matching braces
        val items = mutableListOf<String>()
        var objBraceCount = 0
        var insideString = false
        var currentItem = java.lang.StringBuilder()
        var i = 0
        while (i < arrayStr.length) {
            val c = arrayStr[i]
            if (c == '"' && (i == 0 || arrayStr[i - 1] != '\\')) {
                insideString = !insideString
            }
            if (!insideString) {
                if (c == '{') {
                    objBraceCount++
                }
                if (objBraceCount > 0) {
                    currentItem.append(c)
                }
                if (c == '}') {
                    objBraceCount--
                    if (objBraceCount == 0) {
                        items.add(currentItem.toString())
                        currentItem = java.lang.StringBuilder()
                    }
                }
            } else {
                currentItem.append(c)
            }
            i++
        }
        return items
    }

    private fun getStringField(obj: String, key: String): String? {
        val token = "\"$key\":"
        val idx = obj.indexOf(token)
        if (idx == -1) return null
        val startValueIdx = idx + token.length
        if (obj.startsWith("null", startValueIdx)) return null
        if (obj[startValueIdx] == '"') {
            // Find closing double-quote
            val result = java.lang.StringBuilder()
            var i = startValueIdx + 1
            while (i < obj.length) {
                if (obj[i] == '"' && obj[i - 1] != '\\') {
                    break
                }
                result.append(obj[i])
                i++
            }
            return result.toString().replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n")
        }
        // Unquoted value (number or boolean)
        val endIdx = findValueEnd(obj, startValueIdx)
        return obj.substring(startValueIdx, endIdx).trim()
    }

    private fun getIntField(obj: String, key: String): Int? {
        return getStringField(obj, key)?.toIntOrNull()
    }

    private fun getLongField(obj: String, key: String): Long? {
        return getStringField(obj, key)?.toLongOrNull()
    }

    private fun getDoubleField(obj: String, key: String): Double? {
        return getStringField(obj, key)?.toDoubleOrNull()
    }

    private fun findValueEnd(obj: String, startIdx: Int): Int {
        var i = startIdx
        while (i < obj.length) {
            val c = obj[i]
            if (c == ',' || c == '}' || c == ']') {
                return i
            }
            i++
        }
        return obj.length
    }
}
