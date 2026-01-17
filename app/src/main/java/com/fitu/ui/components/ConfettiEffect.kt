package com.fitu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.fitu.ui.theme.OrangePrimary
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

data class ConfettiParticle(
    val id: Int,
    val x: Float,
    val initialY: Float,
    val size: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float,
    val fallSpeed: Float,
    val wobbleSpeed: Float,
    val wobbleAmount: Float,
    val shape: ConfettiShape
)

enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    STAR
}

@Composable
fun ConfettiExplosion(
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    durationMillis: Int = 4000,
    onComplete: () -> Unit = {}
) {
    val density = LocalDensity.current
    
    val confettiColors = listOf(
        OrangePrimary,
        Color(0xFFFF6B6B),      // Red
        Color(0xFF4ECDC4),      // Teal
        Color(0xFFFFE66D),      // Yellow
        Color(0xFF95E1D3),      // Mint
        Color(0xFFF38181),      // Coral
        Color(0xFFAA96DA),      // Purple
        Color(0xFF81C784),      // Green
        Color(0xFF64B5F6),      // Blue
        Color(0xFFFFB74D)       // Orange
    )

    val particles = remember {
        List(particleCount) { index ->
            ConfettiParticle(
                id = index,
                x = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.5f - 0.1f,
                size = Random.nextFloat() * 12f + 6f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 360f - 180f,
                fallSpeed = Random.nextFloat() * 0.3f + 0.2f,
                wobbleSpeed = Random.nextFloat() * 4f + 2f,
                wobbleAmount = Random.nextFloat() * 0.1f + 0.02f,
                shape = ConfettiShape.entries[Random.nextInt(ConfettiShape.entries.size)]
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        )
        onComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti_wobble")
    val wobble by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val currentY = particle.initialY + progress.value * (1.5f + particle.fallSpeed)
            
            if (currentY < 1.2f) {
                val wobbleOffset = sin(wobble * particle.wobbleSpeed) * particle.wobbleAmount
                val currentX = particle.x + wobbleOffset
                val currentRotation = particle.rotation + progress.value * particle.rotationSpeed * 3f

                val x = currentX * canvasWidth
                val y = currentY * canvasHeight

                rotate(
                    degrees = currentRotation,
                    pivot = Offset(x, y)
                ) {
                    when (particle.shape) {
                        ConfettiShape.RECTANGLE -> {
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 4),
                                size = Size(particle.size, particle.size / 2)
                            )
                        }
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = particle.color,
                                radius = particle.size / 2,
                                center = Offset(x, y)
                            )
                        }
                        ConfettiShape.TRIANGLE -> {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(x, y - particle.size / 2)
                                lineTo(x - particle.size / 2, y + particle.size / 2)
                                lineTo(x + particle.size / 2, y + particle.size / 2)
                                close()
                            }
                            drawPath(path, particle.color)
                        }
                        ConfettiShape.STAR -> {
                            drawCircle(
                                color = particle.color,
                                radius = particle.size / 3,
                                center = Offset(x, y)
                            )
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 6, y - particle.size / 2),
                                size = Size(particle.size / 3, particle.size)
                            )
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 6),
                                size = Size(particle.size, particle.size / 3)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalCelebration(
    show: Boolean,
    goalType: String = "step",
    onDismiss: () -> Unit
) {
    if (show) {
        Box(modifier = Modifier.fillMaxSize()) {
            ConfettiExplosion(
                particleCount = 150,
                durationMillis = 4000,
                onComplete = onDismiss
            )
            
            // Optional: Add celebration text overlay
            CelebrationOverlay(
                goalType = goalType,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun CelebrationOverlay(
    goalType: String,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Scale up
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(300)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(200)
        )
        
        // Fade in
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300)
        )
        
        // Wait
        delay(2500)
        
        // Fade out
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(500)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier
                .androidx.compose.ui.draw.scale(scale.value)
                .androidx.compose.ui.draw.alpha(alpha.value)
        ) {
            androidx.compose.material3.Text(
                text = "🎉",
                fontSize = 72.androidx.compose.ui.unit.sp
            )
            
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.androidx.compose.foundation.layout.height(16.dp)
            )
            
            androidx.compose.material3.Text(
                text = when (goalType) {
                    "step" -> "Step Goal Reached!"
                    "calorie" -> "Calorie Goal Reached!"
                    else -> "Goal Reached!"
                },
                color = Color.White,
                fontSize = 28.androidx.compose.ui.unit.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.androidx.compose.foundation.layout.height(8.dp)
            )
            
            androidx.compose.material3.Text(
                text = "Amazing work! Keep it up! 💪",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.androidx.compose.ui.unit.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
