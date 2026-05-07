package com.boodschappen.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.boodschappen.app.data.local.Category

// ── Color schemes ─────────────────────────────────────────────────────────────

val DarkColorScheme = darkColorScheme(
    primary            = Violet80,
    onPrimary          = Violet10,
    primaryContainer   = Violet30,
    onPrimaryContainer = Violet90,
    secondary          = Emerald80,
    onSecondary        = Emerald10,
    secondaryContainer = Emerald20,
    onSecondaryContainer = Emerald90,
    tertiary           = Rose80,
    onTertiary         = Rose10,
    tertiaryContainer  = Rose20,
    onTertiaryContainer = Rose90,
    error              = Rose80,
    errorContainer     = Rose20,
    onError            = Rose10,
    onErrorContainer   = Rose90,
    background         = Dark900,
    onBackground       = Color(0xFFF1F0FF),
    surface            = Dark800,
    onSurface          = Color(0xFFECEBFF),
    surfaceVariant     = Dark700,
    onSurfaceVariant   = Color(0xFFB8B5D4),
    outline            = Color(0xFF4A4870)
)

val LightColorScheme = lightColorScheme(
    primary            = Violet40,
    onPrimary          = Color.White,
    primaryContainer   = Violet90,
    onPrimaryContainer = Violet10,
    secondary          = Emerald40,
    onSecondary        = Color.White,
    secondaryContainer = Emerald90,
    onSecondaryContainer = Emerald10,
    tertiary           = Rose40,
    onTertiary         = Color.White,
    tertiaryContainer  = Rose90,
    onTertiaryContainer = Rose10,
    error              = Rose40,
    errorContainer     = Rose90,
    onError            = Color.White,
    onErrorContainer   = Rose10,
    background         = Violet95,
    onBackground       = Color(0xFF1A1040),
    surface            = Color.White,
    onSurface          = Color(0xFF1A1040),
    surfaceVariant     = Violet90,
    onSurfaceVariant   = Color(0xFF4A3D8F),
    outline            = Color(0xFF9D8FD4)
)

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun BoodschappenTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ── Glass effect modifier ─────────────────────────────────────────────────────

fun Modifier.glassCard(
    darkTheme: Boolean,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = if (darkTheme)
                listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
            else
                listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.50f))
        ),
        shape = shape
    )

// ── Gradient backgrounds ──────────────────────────────────────────────────────

fun gradientBackground(darkTheme: Boolean): Brush =
    if (darkTheme) {
        Brush.verticalGradient(listOf(GradDarkTop, GradDarkMid, GradDarkBot))
    } else {
        Brush.verticalGradient(listOf(GradLightTop, GradLightMid, GradLightBot))
    }

fun cardBackground(darkTheme: Boolean): Color =
    if (darkTheme) Color(0xFF1C1A3A) else Color.White.copy(alpha = 0.85f)

fun cardCheckedBackground(darkTheme: Boolean): Color =
    if (darkTheme) Color(0xFF12112A) else Color(0xFFF3F0FF)

// ── Category accent color ─────────────────────────────────────────────────────

fun categoryColor(category: String, darkTheme: Boolean): Color {
    return when (Category.fromName(category)) {
        Category.GROENTE_FRUIT -> if (darkTheme) CatGroenteDark else CatGroenteLight
        Category.ZUIVEL        -> if (darkTheme) CatZuivelDark  else CatZuivelLight
        Category.VLEES_VIS     -> if (darkTheme) CatVleesDark   else CatVleesLight
        Category.BAKKERIJ      -> if (darkTheme) CatBakkerijDark else CatBakkerijLight
        Category.DRANKEN       -> if (darkTheme) CatDrankenDark  else CatDrankenLight
        Category.DIEPVRIES     -> if (darkTheme) CatDiepvriesDark else CatDiepvriesLight
        Category.SNOEP_KOEK    -> if (darkTheme) CatSnoepDark   else CatSnoepLight
        Category.HUISHOUDEN    -> if (darkTheme) CatHuishDark   else CatHuishLight
        Category.VERZORGING    -> if (darkTheme) CatVerzDark    else CatVerzLight
        Category.OVERIG        -> if (darkTheme) CatOverigDark  else CatOverigLight
    }
}
