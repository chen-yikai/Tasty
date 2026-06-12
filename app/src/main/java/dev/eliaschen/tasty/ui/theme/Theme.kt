package dev.eliaschen.tasty.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB95B),
    onPrimary = Color(0xFF4B2A00),
    secondary = Color(0xFFE6C39A),
    onSecondary = Color(0xFF422C13),
    tertiary = Orange,
    onTertiary = Color(0xFF4B2A00),
    background = Color(0xFF19130D),
    onBackground = Color(0xFFF1DFD0),
    surface = Color(0xFF19130D),
    onSurface = Color(0xFFF1DFD0),
    surfaceVariant = Color(0xFF524434),
    onSurfaceVariant = Color(0xFFD5C4B0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF9F8D7A)
)

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = Color(0xFF9C6A2D),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFD8A6),
    onTertiary = Color(0xFF3D2A12),
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF3E3D1),
    onSurfaceVariant = Color(0xFF544536),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = Color(0xFF857461),
)

@Composable
fun TastyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}