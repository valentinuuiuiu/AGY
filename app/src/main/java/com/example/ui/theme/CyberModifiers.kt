package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Applies a frosted glassmorphic cyber container with gradient border and dark translucent fill.
 */
fun Modifier.cyberGlass(
    cornerRadius: Dp = 16.dp,
    borderBrush: Brush = CyberCardBorderGradient,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = CyberCard.copy(alpha = 0.82f)
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(borderWidth, borderBrush, RoundedCornerShape(cornerRadius))

/**
 * Holographic corner brackets targeting reticle for AR & Jarvis Camera.
 */
@Composable
fun HolographicReticle(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    bracketLength: Float = 40f,
    strokeWidth: Float = 4f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reticle_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = color.copy(alpha = alpha)

        // Top-Left corner
        drawLine(c, Offset(0f, 0f), Offset(bracketLength, 0f), strokeWidth)
        drawLine(c, Offset(0f, 0f), Offset(0f, bracketLength), strokeWidth)

        // Top-Right corner
        drawLine(c, Offset(w, 0f), Offset(w - bracketLength, 0f), strokeWidth)
        drawLine(c, Offset(w, 0f), Offset(w, bracketLength), strokeWidth)

        // Bottom-Left corner
        drawLine(c, Offset(0f, h), Offset(bracketLength, h), strokeWidth)
        drawLine(c, Offset(0f, h), Offset(0f, h - bracketLength), strokeWidth)

        // Bottom-Right corner
        drawLine(c, Offset(w, h), Offset(w - bracketLength, h), strokeWidth)
        drawLine(c, Offset(w, h), Offset(w, h - bracketLength), strokeWidth)
    }
}

/**
 * Futuristic Audio Waveform Visualizer for live speech and Jarvis voice synthesis.
 */
@Composable
fun CyberWaveformVisualizer(
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    activeColor: Color = NeonCyan,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val barWidth = totalWidth / (barCount * 1.5f)
        val spacing = barWidth * 0.5f
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val wave = if (isActive) {
                val sinVal = kotlin.math.sin(phase + i * 0.45).toFloat()
                (sinVal + 1f) / 2f * 0.8f + 0.2f
            } else {
                0.15f
            }
            val barHeight = maxHeight * wave
            val x = i * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = if (i % 2 == 0) activeColor else NeonPurple,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Concentric pulsing radar ring for Jarvis Live Copilot.
 */
@Composable
fun JarvisRadarPulse(
    modifier: Modifier = Modifier,
    tint: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_radar")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "scale1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "alpha1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "scale2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "alpha2"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = kotlin.math.min(size.width, size.height) / 2f

        // Outer pulsing ring 1
        drawCircle(
            color = tint.copy(alpha = alpha1),
            radius = maxRadius * scale1,
            center = center,
            style = Stroke(width = 3f)
        )

        // Mid pulsing ring 2
        drawCircle(
            color = NeonPurple.copy(alpha = alpha2),
            radius = maxRadius * scale2,
            center = center,
            style = Stroke(width = 2f)
        )

        // Core solid reticle dot
        drawCircle(
            color = tint,
            radius = 6f,
            center = center
        )
    }
}

/**
 * Cyberpunk glowing chip/badge with monospaced style.
 */
@Composable
fun CyberBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    trailingDot: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (trailingDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
