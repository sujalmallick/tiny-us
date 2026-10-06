package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LoveNoteItem
import androidx.compose.foundation.lazy.grid.items
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.ui.theme.PixelIcons
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoveNotesDialog(
    notes: List<LoveNoteItem>,
    boyfriendName: String,
    girlfriendName: String,
    onDismiss: () -> Unit,
    onAddNote: (text: String, author: String) -> Unit
) {
    var showWriteMode by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var noteAuthor by remember { mutableStateOf("From $boyfriendName") }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .padding(vertical = TinySpace.lg)
            .testTag("love_notes_dialog")
    ) {
        TinyDialogHeader(
            title = stringResource(Res.string.ui_secret_love_letters),
            subtitle = stringResource(Res.string.ui_heartfelt_words_left_for_each_other),
            icon = TinyIcons.LongDistance,
            onClose = onDismiss,
            closeTestTag = "close_love_notes"
        )

        if (!showWriteMode) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
            ) {
                items(notes) { note ->
                    NoteCard(note)
                }
            }

            TinyButton(
                text = stringResource(Res.string.ui_write_secret_letter),
                onClick = { showWriteMode = true },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary,
                icon = PixelIcons.Favorite,
                testTag = "write_note_button"
            )
        } else {
            // Write note form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TinySpace.md)
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(Res.string.ui_your_message)) },
                    placeholder = { Text(stringResource(Res.string.ui_write_something_sweet_that_will_make_the)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = TinyFieldShape,
                    colors = tinyTextFieldColors()
                )

                Text(stringResource(Res.string.ui_from), style = TinyType.Label)
                Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                    TinyChip(
                        text = stringResource(Res.string.ui_note_from, boyfriendName),
                        selected = noteAuthor == "From $boyfriendName",
                        onClick = { noteAuthor = "From $boyfriendName" }
                    )
                    TinyChip(
                        text = stringResource(Res.string.ui_note_from, girlfriendName),
                        selected = noteAuthor == "From $girlfriendName",
                        onClick = { noteAuthor = "From $girlfriendName" }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TinySpace.xs),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TinyButton(
                        text = stringResource(Res.string.ui_cancel),
                        onClick = { showWriteMode = false },
                        style = TinyButtonStyle.Ghost
                    )
                    Spacer(modifier = Modifier.width(TinySpace.sm))
                    TinyButton(
                        text = stringResource(Res.string.ui_send_to_mailbox),
                        onClick = {
                            if (noteText.isNotBlank()) {
                                onAddNote(noteText, noteAuthor)
                                showWriteMode = false
                                noteText = ""
                            }
                        },
                        style = TinyButtonStyle.Primary
                    )
                }
            }
        }
    }
}

@Composable
internal fun NoteCard(note: LoveNoteItem) {
    TinyCard(padding = 14.dp, spacing = TinySpace.sm) {
        Text(
            text = "“${note.text}”",
            style = TinyType.Body.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.author,
                style = TinyType.Label.copy(color = TinyColors.Rose),
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(TinySpace.sm))
            Text(
                text = note.date,
                style = TinyType.Micro,
                maxLines = 1
            )
        }
    }
}
