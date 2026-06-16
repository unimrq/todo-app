package com.kanochi.todo.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryVariant,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = HighPriority,
    onError = GreenOnPrimary
)

@Composable
fun TodoAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
