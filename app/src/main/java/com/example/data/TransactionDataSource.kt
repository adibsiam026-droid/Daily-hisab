package com.example.data

import kotlinx.coroutines.flow.Flow

interface TransactionDataSource {
    fun observeTransactions(userId: String): Flow<List<TransactionRecord>>

    suspend fun addTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long
    ): Result<Unit>

    suspend fun updateTransaction(
        userId: String,
        transactionId: String,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long
    ): Result<Unit>

    suspend fun deleteTransaction(
        userId: String,
        transactionId: String
    ): Result<Unit>
}
