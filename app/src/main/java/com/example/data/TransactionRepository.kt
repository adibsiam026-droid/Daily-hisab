package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TransactionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("transactions")
                .orderBy("transactionDateMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long
    ): Result<Unit> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireUserId(userId)
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount out of valid range" }
            val cleanDesc = description.trim().take(200)
            val cleanCategory = category.key.take(40)
            require(cleanCategory.isNotBlank()) { "Category must not be blank" }
            require(transactionDateMillis > 0L) { "Transaction date must be positive" }

            val docRef = db.collection("users")
                .document(uid)
                .collection("transactions")
                .document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "type" to type.name,
                "amount" to amount,
                "category" to cleanCategory,
                "description" to cleanDesc,
                "transactionDateMillis" to transactionDateMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        userId: String,
        transactionId: String,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long
    ): Result<Unit> {
        var docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireUserId(userId)
            require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "Amount out of valid range" }
            val cleanDesc = description.trim().take(200)
            val cleanCategory = category.key.take(40)
            require(cleanCategory.isNotBlank()) { "Category must not be blank" }
            require(transactionDateMillis > 0L) { "Transaction date must be positive" }

            val docRef = db.collection("users")
                .document(uid)
                .collection("transactions")
                .document(transactionId)
            docPath = docRef.path

            docRef.update(
                mapOf(
                    "type" to type.name,
                    "amount" to amount,
                    "category" to cleanCategory,
                    "description" to cleanDesc,
                    "transactionDateMillis" to transactionDateMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(
        userId: String,
        transactionId: String
    ): Result<Unit> {
        var docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireUserId(userId)
            require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }
            val docRef = db.collection("users")
                .document(uid)
                .collection("transactions")
                .document(transactionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
