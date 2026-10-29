package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cyberpunk & Futuristic Glassmorphism Palette
val CyberBackground = Color(0xFF07090E)
val CyberSurface = Color(0xFF0D121F)
val CyberCard = Color(0xFF131B2E)
val CyberCardElevated = Color(0xFF19243C)
val CyberBorder = Color(0xFF1E293B)

val NeonCyan = Color(0xFF00F5D4)
val NeonPurple = Color(0xFF7928CA)
val NeonPink = Color(0xFFFF007F)
val ElectricBlue = Color(0xFF0070F3)
val CyberAmber = Color(0xFFFFB703)
val CyberEmerald = Color(0xFF10B981)
val CyberRose = Color(0xFFF43F5E)

val CyberTextPrimary = Color(0xFFF8FAFC)
val CyberTextSecondary = Color(0xFF94A3B8)
val CyberTextMuted = Color(0xFF64748B)

// Gradients
val CyberNeonGradient = Brush.horizontalGradient(
    listOf(NeonCyan, ElectricBlue, NeonPurple)
)

val CyberCardBorderGradient = Brush.linearGradient(
    listOf(NeonCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.3f), Color.Transparent)
)

val JarvisHoloGradient = Brush.radialGradient(
    listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent)
)
