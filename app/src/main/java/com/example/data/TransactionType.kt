package com.example.data

enum class TransactionType {
    INCOME,
    EXPENSE;

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}
