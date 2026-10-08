package com.antistalk.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Concept A: Midnight Clarity (dark mode) ─────────────────────────────────
// Deep violet-pink glassmorphism. Ambient glow palette.
private val MC_Primary          = Color(0xFF8B5CF6) // Violet
private val MC_OnPrimary        = Color(0xFFFFFFFF)
private val MC_PrimaryContainer = Color(0xFF3F2D8E)
private val MC_OnPrimaryContainer = Color(0xFFDDD6FE)
private val MC_Secondary        = Color(0xFFEC4899) // Hot pink accent
private val MC_OnSecondary      = Color(0xFFFFFFFF)
private val MC_SecondaryContainer = Color(0xFF7E1E4E)
private val MC_OnSecondaryContainer = Color(0xFFFCE7F3)
private val MC_Background       = Color(0xFF0D0B14) // Deep space
private val MC_OnBackground     = Color(0xFFF5F3FF)
private val MC_Surface          = Color(0xFF1A1628) // Elevated glass
private val MC_OnSurface        = Color(0xFFF5F3FF)
private val MC_SurfaceVariant   = Color(0xFF2A2442)
private val MC_OnSurfaceVariant = Color(0xFFA89FC0)
private val MC_Outline          = Color(0xFF4A4268)
private val MC_OutlineVariant   = Color(0xFF2D2950)
private val MC_Error            = Color(0xFFF87171)
private val MC_OnError          = Color(0xFF1A0A0A)

// ─── Concept B: Soft Sanity (light mode) ─────────────────────────────────────
// Warm earth tones: sage green primary, terracotta accent. Calm & encouraging.
private val SS_Primary          = Color(0xFF5B8A6A) // Sage green
private val SS_OnPrimary        = Color(0xFFFFFFFF)
private val SS_PrimaryContainer = Color(0xFFD6EADd) // Soft mint
private val SS_OnPrimaryContainer = Color(0xFF1A3D26)
private val SS_Secondary        = Color(0xFFE8795A) // Terracotta coral
private val SS_OnSecondary      = Color(0xFFFFFFFF)
private val SS_SecondaryContainer = Color(0xFFFDEEE9)
private val SS_OnSecondaryContainer = Color(0xFF5A1E0A)
private val SS_Background       = Color(0xFFF7F4F0) // Warm cream
private val SS_OnBackground     = Color(0xFF2C2420)
private val SS_Surface          = Color(0xFFFFFFFF)
private val SS_OnSurface        = Color(0xFF2C2420)
private val SS_SurfaceVariant   = Color(0xFFF0EBE5)
private val SS_OnSurfaceVariant = Color(0xFF6B5E56)
private val SS_Outline          = Color(0xFFD5CEC7)
private val SS_OutlineVariant   = Color(0xFFE8E0D8)
private val SS_Error            = Color(0xFFC0392B)
private val SS_OnError          = Color(0xFFFFFFFF)

// ─── Exported schemes ─────────────────────────────────────────────────────────
val DarkColors = darkColorScheme(
    primary                = MC_Primary,
    onPrimary              = MC_OnPrimary,
    primaryContainer       = MC_PrimaryContainer,
    onPrimaryContainer     = MC_OnPrimaryContainer,
    secondary              = MC_Secondary,
    onSecondary            = MC_OnSecondary,
    secondaryContainer     = MC_SecondaryContainer,
    onSecondaryContainer   = MC_OnSecondaryContainer,
    background             = MC_Background,
    onBackground           = MC_OnBackground,
    surface                = MC_Surface,
    onSurface              = MC_OnSurface,
    surfaceVariant         = MC_SurfaceVariant,
    onSurfaceVariant       = MC_OnSurfaceVariant,
    outline                = MC_Outline,
    outlineVariant         = MC_OutlineVariant,
    error                  = MC_Error,
    onError                = MC_OnError,
)

val LightColors = lightColorScheme(
    primary                = SS_Primary,
    onPrimary              = SS_OnPrimary,
    primaryContainer       = SS_PrimaryContainer,
    onPrimaryContainer     = SS_OnPrimaryContainer,
    secondary              = SS_Secondary,
    onSecondary            = SS_OnSecondary,
    secondaryContainer     = SS_SecondaryContainer,
    onSecondaryContainer   = SS_OnSecondaryContainer,
    background             = SS_Background,
    onBackground           = SS_OnBackground,
    surface                = SS_Surface,
    onSurface              = SS_OnSurface,
    surfaceVariant         = SS_SurfaceVariant,
    onSurfaceVariant       = SS_OnSurfaceVariant,
    outline                = SS_Outline,
    outlineVariant         = SS_OutlineVariant,
    error                  = SS_Error,
    onError                = SS_OnError,
)

// ─── Concept A & B Shared Design Tokens ─────────────────────────────────────────
val AccentTerracotta    = Color(0xFFE8795A) // Concept B warm coral
val AccentTerracottaLight = Color(0xFFFDEEE9)
val AccentLavender      = Color(0xFF7B6FBF) // Concept B introspective purple
val AccentLavenderLight = Color(0xFFEEEDF9)
val BrandViolet         = Color(0xFF8B5CF6) // Concept A primary violet
val BrandHotPink        = Color(0xFFEC4899) // Concept A glowing pink
val MintGreen           = Color(0xFF34D399) // Concept A positive action green
val WarmSage            = Color(0xFF5B8A6A) // Concept B sage green
val WarmSageLight       = Color(0xFFEBF3EE)

// Monitored app icon gradients
val FbGradient          = listOf(Color(0xFF1877F2), Color(0xFF0D65D9))
val IgGradient          = listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
val MsGradient          = listOf(Color(0xFF0084FF), Color(0xFF00C6FF))
val ZaloGradient        = listOf(Color(0xFF0068FF), Color(0xFF0052CC))
val DefaultAppGradient  = listOf(Color(0xFF6B7280), Color(0xFF4B5563))


