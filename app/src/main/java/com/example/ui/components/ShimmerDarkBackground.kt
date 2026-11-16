package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyanShimmer
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NightSlateSurface
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.VioletGlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ShimmerDarkBackground(
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_bg")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isStreaming) 6500 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isStreaming) 0.16f else 0.09f,
        targetValue = if (isStreaming) 0.28f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isStreaming) 1800 else 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_pulse"
    )

    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isStreaming) 4800 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Deep minimal obsidian-to-slate vertical base gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ObsidianBackground,
                        NightSlateSurface,
                        ObsidianBackground
                    )
                )
            )

            // 2. Subtle top-right electric indigo radial shimmer
            val indigoCenter = Offset(
                x = width * (0.76f + 0.12f * cos(phase)),
                y = height * (0.18f + 0.08f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricIndigo.copy(alpha = pulseAlpha),
                        VioletGlow.copy(alpha = pulseAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = indigoCenter,
                    radius = width.coerceAtLeast(height) * 0.62f
                ),
                center = indigoCenter,
                radius = width.coerceAtLeast(height) * 0.62f
            )

            // 3. Subtle bottom-left cyan-teal radial shimmer
            val cyanCenter = Offset(
                x = width * (0.22f + 0.10f * sin(phase * 0.8f)),
                y = height * (0.78f + 0.07f * cos(phase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanShimmer.copy(alpha = pulseAlpha * 0.85f),
                        ElectricIndigo.copy(alpha = pulseAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = cyanCenter,
                    radius = width.coerceAtLeast(height) * 0.55f
                ),
                center = cyanCenter,
                radius = width.coerceAtLeast(height) * 0.55f
            )

            // 4. Diagonal minimal accent shimmer band
            val bandStart = Offset(width * sweepProgress, 0f)
            val bandEnd = Offset(width * (sweepProgress + 0.35f), height)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        ElectricIndigo.copy(alpha = if (isStreaming) 0.08f else 0.035f),
                        CyanShimmer.copy(alpha = if (isStreaming) 0.07f else 0.03f),
                        Color.Transparent
                    ),
                    start = bandStart,
                    end = bandEnd
                )
            )
        }

        content()
    }
}
