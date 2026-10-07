package com.antistalk.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand: Anti-Stalk purple. Dark primary is lightened for contrast on dark bg.
private val BrandPrimary = Color(0xFF6C4DFF)
private val BrandPrimaryDark = Color(0xFF9D8BFF)
private val BrandPrimaryContainerLight = Color(0xFFE4DEFF)
private val BrandOnPrimaryContainerLight = Color(0xFF1E1052)

val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = BrandOnPrimaryContainerLight,
    background = Color(0xFFFAFAFF),
    onBackground = Color(0xFF14101F),
    surface = Color.White,
    onSurface = Color(0xFF14101F),
    surfaceVariant = Color(0xFFEFEDF7),
    onSurfaceVariant = Color(0xFF4A4458),
    outline = Color(0xFFC9C4DC)
)

val DarkColors = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF1E1052),
    primaryContainer = Color(0xFF3F2FBF),
    onPrimaryContainer = BrandPrimaryContainerLight,
    background = Color(0xFF12101D),
    onBackground = Color(0xFFF2EFFA),
    surface = Color(0xFF1E1B30),
    onSurface = Color(0xFFF2EFFA),
    surfaceVariant = Color(0xFF2A2642),
    onSurfaceVariant = Color(0xFFB9B3CC),
    outline = Color(0xFF4E4965)
)
