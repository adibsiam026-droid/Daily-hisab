package com.example.core.i18n

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {
    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val bengaliMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val bengaliShortMonths = listOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন",
        "জুলা", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )

    private val bengaliShortWeekdays = mapOf(
        Calendar.SATURDAY to "শনি",
        Calendar.SUNDAY to "রবি",
        Calendar.MONDAY to "সোম",
        Calendar.TUESDAY to "মঙ্গল",
        Calendar.WEDNESDAY to "বুধ",
        Calendar.THURSDAY to "বৃহ",
        Calendar.FRIDAY to "শুক্র"
    )

    fun toLocalizedDigits(text: String, language: AppLanguage): String {
        if (language == AppLanguage.EN) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Formats amount in Bangladeshi Taka (৳) with South Asian grouping (e.g. 1,25,000).
     */
    fun formatTaka(
        amount: Double,
        language: AppLanguage,
        includeSign: Boolean = false
    ): String {
        val isNegative = amount < -0.004
        val absVal = abs(amount)
        val totalPaisa = (absVal * 100.0).roundToLong()
        val whole = totalPaisa / 100L
        val paisa = (totalPaisa % 100L).toInt()

        val groupedWhole = formatSouthAsianGrouping(whole)
        val numberStr = if (paisa == 0) {
            groupedWhole
        } else {
            val paisaStr = paisa.toString().padStart(2, '0')
            "$groupedWhole.$paisaStr"
        }

        val prefix = when {
            isNegative -> "-৳"
            includeSign && absVal > 0.004 -> "+৳"
            else -> "৳"
        }
        return toLocalizedDigits("$prefix$numberStr", language)
    }

    fun formatCompactTaka(amount: Double, language: AppLanguage): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        val raw = when {
            absVal >= 10000000.0 -> {
                val cr = absVal / 10000000.0
                if (language == AppLanguage.BN) "${trimDecimal(cr)} কো" else "${trimDecimal(cr)}Cr"
            }
            absVal >= 100000.0 -> {
                val lakh = absVal / 100000.0
                if (language == AppLanguage.BN) "${trimDecimal(lakh)} লা" else "${trimDecimal(lakh)}L"
            }
            absVal >= 1000.0 -> {
                val k = absVal / 1000.0
                "${trimDecimal(k)}k"
            }
            else -> absVal.roundToLong().toString()
        }
        return toLocalizedDigits("${sign}৳$raw", language)
    }

    private fun trimDecimal(value: Double): String {
        val rounded = (value * 10.0).roundToLong() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private fun formatSouthAsianGrouping(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)
        val groups = mutableListOf<String>()
        var idx = remaining.length
        while (idx > 0) {
            val start = maxOf(0, idx - 2)
            groups.add(0, remaining.substring(start, idx))
            idx = start
        }
        return groups.joinToString(",") + "," + lastThree
    }

    fun formatPercent(value: Int, language: AppLanguage): String {
        return toLocalizedDigits("$value%", language)
    }

    fun formatDate(millis: Long, language: AppLanguage, strings: AppStrings): String {
        val targetCal = Calendar.getInstance().apply { timeInMillis = millis }
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(targetCal, todayCal) -> strings.todayLabel
            isSameDay(targetCal, yesterdayCal) -> strings.yesterdayLabel
            else -> formatAbsoluteDate(millis, language)
        }
    }

    fun formatAbsoluteDate(millis: Long, language: AppLanguage): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return if (language == AppLanguage.EN) {
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(millis))
        } else {
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = bengaliShortMonths[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            toLocalizedDigits("$day $month $year", AppLanguage.BN)
        }
    }

    fun formatShortDayMonth(millis: Long, language: AppLanguage): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return if (language == AppLanguage.EN) {
            SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(millis))
        } else {
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = bengaliShortMonths[cal.get(Calendar.MONTH)]
            toLocalizedDigits("$day $month", AppLanguage.BN)
        }
    }

    fun formatWeekdayShort(millis: Long, language: AppLanguage): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return if (language == AppLanguage.EN) {
            SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(millis))
        } else {
            bengaliShortWeekdays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        }
    }

    fun formatMonthYear(cal: Calendar, language: AppLanguage): String {
        return if (language == AppLanguage.EN) {
            SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
        } else {
            val month = bengaliMonths[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            toLocalizedDigits("$month $year", AppLanguage.BN)
        }
    }

    fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    fun startOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
