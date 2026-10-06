package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BirthdayStore
import com.example.data.CoupleDates
import com.example.data.Festival
import com.example.data.FestivalPicks
import com.example.data.FestivalStore
import com.example.data.Festivals
import com.example.data.LetterKind
import com.example.data.Partner
import com.example.engine.GameText
import com.example.engine.PixelArtRenderer
import com.example.progress.Gifts
import com.example.progress.ProgressState
import com.example.resources.*
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/*
 * Plan 09, D: the festivals' screens. Each is a pass-the-phone flow ending in a reveal; the world
 * reacts through the callbacks (crowns, lanterns, gifts under the tree).
 */

/** What a festival's screen tells the world. */
class FestivalCallbacks(
    val onPicnic: (boyPickedForGirl: Int, girlPickedForBoy: Int) -> Unit = { _, _ -> },
    val onLanterns: () -> Unit = {},
    /** The gifts were opened: [boyGave] and [girlGave] are keepsake ids (or [HANDMADE_CARD]). */
    val onGifts: (boyGave: String, girlGave: String) -> Unit = { _, _ -> },
    /** The festival was celebrated this year (for progress). */
    val onCelebrated: (Festival) -> Unit = {}
)

const val HANDMADE_CARD = "card"

@Composable
fun FestivalDialog(
    festival: Festival,
    year: Int,
    store: FestivalStore,
    letters: BirthdayStore,
    progress: ProgressState,
    boyName: String,
    girlName: String,
    callbacks: FestivalCallbacks,
    onDismiss: () -> Unit
) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("festival_dialog")) {
        FestivalPanel(festival, year, store, letters, progress, boyName, girlName, callbacks, onDismiss)
    }
}

@Composable
fun ColumnScope.FestivalPanel(
    festival: Festival,
    year: Int,
    store: FestivalStore,
    letters: BirthdayStore,
    progress: ProgressState,
    boyName: String,
    girlName: String,
    callbacks: FestivalCallbacks,
    onDismiss: () -> Unit
) {
    val title = when (festival) {
        Festival.BLOSSOM_PICNIC -> Res.string.fest_blossom_picnic
        Festival.LANTERN_NIGHT -> Res.string.fest_lantern_night
        Festival.GIFT_EXCHANGE -> Res.string.fest_gift_exchange
    }
    TinyDialogHeader(title = stringResource(title), icon = PixelIcons.Celebration, onClose = onDismiss)
    when (festival) {
        Festival.BLOSSOM_PICNIC -> PicnicFlow(year, store, boyName, girlName, callbacks, onDismiss)
        Festival.LANTERN_NIGHT -> LanternFlow(year, store, letters, boyName, girlName, callbacks, onDismiss)
        Festival.GIFT_EXCHANGE -> GiftFlow(year, store, progress, boyName, girlName, callbacks, onDismiss)
    }
}

@Composable
private fun PassCard(name: String, onReady: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 180.dp).testTag("fest_pass"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TinySpace.md, Alignment.CenterVertically)
    ) {
        Text(stringResource(Res.string.tg_pass_to, name), style = TinyType.Section)
        Text(stringResource(Res.string.tg_no_peeking), style = TinyType.Caption)
        TinyButton(text = stringResource(Res.string.tg_ready, name), onClick = onReady, testTag = "fest_ready")
    }
}

/** The steps every festival goes through: an intro, each one's secret turn, then the reveal. */
private enum class Turn { INTRO, PASS_BOY, BOY, PASS_GIRL, GIRL, REVEAL }

// ── Blossom Picnic ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.PicnicFlow(year: Int, store: FestivalStore, boyName: String, girlName: String, cb: FestivalCallbacks, onDismiss: () -> Unit) {
    val flowers = remember { GameText.array(Res.array.festival_flowers) }
    var turn by remember { mutableStateOf(Turn.INTRO) }
    var boyPick by remember { mutableStateOf(-1) } // for her
    var girlPick by remember { mutableStateOf(-1) } // for him
    key(turn) {
        when (turn) {
            Turn.INTRO -> {
                Text(stringResource(Res.string.fest_picnic_intro), style = TinyType.Body)
                TinyButton(text = stringResource(Res.string.tg_pass_to, boyName), onClick = { turn = Turn.PASS_BOY }, modifier = Modifier.fillMaxWidth(), testTag = "fest_begin")
            }
            Turn.PASS_BOY -> PassCard(boyName) { turn = Turn.BOY }
            Turn.PASS_GIRL -> PassCard(girlName) { turn = Turn.GIRL }
            Turn.BOY, Turn.GIRL -> {
                val isBoy = turn == Turn.BOY
                Text(stringResource(Res.string.fest_picnic_pick, if (isBoy) boyName else girlName, if (isBoy) girlName else boyName), style = TinyType.Section)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    flowers.forEachIndexed { i, f ->
                        FlowerChip(f, PixelArtRenderer.FESTIVAL_FLOWERS[i % PixelArtRenderer.FESTIVAL_FLOWERS.size], "fest_flower_$i") {
                            if (isBoy) { boyPick = i; turn = Turn.PASS_GIRL } else { girlPick = i; turn = Turn.REVEAL }
                        }
                    }
                }
            }
            Turn.REVEAL -> {
                Text(stringResource(Res.string.fest_picnic_reveal, boyName, flowers.getOrElse(boyPick) { "" }, girlName), style = TinyType.Section)
                Text(stringResource(Res.string.fest_picnic_reveal, girlName, flowers.getOrElse(girlPick) { "" }, boyName), style = TinyType.Section)
                Text(stringResource(Res.string.fest_picnic_done), style = TinyType.Caption)
                TinyButton(text = stringResource(Res.string.bench_done), onClick = onDismiss, modifier = Modifier.fillMaxWidth(), testTag = "fest_done")
                LaunchedEffect(Unit) {
                    store.savePicks(Festival.BLOSSOM_PICNIC, year, FestivalPicks(boy = boyPick.toString(), girl = girlPick.toString(), revealed = true))
                    store.markCelebrated(Festival.BLOSSOM_PICNIC, year)
                    cb.onPicnic(boyPick, girlPick)
                    cb.onCelebrated(Festival.BLOSSOM_PICNIC)
                }
            }
        }
    }
}

@Composable
private fun FlowerChip(text: String, color: Color, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = TinyRadius.Pill,
        color = color.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, color),
        modifier = Modifier.heightIn(min = 44.dp).testTag(tag)
    ) {
        Text(text, style = TinyType.Body, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

// ── Lantern Night ──

@Composable
private fun ColumnScope.LanternFlow(
    year: Int, store: FestivalStore, letters: BirthdayStore, boyName: String, girlName: String, cb: FestivalCallbacks, onDismiss: () -> Unit
) {
    val today = CoupleDates.today()
    // Last summer's wishes come back first (the twist): opened as they're shown.
    val returning = remember {
        letters.letters().filter { it.kind == LetterKind.LANTERN && !it.isOpened && it.canOpen(today) }
            .also { list -> list.forEach { letters.markOpened(it.id) } }
    }
    var turn by remember { mutableStateOf(Turn.INTRO) }
    var boyWish by remember { mutableStateOf("") }
    var girlWish by remember { mutableStateOf("") }
    key(turn) {
        when (turn) {
            Turn.INTRO -> {
                for (l in returning) {
                    TinyCard(spacing = 4.dp) {
                        Text(stringResource(Res.string.fest_lantern_last_year, if (l.recipient == Partner.BOY) boyName else girlName), style = TinyType.Label.copy(color = TinyColors.Rose))
                        Text(l.body, style = TinyType.Body)
                    }
                }
                Text(stringResource(Res.string.fest_lantern_intro), style = TinyType.Body)
                TinyButton(text = stringResource(Res.string.tg_pass_to, boyName), onClick = { turn = Turn.PASS_BOY }, modifier = Modifier.fillMaxWidth(), testTag = "fest_begin")
            }
            Turn.PASS_BOY -> PassCard(boyName) { turn = Turn.BOY }
            Turn.PASS_GIRL -> PassCard(girlName) { turn = Turn.GIRL }
            Turn.BOY, Turn.GIRL -> {
                val isBoy = turn == Turn.BOY
                Text(stringResource(Res.string.fest_lantern_write, if (isBoy) boyName else girlName), style = TinyType.Section)
                OutlinedTextField(
                    value = if (isBoy) boyWish else girlWish,
                    onValueChange = { if (isBoy) boyWish = it.take(200) else girlWish = it.take(200) },
                    placeholder = { Text(stringResource(Res.string.fest_lantern_hint)) },
                    minLines = 2, maxLines = 4, shape = TinyFieldShape, colors = tinyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("fest_wish")
                )
                TinyButton(text = stringResource(Res.string.decider_veto_done), onClick = {
                    turn = if (isBoy) Turn.PASS_GIRL else Turn.REVEAL
                }, modifier = Modifier.fillMaxWidth(), testTag = "fest_wish_done")
            }
            Turn.REVEAL -> {
                var released by remember { mutableStateOf(false) }
                if (!released) {
                    TinyButton(text = stringResource(Res.string.fest_lantern_release), onClick = {
                        val southern = Festivals.isSouthern(androidx.compose.ui.text.intl.Locale.current.region)
                        val opens = LocalDate(year + 1, Festivals.month(Festival.LANTERN_NIGHT, southern), Festivals.FIRST_DAY)
                        if (boyWish.isNotBlank()) letters.seal(Partner.BOY, LetterKind.LANTERN, boyWish, boyName, opensOn = opens)
                        if (girlWish.isNotBlank()) letters.seal(Partner.GIRL, LetterKind.LANTERN, girlWish, girlName, opensOn = opens)
                        store.markCelebrated(Festival.LANTERN_NIGHT, year)
                        cb.onLanterns()
                        cb.onCelebrated(Festival.LANTERN_NIGHT)
                        released = true
                    }, modifier = Modifier.fillMaxWidth(), testTag = "fest_release")
                } else {
                    Text(stringResource(Res.string.fest_lantern_done), style = TinyType.Section)
                    TinyButton(text = stringResource(Res.string.bench_done), onClick = onDismiss, modifier = Modifier.fillMaxWidth(), testTag = "fest_done")
                }
            }
        }
    }
}

// ── Gift Exchange ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GiftFlow(
    year: Int, store: FestivalStore, progress: ProgressState, boyName: String, girlName: String, cb: FestivalCallbacks, onDismiss: () -> Unit
) {
    val saved = remember { store.picks(Festival.GIFT_EXCHANGE, year) }
    val waiting = saved.boy.isNotEmpty() && saved.girl.isNotEmpty() && !saved.revealed
    var turn by remember { mutableStateOf(if (waiting) Turn.REVEAL else Turn.INTRO) }
    var picks by remember { mutableStateOf(saved) }
    var opened by remember { mutableStateOf(false) }
    val gifts = remember(progress) { Gifts.GIVEABLE.filter { (progress.keepsakes[it] ?: 0) > 0 } + HANDMADE_CARD }
    fun giftName(id: String) = if (id == HANDMADE_CARD) GameText.get(Res.string.fest_gift_handmade) else GameText.get(keepsakeName(id))

    key(turn) {
        when (turn) {
            Turn.INTRO -> {
                Text(stringResource(Res.string.fest_gift_intro), style = TinyType.Body)
                TinyButton(text = stringResource(Res.string.tg_pass_to, boyName), onClick = { turn = Turn.PASS_BOY }, modifier = Modifier.fillMaxWidth(), testTag = "fest_begin")
            }
            Turn.PASS_BOY -> PassCard(boyName) { turn = Turn.BOY }
            Turn.PASS_GIRL -> PassCard(girlName) { turn = Turn.GIRL }
            Turn.BOY, Turn.GIRL -> {
                val isBoy = turn == Turn.BOY
                var chosen by remember { mutableStateOf("") }
                var note by remember { mutableStateOf("") }
                Text(stringResource(Res.string.fest_gift_pick, if (isBoy) boyName else girlName, if (isBoy) girlName else boyName), style = TinyType.Section)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    gifts.forEachIndexed { i, g ->
                        FlowerChip(giftName(g), if (chosen == g) TinyColors.Rose else Color(0xFFD8D2CC), "fest_gift_$i") { chosen = g }
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it.take(160) },
                    placeholder = { Text(stringResource(Res.string.fest_gift_note_hint)) },
                    minLines = 2, maxLines = 3, shape = TinyFieldShape, colors = tinyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("fest_gift_note")
                )
                TinyButton(text = stringResource(Res.string.decider_veto_done), enabled = chosen.isNotEmpty(), onClick = {
                    picks = if (isBoy) picks.copy(boy = chosen, boyNote = note.trim()) else picks.copy(girl = chosen, girlNote = note.trim())
                    if (isBoy) turn = Turn.PASS_GIRL else {
                        store.savePicks(Festival.GIFT_EXCHANGE, year, picks)
                        turn = Turn.REVEAL
                    }
                }, modifier = Modifier.fillMaxWidth(), testTag = "fest_gift_done")
            }
            Turn.REVEAL -> {
                if (!opened) {
                    Text(stringResource(Res.string.fest_gift_wrapped), style = TinyType.Section)
                    TinyButton(text = stringResource(Res.string.fest_gift_open), onClick = {
                        store.savePicks(Festival.GIFT_EXCHANGE, year, picks.copy(revealed = true))
                        store.markCelebrated(Festival.GIFT_EXCHANGE, year)
                        cb.onGifts(picks.boy, picks.girl)
                        cb.onCelebrated(Festival.GIFT_EXCHANGE)
                        opened = true
                    }, modifier = Modifier.fillMaxWidth(), testTag = "fest_open")
                    TinyButton(text = stringResource(Res.string.ow_not_yet), onClick = onDismiss, modifier = Modifier.fillMaxWidth(), style = TinyButtonStyle.Outline)
                } else {
                    for ((name, gift, note) in listOf(Triple(boyName, picks.boy, picks.boyNote), Triple(girlName, picks.girl, picks.girlNote))) {
                        TinyCard(spacing = 4.dp) {
                            Text(stringResource(Res.string.fest_gift_from, name, giftName(gift)), style = TinyType.BodyStrong)
                            if (note.isNotBlank()) Text(note, style = TinyType.Body)
                        }
                    }
                    TinyButton(text = stringResource(Res.string.bench_done), onClick = onDismiss, modifier = Modifier.fillMaxWidth(), testTag = "fest_done")
                }
            }
        }
    }
}
