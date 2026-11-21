package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

data class MarketExtendedColors(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val cardBorder: Color,
    val elevatedCard: Color,
    val bullish: Color,
    val bearish: Color,
    val warning: Color,
    val tabGlow: Color
)

val LocalMarketExtendedColors = staticCompositionLocalOf {
    MarketExtendedColors(
        isDark = true,
        backgroundBrush = Brush.verticalGradient(listOf(NavyDeep, NavyMid, PurpleDusk)),
        cardBorder = CardBorderDark,
        elevatedCard = CardDarkElevated,
        bullish = TealMint,
        bearish = CoralRed,
        warning = AmberGold,
        tabGlow = TealGlowSoft
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = TealMint,
    onPrimary = Color(0xFF00261D),
    primaryContainer = Color(0xFF073B33),
    onPrimaryContainer = Color(0xFF8CFFDF),
    secondary = VioletGlow,
    onSecondary = Color(0xFF140C38),
    secondaryContainer = Color(0xFF28205A),
    onSecondaryContainer = Color(0xFFDAD4FF),
    tertiary = AmberGold,
    onTertiary = Color(0xFF2B1C00),
    background = NavyDeep,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDarkElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderDark,
    error = CoralRed,
    onError = Color(0xFF2F000A)
)

private val LightColorScheme = lightColorScheme(
    primary = TealLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F7EC),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = VioletLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E2FF),
    onSecondaryContainer = Color(0xFF211461),
    tertiary = Color(0xFFD17E00),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLightElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = CardBorderLight,
    error = Color(0xFFD62849),
    onError = Color.White
)

private val MarketShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MarketChartZoneTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        MarketExtendedColors(
            isDark = true,
            backgroundBrush = Brush.verticalGradient(listOf(NavyDeep, NavyMid, PurpleDusk)),
            cardBorder = CardBorderDark,
            elevatedCard = CardDarkElevated,
            bullish = TealMint,
            bearish = CoralRed,
            warning = AmberGold,
            tabGlow = TealGlowSoft
        )
    } else {
        MarketExtendedColors(
            isDark = false,
            backgroundBrush = Brush.verticalGradient(listOf(LightBg, LightBgBottom)),
            cardBorder = CardBorderLight,
            elevatedCard = CardLightElevated,
            bullish = TealLightPrimary,
            bearish = Color(0xFFD62849),
            warning = Color(0xFFD17E00),
            tabGlow = Color(0x22009B77)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalMarketExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MarketChartTypography,
            shapes = MarketShapes,
            content = content
        )
    }
}
