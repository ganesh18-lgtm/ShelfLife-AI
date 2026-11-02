package com.shelflife.ai.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.shelflife.ai.presentation.theme.FreshEmerald
import com.shelflife.ai.presentation.theme.NeonMint

/**
 * Empty State: Animated floating refrigerator Lottie animation.
 */
@Composable
fun EmptyPantryLottieAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(FRIDGE_LOTTIE_JSON)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.85f
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress }
            )
        } else {
            ProceduralFloatingFridge(size = size)
        }
    }
}

/**
 * Success Item Added: Pop-up checkmark micro-interaction.
 */
@Composable
fun ItemSuccessCheckmarkAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onComplete: (() -> Unit)? = null
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(SUCCESS_CHECK_LOTTIE_JSON)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1.2f
    )

    LaunchedEffect(progress) {
        if (progress >= 0.95f) {
            onComplete?.invoke()
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(650)
        onComplete?.invoke()
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress }
            )
        } else {
            ProceduralSuccessCheckmark(size = size)
        }
    }
}

/**
 * Scanner Overlay: Continuous subtle radar scanning wave animation.
 */
@Composable
fun RadarScanningWaveAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(RADAR_SCAN_LOTTIE_JSON)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress }
            )
        } else {
            ProceduralRadarWave(size = size)
        }
    }
}

/**
 * Fallback Canvas rendering for smooth floating refrigerator animation.
 */
@Composable
private fun ProceduralFloatingFridge(size: Dp) {
    val transition = rememberInfiniteTransition(label = "fridgeFloat")
    val floatOffset by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Canvas(modifier = Modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height

        val fridgeW = width * 0.55f
        val fridgeH = height * 0.72f
        val left = (width - fridgeW) / 2f
        val top = (height - fridgeH) / 2f + floatOffset

        // Fridge outer body (Slate translucent)
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(left, top),
            size = Size(fridgeW, fridgeH),
            cornerRadius = CornerRadius(20f, 20f)
        )
        // Neon mint outline
        drawRoundRect(
            color = Color(0xFF00F5A0),
            topLeft = Offset(left, top),
            size = Size(fridgeW, fridgeH),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 3.5f)
        )

        // Freezer split line
        val splitY = top + fridgeH * 0.38f
        drawLine(
            color = Color(0xFF334155),
            start = Offset(left + 8f, splitY),
            end = Offset(left + fridgeW - 8f, splitY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Upper door handle
        drawLine(
            color = Color(0xFF00F5A0),
            start = Offset(left + fridgeW - 14f, top + fridgeH * 0.12f),
            end = Offset(left + fridgeW - 14f, top + fridgeH * 0.28f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Lower door handle
        drawLine(
            color = Color(0xFF00F5A0),
            start = Offset(left + fridgeW - 14f, splitY + fridgeH * 0.10f),
            end = Offset(left + fridgeW - 14f, splitY + fridgeH * 0.42f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Inner glowing shelf light
        drawCircle(
            color = Color(0xFF10B981).copy(alpha = 0.6f),
            center = Offset(left + fridgeW * 0.5f, splitY + fridgeH * 0.25f),
            radius = 6f
        )
    }
}

/**
 * Fallback Canvas rendering for animated pop-up checkmark.
 */
@Composable
private fun ProceduralSuccessCheckmark(size: Dp) {
    val transition = rememberInfiniteTransition(label = "checkPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = Modifier.size(size)) {
        val radius = this.size.minDimension / 2f * pulseScale * 0.85f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer glow
        drawCircle(
            color = FreshEmerald.copy(alpha = 0.22f),
            radius = radius * 1.25f,
            center = center
        )
        // Background circle
        drawCircle(
            color = FreshEmerald,
            radius = radius,
            center = center
        )

        // Checkmark path
        val path = Path().apply {
            moveTo(center.x - radius * 0.45f, center.y)
            lineTo(center.x - radius * 0.10f, center.y + radius * 0.35f)
            lineTo(center.x + radius * 0.50f, center.y - radius * 0.35f)
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = radius * 0.18f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Fallback Canvas rendering for subtle radar pulse rings.
 */
@Composable
private fun ProceduralRadarWave(size: Dp) {
    val transition = rememberInfiniteTransition(label = "radarWave")
    val waveProgress by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Canvas(modifier = Modifier.size(size)) {
        val maxRadius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Wave 1
        val radius1 = maxRadius * waveProgress
        val alpha1 = (1f - waveProgress).coerceIn(0f, 1f) * 0.45f
        drawCircle(
            color = NeonMint.copy(alpha = alpha1),
            radius = radius1,
            center = center,
            style = Stroke(width = 2.5f)
        )

        // Wave 2 (phase offset)
        val progress2 = (waveProgress + 0.5f) % 1.0f
        val radius2 = maxRadius * progress2
        val alpha2 = (1f - progress2).coerceIn(0f, 1f) * 0.45f
        drawCircle(
            color = NeonMint.copy(alpha = alpha2),
            radius = radius2,
            center = center,
            style = Stroke(width = 2.5f)
        )
    }
}

// Compact, fully compliant Lottie JSON strings for self-contained offline execution:
private const val FRIDGE_LOTTIE_JSON = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":100,"h":100,"nm":"Fridge","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":4,"nm":"FridgeBody","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":1,"k":[{"t":0,"s":[50,52,0]},{"t":30,"s":[50,46,0]},{"t":60,"s":[50,52,0]}]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"ao":0,"shapes":[{"ty":"rc","d":1,"s":{"a":0,"k":[50,70]},"p":{"a":0,"k":[0,0]},"r":{"a":0,"k":8},"nm":"Body"},{"ty":"st","c":{"a":0,"k":[0,0.96,0.63,1]},"w":{"a":0,"k":3},"nm":"Stroke"},{"ty":"fl","c":{"a":0,"k":[0.12,0.16,0.23,1]},"r":1,"nm":"Fill"}]}]}"""
private const val SUCCESS_CHECK_LOTTIE_JSON = """{"v":"5.7.4","fr":30,"ip":0,"op":40,"w":100,"h":100,"nm":"Check","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":4,"nm":"CheckCircle","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[50,50,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":1,"k":[{"t":0,"s":[0,0,100]},{"t":20,"s":[110,110,100]},{"t":30,"s":[100,100,100]}]}},"ao":0,"shapes":[{"ty":"el","d":1,"s":{"a":0,"k":[70,70]},"p":{"a":0,"k":[0,0]},"nm":"Circle"},{"ty":"fl","c":{"a":0,"k":[0.06,0.73,0.51,1]},"r":1,"nm":"Fill"}]}]}"""
private const val RADAR_SCAN_LOTTIE_JSON = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":100,"h":100,"nm":"Radar","ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":4,"nm":"Wave","sr":1,"ks":{"o":{"a":1,"k":[{"t":0,"s":[80]},{"t":60,"s":[0]}]},"r":{"a":0,"k":0},"p":{"a":0,"k":[50,50,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":1,"k":[{"t":0,"s":[20,20,100]},{"t":60,"s":[120,120,100]}]}},"ao":0,"shapes":[{"ty":"el","d":1,"s":{"a":0,"k":[80,80]},"p":{"a":0,"k":[0,0]},"nm":"Circle"},{"ty":"st","c":{"a":0,"k":[0,0.96,0.63,1]},"w":{"a":0,"k":3},"nm":"Stroke"}]}]}"""
