package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DailyHisabColorScheme = lightColorScheme(
    primary = IncomeGreen,
    onPrimary = PureWhite,
    primaryContainer = IncomeGreenSoft,
    onPrimaryContainer = IncomeGreenDeep,
    secondary = TakaGold,
    onSecondary = PureWhite,
    secondaryContainer = TakaGoldSoft,
    onSecondaryContainer = TextPrimary,
    tertiary = ExpenseRed,
    onTertiary = PureWhite,
    tertiaryContainer = ExpenseRedSoft,
    onTertiaryContainer = ExpenseRedDark,
    background = PureWhite,
    onBackground = TextPrimary,
    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = CardStroke,
    outlineVariant = CardStroke,
    error = ExpenseRed,
    onError = PureWhite,
    errorContainer = ExpenseRedSoft,
    onErrorContainer = ExpenseRedDark
)

private val DailyHisabShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DailyHisabTheme(
    useBengaliFont: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DailyHisabColorScheme,
        typography = createAppTypography(useBengaliFont),
        shapes = DailyHisabShapes,
        content = content
    )
}
