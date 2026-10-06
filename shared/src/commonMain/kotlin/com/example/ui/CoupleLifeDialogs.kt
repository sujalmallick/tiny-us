package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.BirthdayStore
import com.example.data.CoupleLifeStore
import com.example.data.DeciderCategory
import com.example.data.DeciderOption
import com.example.data.DinnerDecider
import com.example.data.LetterKind
import com.example.data.MakeUpChoice
import com.example.data.Partner
import com.example.data.SealedLetter
import com.example.data.ThankYouJar
import com.example.engine.GameText
import com.example.resources.*
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

/*
 * Plan 09, C: the screens for the couple-life tools. Each is a panel (for previews) inside a
 * TinyDialog. The world's side of each lives in SceneEngine's "Couple life" section.
 */

/** Which couple-life tool is open. */
enum class CoupleTool { DECIDER, JAR, OPEN_WHEN, BENCH, PHONES_DOWN }

private val WHEEL_COLORS = listOf(
    Color(0xFFFF8FAB), Color(0xFFFFD166), Color(0xFF8ECAE6), Color(0xFFB497E7),
    Color(0xFF6BBF59), Color(0xFFF4A261), Color(0xFF84A59D), Color(0xFFE5677F)
)

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit, tag: String, crossed: Boolean = false, color: Color? = null) {
    Surface(
        onClick = onClick,
        shape = TinyRadius.Pill,
        color = when {
            selected -> TinyColors.RoseSoft
            color != null -> color.copy(alpha = 0.25f)
            else -> TinyColors.Muted
        },
        border = BorderStroke(1.dp, if (selected) TinyColors.Rose else TinyColors.Line),
        modifier = Modifier.heightIn(min = 40.dp).testTag(tag)
    ) {
        Text(
            text,
            style = TinyType.Body.copy(
                color = if (crossed) TinyColors.InkMuted else TinyColors.Ink,
                textDecoration = if (crossed) TextDecoration.LineThrough else null
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

// ── Dinner Decider ──────────────────────────────────────────────────────

private enum class DeciderStep { CATEGORY, FIRST, PASS, SECOND, SPIN, RESULT }

@Composable
fun DinnerDeciderDialog(
    store: CoupleLifeStore,
    boyName: String,
    girlName: String,
    canCook: Boolean,
    onPicked: (String, DeciderCategory) -> Unit,
    onCookIt: () -> Unit,
    onDismiss: () -> Unit
) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("decider_dialog")) {
        DinnerDeciderPanel(store, boyName, girlName, canCook, onPicked, onCookIt, onDismiss)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.DinnerDeciderPanel(
    store: CoupleLifeStore,
    boyName: String,
    girlName: String,
    canCook: Boolean,
    onPicked: (String, DeciderCategory) -> Unit,
    onCookIt: () -> Unit,
    onDismiss: () -> Unit,
    random: Random = Random.Default,
    boyFirst: Boolean = random.nextBoolean()
) {
    var category by remember { mutableStateOf<DeciderCategory?>(null) }
    var options by remember { mutableStateOf(emptyList<DeciderOption>()) }
    var step by remember { mutableStateOf(DeciderStep.CATEGORY) }
    var firstVetoes by remember { mutableStateOf(emptySet<Int>()) }
    var secondVetoes by remember { mutableStateOf(emptySet<Int>()) }
    var picked by remember { mutableIntStateOf(-1) }
    var newOption by remember { mutableStateOf("") }
    val wheelAngle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val firstName = if (boyFirst) boyName else girlName
    val secondName = if (boyFirst) girlName else boyName

    fun load(c: DeciderCategory) {
        val builtIn = GameText.array(
            when (c) {
                DeciderCategory.EAT -> Res.array.decider_eat
                DeciderCategory.WATCH -> Res.array.decider_watch
                DeciderCategory.DO -> Res.array.decider_do
            }
        )
        options = DinnerDecider.wheel(builtIn, store.customOptions(c), random)
    }

    TinyDialogHeader(
        title = stringResource(Res.string.decider_title),
        subtitle = stringResource(Res.string.cl_decider_sub),
        icon = PixelIcons.Restaurant,
        onClose = onDismiss
    )
    key(step) {
        when (step) {
            DeciderStep.CATEGORY -> {
                Text(stringResource(Res.string.decider_what), style = TinyType.Section)
                Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                    for ((c, label) in listOf(
                        DeciderCategory.EAT to Res.string.decider_eat,
                        DeciderCategory.WATCH to Res.string.decider_watch,
                        DeciderCategory.DO to Res.string.decider_do
                    )) {
                        TinyChip(text = stringResource(label), selected = category == c, onClick = {
                            category = c
                            load(c)
                        }, modifier = Modifier.testTag("decider_cat_${c.name.lowercase()}"))
                    }
                }
                val c = category
                if (c != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEachIndexed { i, o -> OptionChip(o.text, false, {}, "decider_opt_$i", color = WHEEL_COLORS[i % WHEEL_COLORS.size]) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                        OutlinedTextField(
                            value = newOption, onValueChange = { newOption = it.take(40) },
                            placeholder = { Text(stringResource(Res.string.decider_add_hint)) },
                            singleLine = true, shape = TinyFieldShape, colors = tinyTextFieldColors(),
                            modifier = Modifier.weight(1f).testTag("decider_add_field")
                        )
                        TinyButton(text = stringResource(Res.string.decider_add), compact = true, style = TinyButtonStyle.Outline, onClick = {
                            if (newOption.isNotBlank()) {
                                store.addCustomOption(c, newOption, if (boyFirst) Partner.BOY else Partner.GIRL)
                                newOption = ""
                                load(c)
                            }
                        })
                    }
                    TinyButton(text = stringResource(Res.string.tg_pass_to, firstName), onClick = { step = DeciderStep.FIRST }, modifier = Modifier.fillMaxWidth(), testTag = "decider_begin")
                }
            }
            DeciderStep.FIRST, DeciderStep.SECOND -> {
                val first = step == DeciderStep.FIRST
                val mine = if (first) firstVetoes else secondVetoes
                Text(stringResource(Res.string.decider_veto_title, if (first) firstName else secondName), style = TinyType.Section)
                Text(stringResource(Res.string.decider_veto_body), style = TinyType.Caption)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEachIndexed { i, o ->
                        OptionChip(o.text, false, {
                            val next = if (i in mine) mine - i else if (mine.size < DinnerDecider.VETOES) mine + i else mine
                            if (first) firstVetoes = next else secondVetoes = next
                        }, "decider_opt_$i", crossed = i in mine, color = WHEEL_COLORS[i % WHEEL_COLORS.size])
                    }
                }
                TinyButton(text = stringResource(Res.string.decider_veto_done), onClick = {
                    step = if (first) DeciderStep.PASS else DeciderStep.SPIN
                }, modifier = Modifier.fillMaxWidth(), testTag = "decider_veto_done")
            }
            DeciderStep.PASS -> Column(
                Modifier.fillMaxWidth().heightIn(min = 180.dp).testTag("decider_pass"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TinySpace.md, Alignment.CenterVertically)
            ) {
                Text(stringResource(Res.string.tg_pass_to, secondName), style = TinyType.Section)
                Text(stringResource(Res.string.tg_no_peeking), style = TinyType.Caption)
                TinyButton(text = stringResource(Res.string.tg_ready, secondName), onClick = { step = DeciderStep.SECOND }, testTag = "decider_ready")
            }
            DeciderStep.SPIN, DeciderStep.RESULT -> {
                val survivors = DinnerDecider.survivors(options.size, firstVetoes, secondVetoes)
                val allBack = (firstVetoes + secondVetoes).size >= options.size
                Text(
                    if (allBack) stringResource(Res.string.decider_none_left) else stringResource(Res.string.decider_survivors, survivors.size),
                    style = TinyType.Caption
                )
                Wheel(options.size, survivors, wheelAngle.value, Modifier.size(180.dp).align(Alignment.CenterHorizontally))
                if (step == DeciderStep.RESULT && picked >= 0) {
                    Text(stringResource(Res.string.decider_result, options[picked].text), style = TinyType.Section, modifier = Modifier.testTag("decider_result"))
                    if (canCook && category == DeciderCategory.EAT) {
                        TinyButton(text = stringResource(Res.string.decider_cook_it), onClick = { onDismiss(); onCookIt() }, modifier = Modifier.fillMaxWidth(), style = TinyButtonStyle.Outline)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                        TinyButton(text = stringResource(Res.string.decider_again), onClick = {
                            firstVetoes = emptySet(); secondVetoes = emptySet(); picked = -1; step = DeciderStep.CATEGORY
                        }, modifier = Modifier.weight(1f), style = TinyButtonStyle.Outline, icon = PixelIcons.Refresh)
                        TinyButton(text = stringResource(Res.string.decider_done), onClick = onDismiss, modifier = Modifier.weight(1f), testTag = "decider_done")
                    }
                } else {
                    TinyButton(text = stringResource(Res.string.decider_spin), onClick = {
                        val choice = DinnerDecider.spin(survivors, random)
                        val slice = 360f / options.size
                        // Land with the chosen slice under the pointer at the top, after a few turns.
                        val target = wheelAngle.value - (wheelAngle.value % 360f) + 360f * 4 + (360f - (choice + 0.5f) * slice)
                        scope.launch {
                            wheelAngle.animateTo(target, tween(2200, easing = FastOutSlowInEasing))
                            picked = choice
                            step = DeciderStep.RESULT
                            category?.let { onPicked(options[choice].text, it) }
                        }
                    }, modifier = Modifier.fillMaxWidth(), testTag = "decider_spin")
                }
            }
        }
    }
}

/** A pixel-ish wheel of [count] slices, crossed-out ones greyed, with a pointer at the top. */
@Composable
private fun Wheel(count: Int, survivors: List<Int>, angle: Float, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val slice = 360f / count.coerceAtLeast(1)
        rotate(angle, center) {
            for (i in 0 until count) {
                val c = if (i in survivors) WHEEL_COLORS[i % WHEEL_COLORS.size] else Color(0xFFD8D2CC)
                drawArc(c, startAngle = -90f + i * slice, sweepAngle = slice, useCenter = true, topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2))
            }
        }
        drawCircle(Color(0xFFFFF8EF), r * 0.18f, center)
        // Pointer
        val w = r * 0.12f
        drawRect(Color(0xFF5B3A4A), Offset(center.x - w / 2f, 0f), Size(w, r * 0.22f))
    }
}

// ── Thank-You Jar ────────────────────────────────────────────────────────

@Composable
fun ThankYouJarDialog(
    store: CoupleLifeStore,
    boyName: String,
    girlName: String,
    onDropped: (fromBoy: Boolean, text: String, filled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("jar_dialog")) {
        ThankYouJarPanel(store, boyName, girlName, onDropped, onDismiss)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.ThankYouJarPanel(
    store: CoupleLifeStore,
    boyName: String,
    girlName: String,
    onDropped: (fromBoy: Boolean, text: String, filled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var fromBoy by remember { mutableStateOf(true) }
    var chip by remember { mutableIntStateOf(-1) }
    var text by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(store.thankYous().size) }
    var justFilled by remember { mutableStateOf(false) }
    val chips = remember { GameText.array(Res.array.thank_you_chips) }

    TinyDialogHeader(title = stringResource(Res.string.jar_title), subtitle = stringResource(Res.string.cl_jar_sub), icon = PixelIcons.Favorite, onClose = onDismiss)
    Canvas(Modifier.size(64.dp, 72.dp).align(Alignment.CenterHorizontally)) {
        val p = size.width / 16f
        drawThankYouJar(this, Offset(size.width / 2f, size.height - 4f * p), if (justFilled) ThankYouJar.FULL else count, p)
    }
    if (justFilled) {
        Text(stringResource(Res.string.jar_full_title), style = TinyType.Section, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(stringResource(Res.string.jar_full_body), style = TinyType.Caption, textAlign = TextAlign.Center)
        TinyButton(text = stringResource(Res.string.bench_done), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        return
    }
    Text(stringResource(Res.string.jar_count, count, ThankYouJar.FULL), style = TinyType.Caption, modifier = Modifier.align(Alignment.CenterHorizontally))
    Text(stringResource(Res.string.jar_from), style = TinyType.Label)
    Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
        TinyChip(text = boyName, selected = fromBoy, onClick = { fromBoy = true })
        TinyChip(text = girlName, selected = !fromBoy, onClick = { fromBoy = false })
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEachIndexed { i, c -> OptionChip(c, chip == i, { chip = if (chip == i) -1 else i; text = "" }, "jar_chip_$i") }
    }
    OutlinedTextField(
        value = text, onValueChange = { text = it.take(120); chip = -1 },
        placeholder = { Text(stringResource(Res.string.jar_text_hint)) },
        shape = TinyFieldShape, colors = tinyTextFieldColors(), maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("jar_text")
    )
    val said = if (chip >= 0) chips[chip] else text.trim()
    TinyButton(text = stringResource(Res.string.jar_drop), enabled = said.isNotEmpty(), onClick = {
        val filled = store.addThankYou(fromBoy, said) != null
        onDropped(fromBoy, said, filled)
        count = store.thankYous().size
        chip = -1
        text = ""
        justFilled = filled
    }, modifier = Modifier.fillMaxWidth(), testTag = "jar_drop")
    val jars = store.filledJars().size
    if (jars > 0) Text(stringResource(Res.string.jar_filled_count, jars), style = TinyType.Caption)
}

// ── "Open when..." letters ──────────────────────────────────────────────

@Composable
fun OpenWhenDialog(store: BirthdayStore, boyName: String, girlName: String, onDismiss: () -> Unit) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("open_when_dialog")) {
        OpenWhenPanel(store, boyName, girlName, onDismiss)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.OpenWhenPanel(store: BirthdayStore, boyName: String, girlName: String, onDismiss: () -> Unit) {
    var writing by remember { mutableStateOf(false) }
    var letters by remember { mutableStateOf(store.letters().filter { it.kind == LetterKind.OPEN_WHEN }) }
    var confirm by remember { mutableStateOf<SealedLetter?>(null) }
    var reading by remember { mutableStateOf<SealedLetter?>(null) }
    val nameOf = { p: Partner -> if (p == Partner.BOY) boyName else girlName }

    TinyDialogHeader(title = stringResource(Res.string.ow_title), subtitle = stringResource(Res.string.cl_open_when_sub), icon = PixelIcons.Mail, onClose = onDismiss)
    if (writing) {
        OpenWhenWriter(store, boyName, girlName, onDone = {
            writing = false
            letters = store.letters().filter { it.kind == LetterKind.OPEN_WHEN }
        })
        return
    }
    val waiting = letters.filter { !it.isOpened }
    if (letters.isEmpty()) Text(stringResource(Res.string.ow_none), style = TinyType.Caption)
    for (p in Partner.entries) {
        val mine = letters.filter { it.recipient == p }
        if (mine.isEmpty()) continue
        Text(stringResource(Res.string.ow_for, nameOf(p)), style = TinyType.Label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            mine.forEach { l ->
                OptionChip(
                    stringResource(Res.string.ow_letter_label, l.occasion), false,
                    { if (l.isOpened) reading = l else confirm = l },
                    "ow_letter_${l.id.take(6)}", crossed = false,
                    color = if (l.isOpened) null else TinyColors.Rose
                )
            }
        }
    }
    if (waiting.isNotEmpty()) Text(stringResource(Res.string.ow_waiting) + ": " + waiting.size, style = TinyType.Caption)
    TinyButton(text = stringResource(Res.string.ow_write), onClick = { writing = true }, modifier = Modifier.fillMaxWidth(), icon = PixelIcons.EditNote, testTag = "ow_write")

    confirm?.let { l ->
        TinyDialog(onDismissRequest = { confirm = null }) {
            Text(stringResource(Res.string.ow_open_q, l.occasion), style = TinyType.Section)
            Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                TinyButton(text = stringResource(Res.string.ow_not_yet), onClick = { confirm = null }, modifier = Modifier.weight(1f), style = TinyButtonStyle.Outline)
                TinyButton(text = stringResource(Res.string.ow_open), onClick = {
                    store.markOpened(l.id)
                    letters = store.letters().filter { it.kind == LetterKind.OPEN_WHEN }
                    reading = letters.firstOrNull { it.id == l.id }
                    confirm = null
                }, modifier = Modifier.weight(1f))
            }
        }
    }
    reading?.let { l -> SealedLetterReader(l, l.author, onClose = { reading = null }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.OpenWhenWriter(store: BirthdayStore, boyName: String, girlName: String, onDone: () -> Unit) {
    var forGirl by remember { mutableStateOf(true) }
    val occasions = remember { GameText.array(Res.array.open_when_occasions) }
    var occasion by remember { mutableIntStateOf(0) }
    var custom by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sealed by remember { mutableStateOf(false) }
    val recipient = if (forGirl) girlName else boyName
    if (sealed) {
        Text(stringResource(Res.string.ow_sealed, recipient), style = TinyType.Body)
        TinyButton(text = stringResource(Res.string.bench_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
        TinyChip(text = stringResource(Res.string.ow_for, girlName), selected = forGirl, onClick = { forGirl = true })
        TinyChip(text = stringResource(Res.string.ow_for, boyName), selected = !forGirl, onClick = { forGirl = false })
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        occasions.forEachIndexed { i, o -> OptionChip(o, custom.isEmpty() && occasion == i, { occasion = i; custom = "" }, "ow_occasion_$i") }
    }
    OutlinedTextField(
        value = custom, onValueChange = { custom = it.take(40) },
        placeholder = { Text(stringResource(Res.string.ow_custom_hint)) },
        singleLine = true, shape = TinyFieldShape, colors = tinyTextFieldColors(), modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = body, onValueChange = { body = it.take(1200) },
        placeholder = { Text(stringResource(Res.string.ow_body_hint)) },
        minLines = 4, maxLines = 8, shape = TinyFieldShape, colors = tinyTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag("ow_body")
    )
    TinyButton(text = stringResource(Res.string.ow_seal), enabled = body.isNotBlank(), onClick = {
        val when_ = custom.trim().ifEmpty { occasions.getOrElse(occasion) { "" } }
        val label = if (custom.isNotBlank() && !when_.startsWith("when", ignoreCase = true)) "when $when_" else when_
        store.seal(if (forGirl) Partner.GIRL else Partner.BOY, LetterKind.OPEN_WHEN, body, if (forGirl) boyName else girlName, occasion = label)
        sealed = true
    }, modifier = Modifier.fillMaxWidth(), testTag = "ow_seal")
}

// ── Make-Up Bench ────────────────────────────────────────────────────────

private enum class BenchStep { INTRO, FIRST, PASS, SECOND, REVEAL }

@Composable
fun MakeUpBenchDialog(
    store: CoupleLifeStore,
    boyName: String,
    girlName: String,
    onStart: () -> Unit,
    onFinish: (MakeUpChoice) -> Unit,
    onCancel: () -> Unit
) {
    TinyDialog(onDismissRequest = onCancel, modifier = Modifier.testTag("bench_dialog")) {
        MakeUpBenchPanel(store, boyName, girlName, onStart, onFinish, onCancel)
    }
}

@Composable
fun ColumnScope.MakeUpBenchPanel(
    store: CoupleLifeStore,
    boyName: String,
    girlName: String,
    onStart: () -> Unit,
    onFinish: (MakeUpChoice) -> Unit,
    onCancel: () -> Unit,
    boyFirst: Boolean = true
) {
    var step by remember { mutableStateOf(BenchStep.INTRO) }
    var boyFelt by remember { mutableStateOf("") }
    var boyNeed by remember { mutableStateOf("") }
    var girlFelt by remember { mutableStateOf("") }
    var girlNeed by remember { mutableStateOf("") }
    var keep by remember { mutableStateOf(false) }
    val firstName = if (boyFirst) boyName else girlName
    val secondName = if (boyFirst) girlName else boyName

    TinyDialogHeader(title = stringResource(Res.string.bench_title), icon = PixelIcons.Favorite, onClose = onCancel)
    key(step) {
        when (step) {
            BenchStep.INTRO -> {
                Text(stringResource(Res.string.bench_intro), style = TinyType.Body)
                TinyButton(text = stringResource(Res.string.tg_pass_to, firstName), onClick = { onStart(); step = BenchStep.FIRST }, modifier = Modifier.fillMaxWidth(), testTag = "bench_begin")
            }
            BenchStep.FIRST, BenchStep.SECOND -> {
                val first = step == BenchStep.FIRST
                val isBoy = first == boyFirst
                Text(stringResource(Res.string.bench_your_turn, if (first) firstName else secondName), style = TinyType.Section)
                OutlinedTextField(
                    value = if (isBoy) boyFelt else girlFelt,
                    onValueChange = { if (isBoy) boyFelt = it.take(300) else girlFelt = it.take(300) },
                    label = { Text(stringResource(Res.string.bench_felt)) },
                    minLines = 2, maxLines = 4, shape = TinyFieldShape, colors = tinyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("bench_felt")
                )
                OutlinedTextField(
                    value = if (isBoy) boyNeed else girlNeed,
                    onValueChange = { if (isBoy) boyNeed = it.take(300) else girlNeed = it.take(300) },
                    label = { Text(stringResource(Res.string.bench_need)) },
                    minLines = 2, maxLines = 4, shape = TinyFieldShape, colors = tinyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("bench_need")
                )
                TinyButton(text = stringResource(Res.string.decider_veto_done), onClick = {
                    step = if (first) BenchStep.PASS else BenchStep.REVEAL
                }, modifier = Modifier.fillMaxWidth(), testTag = "bench_next")
            }
            BenchStep.PASS -> Column(
                Modifier.fillMaxWidth().heightIn(min = 180.dp).testTag("bench_pass"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TinySpace.md, Alignment.CenterVertically)
            ) {
                Text(stringResource(Res.string.tg_pass_to, secondName), style = TinyType.Section)
                Text(stringResource(Res.string.tg_no_peeking), style = TinyType.Caption)
                TinyButton(text = stringResource(Res.string.tg_ready, secondName), onClick = { step = BenchStep.SECOND }, testTag = "bench_ready")
            }
            BenchStep.REVEAL -> {
                for ((name, felt, need) in listOf(Triple(boyName, boyFelt, boyNeed), Triple(girlName, girlFelt, girlNeed))) {
                    TinyCard(spacing = 4.dp) {
                        Text(name, style = TinyType.Label.copy(color = TinyColors.Rose))
                        if (felt.isNotBlank()) Text(stringResource(Res.string.bench_felt) + " " + felt, style = TinyType.Body)
                        if (need.isNotBlank()) Text(stringResource(Res.string.bench_need) + " " + need, style = TinyType.Body)
                    }
                }
                Text(stringResource(Res.string.bench_choose), style = TinyType.Section)
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = { keep = !keep }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(checked = keep, onCheckedChange = { keep = it })
                    Text(stringResource(Res.string.bench_keep), style = TinyType.Body)
                }
                for ((choice, label) in listOf(
                    MakeUpChoice.HUG to Res.string.bench_hug,
                    MakeUpChoice.TEA to Res.string.bench_tea,
                    MakeUpChoice.TALK_LATER to Res.string.bench_talk_later
                )) {
                    TinyButton(text = stringResource(label), onClick = {
                        if (keep) store.keepMakeUp(boyFelt, boyNeed, girlFelt, girlNeed, choice)
                        if (choice == MakeUpChoice.TALK_LATER) store.talkLaterAt = kotlin.time.Clock.System.now().toEpochMilliseconds()
                        onFinish(choice)
                    }, modifier = Modifier.fillMaxWidth(), style = if (choice == MakeUpChoice.HUG) TinyButtonStyle.Primary else TinyButtonStyle.Outline,
                        testTag = "bench_${choice.name.lowercase()}")
                }
            }
        }
    }
}


// ── Phones down ──────────────────────────────────────────────────────────

@Composable
fun PhonesDownDialog(onStart: (Int) -> Unit, onDismiss: () -> Unit) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("pd_dialog")) {
        TinyDialogHeader(title = stringResource(Res.string.pd_title), icon = PixelIcons.Bedtime, onClose = onDismiss)
        Text(stringResource(Res.string.pd_body), style = TinyType.Body)
        Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            for (m in listOf(15, 30, 60)) {
                TinyButton(text = stringResource(Res.string.pd_minutes, m), onClick = { onStart(m); onDismiss() }, modifier = Modifier.weight(1f),
                    style = if (m == 30) TinyButtonStyle.Primary else TinyButtonStyle.Outline, testTag = "pd_$m")
            }
        }
    }
}

/** The night-light over the world while phones are down: a soft dark veil and the time left. */
@Composable
fun PhonesDownOverlay(secondsLeft: Long, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier.background(Color(0xCC070912)).testTag("pd_overlay"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TinySpace.md)
        ) {
            val m = secondsLeft / 60
            val s = secondsLeft % 60
            Text(stringResource(Res.string.pd_left, "$m:${s.toString().padStart(2, '0')}"), style = TinyType.Section.copy(color = Color(0xFFFFE8B0)))
            TinyButton(text = stringResource(Res.string.pd_end_early), onClick = onEnd, style = TinyButtonStyle.Outline, compact = true, testTag = "pd_end")
        }
    }
}
