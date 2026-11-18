package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double = 0.0,
    val category: String = TransactionCategory.OTHER.key,
    val description: String = "",
    val transactionDateMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: TransactionType
        get() = TransactionType.fromRaw(type)

    val parsedCategory: TransactionCategory
        get() = TransactionCategory.fromKey(category, parsedType)
}
