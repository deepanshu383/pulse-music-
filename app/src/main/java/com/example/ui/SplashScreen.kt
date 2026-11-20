package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * Splash & Loading Screen matching Image 2 exactly:
 * - Fluid Particles Backdrop
 * - Glowing centered frosted glass Ranjha emblem
 * - Neon cursive "ranjha" & "MUSIC APP" subtitle
 * - Sleek animated glowing progress bar at the bottom
 */
@Composable
fun SplashScreen(
    onLoaded: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashProgress")

    val progressOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressBeam"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450)
        onLoaded()
    }

    FluidBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Center Hero Branding with dynamic theme adaptability
            DynamicRanjhaBranding(logoSize = 140.dp)

            // Bottom glowing horizontal progress bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(2.5.dp))
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                // Animated glowing beam moving across track
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.4f)
                        .align(Alignment.CenterStart)
                        .offset(x = (180.dp * progressOffset))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    primaryColor.copy(alpha = 0.85f),
                                    Color.White,
                                    secondaryColor.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}
