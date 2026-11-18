package com.example.ui.state

import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppStrings
import com.example.core.i18n.Formatters
import com.example.data.TransactionCategory
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import java.util.Calendar
import kotlin.math.roundToInt

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class BarChartBucket(
    val label: String,
    val fullLabel: String,
    val income: Double,
    val expense: Double
)

data class CategoryBreakdown(
    val category: TransactionCategory,
    val amount: Double,
    val percentage: Int
)

data class PeriodReport(
    val period: ReportPeriod,
    val periodLabel: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val savingsRatePercent: Int,
    val transactionCount: Int,
    val avgDailySpend: Double,
    val topExpenseCategory: CategoryBreakdown?,
    val barBuckets: List<BarChartBucket>,
    val expenseBreakdown: List<CategoryBreakdown>
)

data class DashboardSummary(
    val totalIncomeAllTime: Double,
    val totalExpenseAllTime: Double,
    val remainingBalance: Double,
    val todayIncome: Double,
    val todayExpense: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val last7DaysBuckets: List<BarChartBucket>
)

object ReportAggregator {

    fun buildDashboardSummary(
        transactions: List<TransactionRecord>,
        language: AppLanguage,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardSummary {
        var totalInc = 0.0
        var totalExp = 0.0
        var todayInc = 0.0
        var todayExp = 0.0
        var monthInc = 0.0
        var monthExp = 0.0

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)

        for (tx in transactions) {
            val isIncome = tx.parsedType == TransactionType.INCOME
            if (isIncome) totalInc += tx.amount else totalExp += tx.amount

            val txCal = Calendar.getInstance().apply { timeInMillis = tx.transactionDateMillis }
            if (Formatters.isSameDay(txCal, nowCal)) {
                if (isIncome) todayInc += tx.amount else todayExp += tx.amount
            }
            if (txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth) {
                if (isIncome) monthInc += tx.amount else monthExp += tx.amount
            }
        }

        val last7Days = buildTrailing7DayBuckets(transactions, language, nowMillis)

        return DashboardSummary(
            totalIncomeAllTime = totalInc,
            totalExpenseAllTime = totalExp,
            remainingBalance = totalInc - totalExp,
            todayIncome = todayInc,
            todayExpense = todayExp,
            monthIncome = monthInc,
            monthExpense = monthExp,
            last7DaysBuckets = last7Days
        )
    }

    private fun buildTrailing7DayBuckets(
        transactions: List<TransactionRecord>,
        language: AppLanguage,
        nowMillis: Long
    ): List<BarChartBucket> {
        val buckets = mutableListOf<BarChartBucket>()
        for (daysAgo in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val start = Formatters.startOfDayMillis(dayCal.timeInMillis)
            val end = Formatters.endOfDayMillis(dayCal.timeInMillis)
            var inc = 0.0
            var exp = 0.0
            for (tx in transactions) {
                if (tx.transactionDateMillis in start..end) {
                    if (tx.parsedType == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                }
            }
            buckets.add(
                BarChartBucket(
                    label = Formatters.formatWeekdayShort(dayCal.timeInMillis, language),
                    fullLabel = Formatters.formatShortDayMonth(dayCal.timeInMillis, language),
                    income = inc,
                    expense = exp
                )
            )
        }
        return buckets
    }

    fun buildPeriodReport(
        transactions: List<TransactionRecord>,
        period: ReportPeriod,
        offset: Int,
        language: AppLanguage,
        strings: AppStrings,
        nowMillis: Long = System.currentTimeMillis()
    ): PeriodReport {
        val (rangeStart, rangeEnd, periodLabel) = computePeriodRange(
            period = period,
            offset = offset,
            language = language,
            strings = strings,
            nowMillis = nowMillis
        )

        val filtered = transactions.filter { it.transactionDateMillis in rangeStart..rangeEnd }
        var totalInc = 0.0
        var totalExp = 0.0
        val expenseByCat = mutableMapOf<TransactionCategory, Double>()

        for (tx in filtered) {
            if (tx.parsedType == TransactionType.INCOME) {
                totalInc += tx.amount
            } else {
                totalExp += tx.amount
                val cat = tx.parsedCategory
                expenseByCat[cat] = (expenseByCat[cat] ?: 0.0) + tx.amount
            }
        }

        val net = totalInc - totalExp
        val savingsRate = if (totalInc > 0.0) {
            (((totalInc - totalExp) / totalInc) * 100.0).roundToInt().coerceIn(-999, 100)
        } else 0

        val breakdown = expenseByCat.entries
            .sortedByDescending { it.value }
            .map { (cat, amt) ->
                val pct = if (totalExp > 0.0) ((amt / totalExp) * 100.0).roundToInt() else 0
                CategoryBreakdown(category = cat, amount = amt, percentage = pct)
            }

        val dayCount = when (period) {
            ReportPeriod.DAILY -> 1
            ReportPeriod.WEEKLY -> 7
            ReportPeriod.MONTHLY -> {
                val cal = Calendar.getInstance().apply { timeInMillis = rangeStart }
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        }
        val avgDailySpend = if (dayCount > 0) totalExp / dayCount else totalExp

        val buckets = when (period) {
            ReportPeriod.DAILY -> buildDailyChartBuckets(filtered, rangeStart, language)
            ReportPeriod.WEEKLY -> buildWeeklyChartBuckets(filtered, rangeStart, language)
            ReportPeriod.MONTHLY -> buildMonthlyChartBuckets(filtered, rangeStart, language)
        }

        return PeriodReport(
            period = period,
            periodLabel = periodLabel,
            totalIncome = totalInc,
            totalExpense = totalExp,
            netBalance = net,
            savingsRatePercent = savingsRate,
            transactionCount = filtered.size,
            avgDailySpend = avgDailySpend,
            topExpenseCategory = breakdown.firstOrNull(),
            barBuckets = buckets,
            expenseBreakdown = breakdown
        )
    }

    private fun computePeriodRange(
        period: ReportPeriod,
        offset: Int,
        language: AppLanguage,
        strings: AppStrings,
        nowMillis: Long
    ): Triple<Long, Long, String> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (period) {
            ReportPeriod.DAILY -> {
                cal.add(Calendar.DAY_OF_YEAR, offset)
                val start = Formatters.startOfDayMillis(cal.timeInMillis)
                val end = Formatters.endOfDayMillis(cal.timeInMillis)
                val label = when (offset) {
                    0 -> "${strings.todayLabel} • ${Formatters.formatShortDayMonth(start, language)}"
                    -1 -> "${strings.yesterdayLabel} • ${Formatters.formatShortDayMonth(start, language)}"
                    else -> Formatters.formatAbsoluteDate(start, language)
                }
                Triple(start, end, label)
            }

            ReportPeriod.WEEKLY -> {
                // 7-day window ending on Sunday or Saturday; align week start to Saturday (BD standard) or Monday
                cal.firstDayOfWeek = Calendar.SATURDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                if (cal.timeInMillis > nowMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                }
                cal.add(Calendar.WEEK_OF_YEAR, offset)
                val start = Formatters.startOfDayMillis(cal.timeInMillis)
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = start
                    add(Calendar.DAY_OF_YEAR, 6)
                }
                val end = Formatters.endOfDayMillis(endCal.timeInMillis)
                val label = "${Formatters.formatShortDayMonth(start, language)} – ${
                    Formatters.formatShortDayMonth(end, language)
                }"
                Triple(start, end, label)
            }

            ReportPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, offset)
                val start = Formatters.startOfDayMillis(cal.timeInMillis)
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, lastDay)
                val end = Formatters.endOfDayMillis(cal.timeInMillis)
                val label = Formatters.formatMonthYear(cal, language)
                Triple(start, end, label)
            }
        }
    }

    private fun buildDailyChartBuckets(
        dayTxs: List<TransactionRecord>,
        dayStartMillis: Long,
        language: AppLanguage
    ): List<BarChartBucket> {
        // Group into 4 clear parts of day + or top categories; 4 time-of-day blocks is clean and intuitive
        val labelsEn = listOf("Morning", "Noon", "Evening", "Night")
        val labelsBn = listOf("সকাল", "দুপুর", "বিকাল", "রাত")
        val ranges = listOf(0..11, 12..15, 16..19, 20..23)

        return ranges.mapIndexed { idx, hourRange ->
            var inc = 0.0
            var exp = 0.0
            for (tx in dayTxs) {
                val hour = Calendar.getInstance().apply { timeInMillis = tx.transactionDateMillis }
                    .get(Calendar.HOUR_OF_DAY)
                if (hour in hourRange) {
                    if (tx.parsedType == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                }
            }
            val lbl = if (language == AppLanguage.BN) labelsBn[idx] else labelsEn[idx]
            BarChartBucket(
                label = lbl,
                fullLabel = "$lbl (${Formatters.formatShortDayMonth(dayStartMillis, language)})",
                income = inc,
                expense = exp
            )
        }
    }

    private fun buildWeeklyChartBuckets(
        weekTxs: List<TransactionRecord>,
        weekStartMillis: Long,
        language: AppLanguage
    ): List<BarChartBucket> {
        return (0..6).map { dayOffset ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = weekStartMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val start = Formatters.startOfDayMillis(cal.timeInMillis)
            val end = Formatters.endOfDayMillis(cal.timeInMillis)
            var inc = 0.0
            var exp = 0.0
            for (tx in weekTxs) {
                if (tx.transactionDateMillis in start..end) {
                    if (tx.parsedType == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                }
            }
            BarChartBucket(
                label = Formatters.formatWeekdayShort(start, language),
                fullLabel = Formatters.formatShortDayMonth(start, language),
                income = inc,
                expense = exp
            )
        }
    }

    private fun buildMonthlyChartBuckets(
        monthTxs: List<TransactionRecord>,
        monthStartMillis: Long,
        language: AppLanguage
    ): List<BarChartBucket> {
        val startCal = Calendar.getInstance().apply { timeInMillis = monthStartMillis }
        val maxDay = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weekRanges = listOf(1..7, 8..14, 15..21, 22..maxDay)

        return weekRanges.mapIndexed { idx, days ->
            var inc = 0.0
            var exp = 0.0
            for (tx in monthTxs) {
                val dom = Calendar.getInstance().apply { timeInMillis = tx.transactionDateMillis }
                    .get(Calendar.DAY_OF_MONTH)
                if (dom in days) {
                    if (tx.parsedType == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                }
            }
            val shortLbl = if (language == AppLanguage.BN) {
                Formatters.toLocalizedDigits("সপ্তাহ ${idx + 1}", AppLanguage.BN)
            } else {
                "Wk ${idx + 1}"
            }
            val fullLbl = if (language == AppLanguage.BN) {
                Formatters.toLocalizedDigits("${days.first}-${days.last} তারিখ", AppLanguage.BN)
            } else {
                "Days ${days.first}–${days.last}"
            }
            BarChartBucket(
                label = shortLbl,
                fullLabel = fullLbl,
                income = inc,
                expense = exp
            )
        }
    }
}
