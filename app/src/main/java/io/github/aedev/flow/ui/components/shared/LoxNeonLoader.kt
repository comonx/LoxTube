/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package io.github.aedev.flow.ui.components.shared

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * LOX Neon Loader animation matching the brand styling:
 * - Outer glowing neon ring with cyan-purple-pink gradient and gentle pulse
 * - Inner thin concentric ring
 * - Rotating high-visibility moving light arc with round caps
 * - Centered "LOX" brand text with vibrant gradient and neon glow
 */
@Composable
fun LoxNeonLoader(
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LoxNeonPulse")

    // Outer ring pulse: 2.5s ease-in-out infinite (0.75 -> 1.0 -> 0.75)
    val pulseOpacity by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseOpacity",
    )

    // Moving light ring rotation: 1.4s linear infinite (0 -> 360 deg)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotationAngle",
    )

    // Cached paints for native text rendering to eliminate per-frame allocations
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val glowPaint = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = 0xFF8BEAFF.toInt()
        }
    }
    val textBounds = remember { Rect() }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasMin = min(this.size.width, this.size.height)
            if (canvasMin <= 0f) return@Canvas

            // Scale relative to 100x100 viewBox
            val scale = canvasMin / 100f
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)

            val outerRadius = 43f * scale
            val innerRadius = 40f * scale
            val outerStrokeWidth = 2.5f * scale
            val innerStrokeWidth = 0.8f * scale
            val lightStrokeWidth = 4.0f * scale

            // Ring Gradient: 0% #00D9FF, 45% #635BFF, 100% #F04BFF
            val ringBrush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF00D9FF),
                    0.45f to Color(0xFF635BFF),
                    1.0f to Color(0xFFF04BFF),
                ),
                startX = centerOffset.x - outerRadius,
                endX = centerOffset.x + outerRadius,
            )

            // Light Gradient: 0% #00E5FF, 45% #FFFFFF, 100% #F000FF
            val lightBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF00E5FF),
                    0.45f to Color(0xFFFFFFFF),
                    1.0f to Color(0xFFF000FF),
                ),
                start = Offset(centerOffset.x - outerRadius, centerOffset.y - outerRadius),
                end = Offset(centerOffset.x + outerRadius, centerOffset.y + outerRadius),
            )

            // 1. Outer Ring Glow & Sharp Stroke
            drawCircle(
                brush = ringBrush,
                radius = outerRadius,
                center = centerOffset,
                style = Stroke(width = outerStrokeWidth * 2.4f),
                alpha = 0.28f * pulseOpacity,
            )
            drawCircle(
                brush = ringBrush,
                radius = outerRadius,
                center = centerOffset,
                style = Stroke(width = outerStrokeWidth),
                alpha = pulseOpacity,
            )

            // 2. Inner Ring
            drawCircle(
                brush = ringBrush,
                radius = innerRadius,
                center = centerOffset,
                style = Stroke(width = innerStrokeWidth),
                alpha = 0.55f * pulseOpacity,
            )

            // 3. Rotating Moving Light Track (sweep angle ~133.24 deg from dasharray 100/270.18)
            rotate(degrees = rotationAngle, pivot = centerOffset) {
                val arcTopLeft = Offset(centerOffset.x - outerRadius, centerOffset.y - outerRadius)
                val arcDiameter = Size(outerRadius * 2f, outerRadius * 2f)

                // Neon glow under moving light
                drawArc(
                    brush = lightBrush,
                    startAngle = 0f,
                    sweepAngle = 133.24f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcDiameter,
                    style = Stroke(width = lightStrokeWidth * 2.5f, cap = StrokeCap.Round),
                    alpha = 0.38f,
                )

                // Crisp moving light arc
                drawArc(
                    brush = lightBrush,
                    startAngle = 0f,
                    sweepAngle = 133.24f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcDiameter,
                    style = Stroke(width = lightStrokeWidth, cap = StrokeCap.Round),
                    alpha = 1.0f,
                )
            }

            // 4. Centered "LOX" text with neon glow and horizontal text gradient
            drawIntoCanvas { canvas ->
                val text = "LOX"
                val textSizePx = 29f * scale
                textPaint.textSize = textSizePx
                textPaint.letterSpacing = 0.05f
                textPaint.getTextBounds(text, 0, text.length, textBounds)

                val textX = centerOffset.x
                val textY = centerOffset.y - textBounds.exactCenterY() + (1f * scale)
                val textHalfWidth = textBounds.width() / 2f

                // Glow shadow
                glowPaint.textSize = textSizePx
                glowPaint.letterSpacing = 0.05f
                glowPaint.setShadowLayer(3.5f * scale, 0f, 0f, 0xCC8BEAFF.toInt())
                canvas.nativeCanvas.drawText(text, textX, textY, glowPaint)

                // Gradient fill: 0% #8BEAFF, 50% #D38BFF, 100% #FF69DC
                textPaint.shader = LinearGradient(
                    textX - textHalfWidth, textY,
                    textX + textHalfWidth, textY,
                    intArrayOf(
                        0xFF8BEAFF.toInt(),
                        0xFFD38BFF.toInt(),
                        0xFFFF69DC.toInt(),
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.nativeCanvas.drawText(text, textX, textY, textPaint)
            }
        }
    }
}
