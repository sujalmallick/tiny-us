package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.data.Friend
import com.example.engine.CharacterPose
import com.example.engine.Direction
import com.example.engine.PixelArtRenderer
import com.example.engine.PixelCharacter
import com.example.resources.*
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import org.jetbrains.compose.resources.stringResource

/*
 * Plan 09, I: a friend's small secret, opened by the first favourite gift. Bao's letter he never
 * sent, Leo's sketchbook (he's been drawing the two of them), and Pip's treasure under the pier.
 */

@Composable
fun FriendSecretDialog(friend: Friend, boyName: String, girlName: String, onDismiss: () -> Unit) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("secret_dialog")) {
        FriendSecretPanel(friend, boyName, girlName, onDismiss)
    }
}

@Composable
fun ColumnScope.FriendSecretPanel(friend: Friend, boyName: String, girlName: String, onDismiss: () -> Unit) {
    val (title, intro) = when (friend) {
        Friend.BAO -> Res.string.secret_bao_title to Res.string.secret_bao_intro
        Friend.LEO -> Res.string.secret_leo_title to Res.string.secret_leo_intro
        Friend.PIP -> Res.string.secret_pip_title to Res.string.secret_pip_intro
    }
    TinyDialogHeader(title = stringResource(title), icon = PixelIcons.AutoStories, onClose = onDismiss)
    Text(stringResource(intro), style = TinyType.Body)
    when (friend) {
        Friend.BAO -> {
            // The letter itself, on old paper
            Column(
                Modifier.fillMaxWidth()
                    .clip(TinyRadius.Medium)
                    .background(Color(0xFFF6E7C8))
                    .border(1.dp, Color(0xFFD9B99B), TinyRadius.Medium)
                    .padding(TinySpace.lg)
            ) {
                Text(
                    stringResource(Res.string.secret_bao_letter),
                    style = TinyType.Body.copy(fontStyle = FontStyle.Italic, color = Color(0xFF4A3425))
                )
            }
        }
        Friend.LEO -> {
            Canvas(
                Modifier.fillMaxWidth().height(170.dp).clip(TinyRadius.Medium).background(Color(0xFFFBF7EE))
                    .border(1.dp, Color(0xFFE2D6C2), TinyRadius.Medium)
            ) { drawSketchOfThem(this, boyName, girlName) }
            Text(stringResource(Res.string.secret_leo_caption, boyName, girlName), style = TinyType.Caption)
        }
        Friend.PIP -> {
            Canvas(
                Modifier.fillMaxWidth().height(120.dp).clip(TinyRadius.Medium).background(Color(0xFF8C6A4A))
            ) { drawPipsTreasure(this) }
            Text(stringResource(Res.string.secret_pip_list), style = TinyType.Caption)
        }
    }
    TinyButton(text = stringResource(Res.string.secret_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
}

/** Leo's page: the two of them at the cafe table, in pencil (the colours taken out). */
private fun drawSketchOfThem(scope: androidx.compose.ui.graphics.drawscope.DrawScope, boyName: String, girlName: String) {
    val w = scope.size.width
    val h = scope.size.height
    // Faint ruled lines
    for (i in 1..6) scope.drawRect(Color(0x14000000), Offset(0f, h * i / 7f), Size(w, 1f))
    val p = h / 40f
    val boy = PixelCharacter(isGirl = false, name = boyName, worldX = 0f, worldY = 0f, pose = CharacterPose.SIT, direction = Direction.RIGHT)
    val girl = PixelCharacter(isGirl = true, name = girlName, worldX = 0f, worldY = 0f, pose = CharacterPose.SIT, direction = Direction.LEFT)
    val pencil = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        alpha = 0.85f
    }
    scope.drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(0f, 0f, w, h), pencil)
        PixelArtRenderer.drawCharacter(scope, boy, w * 0.40f, h * 0.86f, pixelSize = p)
        PixelArtRenderer.drawCharacter(scope, girl, w * 0.60f, h * 0.86f, pixelSize = p)
        // The little table between them, and a heart over it in pencil
        scope.drawRect(Color(0xFF8B5A2B), Offset(w * 0.44f, h * 0.70f), Size(w * 0.12f, p))
        scope.drawRect(Color(0xFF8B5A2B), Offset(w * 0.495f, h * 0.70f), Size(p, h * 0.16f))
        canvas.restore()
    }
    val heart = Color(0xFF6C6C6C)
    val hx = w * 0.5f - 2.5f * p
    val hy = h * 0.14f
    scope.drawRect(heart, Offset(hx + p, hy), Size(p, p))
    scope.drawRect(heart, Offset(hx + 3f * p, hy), Size(p, p))
    scope.drawRect(heart, Offset(hx, hy + p), Size(5f * p, 2f * p))
    scope.drawRect(heart, Offset(hx + p, hy + 3f * p), Size(3f * p, p))
    scope.drawRect(heart, Offset(hx + 2f * p, hy + 4f * p), Size(p, p))
}

/** Pip's stash under the third board: a bottle cap, a spoon, a blue button, keys, a heart-shaped stone. */
private fun drawPipsTreasure(scope: androidx.compose.ui.graphics.drawscope.DrawScope) {
    val w = scope.size.width
    val h = scope.size.height
    val p = h / 24f
    fun cell(x: Float, y: Float, cw: Float, ch: Float, c: Color) = scope.drawRect(c, Offset(x * p, y * p), Size(cw * p, ch * p))
    // The boards above, lifted at one end
    for (i in 0 until (w / (12f * p)).toInt() + 1) cell(i * 12f, 0f, 11f, 3f, Color(0xFFB07D52))
    cell(0f, 3f, w / p, 0.6f, Color(0xFF5C3A24))
    // Sand
    cell(0f, 16f, w / p, 8f, Color(0xFFE9C89B))
    val mid = w / p / 2f
    // Bottle cap
    cell(mid - 22f, 13f, 4f, 3f, Color(0xFFC1121F)); cell(mid - 21f, 13f, 2f, 1f, Color(0xFFFFD166))
    // Silver spoon
    cell(mid - 14f, 14f, 6f, 1f, Color(0xFFD0D6DC)); cell(mid - 9f, 12.5f, 3f, 3f, Color(0xFFE9EEF2))
    // Blue button
    cell(mid - 3f, 12f, 4f, 4f, Color(0xFF4A90D9)); cell(mid - 2f, 13f, 1f, 1f, Color(0xFF1D3557)); cell(mid, 14f, 1f, 1f, Color(0xFF1D3557))
    // Keys on a ring
    cell(mid + 4f, 11f, 3f, 3f, Color(0xFFE9C46A)); cell(mid + 5f, 12f, 1f, 1f, Color(0xFF8C6A4A))
    cell(mid + 7f, 13f, 5f, 1f, Color(0xFFE9C46A)); cell(mid + 10f, 14f, 1f, 1f, Color(0xFFE9C46A))
    // The heart-shaped stone, a little apart, glinting
    val hx = mid + 16f
    cell(hx + 1f, 11f, 1f, 1f, Color(0xFFFF8FA3)); cell(hx + 3f, 11f, 1f, 1f, Color(0xFFFF8FA3))
    cell(hx, 12f, 5f, 2f, Color(0xFFFF8FA3)); cell(hx + 1f, 14f, 3f, 1f, Color(0xFFFF8FA3)); cell(hx + 2f, 15f, 1f, 1f, Color(0xFFFF8FA3))
    cell(hx + 1f, 12f, 1f, 1f, Color(0xFFFFE5EC))
}
