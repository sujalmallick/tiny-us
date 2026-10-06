package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.CoupleDates
import com.example.data.PolaroidMemory
import com.example.data.PreferencesManager
import com.example.engine.GameText
import com.example.games.StoryQuiz
import com.example.games.StoryQuizFacts
import com.example.games.StoryQuizTexts
import com.example.games.TinyDecks
import com.example.games.TinyGame
import com.example.games.TinyQuestion
import com.example.games.TinyRound
import com.example.games.TurnStep
import com.example.progress.LittleFirsts
import com.example.progress.ProgressState
import com.example.progress.Seen
import com.example.resources.*
import com.example.scene.SceneType
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

/**
 * Tiny Games (plan 09, B): pick one of three games, then five questions on one shared phone.
 * Private picks hide behind a "pass the phone" card; each reveal plays out in the world through
 * [onReveal]; the round's score goes to [onRoundFinished].
 */
@Composable
fun TinyGamesDialog(
    prefs: PreferencesManager,
    progress: ProgressState,
    photos: List<PolaroidMemory>,
    onReveal: (game: TinyGame, question: TinyQuestion, boyPick: Int, girlPick: Int, match: Boolean) -> Unit,
    onRoundFinished: (game: TinyGame, score: Int, total: Int) -> Unit,
    onDismiss: () -> Unit,
    random: Random = Random.Default
) {
    TinyDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("tiny_games_dialog")) {
        TinyGamesPanel(prefs, progress, photos, onReveal, onRoundFinished, onDismiss, random)
    }
}

/** The games themselves, inside [TinyGamesDialog] (or any column, for previews). */
@Composable
fun ColumnScope.TinyGamesPanel(
    prefs: PreferencesManager,
    progress: ProgressState,
    photos: List<PolaroidMemory>,
    onReveal: (game: TinyGame, question: TinyQuestion, boyPick: Int, girlPick: Int, match: Boolean) -> Unit,
    onRoundFinished: (game: TinyGame, score: Int, total: Int) -> Unit,
    onDismiss: () -> Unit,
    random: Random = Random.Default,
    /** For previews: open on this round, with the pass-the-phone card up when [previewCovered]. */
    previewRound: TinyRound? = null,
    previewCovered: Boolean = false
) {
    val boyName = prefs.boyfriendName
    val girlName = prefs.girlfriendName
    val quiz = remember { StoryQuiz.questions(storyQuizFacts(prefs, progress, photos), storyQuizTexts(), random) }
    val decks = remember {
        mapOf(
            TinyGame.THIS_OR_THAT to TinyDecks.parse("tot_", GameText.array(Res.array.tiny_this_or_that)),
            TinyGame.GUESS_ME to TinyDecks.parse("gm_", GameText.array(Res.array.tiny_guess_me)),
            TinyGame.STORY_QUIZ to quiz
        )
    }
    val recent = remember { mutableSetOf<String>() }
    var round by remember { mutableStateOf(previewRound) }
    var covered by remember { mutableStateOf(previewCovered) }
    var version by remember { mutableIntStateOf(0) } // bumps after each change to the round
    var boyFirst by remember { mutableStateOf(true) }

    fun start(game: TinyGame) {
        val questions = TinyDecks.draw(decks[game].orEmpty(), TinyDecks.ROUND_SIZE, random, recent)
        recent += questions.map { it.id }
        round = TinyRound(game, questions, boyFirst)
        boyFirst = !boyFirst // the other one goes first next time
        covered = game != TinyGame.STORY_QUIZ
        version++
    }

    run {
        TinyDialogHeader(
            title = stringResource(Res.string.tg_title),
            subtitle = stringResource(Res.string.tg_subtitle),
            icon = TinyIcons.MiniGames,
            onClose = onDismiss
        )
        val r = round
        // The round is one object that changes in place, so each change bumps [version] and the
        // screens are keyed on it (otherwise Compose would skip them as unchanged).
        key(version) { when {
            r == null -> GamePicker(progress, quizReady = quiz.size >= StoryQuiz.MIN_QUESTIONS, onPick = ::start)
            r.isFinished && r.step == TurnStep.REVEAL && covered -> RoundResult(
                r,
                onAgain = { start(r.game) },
                onDone = onDismiss
            )
            covered && r.pickerIsBoy != null -> PassThePhone(
                name = if (r.pickerIsBoy == true) boyName else girlName,
                onReady = { covered = false; version++ }
            )
            else -> QuestionCard(
                round = r,
                boyName = boyName,
                girlName = girlName,
                onPick = { option ->
                    r.pick(option)
                    if (r.step == TurnStep.SECOND) covered = true
                    if (r.step == TurnStep.REVEAL) {
                        onReveal(r.game, r.current, r.boyPick(), r.girlPick(), r.isMatch)
                    }
                    version++
                },
                onNext = {
                    if (r.isFinished) {
                        onRoundFinished(r.game, r.score, r.total)
                        covered = true // shows the result
                    } else {
                        r.next()
                        covered = r.game != TinyGame.STORY_QUIZ
                    }
                    version++
                }
            )
        } }
    }
}

@Composable
private fun GamePicker(progress: ProgressState, quizReady: Boolean, onPick: (TinyGame) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
        GameCard(
            title = stringResource(Res.string.tg_this_or_that),
            body = stringResource(Res.string.tg_this_or_that_desc),
            best = progress.best[TinyGame.THIS_OR_THAT.progressId],
            onClick = { onPick(TinyGame.THIS_OR_THAT) },
            tag = "tg_pick_this_or_that"
        )
        GameCard(
            title = stringResource(Res.string.tg_guess_me),
            body = stringResource(Res.string.tg_guess_me_desc),
            best = progress.best[TinyGame.GUESS_ME.progressId],
            onClick = { onPick(TinyGame.GUESS_ME) },
            tag = "tg_pick_guess_me"
        )
        GameCard(
            title = stringResource(Res.string.tg_story_quiz),
            body = stringResource(if (quizReady) Res.string.tg_story_quiz_desc else Res.string.tg_story_quiz_locked),
            best = progress.best[TinyGame.STORY_QUIZ.progressId],
            onClick = { onPick(TinyGame.STORY_QUIZ) },
            enabled = quizReady,
            tag = "tg_pick_story_quiz"
        )
    }
}

@Composable
private fun GameCard(title: String, body: String, best: Int?, onClick: () -> Unit, tag: String, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = TinyRadius.Medium,
        color = if (enabled) TinyColors.Card else TinyColors.Muted,
        border = BorderStroke(1.dp, TinyColors.Line),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Column(Modifier.padding(TinySpace.md), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = TinyType.BodyStrong.copy(color = if (enabled) TinyColors.Ink else TinyColors.InkMuted))
            Text(body, style = TinyType.Caption)
            if (best != null && best > 0 && enabled) {
                Text(stringResource(Res.string.tg_best, best), style = TinyType.Caption.copy(color = TinyColors.Rose))
            }
        }
    }
}

@Composable
private fun PassThePhone(name: String, onReady: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).testTag("tg_pass_phone"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TinySpace.md, Alignment.CenterVertically)
    ) {
        Text(stringResource(Res.string.tg_pass_to, name), style = TinyType.Section, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.tg_no_peeking), style = TinyType.Caption)
        TinyButton(text = stringResource(Res.string.tg_ready, name), onClick = onReady, testTag = "tg_ready")
    }
}

@Composable
private fun QuestionCard(round: TinyRound, boyName: String, girlName: String, onPick: (Int) -> Unit, onNext: () -> Unit) {
    val q = round.current
    val nameOf = { isBoy: Boolean -> if (isBoy) boyName else girlName }
    Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
        Text(stringResource(Res.string.tg_question_of, round.index + 1, round.total), style = TinyType.Caption)
        val who = when (round.game) {
            TinyGame.STORY_QUIZ -> stringResource(Res.string.tg_together)
            TinyGame.GUESS_ME -> when (round.step) {
                TurnStep.FIRST -> stringResource(Res.string.tg_about_you, nameOf(round.isAboutBoy()))
                else -> stringResource(Res.string.tg_guess_for, nameOf(!round.isAboutBoy()), nameOf(round.isAboutBoy()))
            }
            TinyGame.THIS_OR_THAT -> round.pickerIsBoy?.let { stringResource(Res.string.tg_your_pick, nameOf(it)) } ?: ""
        }
        if (round.step != TurnStep.REVEAL && who.isNotEmpty()) {
            Text(who, style = TinyType.Label.copy(color = TinyColors.Rose))
        }
        Text(q.prompt, style = TinyType.Section)

        if (round.step != TurnStep.REVEAL) {
            q.options.forEachIndexed { i, option ->
                Surface(
                    onClick = { onPick(i) },
                    shape = TinyRadius.Medium,
                    color = TinyColors.Muted,
                    border = BorderStroke(1.dp, TinyColors.Line),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tg_option_$i")
                ) {
                    Text(option, style = TinyType.Body, modifier = Modifier.padding(TinySpace.md))
                }
            }
        } else {
            Reveal(round, boyName, girlName)
            TinyButton(
                text = stringResource(if (round.isFinished) Res.string.tg_finish else Res.string.tg_next),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                testTag = "tg_next"
            )
        }
    }
}

@Composable
private fun Reveal(round: TinyRound, boyName: String, girlName: String) {
    val q = round.current
    val match = round.isMatch
    Surface(
        shape = TinyRadius.Medium,
        color = if (match) TinyColors.SageSoft else TinyColors.Muted,
        modifier = Modifier.fillMaxWidth().testTag("tg_reveal")
    ) {
        Column(Modifier.padding(TinySpace.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val headline = when {
                round.game == TinyGame.STORY_QUIZ && match -> stringResource(Res.string.tg_right)
                round.game == TinyGame.STORY_QUIZ -> stringResource(Res.string.tg_wrong, q.options.getOrElse(q.correct ?: 0) { "" })
                match -> stringResource(Res.string.tg_match)
                else -> stringResource(Res.string.tg_miss)
            }
            Text(headline, style = TinyType.BodyStrong.copy(color = if (match) TinyColors.Sage else TinyColors.Ink))
            if (round.game != TinyGame.STORY_QUIZ) {
                Text("$boyName: ${q.options.getOrElse(round.boyPick()) { "" }}", style = TinyType.Body)
                Text("$girlName: ${q.options.getOrElse(round.girlPick()) { "" }}", style = TinyType.Body)
            }
        }
    }
}

@Composable
private fun RoundResult(round: TinyRound, onAgain: () -> Unit, onDone: () -> Unit) {
    val score = round.score
    Column(
        modifier = Modifier.fillMaxWidth().testTag("tg_result"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
    ) {
        Text(
            stringResource(if (round.game == TinyGame.STORY_QUIZ) Res.string.tg_result_quiz else Res.string.tg_result, score, round.total),
            style = TinyType.Section
        )
        Text(
            stringResource(
                when {
                    score * 5 >= round.total * 4 -> Res.string.tg_result_line_high
                    score * 5 >= round.total * 2 -> Res.string.tg_result_line_mid
                    else -> Res.string.tg_result_line_low
                }
            ),
            style = TinyType.Caption,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            TinyButton(text = stringResource(Res.string.tg_play_again), onClick = onAgain, modifier = Modifier.weight(1f), style = TinyButtonStyle.Outline, icon = PixelIcons.Refresh)
            TinyButton(text = stringResource(Res.string.tg_done), onClick = onDone, modifier = Modifier.weight(1f), testTag = "tg_done")
        }
    }
}

/** What the couple's own data can say, for Our Story Quiz. */
fun storyQuizFacts(prefs: PreferencesManager, progress: ProgressState, photos: List<PolaroidMemory>): StoryQuizFacts {
    val anniversary = runCatching { LocalDate.parse(prefs.anniversaryDate) }.getOrNull()
    val today = CoupleDates.today()
    fun name(id: String) = GameText.get(keepsakeName(id))
    val dishIds = listOf("pancakes", "soup", "dumplings", "cookies", "tea")
    val cooked = progress.seenSet(Seen.RECIPES)
    fun have(key: String) = (progress.keepsakes[key] ?: 0) > 0 || (progress.keepsakes["shelf:$key"] ?: 0) > 0
    val kinds = LittleFirsts.DISCOVERY_KINDS
    val catches = listOf("MINNOW", "CARP", "OLD_BOOT", "BOTTLE", "GOLDEN_FISH")
    val earned = progress.firsts.keys
    return StoryQuizFacts(
        anniversaryMonth = anniversary?.month?.number,
        anniversaryWeekday = anniversary?.dayOfWeek?.isoDayNumber,
        daysTogether = anniversary?.daysUntil(today)?.toLong(),
        photos = photos.map { it.title to it.sceneName }.filter { it.first.isNotBlank() && it.second.isNotBlank() },
        allSceneNames = SceneType.entries.map { it.title },
        dishesCooked = dishIds.filter { it in cooked }.map(::name),
        dishesNotCooked = dishIds.filter { it !in cooked }.map(::name),
        found = kinds.filter { have("discovery:$it") }.map(::name),
        notFound = kinds.filter { !have("discovery:$it") }.map(::name),
        firstsEarned = LittleFirsts.ALL.filter { it.id in earned }.map { GameText.get(it.title) },
        firstsNotEarned = LittleFirsts.ALL.filter { it.id !in earned }.map { GameText.get(it.title) },
        caught = catches.filter { have("catch:$it") }.map(::name),
        notCaught = catches.filter { !have("catch:$it") }.map(::name)
    )
}

/** The quiz's questions in the phone's language. */
fun storyQuizTexts(): StoryQuizTexts = StoryQuizTexts(
    month = GameText.get(Res.string.quiz_month),
    weekday = GameText.get(Res.string.quiz_weekday),
    days = GameText.get(Res.string.quiz_days),
    daysOption = { GameText.get(Res.string.quiz_days_option, it) },
    photo = { GameText.get(Res.string.quiz_photo, it) },
    recipe = GameText.get(Res.string.quiz_recipe),
    found = GameText.get(Res.string.quiz_found),
    first = GameText.get(Res.string.quiz_first),
    catch = GameText.get(Res.string.quiz_catch),
    // 2024-01-01 was a Monday, so these are January..December and Monday..Sunday.
    monthNames = (1..12).map { DateText.format(LocalDate(2024, it, 1), "MMMM") },
    weekdayNames = (1..7).map { DateText.format(LocalDate(2024, 1, it), "EEEE") }
)
