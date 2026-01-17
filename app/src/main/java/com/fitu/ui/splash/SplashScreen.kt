package com.fitu.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitu.R
import com.fitu.ui.theme.OrangePrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(-30f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Logo scale animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(key1 = true) {
        // Logo rotation animation
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(key1 = true) {
        // Logo fade in
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            )
        )

        // Text fade in after logo
        delay(400)
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )

        // Wait and then navigate
        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0F),
                        Color(0xFF12121A),
                        Color(0xFF0A0A0F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glow effect behind logo
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale.value)
                        .alpha(alpha.value * 0.3f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    OrangePrimary.copy(alpha = 0.6f),
                                    OrangePrimary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // App Logo - using ic_launcher
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Fitu Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale.value)
                        .rotate(rotation.value)
                        .alpha(alpha.value)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "Fitu",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your AI Fitness Companion",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.alpha(textAlpha.value)
            )
        }

        // Version at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(textAlpha.value),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "v2.0.0",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.height(80.dp)
            )
        }
    }
}
