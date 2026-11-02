package com.shelflife.ai.presentation.scanner

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal adaptive viewfinder overlay that scales responsively across phones, compact
 * cover screens, and large foldable displays.
 */
@Composable
fun ScannerReticle(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "laserTransition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserProgress"
    )

    val maxReticleWidthPx = with(density) { 320.dp.toPx() }
    val maxReticleHeightPx = with(density) { 260.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Adaptive sizing: bounded width and height so it looks sharp on any aspect ratio
            val rectWidth = minOf(canvasWidth * 0.82f, maxReticleWidthPx)
            val rectHeight = minOf(rectWidth * 0.72f, maxReticleHeightPx, canvasHeight * 0.5f)
            val left = (canvasWidth - rectWidth) / 2f
            val top = (canvasHeight - rectHeight) / 2.3f
            val right = left + rectWidth
            val bottom = top + rectHeight

            // Darkened vignette background outside cutout
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                )
            }

            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.58f))
            }

            // High-visibility green corner brackets
            val cornerLength = minOf(44f, rectWidth * 0.18f)
            val strokeWidth = 6f
            val cornerColor = Color(0xFF00E676) // Vivid Accent Green

            // Top-Left Corner
            drawLine(
                color = cornerColor,
                start = Offset(left, top + cornerLength),
                end = Offset(left, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Top-Right Corner
            drawLine(
                color = cornerColor,
                start = Offset(right - cornerLength, top),
                end = Offset(right, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, top),
                end = Offset(right, top + cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Left Corner
            drawLine(
                color = cornerColor,
                start = Offset(left, bottom - cornerLength),
                end = Offset(left, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(left, bottom),
                end = Offset(left + cornerLength, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Right Corner
            drawLine(
                color = cornerColor,
                start = Offset(right - cornerLength, bottom),
                end = Offset(right, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = cornerColor,
                start = Offset(right, bottom),
                end = Offset(right, bottom - cornerLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Dynamic Scanning Laser Line with Glow
            val laserY = top + (rectHeight * laserProgress)
            // Outer glow
            drawLine(
                color = Color(0xFF00E676).copy(alpha = 0.35f),
                start = Offset(left + 12f, laserY),
                end = Offset(right - 12f, laserY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            // Core beam
            drawLine(
                color = Color.White.copy(alpha = 0.95f),
                start = Offset(left + 20f, laserY),
                end = Offset(right - 20f, laserY),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}
