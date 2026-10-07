package com.antistalk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Shared type scale so every screen uses the same headers/captions.
// Body copy keeps the Material3 default.
val AppTypography = Typography(
    displayMedium = TextStyle(
        fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp
    )
)
