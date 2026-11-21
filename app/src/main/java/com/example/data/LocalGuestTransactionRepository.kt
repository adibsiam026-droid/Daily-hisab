package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LocalGuestTransactionRepository(context: Context) : TransactionDataSource {

    private val storageFile = File(context.applicationContext.filesDir, "guest_transactions.json")
    private val mutex = Mutex()
    private val _records = MutableStateFlow(readFromDisk())

    override fun observeTransactions(userId: String): Flow<List<TransactionRecord>> =
        _records.asStateFlow()

    override suspend fun addTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long
    ): Result<Unit> = runCatching {
        require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount out of valid range" }
        val cleanDesc = description.trim().take(200)
        val cleanCategory = category.key.take(40)
        require(cleanCategory.isNotBlank()) { "Category must not be blank" }
        require(transactionDateMillis > 0L) { "Transaction date must be positive" }

        val record = TransactionRecord(
            id = UUID.randomUUID().toString(),
            userId = GUEST_USER_ID,
            type = type.name,
            amount = amount,
            category = cleanCategory,
            description = cleanDesc,
            transactionDateMillis = transactionDateMillis
        )

        mutex.withLock {
            val updated = (_records.value + record)
                .sortedByDescending { it.transactionDateMillis }
            writeToDisk(updated)
            _records.value = updated
        }
    }

    override suspend fun updateTransaction(
        userId: String,
        transactionId: String,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long
    ): Result<Unit> = runCatching {
        require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }
        require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount out of valid range" }
        val cleanDesc = description.trim().take(200)
        val cleanCategory = category.key.take(40)
        require(cleanCategory.isNotBlank()) { "Category must not be blank" }
        require(transactionDateMillis > 0L) { "Transaction date must be positive" }

        mutex.withLock {
            val updated = _records.value.map { existing ->
                if (existing.id == transactionId) {
                    existing.copy(
                        type = type.name,
                        amount = amount,
                        category = cleanCategory,
                        description = cleanDesc,
                        transactionDateMillis = transactionDateMillis
                    )
                } else {
                    existing
                }
            }.sortedByDescending { it.transactionDateMillis }
            writeToDisk(updated)
            _records.value = updated
        }
    }

    override suspend fun deleteTransaction(
        userId: String,
        transactionId: String
    ): Result<Unit> = runCatching {
        require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }
        mutex.withLock {
            val updated = _records.value.filterNot { it.id == transactionId }
            writeToDisk(updated)
            _records.value = updated
        }
    }

    suspend fun getSnapshot(): List<TransactionRecord> = mutex.withLock {
        _records.value
    }

    suspend fun clearAll() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (storageFile.exists()) {
                    storageFile.delete()
                }
            }
            _records.value = emptyList()
        }
    }

    private fun readFromDisk(): List<TransactionRecord> {
        if (!storageFile.exists()) return emptyList()
        return try {
            val raw = storageFile.readText()
            if (raw.isBlank()) return emptyList()
            val array = JSONArray(raw)
            val list = ArrayList<TransactionRecord>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TransactionRecord(
                        id = obj.optString("id"),
                        userId = GUEST_USER_ID,
                        type = obj.optString("type", TransactionType.EXPENSE.name),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", TransactionCategory.OTHER.key),
                        description = obj.optString("description", ""),
                        transactionDateMillis = obj.optLong("transactionDateMillis", 0L)
                    )
                )
            }
            list.filter { it.id.isNotBlank() && it.amount > 0.0 && it.transactionDateMillis > 0L }
                .sortedByDescending { it.transactionDateMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun writeToDisk(records: List<TransactionRecord>) {
        withContext(Dispatchers.IO) {
            val array = JSONArray()
            for (rec in records) {
                array.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("type", rec.type)
                        put("amount", rec.amount)
                        put("category", rec.category)
                        put("description", rec.description)
                        put("transactionDateMillis", rec.transactionDateMillis)
                    }
                )
            }
            val tmpFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
            tmpFile.writeText(array.toString())
            if (!tmpFile.renameTo(storageFile)) {
                storageFile.writeText(array.toString())
                tmpFile.delete()
            }
        }
    }

    companion object {
        const val GUEST_USER_ID = "guest_local"
    }
}
