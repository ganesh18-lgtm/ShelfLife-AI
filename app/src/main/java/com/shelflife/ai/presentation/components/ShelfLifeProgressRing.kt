package com.shelflife.ai.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.presentation.theme.CriticalCrimson
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.WarningAmber

/**
 * Radial Canvas progress ring rendering the exact remaining shelf-life percentage
 * with pulsing alert animation for Critical urgency (< 48 hours).
 */
@Composable
fun ShelfLifeProgressRing(
    remainingPercentage: Int, // 0 to 100
    urgency: ExpiryUrgency,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp
) {
    val clamped = remainingPercentage.coerceIn(0, 100)
    val sweepAngle = 360f * (clamped / 100f)

    val isCritical = urgency == ExpiryUrgency.CRITICAL || urgency == ExpiryUrgency.EXPIRED
    val transition = rememberInfiniteTransition(label = "ringPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = if (isCritical) 0.6f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Background Track
            drawArc(
                color = SurfaceBorder,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            // Dynamic Progress Sweep Arc
            if (sweepAngle > 0f) {
                drawArc(
                    color = accentColor.copy(alpha = pulseAlpha),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Percentage Text Label inside ring
        Text(
            text = "$clamped%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (size < 40.dp) 8.5.sp else 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = accentColor.copy(alpha = pulseAlpha)
        )
    }
}

/**
 * Overloaded variant computing exact remaining shelf-life percentage from timestamps.
 */
@Composable
fun ShelfLifeProgressRing(
    expiryTimestamp: Long,
    urgency: ExpiryUrgency,
    modifier: Modifier = Modifier,
    addedTimestamp: Long = System.currentTimeMillis() - 7L * 24 * 3600 * 1000L,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp
) {
    val now = System.currentTimeMillis()
    val totalLife = (expiryTimestamp - addedTimestamp).coerceAtLeast(1L)
    val remainingLife = (expiryTimestamp - now).coerceAtLeast(0L)
    val percentage = ((remainingLife.toFloat() / totalLife.toFloat()) * 100f).toInt().coerceIn(0, 100)

    val accent = when (urgency) {
        ExpiryUrgency.CRITICAL, ExpiryUrgency.EXPIRED -> CriticalCrimson
        ExpiryUrgency.WARNING -> WarningAmber
        ExpiryUrgency.FRESH -> EmeraldPrimary
    }

    ShelfLifeProgressRing(
        remainingPercentage = percentage,
        urgency = urgency,
        accentColor = accent,
        modifier = modifier,
        size = size,
        strokeWidth = strokeWidth
    )
}
