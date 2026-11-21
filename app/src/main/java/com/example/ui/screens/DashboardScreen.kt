package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Formatters
import com.example.core.i18n.LocalAppStrings
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.UserAvatarButton
import com.example.ui.state.ReportAggregator
import com.example.ui.theme.CardStroke
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBorder
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBorder
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.IncomeGreenDeep
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseUser

@Composable
fun DashboardScreen(
    transactions: List<TransactionRecord>,
    currentUser: FirebaseUser?,
    isGuest: Boolean,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpenProfile: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val summary = remember(transactions, language) {
        ReportAggregator.buildDashboardSummary(transactions, language)
    }
    val recentTransactions = remember(transactions) {
        transactions.take(6)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val greetingName = if (isGuest) {
                            strings.guestDisplayName
                        } else {
                            currentUser?.displayName
                                ?.trim()
                                ?.split(" ")
                                ?.firstOrNull()
                                ?.takeIf { it.isNotBlank() }
                        }
                        Text(
                            text = if (greetingName != null) "${strings.welcomeBack}, $greetingName" else strings.appName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isGuest) strings.guestModeBadge else strings.appTagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageToggleChip(
                            currentLanguage = language,
                            onToggle = onLanguageChange
                        )
                        UserAvatarButton(
                            user = currentUser,
                            isGuest = isGuest,
                            onClick = onOpenProfile
                        )
                    }
                }
            }

            // Net Remaining Balance Hero Card
            item(key = "balance_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(IncomeGreenDeep, IncomeGreenDark, IncomeGreen)
                            )
                        )
                        .padding(20.dp)
                        .testTag("balance_hero_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.remainingBalance,
                                style = MaterialTheme.typography.labelLarge,
                                color = PureWhite.copy(alpha = 0.86f)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PureWhite.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${strings.thisMonthLabel}: ${
                                        Formatters.formatTaka(
                                            summary.monthIncome - summary.monthExpense,
                                            language,
                                            includeSign = true
                                        )
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Text(
                            text = Formatters.formatTaka(summary.remainingBalance, language),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )

                        // Quick Action Buttons inside Hero
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAddIncome,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PureWhite,
                                    contentColor = IncomeGreenDeep
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_add_income_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.addIncome,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = onAddExpense,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ExpenseRed,
                                    contentColor = PureWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_add_expense_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.addExpense,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Green Income Card & Red Expense Card
            item(key = "income_expense_cards") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryStatCard(
                        title = strings.totalIncome,
                        amountText = Formatters.formatTaka(summary.totalIncomeAllTime, language),
                        todayLabel = "${strings.todayLabel}: ${
                            Formatters.formatTaka(summary.todayIncome, language, includeSign = true)
                        }",
                        containerColor = IncomeGreenSoft,
                        borderColor = IncomeGreenBorder,
                        accentColor = IncomeGreen,
                        darkAccentColor = IncomeGreenDeep,
                        icon = Icons.Rounded.ArrowUpward,
                        onClick = onAddIncome,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_income_card")
                    )

                    SummaryStatCard(
                        title = strings.totalExpense,
                        amountText = Formatters.formatTaka(summary.totalExpenseAllTime, language),
                        todayLabel = "${strings.todayLabel}: ${
                            Formatters.formatTaka(-summary.todayExpense, language, includeSign = true)
                        }",
                        containerColor = ExpenseRedSoft,
                        borderColor = ExpenseRedBorder,
                        accentColor = ExpenseRed,
                        darkAccentColor = ExpenseRedDark,
                        icon = Icons.Rounded.ArrowDownward,
                        onClick = onAddExpense,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_expense_card")
                    )
                }
            }

            // 7-Day Income vs Expense Chart
            item(key = "weekly_chart") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, CardStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.weeklyPulseTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            TextButton(
                                onClick = onNavigateToReports,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("dashboard_full_reports_btn")
                            ) {
                                Text(
                                    text = strings.seeFullReport,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IncomeGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IncomeExpenseBarChart(
                            buckets = summary.last7DaysBuckets,
                            language = language
                        )
                    }
                }
            }

            // Recent Transactions Header
            item(key = "recent_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentTransactions,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (transactions.isNotEmpty()) {
                        TextButton(
                            onClick = onNavigateToHistory,
                            modifier = Modifier.testTag("dashboard_view_all_btn")
                        ) {
                            Text(
                                text = strings.viewAll,
                                style = MaterialTheme.typography.labelLarge,
                                color = IncomeGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item(key = "empty_state") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, CardStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hisab_hero),
                                contentDescription = strings.emptyTransactionsTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Text(
                                text = strings.emptyTransactionsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.emptyTransactionsSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentTransactions, key = { it.id }) { record ->
                    TransactionItemCard(
                        record = record,
                        language = language,
                        onClick = { onEditTransaction(record) }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    amountText: String,
    todayLabel: String,
    containerColor: Color,
    borderColor: Color,
    accentColor: Color,
    darkAccentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = darkAccentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = darkAccentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = todayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
