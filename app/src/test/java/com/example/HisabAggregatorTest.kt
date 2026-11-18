package com.example

import com.example.core.i18n.AppLanguage
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.EnglishStrings
import com.example.core.i18n.Formatters
import com.example.data.TransactionCategory
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.state.ReportAggregator
import com.example.ui.state.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class HisabAggregatorTest {

    @Test
    fun `formatTaka uses Bangladeshi grouping and Bengali numerals`() {
        assertEquals("৳1,25,000", Formatters.formatTaka(125000.0, AppLanguage.EN))
        assertEquals("৳১,২৫,০০০", Formatters.formatTaka(125000.0, AppLanguage.BN))
        assertEquals("+৳500.50", Formatters.formatTaka(500.5, AppLanguage.EN, includeSign = true))
        assertEquals("-৳২৫০", Formatters.formatTaka(-250.0, AppLanguage.BN, includeSign = true))
    }

    @Test
    fun `dashboard and period reports calculate totals, balance, and category breakdown`() {
        val now = 1_763_474_400_000L // Fixed timestamp
        val records = listOf(
            TransactionRecord(
                id = "1",
                userId = "u1",
                type = TransactionType.INCOME.name,
                amount = 20000.0,
                category = TransactionCategory.SALARY.key,
                description = "Salary",
                transactionDateMillis = now
            ),
            TransactionRecord(
                id = "2",
                userId = "u1",
                type = TransactionType.EXPENSE.name,
                amount = 1500.0,
                category = TransactionCategory.FOOD.key,
                description = "Lunch",
                transactionDateMillis = now
            ),
            TransactionRecord(
                id = "3",
                userId = "u1",
                type = TransactionType.EXPENSE.name,
                amount = 500.0,
                category = TransactionCategory.TRANSPORT.key,
                description = "CNG",
                transactionDateMillis = now
            )
        )

        val summary = ReportAggregator.buildDashboardSummary(records, AppLanguage.EN, now)
        assertEquals(20000.0, summary.totalIncomeAllTime, 0.01)
        assertEquals(2000.0, summary.totalExpenseAllTime, 0.01)
        assertEquals(18000.0, summary.remainingBalance, 0.01)

        val dailyReport = ReportAggregator.buildPeriodReport(
            transactions = records,
            period = ReportPeriod.DAILY,
            offset = 0,
            language = AppLanguage.BN,
            strings = BengaliStrings,
            nowMillis = now
        )
        assertEquals(20000.0, dailyReport.totalIncome, 0.01)
        assertEquals(2000.0, dailyReport.totalExpense, 0.01)
        assertEquals(18000.0, dailyReport.netBalance, 0.01)
        assertEquals(90, dailyReport.savingsRatePercent)
        assertEquals(TransactionCategory.FOOD, dailyReport.topExpenseCategory?.category)
        assertEquals(75, dailyReport.expenseBreakdown.first().percentage)

        val weeklyReport = ReportAggregator.buildPeriodReport(
            transactions = records,
            period = ReportPeriod.WEEKLY,
            offset = 0,
            language = AppLanguage.EN,
            strings = EnglishStrings,
            nowMillis = now
        )
        assertEquals(7, weeklyReport.barBuckets.size)
        assertEquals(3, weeklyReport.transactionCount)
    }
}
