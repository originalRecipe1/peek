package org.peek.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF355F5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8ECE4),
    onPrimaryContainer = Color(0xFF00201D),
    surface = Color(0xFFF8FAF8),
    onSurface = Color(0xFF191C1B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD0C8),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF1B4F4A),
    onPrimaryContainer = Color(0xFFB8ECE4),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE0E3E1),
)

@Composable
fun PeekTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
