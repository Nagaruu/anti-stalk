package com.antistalk.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Dark Mode Palette: Forest Midnight (Moss green / Deep charcoal) ─────────
// Unified brand green identity in low light
private val Dark_Primary             = Color(0xFF6EAA81) // Moss green highlight
private val Dark_OnPrimary           = Color(0xFF0F2417)
private val Dark_PrimaryContainer    = Color(0xFF24442F)
private val Dark_OnPrimaryContainer  = Color(0xFFD2EED8)
private val Dark_Secondary           = Color(0xFFE89A7A) // Terracotta warm accent
private val Dark_OnSecondary         = Color(0xFFFFFFFF)
private val Dark_SecondaryContainer   = Color(0xFF5A2A1A)
private val Dark_OnSecondaryContainer = Color(0xFFFDEEE9)
private val Dark_Background          = Color(0xFF121613) // Deep charcoal with moss undertone
private val Dark_OnBackground        = Color(0xFFF0F4F1)
private val Dark_Surface             = Color(0xFF1A221C) // Deep moss surface
private val Dark_OnSurface           = Color(0xFFF0F4F1)
private val Dark_SurfaceVariant      = Color(0xFF253128)
private val Dark_OnSurfaceVariant    = Color(0xFFA1B3A5)
private val Dark_Outline             = Color(0xFF435547)
private val Dark_OutlineVariant      = Color(0xFF29372D)
private val Dark_Error               = Color(0xFFF87171)
private val Dark_OnError             = Color(0xFF1A0A0A)

// ─── Light Mode Palette: Soft Sanity (Sage green / Warm cream pastel) ────────
// Warm earth tones: moss/sage green primary, terracotta accent. Calm & encouraging.
private val SS_Primary          = Color(0xFF4B7B58) // Sage / Moss green
private val SS_OnPrimary        = Color(0xFFFFFFFF)
private val SS_PrimaryContainer = Color(0xFFD6EADd) // Soft mint pastel
private val SS_OnPrimaryContainer = Color(0xFF1A3D26)
private val SS_Secondary        = Color(0xFFE8795A) // Terracotta coral
private val SS_OnSecondary      = Color(0xFFFFFFFF)
private val SS_SecondaryContainer = Color(0xFFFDEEE9)
private val SS_OnSecondaryContainer = Color(0xFF5A1E0A)
private val SS_Background       = Color(0xFFF7F4F0) // Warm cream / Be pastel
private val SS_OnBackground     = Color(0xFF2C2420)
private val SS_Surface          = Color(0xFFFFFFFF)
private val SS_OnSurface        = Color(0xFF2C2420)
private val SS_SurfaceVariant   = Color(0xFFF0EBE5) // Pastel beige
private val SS_OnSurfaceVariant = Color(0xFF6B5E56)
private val SS_Outline          = Color(0xFFD5CEC7)
private val SS_OutlineVariant   = Color(0xFFE8E0D8)
private val SS_Error            = Color(0xFFC0392B)
private val SS_OnError          = Color(0xFFFFFFFF)

// ─── Exported schemes ─────────────────────────────────────────────────────────
val DarkColors = darkColorScheme(
    primary                = Dark_Primary,
    onPrimary              = Dark_OnPrimary,
    primaryContainer       = Dark_PrimaryContainer,
    onPrimaryContainer     = Dark_OnPrimaryContainer,
    secondary              = Dark_Secondary,
    onSecondary            = Dark_OnSecondary,
    secondaryContainer     = Dark_SecondaryContainer,
    onSecondaryContainer   = Dark_OnSecondaryContainer,
    background             = Dark_Background,
    onBackground           = Dark_OnBackground,
    surface                = Dark_Surface,
    onSurface              = Dark_OnSurface,
    surfaceVariant         = Dark_SurfaceVariant,
    onSurfaceVariant       = Dark_OnSurfaceVariant,
    outline                = Dark_Outline,
    outlineVariant         = Dark_OutlineVariant,
    error                  = Dark_Error,
    onError                = Dark_OnError,
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

// ─── Brand Design Tokens (Moss Green & Pastel Beige) ──────────────────────────
val AccentTerracotta        = Color(0xFFE8795A) // Warm coral accent
val AccentTerracottaLight   = Color(0xFFFDEEE9)
val AccentLavender          = Color(0xFF6EAA81) // Aligned with brand moss
val AccentLavenderLight     = Color(0xFFEBF3EE)

// Core brand moss greens
val WarmSage                = Color(0xFF4B7B58) // Primary brand moss green
val WarmSageDark            = Color(0xFF386641) // Dark forest moss (Primary button)
val WarmSageLight           = Color(0xFFEBF3EE) // Pale moss / beige tint
val BrandMossGreen          = Color(0xFF4B7B58) // Primary moss green
val BrandMossGreenLight     = Color(0xFF6EAA81) // Highlight moss green for dark mode
val MintGreen               = Color(0xFF4EAA73) // Positive status green

// Backward-compatibility aliases remapped to Brand Moss Green
val BrandViolet             = Color(0xFF4B7B58) // Mapped to brand moss green to avoid stray blue/purple
val BrandHotPink            = Color(0xFFE8795A) // Mapped to warm coral

// Monitored app icon gradients
val FbGradient          = listOf(Color(0xFF1877F2), Color(0xFF0D65D9))
val IgGradient          = listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
val MsGradient          = listOf(Color(0xFF0084FF), Color(0xFF00C6FF))
val ZaloGradient        = listOf(Color(0xFF0068FF), Color(0xFF0052CC))
val DefaultAppGradient  = listOf(Color(0xFF6B7280), Color(0xFF4B5563))


