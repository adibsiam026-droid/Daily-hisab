package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Formatters
import com.example.core.i18n.LocalAppStrings
import com.example.ui.state.BarChartBucket
import com.example.ui.state.CategoryBreakdown
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
import kotlin.math.max

@Composable
fun IncomeExpenseBarChart(
    buckets: List<BarChartBucket>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var selectedIndex by remember(buckets) { mutableIntStateOf(-1) }
    var animateTrigger by remember(buckets) { mutableStateOf(false) }

    LaunchedEffect(buckets) {
        animateTrigger = true
    }

    val animProgress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "barChartProgress"
    )

    val maxVal = remember(buckets) {
        val peak = buckets.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
        if (peak <= 0.0) 1000.0 else peak * 1.15
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend + Hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = IncomeGreen, label = strings.incomeTab)
                LegendDot(color = ExpenseRed, label = strings.expenseTab)
            }
            Text(
                text = strings.tapBarHint,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        // Interactive Tooltip
        AnimatedVisibility(visible = selectedIndex in buckets.indices) {
            if (selectedIndex in buckets.indices) {
                val bucket = buckets[selectedIndex]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceMuted,
                    border = BorderStroke(1.dp, CardStroke),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bucket.fullLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = Formatters.formatTaka(bucket.income, language, includeSign = true),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Text(
                                text = Formatters.formatTaka(-bucket.expense, language, includeSign = true),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-axis compact scale labels
            Column(
                modifier = Modifier
                    .height(152.dp)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = Formatters.formatCompactTaka(maxVal, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Text(
                    text = Formatters.formatCompactTaka(maxVal / 2.0, language),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Text(
                    text = Formatters.toLocalizedDigits("৳0", language),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(buckets) {
                                detectTapGestures { offset ->
                                    if (buckets.isNotEmpty()) {
                                        val slotW = size.width.toFloat() / buckets.size
                                        val tapped = (offset.x / slotW).toInt()
                                            .coerceIn(0, buckets.lastIndex)
                                        selectedIndex = if (selectedIndex == tapped) -1 else tapped
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                        // Gridlines (top, mid, baseline)
                        listOf(0.05f, 0.525f, 1f).forEachIndexed { idx, frac ->
                            val y = h * frac
                            drawLine(
                                color = CardStroke,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = if (idx == 2) 2f else 1.5f,
                                pathEffect = if (idx == 2) null else dashEffect
                            )
                        }

                        if (buckets.isEmpty()) return@Canvas

                        val slotWidth = w / buckets.size
                        val barWidth = (slotWidth * 0.28f).coerceIn(10f, 34f)
                        val pairGap = (slotWidth * 0.08f).coerceIn(4f, 12f)
                        val usableHeight = h * 0.92f
                        val cornerRadius = CornerRadius(6f, 6f)

                        buckets.forEachIndexed { index, bucket ->
                            val centerX = slotWidth * index + slotWidth / 2f
                            val isSelected = index == selectedIndex

                            if (isSelected) {
                                drawRoundRect(
                                    color = SurfaceMuted.copy(alpha = 0.85f),
                                    topLeft = Offset(slotWidth * index + 4f, 0f),
                                    size = Size(slotWidth - 8f, h),
                                    cornerRadius = CornerRadius(14f, 14f)
                                )
                            }

                            val incomeHeight = ((bucket.income / maxVal).toFloat()
                                .coerceIn(0f, 1f)) * usableHeight * animProgress
                            val expenseHeight = ((bucket.expense / maxVal).toFloat()
                                .coerceIn(0f, 1f)) * usableHeight * animProgress

                            val minVisibleH = 6f
                            val incDrawH = if (bucket.income > 0.0) max(incomeHeight, minVisibleH) else 0f
                            val expDrawH = if (bucket.expense > 0.0) max(expenseHeight, minVisibleH) else 0f

                            // Income bar (green)
                            if (incDrawH > 0f) {
                                drawRoundRect(
                                    color = IncomeGreen,
                                    topLeft = Offset(
                                        x = centerX - pairGap / 2f - barWidth,
                                        y = h - incDrawH
                                    ),
                                    size = Size(barWidth, incDrawH),
                                    cornerRadius = cornerRadius
                                )
                            }

                            // Expense bar (red)
                            if (expDrawH > 0f) {
                                drawRoundRect(
                                    color = ExpenseRed,
                                    topLeft = Offset(
                                        x = centerX + pairGap / 2f,
                                        y = h - expDrawH
                                    ),
                                    size = Size(barWidth, expDrawH),
                                    cornerRadius = cornerRadius
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    buckets.forEachIndexed { idx, bucket ->
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Medium,
                            color = if (idx == selectedIndex) TextPrimary else TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    breakdown: List<CategoryBreakdown>,
    totalExpense: Double,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var animateTrigger by remember(breakdown) { mutableStateOf(false) }

    LaunchedEffect(breakdown) {
        animateTrigger = true
    }

    val sweepProgress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "donutProgress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(176.dp)) {
                val strokeWidth = 28.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                if (totalExpense <= 0.0 || breakdown.isEmpty()) {
                    drawArc(
                        color = SurfaceMuted,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    return@Canvas
                }

                var currentAngle = -90f
                val gapDegrees = if (breakdown.size > 1) 3.5f else 0f

                breakdown.forEach { item ->
                    val fraction = (item.amount / totalExpense).toFloat().coerceIn(0f, 1f)
                    val fullSweep = fraction * 360f * sweepProgress
                    val drawSweep = (fullSweep - gapDegrees).coerceAtLeast(1f)
                    drawArc(
                        color = item.category.accentColor,
                        startAngle = currentAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    currentAngle += fullSweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = strings.totalExpense,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Formatters.formatCompactTaka(totalExpense, language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            breakdown.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(item.category.accentColor)
                            )
                            Text(
                                text = item.category.localizedLabel(language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = Formatters.formatPercent(item.percentage, language),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = Formatters.formatTaka(item.amount, language),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                        color = item.category.accentColor,
                        trackColor = SurfaceMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}
