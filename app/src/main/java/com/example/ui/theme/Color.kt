package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val IndigoPrimary = Color(0xFF4F46E5)
val IndigoLight = Color(0xFF6366F1)
val IndigoDark = Color(0xFF3730A3)
val IndigoContainer = Color(0xFFEEF2FF)
val OnIndigoContainer = Color(0xFF1E1B4B)

val SkySecondary = Color(0xFF0284C7)
val SkyContainer = Color(0xFFE0F2FE)
val OnSkyContainer = Color(0xFF0369A1)

val EmeraldTertiary = Color(0xFF059669)
val EmeraldContainer = Color(0xFFD1FAE5)

val AmberWarning = Color(0xFFD97706)
val RoseUrgent = Color(0xFFE11D48)

val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceVariantLight = Color(0xFF64748B)

val BackgroundDark = Color(0xFF0B1120)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceVariantDark = Color(0xFF334155)
val OnSurfaceDark = Color(0xFFF8FAFC)
val OnSurfaceVariantDark = Color(0xFF94A3B8)

// Modern Gradients
val HeroGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF0284C7))
)

val HeroGradientDark = Brush.horizontalGradient(
    colors = listOf(Color(0xFF3730A3), Color(0xFF5B21B6), Color(0xFF0369A1))
)

val CardGlowGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFF0284C7).copy(alpha = 0.05f))
)

val PurpleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
)

val EmeraldGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF059669))
)

val AmberGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
)

// Note pastel colors
val NotePastelPurple = Color(0xFFF3E8FF)
val NotePastelBlue = Color(0xFFE0F2FE)
val NotePastelGreen = Color(0xFFDCFCE7)
val NotePastelAmber = Color(0xFFFEF3C7)
val NotePastelRose = Color(0xFFFFE4E6)
val NotePastelSlate = Color(0xFFF1F5F9)

// For backward compatibility with template
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
