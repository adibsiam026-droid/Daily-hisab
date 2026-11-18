package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Formatters
import com.example.core.i18n.LocalAppStrings
import com.example.data.TransactionCategory
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.CardStroke
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class HistoryTypeFilter {
    ALL, INCOME, EXPENSE
}

@Composable
fun HistoryScreen(
    transactions: List<TransactionRecord>,
    language: AppLanguage,
    onEditTransaction: (TransactionRecord) -> Unit,
    onDeleteTransaction: (TransactionRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(HistoryTypeFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<TransactionCategory?>(null) }
    var pendingDeleteRecord by remember { mutableStateOf<TransactionRecord?>(null) }

    val filteredTransactions = remember(transactions, searchQuery, typeFilter, categoryFilter, language) {
        val query = searchQuery.trim().lowercase()
        transactions.filter { tx ->
            val matchesType = when (typeFilter) {
                HistoryTypeFilter.ALL -> true
                HistoryTypeFilter.INCOME -> tx.parsedType == TransactionType.INCOME
                HistoryTypeFilter.EXPENSE -> tx.parsedType == TransactionType.EXPENSE
            }
            val matchesCategory = categoryFilter == null || tx.parsedCategory == categoryFilter
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                tx.description.lowercase().contains(query) ||
                    tx.parsedCategory.labelEn.lowercase().contains(query) ||
                    tx.parsedCategory.labelBn.lowercase().contains(query)
            }
            matchesType && matchesCategory && matchesSearch
        }
    }

    val filteredIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount }
    }
    val filteredExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    val groupedByDate = remember(filteredTransactions, language, strings) {
        filteredTransactions.groupBy { tx ->
            Formatters.formatDate(tx.transactionDateMillis, language, strings)
        }
    }

    if (pendingDeleteRecord != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteRecord = null },
            containerColor = PureWhite,
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pendingDeleteRecord!!
                        pendingDeleteRecord = null
                        onDeleteTransaction(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRecord = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
                Text(
                    text = strings.historyTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Search Field
            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = strings.cancel,
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IncomeGreen,
                        unfocusedBorderColor = CardStroke
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            }

            // Type Filter Chips
            item(key = "type_filters") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipButton(
                        label = strings.filterAll,
                        selected = typeFilter == HistoryTypeFilter.ALL,
                        activeColor = TextPrimary,
                        activeBg = SurfaceMuted,
                        onClick = {
                            typeFilter = HistoryTypeFilter.ALL
                            categoryFilter = null
                        },
                        modifier = Modifier.testTag("filter_all")
                    )
                    FilterChipButton(
                        label = strings.filterIncome,
                        selected = typeFilter == HistoryTypeFilter.INCOME,
                        activeColor = IncomeGreen,
                        activeBg = IncomeGreenSoft,
                        onClick = {
                            typeFilter = HistoryTypeFilter.INCOME
                            if (categoryFilter?.isExpense == true) categoryFilter = null
                        },
                        modifier = Modifier.testTag("filter_income")
                    )
                    FilterChipButton(
                        label = strings.filterExpense,
                        selected = typeFilter == HistoryTypeFilter.EXPENSE,
                        activeColor = ExpenseRed,
                        activeBg = ExpenseRedSoft,
                        onClick = {
                            typeFilter = HistoryTypeFilter.EXPENSE
                            if (categoryFilter?.isExpense == false) categoryFilter = null
                        },
                        modifier = Modifier.testTag("filter_expense")
                    )
                }
            }

            // Category Filter Chips
            item(key = "category_filters") {
                val visibleCategories = when (typeFilter) {
                    HistoryTypeFilter.INCOME -> TransactionCategory.incomeCategories
                    HistoryTypeFilter.EXPENSE -> TransactionCategory.expenseCategories
                    HistoryTypeFilter.ALL -> TransactionCategory.expenseCategories + TransactionCategory.incomeCategories
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipButton(
                        label = strings.allCategories,
                        selected = categoryFilter == null,
                        activeColor = IncomeGreen,
                        activeBg = IncomeGreenSoft,
                        onClick = { categoryFilter = null }
                    )
                    visibleCategories.forEach { cat ->
                        FilterChipButton(
                            label = cat.localizedLabel(language),
                            selected = categoryFilter == cat,
                            activeColor = if (cat.isExpense) cat.accentColor else IncomeGreen,
                            activeBg = if (cat.isExpense) cat.accentColor.copy(alpha = 0.14f) else IncomeGreenSoft,
                            onClick = {
                                categoryFilter = if (categoryFilter == cat) null else cat
                            }
                        )
                    }
                }
            }

            // Filtered Summary Strip
            item(key = "summary_strip") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, CardStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${
                                Formatters.toLocalizedDigits(
                                    filteredTransactions.size.toString(),
                                    language
                                )
                            } ${strings.itemsLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = Formatters.formatTaka(filteredIncome, language, includeSign = true),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Text(
                                text = Formatters.formatTaka(-filteredExpense, language, includeSign = true),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item(key = "empty_history") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, CardStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = strings.noMatchingTransactions,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotEmpty() || typeFilter != HistoryTypeFilter.ALL || categoryFilter != null) {
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        typeFilter = HistoryTypeFilter.ALL
                                        categoryFilter = null
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(strings.clearFilters)
                                }
                            }
                        }
                    }
                }
            } else {
                groupedByDate.forEach { (dateLabel, dayRecords) ->
                    item(key = "date_header_$dateLabel") {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(dayRecords, key = { it.id }) { record ->
                        TransactionItemCard(
                            record = record,
                            language = language,
                            onClick = { onEditTransaction(record) },
                            onDeleteClick = { pendingDeleteRecord = record }
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) activeBg else PureWhite,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) activeColor else CardStroke
        ),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeColor else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}
