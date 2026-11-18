package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.core.i18n.AppLanguage
import com.example.ui.theme.ChartAmber
import com.example.ui.theme.ChartBlue
import com.example.ui.theme.ChartCyan
import com.example.ui.theme.ChartIndigo
import com.example.ui.theme.ChartOrange
import com.example.ui.theme.ChartPink
import com.example.ui.theme.ChartPurple
import com.example.ui.theme.ChartSlate
import com.example.ui.theme.ChartTeal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

enum class TransactionCategory(
    val key: String,
    val labelEn: String,
    val labelBn: String,
    val icon: ImageVector,
    val accentColor: Color,
    val isExpense: Boolean
) {
    // Expense categories
    FOOD("FOOD", "Food & Dining", "খাবার ও নাস্তা", Icons.Rounded.Restaurant, ChartOrange, true),
    GROCERIES("GROCERIES", "Bazar / Groceries", "বাজার-সদাই", Icons.Rounded.ShoppingCart, IncomeGreen, true),
    TRANSPORT("TRANSPORT", "Transport", "যাতায়াত", Icons.Rounded.DirectionsBus, ChartBlue, true),
    BILLS("BILLS", "Bills & Utilities", "বিল ও ইউটিলিটি", Icons.AutoMirrored.Rounded.ReceiptLong, ChartAmber, true),
    SHOPPING("SHOPPING", "Shopping", "কেনাকাটা", Icons.Rounded.ShoppingBag, ChartPink, true),
    HEALTH("HEALTH", "Health & Meds", "স্বাস্থ্য ও ঔষধ", Icons.Rounded.LocalHospital, ExpenseRed, true),
    EDUCATION("EDUCATION", "Education", "শিক্ষা", Icons.Rounded.School, ChartIndigo, true),
    FAMILY("FAMILY", "Family & Rent", "পরিবার ও বাসাভাড়া", Icons.Rounded.Home, ChartTeal, true),
    ENTERTAINMENT("ENTERTAINMENT", "Entertainment", "বিনোদন", Icons.Rounded.Movie, ChartPurple, true),
    OTHER("OTHER", "Other", "অন্যান্য", Icons.Rounded.Category, ChartSlate, true),

    // Income sources
    SALARY("SALARY", "Salary", "বেতন", Icons.Rounded.Work, IncomeGreen, false),
    BUSINESS("BUSINESS", "Business", "ব্যবসা", Icons.Rounded.Storefront, ChartTeal, false),
    FREELANCE("FREELANCE", "Freelance", "ফ্রিল্যান্সিং", Icons.Rounded.LaptopMac, ChartBlue, false),
    GIFT("GIFT", "Gift / Bonus", "উপহার / বোনাস", Icons.Rounded.CardGiftcard, ChartAmber, false),
    INVESTMENT("INVESTMENT", "Investment", "বিনিয়োগ", Icons.AutoMirrored.Rounded.TrendingUp, ChartCyan, false),
    INCOME("INCOME", "General Income", "সাধারণ আয়", Icons.Rounded.AccountBalanceWallet, IncomeGreen, false);

    fun localizedLabel(language: AppLanguage): String =
        if (language == AppLanguage.BN) labelBn else labelEn

    companion object {
        val expenseCategories: List<TransactionCategory>
            get() = entries.filter { it.isExpense }

        val incomeCategories: List<TransactionCategory>
            get() = entries.filter { !it.isExpense }

        fun fromKey(key: String?, fallbackType: TransactionType = TransactionType.EXPENSE): TransactionCategory {
            return entries.firstOrNull { it.key == key }
                ?: if (fallbackType == TransactionType.INCOME) INCOME else OTHER
        }
    }
}
