package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalThemeMode

/**
 * Dynamic Ranjha App Logo that adapts dynamically to the mobile theme:
 * - When themed icons / dynamic Monet theme is active, its flute silhouette, soundwaves,
 *   and ambient card glow adopt the phone's wallpaper & accent colors.
 * - In Dark / Midnight / AMOLED modes, it renders deep glowing glassmorphism.
 * - In Light mode, it renders luminous bright frosted glass with vibrant dynamic accents.
 */
@Composable
fun DynamicRanjhaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 130.dp
) {
    val themeMode = LocalThemeMode.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.MIDNIGHT, AppThemeMode.AMOLED -> true
        AppThemeMode.LIGHT -> false
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val squircleShape = remember(size) { RoundedCornerShape(size * 0.24f) }

    // Dynamic glass squircle background
    val bgBrush = remember(isDark, primaryColor) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.18f),
                    Color(0xFF0F172A).copy(alpha = 0.92f),
                    Color(0xFF0B1120)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFF1F5F9).copy(alpha = 0.90f),
                    primaryColor.copy(alpha = 0.08f)
                )
            )
        }
    }

    // Dynamic glowing outer border
    val borderBrush = remember(isDark, primaryColor, secondaryColor) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.70f),
                    Color.White.copy(alpha = 0.20f),
                    secondaryColor.copy(alpha = 0.50f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.60f),
                    Color.White.copy(alpha = 0.90f),
                    secondaryColor.copy(alpha = 0.40f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(squircleShape)
            .background(bgBrush)
            .border(1.5.dp, borderBrush, squircleShape)
            .drawWithCache {
                // Soft dynamic ambient glow behind the flute emblem
                val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
                val glowRadius = this.size.width * 0.45f
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isDark) 0.35f else 0.18f),
                        secondaryColor.copy(alpha = if (isDark) 0.15f else 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                )
                onDrawBehind {
                    drawCircle(brush = glowBrush, radius = glowRadius, center = center)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Ranjha "R" Flute + Soundwave Vector Emblem
        // Masked with dynamic primary -> secondary -> tertiary gradient brush
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_monochrome),
            contentDescription = "Ranjha Emblem",
            tint = Color.White,
            modifier = Modifier
                .size(size * 0.85f)
                .graphicsLayer(alpha = 0.99f)
                .drawWithCache {
                    val gradient = Brush.linearGradient(
                        colors = listOf(
                            primaryColor,
                            secondaryColor,
                            tertiaryColor
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = gradient, blendMode = BlendMode.SrcIn)
                    }
                }
        )
    }
}

/**
 * Full dynamic hero branding (Dynamic Logo + Cursive "ranjha" + Tracked "MUSIC APP").
 * All elements dynamically respond to the mobile theme and dynamic system colors.
 */
@Composable
fun DynamicRanjhaBranding(
    modifier: Modifier = Modifier,
    logoSize: Dp = 130.dp
) {
    val themeMode = LocalThemeMode.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.MIDNIGHT, AppThemeMode.AMOLED -> true
        AppThemeMode.LIGHT -> false
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DynamicRanjhaLogo(size = logoSize)

        Spacer(modifier = Modifier.height(24.dp))

        // Cursive glowing script "ranjha"
        Text(
            text = "ranjha",
            fontSize = 38.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Clean tracked uppercase "MUSIC APP"
        Text(
            text = "MUSIC APP",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            letterSpacing = 4.sp
        )
    }
}
