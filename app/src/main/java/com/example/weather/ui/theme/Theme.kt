package com.example.weather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeutralLightScheme = lightColorScheme(
    primary        = Color(0xFF222222),
    onPrimary      = Color.White,
    secondary      = Color(0xFF616161),
    onSecondary    = Color.White,
    background     = Color.White,
    onBackground   = Color(0xFF111111),
    surface        = Color.White,
    onSurface      = Color(0xFF111111),
    surfaceVariant = Color(0xFFF6F6F6),
    outline        = Color(0xFFE0E0E0)
)

private val AppTypography = Typography()

@Composable
fun viewmodelTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = NeutralLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
