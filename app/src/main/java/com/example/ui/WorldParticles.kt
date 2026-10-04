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

internal fun drawGroundFallenParticles(
    scope: DrawScope,
    fallen: List<FallenParticle>,
    p: Float,
    cw: Float,
    ch: Float
) {
    if (fallen.isEmpty()) return
    val wu = com.example.scene.WeatherLayout.weatherUnit(cw, p)
    for (i in fallen.indices) {
        val fp = fallen[i]
        val px = fp.normX * cw
        val py = fp.normY * ch
        val color = fp.color.copy(alpha = fp.alpha)
        // Tossed leaves and petals tumble in the air; resting ones lie flat.
        val tumble = if (fp.isSwept) kotlin.math.abs(kotlin.math.cos(fp.sweptLife * 9f + fp.styleVariant)).coerceIn(0.35f, 1f) else 1f
        when (fp.type) {
            ParticleType.AUTUMN_LEAF -> {
                // Retro 16-bit pixel art fallen autumn leaf on grass
                val s = fp.size * wu * 0.45f * tumble
                when (fp.styleVariant % 3) {
                    0 -> {
                        // Tilted leaf with curl & darker shadow pixel
                        scope.drawRect(color, Offset(px, py), Size(s * 1.5f, s * 0.9f))
                        scope.drawRect(color, Offset(px + s * 0.4f, py - s * 0.5f), Size(s * 0.8f, s * 0.5f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.55f).coerceIn(0f, 1f)), Offset(px + s * 0.2f, py + s * 0.9f), Size(s * 1.1f, s * 0.35f))
                        scope.drawRect(Color(0xFF5E2B0C).copy(alpha = fp.alpha), Offset(px - s * 0.3f, py + s * 0.2f), Size(s * 0.35f, s * 0.3f))
                    }
                    1 -> {
                        // Staggered stepped cluster leaf
                        scope.drawRect(color, Offset(px, py), Size(s * 1.2f, s * 1.2f))
                        scope.drawRect(color, Offset(px - s * 0.4f, py + s * 0.3f), Size(s * 0.5f, s * 0.6f))
                        scope.drawRect(color, Offset(px + s * 1.1f, py + s * 0.2f), Size(s * 0.5f, s * 0.7f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.50f).coerceIn(0f, 1f)), Offset(px, py + s * 1.2f), Size(s * 1.2f, s * 0.3f))
                    }
                    else -> {
                        // Compact curled leaf
                        scope.drawRect(color, Offset(px, py), Size(s * 1.4f, s * 0.8f))
                        scope.drawRect(color, Offset(px + s * 0.2f, py + s * 0.7f), Size(s * 0.8f, s * 0.4f))
                    }
                }
            }
            ParticleType.SAKURA_PETAL -> {
                // Soft pink fallen sakura petals resting on meadow grass
                val s = fp.size * wu * 0.42f * tumble
                when (fp.styleVariant % 3) {
                    0 -> {
                        // Single delicate petal
                        scope.drawRect(color, Offset(px, py), Size(s * 1.3f, s * 0.8f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.75f).coerceIn(0f, 1f)), Offset(px + s * 0.3f, py - s * 0.35f), Size(s * 0.7f, s * 0.4f))
                    }
                    1 -> {
                        // Pair of resting petals
                        scope.drawRect(color, Offset(px, py), Size(s * 1.2f, s * 0.7f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.85f).coerceIn(0f, 1f)), Offset(px + s * 0.8f, py + s * 0.4f), Size(s * 1.0f, s * 0.6f))
                    }
                    else -> {
                        // Tiny curved petal
                        scope.drawRect(color, Offset(px, py), Size(s * 0.9f, s * 0.9f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.65f).coerceIn(0f, 1f)), Offset(px + s * 0.2f, py - s * 0.25f), Size(s * 0.5f, s * 0.3f))
                    }
                }
            }
            ParticleType.SNOWFLAKE -> {
                // Small fallen snow tufts/patches resting on the grass (not swipeable)
                val s = fp.size * wu * 0.40f
                when (fp.styleVariant % 3) {
                    0 -> {
                        // Horizontal snow cap on grass blade
                        scope.drawRect(color, Offset(px, py), Size(s * 2.0f, s * 0.7f))
                        scope.drawRect(color.copy(alpha = (fp.alpha * 0.60f).coerceIn(0f, 1f)), Offset(px + s * 0.3f, py + s * 0.7f), Size(s * 1.4f, s * 0.4f))
                    }
                    1 -> {
                        // Soft snow puff
                        scope.drawRect(color, Offset(px, py), Size(s * 1.4f, s * 1.1f))
                        scope.drawRect(color, Offset(px - s * 0.4f, py + s * 0.3f), Size(s * 0.5f, s * 0.6f))
                    }
                    else -> {
                        // Little snow dusting
                        scope.drawRect(color, Offset(px, py), Size(s * 1.6f, s * 0.6f))
                    }
                }
            }
            else -> {}
        }
    }
}

internal fun drawBackgroundSeasonalParticles(scope: DrawScope, particles: List<PixelParticle>, p: Float) {
    for (i in particles.indices) {
        val pt = particles[i]
        if ((pt.type == ParticleType.SAKURA_PETAL || pt.type == ParticleType.AUTUMN_LEAF ||
            pt.type == ParticleType.SNOWFLAKE || pt.type == ParticleType.DANDELION_FLUFF ||
            pt.type == ParticleType.WIND_BREEZE || pt.type == ParticleType.RAIN_DROP) && pt.depth < 0.96f) {
            drawSingleParticle(scope, pt, p)
        }
    }
}

/** Only the falling weather (rain, snow, petals, leaves, fluff), for the sky and ground beyond the stage. */
internal fun drawFallingWeather(scope: DrawScope, particles: List<PixelParticle>, p: Float) {
    for (i in particles.indices) {
        val pt = particles[i]
        if (pt.type == ParticleType.RAIN_DROP || pt.type == ParticleType.SNOWFLAKE ||
            pt.type == ParticleType.SAKURA_PETAL || pt.type == ParticleType.AUTUMN_LEAF ||
            pt.type == ParticleType.DANDELION_FLUFF
        ) {
            drawSingleParticle(scope, pt, p)
        }
    }
}

internal fun drawForegroundParticles(scope: DrawScope, particles: List<PixelParticle>, p: Float) {
    for (i in particles.indices) {
        val pt = particles[i]
        val isSeasonal = pt.type == ParticleType.SAKURA_PETAL || pt.type == ParticleType.AUTUMN_LEAF ||
            pt.type == ParticleType.SNOWFLAKE || pt.type == ParticleType.DANDELION_FLUFF ||
            pt.type == ParticleType.WIND_BREEZE
        val isBackgroundRain = pt.type == ParticleType.RAIN_DROP && pt.depth < 0.96f
        if ((!isSeasonal && !isBackgroundRain) || pt.depth >= 0.96f) {
            drawSingleParticle(scope, pt, p)
        }
    }
}

internal fun drawParticles(scope: DrawScope, particles: List<PixelParticle>, p: Float) {
    drawForegroundParticles(scope, particles, p)
}

internal fun drawSingleParticle(scope: DrawScope, pt: PixelParticle, p: Float) {
    val color = pt.color.copy(alpha = pt.alpha)
    // Weather sizes follow the canvas width (not the capped pixel scale), so rain, snow,
    // petals and leaves stay easy to see on wide, high-density phones.
    val wu = weatherUnit(scope, p)
    when (pt.type) {
        ParticleType.HEART -> {
            val s = pt.size
            scope.drawRect(color, Offset(pt.x - s * 0.4f, pt.y - s * 0.3f), Size(s * 0.4f, s * 0.4f))
            scope.drawRect(color, Offset(pt.x + s * 0.1f, pt.y - s * 0.3f), Size(s * 0.4f, s * 0.4f))
            scope.drawRect(color, Offset(pt.x - s * 0.5f, pt.y), Size(s * 1.1f, s * 0.4f))
            scope.drawRect(color, Offset(pt.x - s * 0.3f, pt.y + s * 0.4f), Size(s * 0.7f, s * 0.3f))
            scope.drawRect(color, Offset(pt.x - s * 0.1f, pt.y + s * 0.7f), Size(s * 0.3f, s * 0.2f))
        }
        ParticleType.LEAF, ParticleType.PETAL -> {
            scope.drawRect(color, Offset(pt.x, pt.y), Size(pt.size, pt.size * 0.7f))
        }
        ParticleType.STAR, ParticleType.SPARKLE -> {
            val s = pt.size
            scope.drawRect(color, Offset(pt.x, pt.y - s * 0.5f), Size(s * 0.35f, s * 1.4f))
            scope.drawRect(color, Offset(pt.x - s * 0.5f, pt.y), Size(s * 1.4f, s * 0.35f))
        }
        ParticleType.STEAM, ParticleType.CHIMNEY_SMOKE -> {
            scope.drawRect(color, Offset(pt.x, pt.y), Size(pt.size, pt.size))
        }
        ParticleType.FIREFLY -> {
            // Pulsating glowing firefly
            scope.drawRect(color, Offset(pt.x, pt.y), Size(pt.size, pt.size))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.4f), Offset(pt.x - p, pt.y - p), Size(pt.size + 2 * p, pt.size + 2 * p))
        }
        ParticleType.SHOOTING_STAR -> {
            // Fast streak with fading tail
            scope.drawRect(color, Offset(pt.x, pt.y), Size(5 * p, 2 * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.5f), Offset(pt.x - 6 * p, pt.y - 3 * p), Size(6 * p, 1.5f * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.2f), Offset(pt.x - 12 * p, pt.y - 6 * p), Size(6 * p, 1 * p))
        }
        ParticleType.SLEEP_Z -> {
            val s = pt.size
            scope.drawRect(color, Offset(pt.x, pt.y), Size(s, s * 0.3f))
            scope.drawRect(color, Offset(pt.x + s * 0.35f, pt.y + s * 0.35f), Size(s * 0.35f, s * 0.35f))
            scope.drawRect(color, Offset(pt.x, pt.y + s * 0.7f), Size(s, s * 0.3f))
        }
        ParticleType.WATER_RIPPLE -> {
            val rx = (pt.size * p * 0.9f).coerceAtLeast(6f)
            val ry = rx * 0.42f
            scope.drawOval(
                color = color.copy(alpha = (pt.alpha * 0.65f).coerceIn(0f, 1f)),
                topLeft = Offset(pt.x - rx, pt.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1.3f * p)
            )
        }
        ParticleType.MUSIC_NOTE -> {
            // Retro 16-bit musical eighth note
            scope.drawOval(color.copy(alpha = pt.alpha), topLeft = Offset(pt.x, pt.y + 4f * p), size = Size(3f * p, 2.5f * p))
            scope.drawRect(color.copy(alpha = pt.alpha), Offset(pt.x + 2.4f * p, pt.y), Size(1.2f * p, 5f * p))
            scope.drawRect(color.copy(alpha = pt.alpha), Offset(pt.x + 3.2f * p, pt.y), Size(2f * p, 1.8f * p))
        }
        ParticleType.RAIN_DROP -> drawRainSprite(scope, pt, p)
        ParticleType.RAIN_SPLASH -> {
            // Expanding ground splash puddle ripple
            val r = (pt.size * p * 0.85f).coerceAtLeast(8f)
            scope.drawRect(color.copy(alpha = (pt.alpha * 0.85f).coerceIn(0f, 1f)), Offset(pt.x - r, pt.y), Size(r * 2f, 1.6f * p))
            scope.drawRect(color.copy(alpha = (pt.alpha * 0.40f).coerceIn(0f, 1f)), Offset(pt.x - r * 0.5f, pt.y - 1f * p), Size(r, 1f * p))
        }
        ParticleType.SAKURA_PETAL -> drawPetalSprite(scope, pt, p)
        ParticleType.AUTUMN_LEAF -> drawLeafSprite(scope, pt, p)
        ParticleType.SNOWFLAKE -> drawSnowSprite(scope, pt, p)
        ParticleType.DANDELION_FLUFF -> {
            // Soft white floating tuft with a few seed hairs
            val f = wu * 0.9f
            scope.drawRect(color, Offset(pt.x, pt.y), Size(f, f))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.6f), Offset(pt.x - f * 0.6f, pt.y - f * 0.4f), Size(f * 2.2f, f * 0.4f))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.45f), Offset(pt.x + f * 0.3f, pt.y - f * 1.1f), Size(f * 0.4f, f * 0.9f))
        }
        ParticleType.WIND_BREEZE -> {
            // Wind gust line
            val len = wu * 7f
            scope.drawRect(color, Offset(pt.x, pt.y), Size(len, 1.6f * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.5f), Offset(pt.x - len * 0.25f, pt.y), Size(len * 0.25f, 1f * p))
            scope.drawRect(color.copy(alpha = pt.alpha * 0.4f), Offset(pt.x + len, pt.y), Size(len * 0.25f, 1f * p))
        }
    }
}

/** Visual unit for weather particles: about 1/150 of the canvas width, never below the pixel scale. */
private fun weatherUnit(scope: DrawScope, p: Float): Float = com.example.scene.WeatherLayout.weatherUnit(scope.size.width, p)

// ── Falling weather as pixel sprites ────────────────────────────────────────────────────────
// One block is [p] (a scene pixel: one game pixel in the pixel renderer). Nearer particles
// (higher depth) are drawn with 2x2 blocks; colours are solid, so weather reads crisply on
// any sky. A particle fading out after a weather change simply disappears once mostly faded.

/** Draws [rows] ('#' main colour, 'h' highlight, 's' shade) centred on the particle. */
private fun drawWeatherPattern(scope: DrawScope, pt: PixelParticle, p: Float, rows: Array<String>, main: Color, highlight: Color, shade: Color, block: Int) {
    val b = p * block
    val w = rows[0].length
    val ox = pt.x - w * b / 2f
    val oy = pt.y - rows.size * b / 2f
    for ((r, row) in rows.withIndex()) {
        for ((c, ch) in row.withIndex()) {
            val col = when (ch) {
                '#' -> main
                'h' -> highlight
                's' -> shade
                else -> continue
            }
            scope.drawRect(col, Offset(ox + c * b, oy + r * b), Size(b, b))
        }
    }
}

private fun nearBlock(pt: PixelParticle, threshold: Float) = if (pt.depth >= threshold) 2 else 1

private fun drawRainSprite(scope: DrawScope, pt: PixelParticle, p: Float) {
    if (pt.alpha < 0.3f) return
    val near = pt.depth >= 0.95f
    val body = if (near) Color(0xFFD3EAFF) else Color(0xFF8DB6DC)
    val len = if (near) 7 else 4
    // A one-pixel streak with a brighter leading tip.
    scope.drawRect(body, Offset(pt.x, pt.y - (len - 1) * p), Size(p, (len - 1) * p))
    scope.drawRect(if (near) Color(0xFFF2F9FF) else Color(0xFFB4D2EC), Offset(pt.x, pt.y), Size(p, p))
}

private val PETAL_OPEN = arrayOf(".h#", "##.", "#..")
private val PETAL_EDGE = arrayOf(".#", "#.", "#.")

private fun drawPetalSprite(scope: DrawScope, pt: PixelParticle, p: Float) {
    if (pt.alpha < 0.3f) return
    // Tumbling: the petal shows its face, then its edge, in steps.
    val open = kotlin.math.cos(pt.phase * 1.4f) > -0.2f
    val main = pt.color.copy(alpha = 1f)
    drawWeatherPattern(
        scope, pt, p, if (open) PETAL_OPEN else PETAL_EDGE,
        main, Color(0xFFFFEEF4), main, nearBlock(pt, 1.15f)
    )
}

private val LEAF_OPEN = arrayOf("..#..", ".#h#.", "##s##", ".#s#.", "..s..")
private val LEAF_EDGE = arrayOf(".#.", "#h#", ".s.", ".s.")

private fun drawLeafSprite(scope: DrawScope, pt: PixelParticle, p: Float) {
    if (pt.alpha < 0.3f) return
    val open = kotlin.math.cos(pt.phase * 1.2f) > -0.3f
    val main = pt.color.copy(alpha = 1f)
    val vein = Color(0xFF6B3410)
    val light = Color(
        red = (main.red + 0.18f).coerceAtMost(1f),
        green = (main.green + 0.14f).coerceAtMost(1f),
        blue = main.blue,
        alpha = 1f
    )
    drawWeatherPattern(scope, pt, p, if (open) LEAF_OPEN else LEAF_EDGE, main, light, vein, nearBlock(pt, 1.2f))
}

private val SNOW_SMALL = arrayOf("#")
private val SNOW_PLUS = arrayOf(".#.", "#h#", ".#.")
private val SNOW_STAR = arrayOf("..#..", ".#.#.", "##h##", ".#.#.", "..#..")

private fun drawSnowSprite(scope: DrawScope, pt: PixelParticle, p: Float) {
    if (pt.alpha < 0.3f) return
    val rows = when {
        pt.depth >= 1.15f -> SNOW_STAR
        pt.depth >= 0.82f -> SNOW_PLUS
        else -> SNOW_SMALL
    }
    val main = if (pt.depth >= 0.82f) Color(0xFFE6F2FF) else Color(0xFFC9DDF0)
    drawWeatherPattern(scope, pt, p, rows, main, Color.White, main, 1)
}
