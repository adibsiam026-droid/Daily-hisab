package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TransactionCategory
import com.example.data.TransactionDataSource
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HisabViewModel(
    private val repository: TransactionDataSource,
    private val currentUserId: String
) : ViewModel() {

    val transactionsState: StateFlow<UiState<List<TransactionRecord>>> =
        repository.observeTransactions(currentUserId)
            .map<List<TransactionRecord>, UiState<List<TransactionRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing transactions", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load transactions"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _selectedReportPeriod = MutableStateFlow(ReportPeriod.WEEKLY)
    val selectedReportPeriod: StateFlow<ReportPeriod> = _selectedReportPeriod.asStateFlow()

    private val _reportOffset = MutableStateFlow(0)
    val reportOffset: StateFlow<Int> = _reportOffset.asStateFlow()

    fun selectReportPeriod(period: ReportPeriod) {
        _selectedReportPeriod.value = period
        _reportOffset.value = 0
    }

    fun stepReportOffset(delta: Int) {
        val next = _reportOffset.value + delta
        if (next <= 0) {
            _reportOffset.value = next
        }
    }

    fun saveTransaction(
        existingId: String?,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = if (existingId.isNullOrBlank()) {
                repository.addTransaction(
                    userId = currentUserId,
                    type = type,
                    amount = amount,
                    category = category,
                    description = description,
                    transactionDateMillis = transactionDateMillis
                )
            } else {
                repository.updateTransaction(
                    userId = currentUserId,
                    transactionId = existingId,
                    type = type,
                    amount = amount,
                    category = category,
                    description = description,
                    transactionDateMillis = transactionDateMillis
                )
            }
            _isMutating.value = false
            onResult(result)
        }
    }

    fun deleteTransaction(
        transactionId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteTransaction(currentUserId, transactionId)
            _isMutating.value = false
            onResult(result)
        }
    }

    private companion object {
        const val TAG = "HisabVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
