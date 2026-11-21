package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalGuestTransactionRepository
import com.example.data.TransactionCategory
import com.example.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalGuestTransactionRepositoryTest {

    @Test
    fun `guest repository persists add, update, and delete on device`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalGuestTransactionRepository(context)
        repo.clearAll()

        val now = 1_763_474_400_000L
        val addRes = repo.addTransaction(
            userId = LocalGuestTransactionRepository.GUEST_USER_ID,
            type = TransactionType.INCOME,
            amount = 5000.0,
            category = TransactionCategory.FREELANCE,
            description = "Design work",
            transactionDateMillis = now
        )
        assertTrue(addRes.isSuccess)

        val reloaded = LocalGuestTransactionRepository(context)
        val initialList = reloaded.observeTransactions(LocalGuestTransactionRepository.GUEST_USER_ID).first()
        assertEquals(1, initialList.size)
        val created = initialList.first()
        assertEquals(5000.0, created.amount, 0.01)

        val updateRes = reloaded.updateTransaction(
            userId = LocalGuestTransactionRepository.GUEST_USER_ID,
            transactionId = created.id,
            type = TransactionType.EXPENSE,
            amount = 750.0,
            category = TransactionCategory.FOOD,
            description = "Dinner",
            transactionDateMillis = now
        )
        assertTrue(updateRes.isSuccess)

        val afterUpdate = reloaded.observeTransactions(LocalGuestTransactionRepository.GUEST_USER_ID).first()
        assertEquals(TransactionType.EXPENSE, afterUpdate.first().parsedType)
        assertEquals(750.0, afterUpdate.first().amount, 0.01)

        val deleteRes = reloaded.deleteTransaction(LocalGuestTransactionRepository.GUEST_USER_ID, created.id)
        assertTrue(deleteRes.isSuccess)
        assertTrue(reloaded.getSnapshot().isEmpty())
    }
}
