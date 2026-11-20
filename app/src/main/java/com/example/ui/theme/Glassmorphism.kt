package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * High-performance GPU-cached fluid backdrop with theme-adaptive background and
 * luminous gradient orbs & ambient bubbles. Uses drawWithCache so shaders are
 * created once per layout size/theme with zero per-frame allocations during scroll.
 */
@Composable
fun FluidBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val themeMode = LocalThemeMode.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveMode = when (themeMode) {
        AppThemeMode.SYSTEM -> if (isSystemDark) AppThemeMode.MIDNIGHT else AppThemeMode.LIGHT
        else -> themeMode
    }

    val bgBrush = remember(effectiveMode) {
        val colors = when (effectiveMode) {
            AppThemeMode.MIDNIGHT -> listOf(
                Color(0xFF090D1A),
                Color(0xFF0C1224),
                Color(0xFF090D1A)
            )
            AppThemeMode.AMOLED -> listOf(
                Color(0xFF000000),
                Color(0xFF060606),
                Color(0xFF000000)
            )
            AppThemeMode.LIGHT -> listOf(
                Color(0xFFF4F6FB),
                Color(0xFFE9EDF5),
                Color(0xFFF4F6FB)
            )
            AppThemeMode.SYSTEM -> listOf(
                Color(0xFF090D1A),
                Color(0xFF0C1224),
                Color(0xFF090D1A)
            )
        }
        Brush.verticalGradient(colors = colors)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .drawWithCache {
                val width = size.width
                val height = size.height

                val orb1Center = Offset(width * 0.85f, height * 0.15f)
                val orb1Radius = (width * 0.45f).coerceAtLeast(1f)
                val orb2Center = Offset(width * 0.12f, height * 0.45f)
                val orb2Radius = (width * 0.40f).coerceAtLeast(1f)
                val orb3Center = Offset(width * 0.88f, height * 0.75f)
                val orb3Radius = (width * 0.42f).coerceAtLeast(1f)

                val orb1Brush = when (effectiveMode) {
                    AppThemeMode.LIGHT -> Brush.radialGradient(
                        listOf(Color(0x35E0E7FF), Color(0x18C7D2FE), Color.Transparent),
                        center = orb1Center,
                        radius = orb1Radius
                    )
                    AppThemeMode.AMOLED -> Brush.radialGradient(
                        listOf(Color(0x221E3A8A), Color(0x0C1E3A8A), Color.Transparent),
                        center = orb1Center,
                        radius = orb1Radius
                    )
                    else -> Brush.radialGradient(
                        listOf(Color(0x351E3A8A), Color(0x181E3A8A), Color.Transparent),
                        center = orb1Center,
                        radius = orb1Radius
                    )
                }

                val orb2Brush = when (effectiveMode) {
                    AppThemeMode.LIGHT -> Brush.radialGradient(
                        listOf(Color(0x30FBCFE8), Color(0x12FBCFE8), Color.Transparent),
                        center = orb2Center,
                        radius = orb2Radius
                    )
                    AppThemeMode.AMOLED -> Brush.radialGradient(
                        listOf(Color(0x203B1D78), Color(0x0A3B1D78), Color.Transparent),
                        center = orb2Center,
                        radius = orb2Radius
                    )
                    else -> Brush.radialGradient(
                        listOf(Color(0x303B1D78), Color(0x143B1D78), Color.Transparent),
                        center = orb2Center,
                        radius = orb2Radius
                    )
                }

                val orb3Brush = if (effectiveMode == AppThemeMode.MIDNIGHT || effectiveMode == AppThemeMode.SYSTEM) {
                    Brush.radialGradient(
                        listOf(Color(0x280284C7), Color(0x100284C7), Color.Transparent),
                        center = orb3Center,
                        radius = orb3Radius
                    )
                } else null

                val bubbleTint = if (effectiveMode == AppThemeMode.LIGHT) Color(0x306366F1) else Color(0x2038E5FF)
                val cachedBubbles = listOf(
                    Triple(Offset(width * 0.25f, height * 0.22f), 22f, Brush.radialGradient(listOf(Color(0x35FFFFFF), bubbleTint, Color.Transparent), Offset(width * 0.25f, height * 0.22f), 33f)),
                    Triple(Offset(width * 0.78f, height * 0.32f), 18f, Brush.radialGradient(listOf(Color(0x35FFFFFF), bubbleTint, Color.Transparent), Offset(width * 0.78f, height * 0.32f), 27f)),
                    Triple(Offset(width * 0.18f, height * 0.68f), 26f, Brush.radialGradient(listOf(Color(0x35FFFFFF), bubbleTint, Color.Transparent), Offset(width * 0.18f, height * 0.68f), 39f)),
                    Triple(Offset(width * 0.82f, height * 0.62f), 16f, Brush.radialGradient(listOf(Color(0x35FFFFFF), bubbleTint, Color.Transparent), Offset(width * 0.82f, height * 0.62f), 24f)),
                    Triple(Offset(width * 0.45f, height * 0.82f), 20f, Brush.radialGradient(listOf(Color(0x35FFFFFF), bubbleTint, Color.Transparent), Offset(width * 0.45f, height * 0.82f), 30f))
                )

                onDrawBehind {
                    drawCircle(brush = orb1Brush, radius = orb1Radius, center = orb1Center)
                    drawCircle(brush = orb2Brush, radius = orb2Radius, center = orb2Center)
                    if (orb3Brush != null) {
                        drawCircle(brush = orb3Brush, radius = orb3Radius, center = orb3Center)
                    }
                    cachedBubbles.forEach { (center, radius, brush) ->
                        drawCircle(brush = brush, radius = radius, center = center)
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * Fast composable Modifier for frosted glass cards with remembered brushes
 * (avoids heavy composed{} subcomposition overhead during LazyColumn scrolling).
 */
@Composable
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(20.dp),
    fillAlpha: Float = 0.22f,
    strokeAlpha: Float = 0.35f
): Modifier {
    val themeMode = LocalThemeMode.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveMode = when (themeMode) {
        AppThemeMode.SYSTEM -> if (isSystemDark) AppThemeMode.MIDNIGHT else AppThemeMode.LIGHT
        else -> themeMode
    }

    val fillBrush = remember(effectiveMode, fillAlpha) {
        val fillColors = when (effectiveMode) {
            AppThemeMode.MIDNIGHT -> listOf(
                Color(0xFF283B5E).copy(alpha = fillAlpha + 0.08f),
                Color(0xFF141F36).copy(alpha = fillAlpha)
            )
            AppThemeMode.AMOLED -> listOf(
                Color(0xFF1E1E1E).copy(alpha = (fillAlpha * 1.5f).coerceIn(0.2f, 0.75f)),
                Color(0xFF111111).copy(alpha = (fillAlpha * 1.2f).coerceIn(0.15f, 0.65f))
            )
            AppThemeMode.LIGHT -> listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.88f),
                Color(0xFFF1F5F9).copy(alpha = 0.76f)
            )
            AppThemeMode.SYSTEM -> listOf(
                Color(0xFF283B5E).copy(alpha = fillAlpha + 0.08f),
                Color(0xFF141F36).copy(alpha = fillAlpha)
            )
        }
        Brush.verticalGradient(colors = fillColors)
    }

    val strokeBrush = remember(effectiveMode, strokeAlpha) {
        val strokeColors = when (effectiveMode) {
            AppThemeMode.LIGHT -> listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFCBD5E1).copy(alpha = 0.7f),
                Color(0xFF94A3B8).copy(alpha = 0.35f)
            )
            AppThemeMode.AMOLED -> listOf(
                Color(0xFF454545).copy(alpha = strokeAlpha),
                Color(0xFF2E2E2E).copy(alpha = strokeAlpha * 0.5f),
                Color(0xFF1A1A1A).copy(alpha = strokeAlpha * 0.2f)
            )
            else -> listOf(
                Color.White.copy(alpha = strokeAlpha),
                Color.White.copy(alpha = strokeAlpha * 0.25f),
                Color(0xFF88A0C0).copy(alpha = strokeAlpha * 0.15f)
            )
        }
        Brush.verticalGradient(colors = strokeColors)
    }

    return this
        .clip(shape)
        .background(fillBrush, shape = shape)
        .border(width = 1.dp, brush = strokeBrush, shape = shape)
}

/**
 * Reusable Glassmorphic Card Container
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    fillAlpha: Float = 0.22f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val baseModifier = modifier.glassmorphic(shape = shape, fillAlpha = fillAlpha)
    val finalModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier
    Box(
        modifier = finalModifier,
        content = content
    )
}

/**
 * Glassmorphic Pill Button (e.g. "Play Now", "View Lyrics", "Play", "Shuffle")
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlight: Boolean = false,
    contentColor: Color = Color.White
) {
    val themeMode = LocalThemeMode.current
    val effectiveTextColor = if (themeMode == AppThemeMode.LIGHT && !highlight && contentColor == Color.White) {
        Color(0xFF0F172A)
    } else contentColor

    val bgBrush = remember(themeMode, highlight) {
        if (highlight) {
            Brush.horizontalGradient(
                listOf(
                    RanjhaNeonPink.copy(alpha = 0.85f),
                    RanjhaNeonPurple.copy(alpha = 0.95f)
                )
            )
        } else if (themeMode == AppThemeMode.LIGHT) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.9f),
                    Color(0xFFEEF2F8).copy(alpha = 0.8f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x353D5278),
                    Color(0x221B273E)
                )
            )
        }
    }

    val borderBrush = remember(themeMode, highlight) {
        Brush.verticalGradient(
            listOf(
                if (highlight) Color.White.copy(alpha = 0.6f)
                else if (themeMode == AppThemeMode.LIGHT) Color(0x6094A3B8)
                else Color(0x45FFFFFF),
                Color(0x15FFFFFF)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(bgBrush, shape = RoundedCornerShape(24.dp))
            .border(1.dp, borderBrush, shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveTextColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = effectiveTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Circular Glass Action Button for top bars and media action buttons
 */
@Composable
fun CircularGlassButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Color.White,
    badgeCount: Int = 0,
    hasRedDot: Boolean = false
) {
    val themeMode = LocalThemeMode.current
    val effectiveTint = if (themeMode == AppThemeMode.LIGHT && tint == Color.White) {
        Color(0xFF0F172A)
    } else tint

    val bgBrush = remember(themeMode) {
        if (themeMode == AppThemeMode.LIGHT) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.85f),
                    Color(0xFFE2E8F0).copy(alpha = 0.7f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x303B4F73),
                    Color(0x1C1C283F)
                )
            )
        }
    }

    val borderBrush = remember(themeMode) {
        Brush.verticalGradient(
            listOf(
                if (themeMode == AppThemeMode.LIGHT) Color(0x6094A3B8) else Color(0x40FFFFFF),
                Color(0x10FFFFFF)
            )
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(bgBrush, shape = CircleShape)
            .border(1.dp, borderBrush, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = effectiveTint,
            modifier = Modifier.size(iconSize)
        )

        if (hasRedDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3860))
            )
        } else if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(RanjhaNeonPink),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Lightweight single-Canvas animated audio waveform equalizer bars.
 * Uses 1 animation clock instead of N layout recompositions, and stops completely when paused.
 */
@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    activeColor: Color = RanjhaCyan,
    minHeight: Dp = 4.dp,
    maxHeight: Dp = 24.dp
) {
    val phaseState = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveformAnim")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val barBrush = remember(activeColor) {
        Brush.verticalGradient(listOf(activeColor, RanjhaNeonPurple))
    }

    val totalWidth = (barCount * 5.5f).dp
    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(maxHeight)
    ) {
        val phase = phaseState.value
        val barWidthPx = 3.dp.toPx()
        val gapPx = 2.5.dp.toPx()
        val minHPx = minHeight.toPx()
        val maxHPx = size.height
        val cornerPx = 1.5.dp.toPx()

        for (i in 0 until barCount) {
            val scale = if (isPlaying) {
                val wave = abs(sin(phase + i * 0.65f).toFloat())
                0.2f + 0.8f * wave
            } else {
                0.25f
            }
            val h = minHPx + (maxHPx - minHPx) * scale
            val x = i * (barWidthPx + gapPx)
            val y = (maxHPx - h) / 2f
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, h),
                cornerRadius = CornerRadius(cornerPx, cornerPx)
            )
        }
    }
}
