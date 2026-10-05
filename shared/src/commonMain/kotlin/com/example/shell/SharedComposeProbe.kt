package com.example.shell

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HEART = listOf(
    "01100110",
    "11111111",
    "11111111",
    "11111111",
    "01111110",
    "00111100",
    "00011000"
)

/**
 * Plan 08, stage S0: the first Compose screen shared by Android and iOS. It draws a pixel heart
 * with the same DrawScope calls the world renderer uses, to prove the shared UI path end to end.
 * Replaced by the real world renderer in stage S3.
 */
@Composable
fun SharedComposeProbe(modifier: Modifier = Modifier) {
    val beat by rememberInfiniteTransition(label = "heartbeat").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 600, easing = LinearEasing), RepeatMode.Reverse),
        label = "beat"
    )
    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF1A1B29)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val cell = size.minDimension / 10f * beat
            val left = (size.width - cell * 8) / 2f
            val top = (size.height - cell * 7) / 2f
            HEART.forEachIndexed { row, line ->
                line.forEachIndexed { col, bit ->
                    if (bit == '1') {
                        drawRect(
                            color = if (row == 1 && col == 2) Color(0xFFFFD6E0) else Color(0xFFFF6F91),
                            topLeft = Offset(left + col * cell, top + row * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }
        }
        Text(
            text = "Shared Kotlin UI is running",
            color = Color(0xFFFFD49E),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "This screen comes from the same Compose code that will draw the Android world on iOS.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
