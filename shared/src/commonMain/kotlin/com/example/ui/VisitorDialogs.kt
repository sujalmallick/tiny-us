package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Painting
import com.example.engine.AvatarLook
import com.example.resources.*
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import org.jetbrains.compose.resources.stringResource

/*
 * Plan 11: the painter's paintings (the newest first, flip through the rest) and the old couple's
 * anniversary note.
 */

@Composable
fun PaintingsDialog(paintings: List<Painting>, boyLook: AvatarLook, girlLook: AvatarLook, boyName: String, girlName: String, onDismiss: () -> Unit) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("paintings_dialog")) {
        PaintingsPanel(paintings, boyLook, girlLook, boyName, girlName, onDismiss)
    }
}

@Composable
fun ColumnScope.PaintingsPanel(paintings: List<Painting>, boyLook: AvatarLook, girlLook: AvatarLook, boyName: String, girlName: String, onDismiss: () -> Unit) {
    if (paintings.isEmpty()) return
    var index by remember { mutableIntStateOf(paintings.lastIndex) }
    val painting = paintings[index.coerceIn(0, paintings.lastIndex)]
    TinyDialogHeader(
        title = stringResource(Res.string.painting_title),
        subtitle = stringResource(Res.string.painting_of, boyName, girlName),
        icon = PixelIcons.Palette,
        onClose = onDismiss
    )
    Canvas(Modifier.fillMaxWidth().height(220.dp).padding(TinySpace.sm)) {
        val w = size.width * 0.86f
        val h = (w * 0.72f).coerceAtMost(size.height * 0.92f)
        val ww = h / 0.72f
        drawPainting(this, painting, (size.width - ww) / 2f, (size.height - h) / 2f, ww, h, boyLook, girlLook)
    }
    val date = DateText.format(kotlinx.datetime.LocalDate.fromEpochDays(painting.epochDay.toInt()), "d MMM yyyy")
    Text(stringResource(Res.string.painting_caption, date), style = TinyType.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    if (paintings.size > 1) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.sm), verticalAlignment = Alignment.CenterVertically) {
            TinyButton(
                text = stringResource(Res.string.painting_older),
                onClick = { if (index > 0) index-- },
                style = TinyButtonStyle.Outline,
                compact = true,
                enabled = index > 0,
                modifier = Modifier.weight(1f)
            )
            Text(stringResource(Res.string.painting_count, index + 1, paintings.size), style = TinyType.Caption)
            TinyButton(
                text = stringResource(Res.string.painting_newer),
                onClick = { if (index < paintings.lastIndex) index++ },
                style = TinyButtonStyle.Outline,
                compact = true,
                enabled = index < paintings.lastIndex,
                modifier = Modifier.weight(1f)
            )
        }
    }
    TinyButton(text = stringResource(Res.string.secret_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
}

@Composable
fun AnniversaryNoteDialog(onDismiss: () -> Unit) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("old_couple_note_dialog")) {
        AnniversaryNotePanel(onDismiss)
    }
}

@Composable
fun ColumnScope.AnniversaryNotePanel(onDismiss: () -> Unit) {
    TinyDialogHeader(title = stringResource(Res.string.old_couple_note_title), icon = PixelIcons.Mail, onClose = onDismiss)
    Text(stringResource(Res.string.old_couple_note_intro), style = TinyType.Body)
    Column(
        Modifier.fillMaxWidth()
            .clip(TinyRadius.Medium)
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2D6C2), TinyRadius.Medium)
            .padding(TinySpace.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(Res.string.old_couple_note_text),
            style = TinyType.Title.copy(fontStyle = FontStyle.Italic, color = Color(0xFF4A3425)),
            textAlign = TextAlign.Center
        )
        Text(stringResource(Res.string.old_couple_note_signed), style = TinyType.Caption, modifier = Modifier.padding(top = TinySpace.md))
    }
    TinyButton(text = stringResource(Res.string.secret_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
}
