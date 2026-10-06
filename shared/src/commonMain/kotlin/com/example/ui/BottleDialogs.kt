package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.Partner
import com.example.data.SealedLetter
import com.example.resources.*
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import org.jetbrains.compose.resources.stringResource

/*
 * A message in a bottle (plan 09, F): one of them writes a short note at the pier and tosses it
 * in; it washes up on the line on another day, for the other one to open. Sealed, on trust.
 */

/** Writing the note: who it's for (the other one, unless changed), the words, and the toss. */
@Composable
fun BottleWriter(
    boyName: String,
    girlName: String,
    onToss: (recipient: Partner, body: String) -> Unit,
    onDismiss: () -> Unit
) {
    var recipient by remember { mutableStateOf(Partner.GIRL) }
    var body by remember { mutableStateOf("") }
    val fromName = if (recipient == Partner.GIRL) boyName else girlName
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("bottle_writer")) {
        TinyDialogHeader(title = stringResource(Res.string.bottle_write_title), icon = PixelIcons.Favorite, onClose = onDismiss)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            TinyButton(
                text = stringResource(Res.string.bottle_write_for, girlName),
                onClick = { recipient = Partner.GIRL },
                style = if (recipient == Partner.GIRL) TinyButtonStyle.Primary else TinyButtonStyle.Outline,
                compact = true,
                testTag = "bottle_for_girl",
                modifier = Modifier.weight(1f)
            )
            TinyButton(
                text = stringResource(Res.string.bottle_write_for, boyName),
                onClick = { recipient = Partner.BOY },
                style = if (recipient == Partner.BOY) TinyButtonStyle.Primary else TinyButtonStyle.Outline,
                compact = true,
                testTag = "bottle_for_boy",
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = body,
            onValueChange = { body = it.take(400) },
            placeholder = { Text(stringResource(Res.string.bottle_write_hint)) },
            modifier = Modifier.fillMaxWidth().testTag("bottle_body"),
            minLines = 3,
            maxLines = 6,
            shape = TinyFieldShape,
            colors = tinyTextFieldColors()
        )
        Text(stringResource(Res.string.bottle_write_from, fromName), style = TinyType.Label)
        TinyButton(
            text = stringResource(Res.string.bottle_toss),
            onClick = { if (body.isNotBlank()) onToss(recipient, body) },
            enabled = body.isNotBlank(),
            testTag = "bottle_toss",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The bottle on the line: who it's for, then (when they open it) the note and who wrote it. */
@Composable
fun BottleLetterDialog(
    letter: SealedLetter,
    recipientName: String,
    onOpened: () -> Unit,
    onDismiss: () -> Unit
) {
    var open by remember(letter.id) { mutableStateOf(false) }
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("bottle_letter")) {
        TinyDialogHeader(title = stringResource(Res.string.bottle_landed_title), icon = PixelIcons.Favorite, onClose = onDismiss)
        if (!open) {
            Text(stringResource(Res.string.bottle_landed_for, recipientName), style = TinyType.Body)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                TinyButton(
                    text = stringResource(Res.string.bottle_later),
                    onClick = onDismiss,
                    style = TinyButtonStyle.Outline,
                    testTag = "bottle_later",
                    modifier = Modifier.weight(1f)
                )
                TinyButton(
                    text = stringResource(Res.string.bottle_open),
                    onClick = {
                        open = true
                        onOpened()
                    },
                    testTag = "bottle_open",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(letter.body, style = TinyType.Body, modifier = Modifier.testTag("bottle_body_text"))
            Text(stringResource(Res.string.bottle_write_from, letter.author), style = TinyType.Label)
            TinyButton(text = stringResource(Res.string.bottle_keep), onClick = onDismiss, testTag = "bottle_keep", modifier = Modifier.fillMaxWidth())
        }
    }
}
