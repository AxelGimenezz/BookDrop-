package com.aistudio.bookdrop.mvp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimaryContainer,
    onPrimary = PurpleOnPrimaryContainer,
    primaryContainer = PurplePrimary,
    onPrimaryContainer = PurpleOnPrimary,
    background = BookDropOnBackground,
    onBackground = BookDropBackground,
    surface = BookDropOnSurfaceVariant,
    onSurface = BookDropBackground,
    error = BookDropError
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,
    secondaryContainer = PurpleSecondaryContainer,
    onSecondaryContainer = PurpleOnSecondaryContainer,
    background = BookDropBackground,
    onBackground = BookDropOnBackground,
    surface = BookDropSurface,
    onSurface = BookDropOnSurface,
    surfaceVariant = BookDropSurfaceVariant,
    onSurfaceVariant = BookDropOnSurfaceVariant,
    outline = BookDropOutline,
    error = BookDropError
)

@Composable
fun BookDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
