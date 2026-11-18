package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PieChartOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Formatters
import com.example.core.i18n.LocalAppStrings
import com.example.data.TransactionRecord
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.state.ReportAggregator
import com.example.ui.state.ReportPeriod
import com.example.ui.theme.CardStroke
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedBorder
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBorder
import com.example.ui.theme.IncomeGreenDeep
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TakaGold
import com.example.ui.theme.TakaGoldSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ReportsScreen(
    transactions: List<TransactionRecord>,
    selectedPeriod: ReportPeriod,
    periodOffset: Int,
    language: AppLanguage,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onStepOffset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val report = remember(transactions, selectedPeriod, periodOffset, language, strings) {
        ReportAggregator.buildPeriodReport(
            transactions = transactions,
            period = selectedPeriod,
            offset = periodOffset,
            language = language,
            strings = strings
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "title") {
                Text(
                    text = strings.reportsTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Daily / Weekly / Monthly Period Switcher
            item(key = "period_switcher") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, CardStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val periods = listOf(
                            ReportPeriod.DAILY to strings.periodDaily,
                            ReportPeriod.WEEKLY to strings.periodWeekly,
                            ReportPeriod.MONTHLY to strings.periodMonthly
                        )
                        periods.forEach { (period, label) ->
                            val selected = period == selectedPeriod
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) IncomeGreen else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectPeriod(period) }
                                    .testTag("report_period_${period.name}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) PureWhite else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Period Navigator (< Current Period >)
            item(key = "period_nav") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, CardStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onStepOffset(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("report_prev_period")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous",
                                tint = TextPrimary
                            )
                        }

                        Text(
                            text = report.periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { onStepOffset(1) },
                            enabled = periodOffset < 0,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("report_next_period")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next",
                                tint = if (periodOffset < 0) TextPrimary else TextTertiary
                            )
                        }
                    }
                }
            }

            // Green Income Card & Red Expense Card
            item(key = "report_summary_cards") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportMetricCard(
                            label = strings.totalIncome,
                            value = Formatters.formatTaka(report.totalIncome, language),
                            containerColor = IncomeGreenSoft,
                            borderColor = IncomeGreenBorder,
                            textColor = IncomeGreenDeep,
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            label = strings.totalExpense,
                            value = Formatters.formatTaka(report.totalExpense, language),
                            containerColor = ExpenseRedSoft,
                            borderColor = ExpenseRedBorder,
                            textColor = ExpenseRedDark,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val netPositive = report.netBalance >= 0
                        ReportMetricCard(
                            label = strings.netBalance,
                            value = Formatters.formatTaka(report.netBalance, language, includeSign = true),
                            containerColor = if (netPositive) IncomeGreenSoft else ExpenseRedSoft,
                            borderColor = if (netPositive) IncomeGreenBorder else ExpenseRedBorder,
                            textColor = if (netPositive) IncomeGreenDeep else ExpenseRedDark,
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            label = strings.savingsRate,
                            value = Formatters.formatPercent(report.savingsRatePercent, language),
                            containerColor = TakaGoldSoft,
                            borderColor = TakaGold.copy(alpha = 0.35f),
                            textColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Key Highlights Strip
            item(key = "highlights") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, CardStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HighlightItem(
                            label = strings.transactionCountLabel,
                            value = Formatters.toLocalizedDigits(report.transactionCount.toString(), language),
                            modifier = Modifier.weight(1f)
                        )
                        HighlightItem(
                            label = strings.avgDailySpendLabel,
                            value = Formatters.formatCompactTaka(report.avgDailySpend, language),
                            modifier = Modifier.weight(1f)
                        )
                        HighlightItem(
                            label = strings.topCategoryLabel,
                            value = report.topExpenseCategory?.category?.localizedLabel(language) ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Income vs Expense Comparison Bar Chart
            item(key = "bar_chart") {
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
                        Text(
                            text = strings.incomeVsExpenseChartTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IncomeExpenseBarChart(
                            buckets = report.barBuckets,
                            language = language
                        )
                    }
                }
            }

            // Expense Breakdown Donut Chart
            item(key = "donut_chart") {
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
                        Text(
                            text = strings.expenseByCategoryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (report.expenseBreakdown.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PieChartOutline,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = strings.noDataForPeriod,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            CategoryDonutChart(
                                breakdown = report.expenseBreakdown,
                                totalExpense = report.totalExpense,
                                language = language
                            )
                        }
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
private fun ReportMetricCard(
    label: String,
    value: String,
    containerColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HighlightItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
