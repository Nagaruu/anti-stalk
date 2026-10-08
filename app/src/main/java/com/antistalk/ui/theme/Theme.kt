package com.antistalk.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App theme. [darkTheme] comes from the saved setting
 * (light / dark / follow system), resolved in MainActivity.
 *
 * Hybrid mode:
 *  - Dark  → "Midnight Clarity": violet-pink palette + Plus Jakarta Sans
 *  - Light → "Soft Sanity": sage-green/coral palette + Nunito
 */
@Composable
fun AntiStalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors     = if (darkTheme) DarkColors     else LightColors
    val typography = if (darkTheme) DarkTypography else LightTypography

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography  = typography,
        content     = { Surface(content = content) }
    )
}

