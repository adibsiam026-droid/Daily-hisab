package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Formatters
import com.example.core.i18n.LocalAppStrings
import com.example.data.TransactionCategory
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.theme.CardStroke
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    initialType: TransactionType,
    editingRecord: TransactionRecord?,
    language: AppLanguage,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        existingId: String?,
        type: TransactionType,
        amount: Double,
        category: TransactionCategory,
        description: String,
        transactionDateMillis: Long
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember(editingRecord, initialType) {
        mutableStateOf(editingRecord?.parsedType ?: initialType)
    }
    var amountText by remember(editingRecord) {
        mutableStateOf(
            editingRecord?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var description by remember(editingRecord) {
        mutableStateOf(editingRecord?.description ?: "")
    }
    var selectedCategory by remember(editingRecord, selectedType) {
        val initialCat = editingRecord?.parsedCategory
        mutableStateOf(
            when {
                initialCat != null && (initialCat.isExpense == (selectedType == TransactionType.EXPENSE)) -> initialCat
                selectedType == TransactionType.INCOME -> TransactionCategory.SALARY
                else -> TransactionCategory.FOOD
            }
        )
    }
    var selectedDateMillis by remember(editingRecord) {
        mutableLongStateOf(editingRecord?.transactionDateMillis ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val accentColor = if (selectedType == TransactionType.INCOME) IncomeGreen else ExpenseRed
    val softColor = if (selectedType == TransactionType.INCOME) IncomeGreenSoft else ExpenseRedSoft

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToUtcPickerMillis(selectedDateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            selectedDateMillis = utcPickerMillisToLocalMillis(utcMillis, selectedDateMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRecord == null) strings.addTransactionTitle else strings.editTransactionTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.cancel,
                        tint = TextSecondary
                    )
                }
            }

            // Type Switcher (Income Green / Expense Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TypeSelectorPill(
                    label = strings.incomeTab,
                    icon = Icons.Rounded.ArrowUpward,
                    selected = selectedType == TransactionType.INCOME,
                    activeColor = IncomeGreen,
                    activeBg = IncomeGreenSoft,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        validationError = null
                        if (selectedCategory.isExpense) {
                            selectedCategory = TransactionCategory.SALARY
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_type_income")
                )
                TypeSelectorPill(
                    label = strings.expenseTab,
                    icon = Icons.Rounded.ArrowDownward,
                    selected = selectedType == TransactionType.EXPENSE,
                    activeColor = ExpenseRed,
                    activeBg = ExpenseRedSoft,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        validationError = null
                        if (!selectedCategory.isExpense) {
                            selectedCategory = TransactionCategory.FOOD
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_type_expense")
                )
            }

            // Amount Input (৳)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val cleaned = sanitizeDecimalInput(raw)
                        amountText = cleaned
                        validationError = null
                    },
                    label = { Text(strings.amountLabel) },
                    placeholder = { Text(strings.amountHint) },
                    prefix = {
                        Text(
                            text = "৳ ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = CardStroke,
                        focusedLabelColor = accentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_amount_input")
                )

                // Quick-Add Taka Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 500, 1000, 5000).forEach { quickVal ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = softColor,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val updated = current + quickVal
                                    amountText = if (updated % 1.0 == 0.0) {
                                        updated.toLong().toString()
                                    } else {
                                        updated.toString()
                                    }
                                    validationError = null
                                }
                        ) {
                            Text(
                                text = Formatters.toLocalizedDigits("+৳$quickVal", language),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Date Picker Row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceMuted,
                border = BorderStroke(1.dp, CardStroke),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true }
                    .testTag("form_date_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = strings.dateLabel,
                            tint = accentColor
                        )
                        Text(
                            text = strings.dateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = Formatters.formatDate(selectedDateMillis, language, strings),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            // Description / Note (Primary for Income, Optional Note for Expense)
            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length <= 200) {
                        description = it
                        validationError = null
                    }
                },
                label = {
                    Text(
                        if (selectedType == TransactionType.INCOME) {
                            strings.descriptionLabel
                        } else {
                            strings.noteOptionalLabel
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.INCOME) {
                            strings.descriptionRequiredHint
                        } else {
                            strings.noteOptionalHint
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = CardStroke,
                    focusedLabelColor = accentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_description_input")
            )

            // Category / Income Source Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (selectedType == TransactionType.EXPENSE) {
                        strings.categoryLabel
                    } else {
                        strings.incomeSourceLabel
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )

                val categories = if (selectedType == TransactionType.EXPENSE) {
                    TransactionCategory.expenseCategories
                } else {
                    TransactionCategory.incomeCategories
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val chipColor = if (selectedType == TransactionType.INCOME) IncomeGreen else cat.accentColor
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) chipColor.copy(alpha = 0.14f) else SurfaceMuted,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chipColor else CardStroke
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = cat }
                                .testTag("category_chip_${cat.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) chipColor else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = cat.localizedLabel(language),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) chipColor else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (validationError != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ExpenseRedSoft,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0.0) {
                        validationError = strings.errorInvalidAmount
                        return@Button
                    }
                    if (selectedType == TransactionType.INCOME && description.trim().isEmpty()) {
                        validationError = strings.errorDescriptionRequired
                        return@Button
                    }
                    onSubmit(
                        editingRecord?.id,
                        selectedType,
                        parsedAmount,
                        selectedCategory,
                        description.trim(),
                        selectedDateMillis
                    )
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("form_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = PureWhite,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = when {
                            editingRecord != null -> strings.updateEntry
                            selectedType == TransactionType.INCOME -> strings.saveIncome
                            else -> strings.saveExpense
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TypeSelectorPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    activeBg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) activeBg else SurfaceMuted,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) activeColor else CardStroke
        ),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) activeColor else CardStroke),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeColor else TextSecondary
            )
        }
    }
}

private fun sanitizeDecimalInput(input: String): String {
    val builder = StringBuilder()
    var hasDot = false
    var decimalCount = 0
    for (ch in input) {
        if (ch in '0'..'9') {
            if (hasDot) {
                if (decimalCount < 2) {
                    builder.append(ch)
                    decimalCount++
                }
            } else if (builder.length < 10) {
                builder.append(ch)
            }
        } else if (ch == '.' && !hasDot) {
            hasDot = true
            if (builder.isEmpty()) builder.append('0')
            builder.append('.')
        }
    }
    return builder.toString()
}

private fun localMillisToUtcPickerMillis(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }
    return utcCal.timeInMillis
}

private fun utcPickerMillisToLocalMillis(utcMillis: Long, preserveTimeFromMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    val timeCal = Calendar.getInstance().apply { timeInMillis = preserveTimeFromMillis }
    val resultCal = Calendar.getInstance().apply {
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            timeCal.get(Calendar.HOUR_OF_DAY),
            timeCal.get(Calendar.MINUTE),
            timeCal.get(Calendar.SECOND)
        )
    }
    return resultCal.timeInMillis
}
