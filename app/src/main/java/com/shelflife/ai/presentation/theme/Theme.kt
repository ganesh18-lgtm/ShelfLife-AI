package com.shelflife.ai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.shelflife.ai.domain.model.ExpiryUrgency

/**
 * Crisp Minimalist Slate Color Scheme (Scandinavian-inspired modern utilitarian baseline).
 */
private val CrispMinimalistSlateColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmerald,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldDark,
    secondary = SlateNavy,
    onSecondary = Color.White,
    secondaryContainer = SurfaceSubtle,
    onSecondaryContainer = SlateNavy,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    tertiaryContainer = WarningAmberSoft,
    onTertiaryContainer = WarningAmberDark,
    background = BackgroundCanvas,
    onBackground = SlateNavy,
    surface = SurfaceWhite,
    onSurface = SlateNavy,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = SlateText,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderHover,
    error = CriticalCrimson,
    onError = Color.White,
    errorContainer = CriticalCrimsonSoft,
    onErrorContainer = CriticalCrimsonDark
)

data class UrgencyStyle(
    val mainColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val gradient: Brush
)

@Composable
fun getUrgencyStyle(urgency: ExpiryUrgency): UrgencyStyle {
    return when (urgency) {
        ExpiryUrgency.EXPIRED -> UrgencyStyle(
            mainColor = ExpiredDarkRed,
            containerColor = ExpiredDarkRedContainer,
            borderColor = ExpiredDarkRed,
            gradient = CriticalGradient
        )
        ExpiryUrgency.CRITICAL -> UrgencyStyle(
            mainColor = CriticalCrimson,
            containerColor = CriticalCrimsonSoft,
            borderColor = CriticalCrimsonBorder,
            gradient = CriticalGradient
        )
        ExpiryUrgency.WARNING -> UrgencyStyle(
            mainColor = WarningAmberDark,
            containerColor = WarningAmberSoft,
            borderColor = WarningAmberBorder,
            gradient = WarningGradient
        )
        ExpiryUrgency.FRESH -> UrgencyStyle(
            mainColor = EmeraldPrimary,
            containerColor = EmeraldSoft,
            borderColor = EmeraldBorder,
            gradient = FreshGradient
        )
    }
}

@Composable
fun ShelfLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Model Design Standard: Crisp Minimalist Slate
    MaterialTheme(
        colorScheme = CrispMinimalistSlateColorScheme,
        typography = Typography,
        content = content
    )
}
