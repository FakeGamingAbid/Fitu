// app/src/main/java/com/fitu/ui/components/ShimmerEffect.kt
package com.fitu.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(shimmerBrush(), RoundedCornerShape(8.dp))
    )
}

@Composable
fun ShimmerMealCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
            )
        }
    }
}

@Composable
fun ShimmerStatsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
        )
    }
}
