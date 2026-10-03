package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Text
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FallenParticle
import com.example.engine.LoftSprites
import com.example.engine.ParticleType
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelParticle
import com.example.engine.CharacterMotionTween
import com.example.engine.WorldSprites
import com.example.engine.RoomTheme
import com.example.scene.EnvironmentType
import com.example.scene.WeatherType
import com.example.scene.SceneEngine
import com.example.scene.CouchPhase
import com.example.scene.CatState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal class BubblePos(var left: Float = 0f, var top: Float = 0f)

@Composable
internal fun BoxScope.PixelSpeechBubblesOverlay(
    boyText: String?,
    girlText: String?,
    boyHeadX: Float,
    boyHeadY: Float,
    girlHeadX: Float,
    girlHeadY: Float,
    viewportWidth: Float,
    viewportHeight: Float
) {
    if (boyText.isNullOrEmpty() && girlText.isNullOrEmpty()) return

    val density = LocalDensity.current
    val boyPos = remember { BubblePos() }
    val girlPos = remember { BubblePos() }

    Layout(
        content = {
            if (!boyText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .layoutId("boy")
                        .widthIn(min = 64.dp, max = 155.dp)
                        .drawBehind {
                            drawPixelBubble(
                                isGirl = false,
                                charHeadX = boyHeadX,
                                bubbleLeft = boyPos.left
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BubbleInnerRow(text = boyText, isGirl = false)
                }
            }
            if (!girlText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .layoutId("girl")
                        .widthIn(min = 64.dp, max = 155.dp)
                        .drawBehind {
                            drawPixelBubble(
                                isGirl = true,
                                charHeadX = girlHeadX,
                                bubbleLeft = girlPos.left
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BubbleInnerRow(text = girlText, isGirl = true)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        val boyMeasurable = measurables.firstOrNull { it.layoutId == "boy" }
        val girlMeasurable = measurables.firstOrNull { it.layoutId == "girl" }

        val boyPlaceable = boyMeasurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val girlPlaceable = girlMeasurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))

        val margin = with(density) { 10.dp.toPx() }
        val minGap = with(density) { 8.dp.toPx() }
        val tailHeight = with(density) { 5.dp.toPx() }
        val headGap = with(density) { 3.dp.toPx() }
        val topMargin = with(density) { 72.dp.toPx() }

        var targetBoyX = 0
        var targetBoyY = 0
        var targetGirlX = 0
        var targetGirlY = 0

        if (boyPlaceable != null && girlPlaceable == null) {
            val bw = boyPlaceable.width.toFloat()
            val bh = boyPlaceable.height.toFloat()
            val bx = (boyHeadX - bw / 2f).coerceIn(margin, (viewportWidth - bw - margin).coerceAtLeast(margin))
            val by = (boyHeadY - bh - tailHeight - headGap).coerceAtLeast(topMargin)
            targetBoyX = bx.toInt()
            targetBoyY = by.toInt()
            boyPos.left = bx
            boyPos.top = by
        } else if (boyPlaceable == null && girlPlaceable != null) {
            val gw = girlPlaceable.width.toFloat()
            val gh = girlPlaceable.height.toFloat()
            val gx = (girlHeadX - gw / 2f).coerceIn(margin, (viewportWidth - gw - margin).coerceAtLeast(margin))
            val gy = (girlHeadY - gh - tailHeight - headGap).coerceAtLeast(topMargin)
            targetGirlX = gx.toInt()
            targetGirlY = gy.toInt()
            girlPos.left = gx
            girlPos.top = gy
        } else if (boyPlaceable != null && girlPlaceable != null) {
            val bw = boyPlaceable.width.toFloat()
            val bh = boyPlaceable.height.toFloat()
            val gw = girlPlaceable.width.toFloat()
            val gh = girlPlaceable.height.toFloat()

            val boyIsLeft = boyHeadX <= girlHeadX
            val leftPlaceable = if (boyIsLeft) boyPlaceable else girlPlaceable
            val rightPlaceable = if (boyIsLeft) girlPlaceable else boyPlaceable
            val leftHeadX = if (boyIsLeft) boyHeadX else girlHeadX
            val rightHeadX = if (boyIsLeft) girlHeadX else boyHeadX
            val leftHeadY = if (boyIsLeft) boyHeadY else girlHeadY
            val rightHeadY = if (boyIsLeft) girlHeadY else boyHeadY
            val lw = leftPlaceable.width.toFloat()
            val lh = leftPlaceable.height.toFloat()
            val rw = rightPlaceable.width.toFloat()
            val rh = rightPlaceable.height.toFloat()

            val totalNeeded = lw + minGap + rw
            val availWidth = (viewportWidth - 2 * margin).coerceAtLeast(100f)

            var lx: Float
            var rx: Float
            var ly: Float
            var ry: Float

            if (totalNeeded <= availWidth) {
                // Both fit side-by-side cleanly at natural character height!
                val idealLx = leftHeadX - lw / 2f
                val idealRx = rightHeadX - rw / 2f

                if (idealLx + lw + minGap <= idealRx) {
                    // No horizontal collision when centered!
                    lx = idealLx.coerceIn(margin, (viewportWidth - lw - margin).coerceAtLeast(margin))
                    rx = idealRx.coerceIn(margin, (viewportWidth - rw - margin).coerceAtLeast(margin))
                } else {
                    // Overlap would occur: separate gracefully around midpoint between the two characters!
                    val midX = (leftHeadX + rightHeadX) / 2f
                    lx = midX - minGap / 2f - lw
                    rx = midX + minGap / 2f

                    // Clamp within screen boundaries while maintaining at least minGap separation
                    if (lx < margin) {
                        val shift = margin - lx
                        lx += shift
                        rx += shift
                    } else if (rx + rw > viewportWidth - margin) {
                        val shift = (rx + rw) - (viewportWidth - margin)
                        lx -= shift
                        rx -= shift
                    }
                    lx = lx.coerceAtLeast(margin)
                    rx = rx.coerceAtLeast(lx + lw + minGap)
                }
                ly = (leftHeadY - lh - tailHeight - headGap).coerceAtLeast(topMargin)
                ry = (rightHeadY - rh - tailHeight - headGap).coerceAtLeast(topMargin)
            } else {
                // Screen too narrow for both full bubbles side-by-side:
                // Minimal vertical stagger (only one bubble height + 6dp, NOT far away!)
                lx = (leftHeadX - lw / 2f).coerceIn(margin, (viewportWidth - lw - margin).coerceAtLeast(margin))
                rx = (rightHeadX - rw / 2f).coerceIn(margin, (viewportWidth - rw - margin).coerceAtLeast(margin))
                // Lower bubble right above its character
                ry = (rightHeadY - rh - tailHeight - headGap).coerceAtLeast(topMargin)
                // Upper bubble placed just above the lower bubble
                ly = (ry - lh - with(density) { 6.dp.toPx() }).coerceAtLeast(topMargin)
            }

            if (boyIsLeft) {
                targetBoyX = lx.toInt()
                targetBoyY = ly.toInt()
                targetGirlX = rx.toInt()
                targetGirlY = ry.toInt()
                boyPos.left = lx
                boyPos.top = ly
                girlPos.left = rx
                girlPos.top = ry
            } else {
                targetGirlX = lx.toInt()
                targetGirlY = ly.toInt()
                targetBoyX = rx.toInt()
                targetBoyY = ry.toInt()
                girlPos.left = lx
                girlPos.top = ly
                boyPos.left = rx
                boyPos.top = ry
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            boyPlaceable?.place(targetBoyX, targetBoyY)
            girlPlaceable?.place(targetGirlX, targetGirlY)
        }
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelBubble(
    isGirl: Boolean,
    charHeadX: Float,
    bubbleLeft: Float
) {
    val w = size.width
    val h = size.height
    val p = 2.dp.toPx()

    // 1. Pixelated drop shadow (offset down-right)
    drawRect(Color(0x4D000000), Offset(p, p), Size(w, h))

    val borderColor = if (isGirl) Color(0xFFFF3366) else Color(0xFF1E202C)
    val bgColor = if (isGirl) Color(0xFFFFF0F5) else Color(0xFFFFFFFF)

    // 2. Stepped corner border
    drawRect(borderColor, Offset(0f, p), Size(w, h - 2 * p))
    drawRect(borderColor, Offset(p, 0f), Size(w - 2 * p, h))

    // 3. Background fill
    drawRect(bgColor, Offset(p, p), Size(w - 2 * p, h - 2 * p))

    // 4. Pixelated speech tail pointing to character's head
    val localTailX = (charHeadX - bubbleLeft).coerceIn(6 * p, w - 6 * p)
    // Outer tail border (downward pointer)
    drawRect(borderColor, Offset(localTailX - 2.5f * p, h), Size(5 * p, 2.5f * p))
    // Inner tail fill
    drawRect(bgColor, Offset(localTailX - 1.5f * p, h - p), Size(3 * p, 2.5f * p))
}

@Composable
internal fun BubbleInnerRow(
    text: String,
    isGirl: Boolean
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val iconColor = if (isGirl) Color(0xFFFF4D6D) else Color(0xFFC9184A)
                val s = size.width
                // Pixel heart icon
                drawRect(iconColor, Offset(s * 0.15f, s * 0.15f), Size(s * 0.32f, s * 0.32f))
                drawRect(iconColor, Offset(s * 0.53f, s * 0.15f), Size(s * 0.32f, s * 0.32f))
                drawRect(iconColor, Offset(s * 0.05f, s * 0.40f), Size(s * 0.90f, s * 0.25f))
                drawRect(iconColor, Offset(s * 0.20f, s * 0.65f), Size(s * 0.60f, s * 0.20f))
                drawRect(iconColor, Offset(s * 0.35f, s * 0.85f), Size(s * 0.30f, s * 0.15f))
            }
        }
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = if (isGirl) Color(0xFFC9184A) else Color(0xFF1E202C),
            letterSpacing = 0.3.sp,
            lineHeight = 15.5.sp,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
internal fun BoxScope.PixelMessageBox(
    message: String,
    alpha: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 82.dp, start = 18.dp, end = 18.dp)
            .fillMaxWidth(0.88f)
            .graphicsLayer { this.alpha = alpha() }
            .drawBehind {
                val w = size.width
                val h = size.height
                val p = 2.5.dp.toPx() // Stepped pixel unit

                // 1. Pixelated drop shadow (offset down-right)
                drawRect(Color(0x77000000), Offset(p, p), Size(w, h))

                // 2. Stepped corner outer pixel border
                drawRect(Color(0xFF1E202C), Offset(0f, p), Size(w, h - 2 * p))
                drawRect(Color(0xFF1E202C), Offset(p, 0f), Size(w - 2 * p, h))

                // 3. Warm cream parchment background
                drawRect(Color(0xFFFFFDF8), Offset(p, p), Size(w - 2 * p, h - 2 * p))

                // 4. Retro inner trim line (blush pink / rose)
                val trimColor = Color(0xFFFFB5C2)
                drawRect(trimColor, Offset(2 * p, 2 * p), Size(w - 4 * p, p)) // Top
                drawRect(trimColor, Offset(2 * p, h - 3 * p), Size(w - 4 * p, p)) // Bottom
                drawRect(trimColor, Offset(2 * p, 2 * p), Size(p, h - 4 * p)) // Left
                drawRect(trimColor, Offset(w - 3 * p, 2 * p), Size(p, h - 4 * p)) // Right

                // 5. Corner pixel gem rivets
                val gemColor = Color(0xFFC9184A)
                drawRect(gemColor, Offset(2 * p, 2 * p), Size(p, p))
                drawRect(gemColor, Offset(w - 3 * p, 2 * p), Size(p, p))
                drawRect(gemColor, Offset(2 * p, h - 3 * p), Size(p, p))
                drawRect(gemColor, Offset(w - 3 * p, h - 3 * p), Size(p, p))
            }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF2B2D42),
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
