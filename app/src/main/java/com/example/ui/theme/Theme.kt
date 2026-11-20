package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val id: String, val displayName: String, val description: String) {
    SYSTEM("system", "Dynamic System", "Matches phone wallpaper & theme"),
    MIDNIGHT("midnight", "Midnight Neon", "Deep blue & purple glow"),
    AMOLED("amoled", "AMOLED Pure Black", "True pitch black for OLED screens"),
    LIGHT("light", "Light Glass", "Modern bright glassmorphic");

    companion object {
        fun fromId(id: String?): AppThemeMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}

val LocalThemeMode = staticCompositionLocalOf { AppThemeMode.SYSTEM }

private val MidnightColorScheme =
  darkColorScheme(
    primary = RanjhaNeonPink,
    secondary = RanjhaCyan,
    tertiary = RanjhaNeonPurple,
    background = MidnightNavy,
    surface = MidnightNavySurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceContainer = MidnightNavyCard,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,
    primaryContainer = RanjhaNeonPink.copy(alpha = 0.15f),
    onPrimaryContainer = RanjhaNeonPink,
    outline = TextTertiary,
    outlineVariant = DarkSurfaceVariant
  )

private val AmoledColorScheme =
  darkColorScheme(
    primary = RanjhaNeonPink,
    secondary = RanjhaCyan,
    tertiary = RanjhaNeonPurple,
    background = Color(0xFF000000),
    surface = Color(0xFF080808),
    surfaceVariant = Color(0xFF222222),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFA0A0A0),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed,
    primaryContainer = RanjhaNeonPink.copy(alpha = 0.15f),
    onPrimaryContainer = RanjhaNeonPink,
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF262626)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = RanjhaNeonPink,
    secondary = Color(0xFF0284C7),
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFFF4F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    surfaceContainer = Color(0xFFEAEFF8),
    surfaceContainerHigh = Color(0xFFDFE6F2),
    surfaceContainerHighest = Color(0xFFD4DEEC),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,
    primaryContainer = RanjhaNeonPink.copy(alpha = 0.12f),
    onPrimaryContainer = RanjhaNeonPink,
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
  )

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isSystemDark) MidnightColorScheme else LightColorScheme
            }
        }
        AppThemeMode.MIDNIGHT -> MidnightColorScheme
        AppThemeMode.AMOLED -> AmoledColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
