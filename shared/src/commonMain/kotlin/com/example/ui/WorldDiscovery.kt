package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.scene.autonomy.Discovery
import com.example.scene.autonomy.DiscoveryKind

private val StemGreen = Color(0xFF5FA05A)
private val PetalPink = Color(0xFFFF9EBB)
private val PetalCenter = Color(0xFFFFD166)
private val LeafRed = Color(0xFFD9573B)
private val LeafDark = Color(0xFF9A2F1E)
private val Paper = Color(0xFFFFF4E0)
private val PaperShade = Color(0xFFE9D8BC)
private val NoteHeart = Color(0xFFFF6F91)
private val MouseGrey = Color(0xFFB8B2C0)
private val MouseDark = Color(0xFF7D7688)
private val ShellPink = Color(0xFFFFC2CF)
private val ShellLine = Color(0xFFE38FA3)
private val PebbleGrey = Color(0xFF8E96A3)
private val Twinkle = Color(0xFFFFF3B0)

/**
 * The small thing currently waiting to be found, drawn as a tiny pixel sprite on the ground.
 * It fades in, and a soft twinkle now and then catches the eye.
 */
fun drawDiscovery(scope: DrawScope, cw: Float, ch: Float, p: Float, d: Discovery, time: Float) {
    if (!d.active) return
    val alpha = (d.age / 1.2f).coerceIn(0f, 1f)
    if (alpha <= 0f) return
    val x = cw * d.x
    val y = ch * d.y
    fun px(c: Color, dx: Float, dy: Float, w: Float, h: Float) =
        scope.drawRect(c.copy(alpha = c.alpha * alpha), Offset(x + dx * p, y + dy * p), Size(w * p, h * p))

    when (d.kind) {
        DiscoveryKind.WILDFLOWER -> {
            px(StemGreen, 0f, -3f, 1f, 3f)
            px(StemGreen, 1f, -2f, 1f, 1f)
            px(PetalPink, -1f, -5f, 3f, 1f)
            px(PetalPink, 0f, -6f, 1f, 3f)
            px(PetalCenter, 0f, -5f, 1f, 1f)
        }
        DiscoveryKind.RED_LEAF -> {
            px(LeafRed, -1f, -2f, 3f, 1f)
            px(LeafRed, 0f, -3f, 2f, 1f)
            px(LeafRed, -1f, -1f, 2f, 1f)
            px(LeafDark, 1f, -1f, 1f, 1f)
        }
        DiscoveryKind.LOVE_NOTE -> {
            px(PaperShade, -2f, -1f, 4f, 1f)
            px(Paper, -2f, -3f, 4f, 2f)
            px(NoteHeart, 0f, -2f, 1f, 1f)
        }
        DiscoveryKind.MOCHI_TOY -> {
            px(MouseGrey, -2f, -2f, 3f, 2f)
            px(MouseDark, -2f, -3f, 1f, 1f)
            px(MouseDark, 1f, -1f, 2f, 1f)
            px(Color(0xFF2B2B2B), -2f, -2f, 1f, 1f)
        }
        DiscoveryKind.SEASHELL -> {
            px(ShellPink, -2f, -2f, 4f, 2f)
            px(ShellPink, -1f, -3f, 2f, 1f)
            px(ShellLine, -1f, -2f, 1f, 2f)
            px(ShellLine, 1f, -2f, 1f, 2f)
        }
        DiscoveryKind.STAR_PEBBLE -> {
            px(PebbleGrey, -1f, -2f, 3f, 2f)
            px(Twinkle, 0f, -2f, 1f, 1f)
        }
    }

    // A soft stepped twinkle every couple of seconds, so it reads as "something's there".
    val beat = (time * 0.7f + d.x * 5f) % 2f
    if (beat < 0.35f) {
        val tw = Twinkle.copy(alpha = alpha * (1f - beat / 0.35f))
        scope.drawRect(tw, Offset(x + 1f * p, y - 9f * p), Size(p, 3f * p))
        scope.drawRect(tw, Offset(x, y - 8f * p), Size(3f * p, p))
    }
}
