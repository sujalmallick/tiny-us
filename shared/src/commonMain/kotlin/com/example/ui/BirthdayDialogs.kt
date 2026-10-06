package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Birthdays
import com.example.data.CoupleDates
import com.example.data.SealedLetter
import com.example.resources.*
import com.example.scene.SceneEngine
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/*
 * Plan 09, A: entering birthdays, the surprise's overlay (the wish and the sealed letter), and
 * writing a sealed letter ahead of the day.
 */

/** How a saved birthday reads: "Mar 14, 1998", or "Mar 14" when the year isn't shown. */
fun birthdayLabel(date: LocalDate): String =
    if (Birthdays.hasYear(date)) DateText.format(date, "MMM d, yyyy") else DateText.format(date, "MMM d")

/**
 * A birthday field: shows the date (or "Add a birthday") and opens a date picker with a
 * "Don't show the year" option. [value] and [onValueChange] use the saved `yyyy-mm-dd` text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, testTag: String? = null) {
    val date = Birthdays.parse(value)
    var picking by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = TinyType.Label)
        Spacer(Modifier.padding(top = TinySpace.xs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(TinyFieldShape)
                .background(TinyColors.Card, TinyFieldShape)
                .border(1.dp, TinyColors.Line, TinyFieldShape)
                .clickable { picking = true }
                .padding(horizontal = 14.dp, vertical = TinySpace.md)
                .let { if (testTag != null) it.testTag(testTag) else it },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(PixelIcons.CalendarMonth, contentDescription = null, tint = TinyColors.Rose)
            Spacer(Modifier.width(TinySpace.md))
            Text(
                text = date?.let(::birthdayLabel) ?: stringResource(Res.string.bday_add),
                style = if (date != null) TinyType.Body else TinyType.Body.copy(color = TinyColors.InkMuted)
            )
        }
    }
    if (!picking) return

    var hideYear by remember { mutableStateOf(date != null && !Birthdays.hasYear(date)) }
    val zone = TimeZone.currentSystemDefault()
    val initial = remember(value) {
        val shown = date?.let { if (Birthdays.hasYear(it)) it else LocalDate(2000, it.month, it.day) } ?: LocalDate(2000, 1, 1)
        shown.atStartOfDayIn(zone).toEpochMilliseconds()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val picked = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                    onValueChange(Birthdays.format(picked.month.ordinal + 1, picked.day, if (hideYear) null else picked.year))
                }
                picking = false
            }) { Text(stringResource(Res.string.ui_confirm), style = TinyType.Label.copy(color = TinyColors.Rose)) }
        },
        dismissButton = {
            TextButton(onClick = { picking = false }) {
                Text(stringResource(Res.string.ui_cancel), style = TinyType.Label.copy(color = TinyColors.InkMuted))
            }
        }
    ) {
        Column {
            DatePicker(state = state)
            Row(
                modifier = Modifier.fillMaxWidth().clickable { hideYear = !hideYear }.padding(horizontal = TinySpace.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = hideYear, onCheckedChange = { hideYear = it })
                Text(stringResource(Res.string.bday_hide_year), style = TinyType.Body)
            }
        }
    }
}

/**
 * Shows the surprise's wish and letter while they're due, over the world. Placed once next to
 * the world view; while it's on screen the surprise waits for it (without it, those beats are
 * skipped and the letter stays sealed for later).
 */
@Composable
fun BirthdaySurpriseOverlay(engine: SceneEngine, authorNameOf: (SealedLetter) -> String = { it.author }) {
    DisposableEffect(engine) {
        engine.birthdayOverlayShowing = true
        onDispose { engine.birthdayOverlayShowing = false }
    }
    val surprise = engine.birthdaySurprise
    if (surprise.wantsWish) {
        BirthdayWishDialog(onWish = { engine.submitBirthdayWish(it) })
    }
    surprise.currentLetter?.let { letter ->
        SealedLetterReader(letter, authorNameOf(letter), onClose = { engine.closeBirthdayLetter() })
    }
}

@Composable
private fun BirthdayWishDialog(onWish: (String) -> Unit) {
    var wish by remember { mutableStateOf("") }
    TinyDialog(onDismissRequest = { onWish("") }, modifier = Modifier.testTag("birthday_wish_dialog")) {
        TinyDialogHeader(
            title = stringResource(Res.string.bday_wish_title),
            subtitle = stringResource(Res.string.bday_wish_body),
            icon = PixelIcons.Favorite
        )
        OutlinedTextField(
            value = wish,
            onValueChange = { wish = it.take(200) },
            placeholder = { Text(stringResource(Res.string.bday_wish_hint)) },
            modifier = Modifier.fillMaxWidth().testTag("birthday_wish_field"),
            minLines = 2,
            maxLines = 4,
            shape = TinyFieldShape,
            colors = tinyTextFieldColors()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            TinyButton(
                text = stringResource(Res.string.bday_wish_skip),
                onClick = { onWish("") },
                modifier = Modifier.weight(1f),
                style = TinyButtonStyle.Outline
            )
            TinyButton(
                text = stringResource(Res.string.bday_wish_save),
                onClick = { onWish(wish) },
                modifier = Modifier.weight(1f),
                testTag = "birthday_wish_save"
            )
        }
    }
}

/** A sealed letter, opened. */
@Composable
fun SealedLetterReader(letter: SealedLetter, authorName: String, onClose: () -> Unit) {
    TinyDialog(onDismissRequest = onClose, modifier = Modifier.testTag("sealed_letter_reader")) {
        TinyDialogHeader(
            title = stringResource(Res.string.bday_letter_title),
            subtitle = letter.occasion.ifEmpty { null },
            icon = PixelIcons.Favorite,
            onClose = onClose
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .background(TinyColors.Card, TinyFieldShape)
                .padding(TinySpace.lg)
        ) {
            Text(letter.body, style = TinyType.Body)
            if (authorName.isNotBlank()) {
                Text(
                    stringResource(Res.string.bday_letter_from, authorName),
                    style = TinyType.Label.copy(color = TinyColors.Rose),
                    modifier = Modifier.padding(top = TinySpace.md)
                )
            }
        }
        TinyButton(text = stringResource(Res.string.bday_letter_close), onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Writing a sealed letter for [recipientName], to open on [opensOn]. [authorNames] are the two
 * names to sign with; the one who isn't the recipient comes first.
 */
@Composable
fun SealedLetterWriter(
    recipientName: String,
    authorName: String,
    opensOn: LocalDate,
    onSeal: (body: String) -> Unit,
    onDismiss: () -> Unit
) {
    var body by remember { mutableStateOf("") }
    var sealed by remember { mutableStateOf(false) }
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("sealed_letter_writer")) {
        TinyDialogHeader(
            title = stringResource(Res.string.bday_letter_write_title, recipientName),
            icon = PixelIcons.Favorite,
            onClose = onDismiss
        )
        if (sealed) {
            Text(stringResource(Res.string.bday_letter_sealed, birthdayLabel(Birthdays.next(opensOn, CoupleDates.today()))), style = TinyType.Body)
            TinyButton(text = stringResource(Res.string.bday_letter_close), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            return@TinyDialog
        }
        OutlinedTextField(
            value = body,
            onValueChange = { body = it.take(1200) },
            placeholder = { Text(stringResource(Res.string.bday_letter_write_hint)) },
            modifier = Modifier.fillMaxWidth().testTag("sealed_letter_body"),
            minLines = 4,
            maxLines = 10,
            shape = TinyFieldShape,
            colors = tinyTextFieldColors()
        )
        Text(stringResource(Res.string.bday_letter_from, authorName), style = TinyType.Label)
        TinyButton(
            text = stringResource(Res.string.bday_letter_seal),
            onClick = {
                if (body.isNotBlank()) {
                    onSeal(body)
                    sealed = true
                }
            },
            enabled = body.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            testTag = "sealed_letter_seal"
        )
    }
}

/**
 * "A birthday is coming": shown in the two weeks before either birthday (when hints are on),
 * with a button to write a sealed letter for it. Shows nothing otherwise.
 */
@Composable
fun BirthdayLetterHint(store: com.example.data.BirthdayStore, boyName: String, girlName: String, modifier: Modifier = Modifier) {
    if (!store.letterHintsEnabled) return
    val today = CoupleDates.today()
    val birthdays = store.birthdays()
    val partner = Birthdays.upcoming(today, birthdays) ?: return
    val birthday = birthdays[partner] ?: return
    val day = Birthdays.next(birthday, today)
    val recipient = if (partner == com.example.data.Partner.BOY) boyName else girlName
    val author = if (partner == com.example.data.Partner.BOY) girlName else boyName
    var writing by remember { mutableStateOf(false) }
    var sealed by remember(day) { mutableStateOf(store.hasBirthdayLetter(partner, day)) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TinyColors.RoseSoft, TinyFieldShape)
            .padding(TinySpace.md)
            .testTag("birthday_letter_hint"),
        verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
    ) {
        Text(stringResource(Res.string.bday_letter_hint_title), style = TinyType.BodyStrong)
        if (sealed) {
            Text(stringResource(Res.string.bday_letter_already_sealed), style = TinyType.Caption)
        } else {
            Text(stringResource(Res.string.bday_letter_hint_body, recipient, Birthdays.daysUntil(birthday, today)), style = TinyType.Caption)
            TinyButton(
                text = stringResource(Res.string.bday_letter_write_button),
                onClick = { writing = true },
                style = TinyButtonStyle.Secondary,
                compact = true,
                testTag = "birthday_letter_write"
            )
        }
    }
    if (writing) {
        SealedLetterWriter(
            recipientName = recipient,
            authorName = author,
            opensOn = day,
            onSeal = { body ->
                store.seal(partner, com.example.data.LetterKind.BIRTHDAY, body, author, opensOn = day)
                sealed = true
            },
            onDismiss = { writing = false }
        )
    }
}
