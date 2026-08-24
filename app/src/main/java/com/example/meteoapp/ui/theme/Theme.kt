package com.quaderno.appmeteo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = NavyDark,
    background = SkyBgTop,
    surface = CloudTop,
    onPrimary = CloudTop,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MeteoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MeteoTypography,
        content = content
    )
}
