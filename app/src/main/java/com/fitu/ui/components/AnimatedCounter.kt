// app/src/main/java/com/fitu/ui/components/AnimatedCounter.kt
package com.fitu.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
) {
    var oldCount by remember { mutableIntStateOf(count) }
    
    SideEffect {
        oldCount = count
    }

    Row(modifier = modifier) {
        val countString = count.toString()
        val oldCountString = oldCount.toString()
        
        for (i in countString.indices) {
            val oldChar = oldCountString.getOrNull(i)
            val newChar = countString[i]
            val char = if (oldChar == newChar) {
                oldCountString[i]
            } else {
                countString[i]
            }

            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (count > oldCount) {
                        (slideInVertically { -it } + fadeIn(tween(300))) togetherWith
                                (slideOutVertically { it } + fadeOut(tween(300)))
                    } else {
                        (slideInVertically { it } + fadeIn(tween(300))) togetherWith
                                (slideOutVertically { -it } + fadeOut(tween(300)))
                    }
                },
                label = "counter"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = style,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun AnimatedFormattedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
) {
    AnimatedCounter(
        count = count,
        modifier = modifier,
        style = style
    )
}
