package com.example.ui

import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AdventureStatus
import com.example.data.DailyMomentResponse
import com.example.data.DailyPromptCatalog
import com.example.data.DateAdventure
import com.example.data.DateAdventureCatalog
import com.example.data.LongDistanceSignal
import com.example.data.LongDistanceSignalType
import com.example.data.MiniGameCatalog
import com.example.data.MiniGameRound
import com.example.data.MiniGameType
import com.example.data.PreferencesManager
import com.example.data.RelationshipTimeManager
import com.example.data.SharedMoodType
import com.example.data.WorldEvent
import com.example.data.WorldEventBus
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import com.example.widget.TinyUsWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.example.ui.theme.PixelIcons

// -----------------------------------------------------------------------------
// 1. TINY DATE ADVENTURES DIALOG
// -----------------------------------------------------------------------------

@Composable
fun DateAdventuresDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var savedAdventures by remember { mutableStateOf(prefs.getDateAdventures()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(savedAdventures) {
        savedAdventures.map { it.category }.distinct()
    }

    val filteredList = remember(savedAdventures, selectedCategory) {
        if (selectedCategory == null) savedAdventures
        else savedAdventures.filter { it.category == selectedCategory }
    }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("date_adventures_dialog"),
        widthFraction = 0.94f,
        maxHeight = 680.dp
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_tiny_date_adventures),
            subtitle = stringResource(R.string.ui_real_world_moments_to_experience_togethe),
            icon = TinyIcons.DateAdventures,
            onClose = onDismiss,
            closeTestTag = "close_date_adventures"
        )

        // Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                CategoryChip(
                    label = stringResource(R.string.ui_all),
                    isSelected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
            }
            items(categories) { cat ->
                CategoryChip(
                    label = cat.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    isSelected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                )
            }
        }

        // Adventures List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(TinySpace.md)
        ) {
            items(filteredList, key = { it.id }) { adventure ->
                DateAdventureCard(
                    adventure = adventure,
                    boyfriendName = prefs.boyfriendName,
                    girlfriendName = prefs.girlfriendName,
                    onUpdate = { updated ->
                        val list = savedAdventures.toMutableList()
                        val idx = list.indexOfFirst { it.id == updated.id }
                        if (idx != -1) {
                            list[idx] = updated
                        } else {
                            list.add(updated)
                        }
                        prefs.saveDateAdventures(list)
                        savedAdventures = prefs.getDateAdventures()

                        if (updated.isFullyCompleted) {
                            WorldEventBus.post(
                                WorldEvent.DateAdventureCompleted(
                                    id = UUID.randomUUID().toString(),
                                    timestamp = System.currentTimeMillis(),
                                    adventureId = updated.id,
                                    title = updated.title,
                                    completedBy = if (updated.completedByBoy && updated.completedByGirl) "both" else if (updated.completedByBoy) "boy" else "girl"
                                )
                            )
                        }
                        TinyUsWidgetProvider.updateAllWidgets(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TinyChip(text = label, selected = isSelected, onClick = onClick)
}

@Composable
private fun DateAdventureCard(
    adventure: DateAdventure,
    boyfriendName: String,
    girlfriendName: String,
    onUpdate: (DateAdventure) -> Unit
) {
    val isCompleted = adventure.isFullyCompleted
    val isAccepted = adventure.status == AdventureStatus.ACCEPTED || adventure.status == AdventureStatus.IN_PROGRESS

    TinyCard(
        color = if (isCompleted) TinyColors.SageSoft else TinyColors.Card,
        spacing = TinySpace.sm
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = adventure.title,
                style = TinyType.Section,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(TinySpace.sm))
            when {
                isCompleted -> TinyTag("Completed", color = TinyColors.Sage, background = TinyColors.Card)
                isAccepted -> TinyTag("Active", color = TinyColors.Rose, background = TinyColors.RoseSoft)
                adventure.status == AdventureStatus.SKIPPED -> TinyTag("Skipped")
                else -> TinyTag("Available")
            }
        }

        Text(
            text = adventure.description,
            style = TinyType.Body.copy(color = TinyColors.InkMuted)
        )

        val artifact = adventure.unlockedArtifact
        if (!artifact.isNullOrEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    PixelIcons.CardGiftcard,
                    contentDescription = null,
                    tint = TinyColors.Rose,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Unlocks: ${artifact.replace("_", " ")} in Tiny Home",
                    style = TinyType.Micro.copy(color = TinyColors.Rose)
                )
            }
        }

        // Action / Lifecycle buttons
        if (isCompleted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    PixelIcons.Check,
                    contentDescription = null,
                    tint = TinyColors.Sage,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.ui_cherished_memory_unlocked_in_our_tiny_us),
                    style = TinyType.Caption.copy(color = TinyColors.Sage, fontWeight = FontWeight.Medium)
                )
            }
        } else if (isAccepted) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
            ) {
                Text(
                    text = stringResource(R.string.ui_asynchronous_progress),
                    style = TinyType.Micro
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                    // Partner A completion (Boy)
                    PartnerProgressToggle(
                        name = boyfriendName,
                        done = adventure.completedByBoy,
                        onClick = {
                            val nextBoy = !adventure.completedByBoy
                            val completeBoth = nextBoy && adventure.completedByGirl
                            onUpdate(
                                adventure.copy(
                                    completedByBoy = nextBoy,
                                    status = if (completeBoth) AdventureStatus.COMPLETED else AdventureStatus.IN_PROGRESS,
                                    completedTimestamp = if (completeBoth) System.currentTimeMillis() else null
                                )
                            )
                        }
                    )

                    // Partner B completion (Girl)
                    PartnerProgressToggle(
                        name = girlfriendName,
                        done = adventure.completedByGirl,
                        onClick = {
                            val nextGirl = !adventure.completedByGirl
                            val completeBoth = adventure.completedByBoy && nextGirl
                            onUpdate(
                                adventure.copy(
                                    completedByGirl = nextGirl,
                                    status = if (completeBoth) AdventureStatus.COMPLETED else AdventureStatus.IN_PROGRESS,
                                    completedTimestamp = if (completeBoth) System.currentTimeMillis() else null
                                )
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TinyButton(
                        text = stringResource(R.string.ui_complete_together),
                        onClick = {
                            onUpdate(
                                adventure.copy(
                                    completedByBoy = true,
                                    completedByGirl = true,
                                    status = AdventureStatus.COMPLETED,
                                    completedTimestamp = System.currentTimeMillis()
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        style = TinyButtonStyle.Primary,
                        icon = PixelIcons.Check
                    )
                    TinyButton(
                        text = stringResource(R.string.ui_skip),
                        onClick = {
                            onUpdate(adventure.copy(status = AdventureStatus.SKIPPED))
                        },
                        style = TinyButtonStyle.Ghost
                    )
                }
            }
        } else {
            TinyButton(
                text = stringResource(R.string.ui_accept_adventure),
                onClick = {
                    onUpdate(
                        adventure.copy(status = AdventureStatus.ACCEPTED)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary
            )
        }
    }
}

/** Per-partner completion toggle on an active adventure. Visual pill; Surface keeps a 48dp touch target. */
@Composable
private fun PartnerProgressToggle(
    name: String,
    done: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = TinyRadius.Pill,
        color = if (done) TinyColors.SageSoft else TinyColors.Muted,
        contentColor = if (done) TinyColors.Sage else TinyColors.Ink,
        border = BorderStroke(1.dp, if (done) TinyColors.Sage.copy(alpha = 0.6f) else TinyColors.Line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TinySpace.md, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (done) {
                Icon(PixelIcons.Check, contentDescription = null, tint = TinyColors.Sage, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(TinySpace.xs))
            }
            Text(
                text = name,
                style = TinyType.Micro.copy(color = if (done) TinyColors.Sage else TinyColors.Ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 2. DAILY TINY MOMENT (DAILY REFLECTION) DIALOG
// -----------------------------------------------------------------------------

@Composable
fun DailyMomentPromptDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val dayIndex = remember { RelationshipTimeManager.calculateTinyUsDay().toInt() }
    val currentPrompt = remember { DailyPromptCatalog.getPromptForDay(dayIndex) }

    var savedResponse by remember { mutableStateOf(prefs.getDailyMomentResponseForDate(todayStr, currentPrompt.id)) }
    var answerA by remember { mutableStateOf(savedResponse.boyAnswer ?: "") }
    var answerB by remember { mutableStateOf(savedResponse.girlAnswer ?: "") }
    var revealPartnerAnswers by remember {
        mutableStateOf(savedResponse.isRevealed || savedResponse.isBothAnswered)
    }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("daily_moment_prompt_dialog"),
        verticalSpacing = TinySpace.md
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_daily_tiny_moment),
            subtitle = stringResource(R.string.ui_one_gentle_reflection_for_both_of_you_to),
            icon = TinyIcons.DailyMoment,
            onClose = onDismiss
        )

        // Prompt Card
        TinyCard(spacing = TinySpace.sm) {
            TinyTag(
                text = currentPrompt.category.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                color = TinyColors.Rose,
                background = TinyColors.RoseSoft
            )
            Text(
                text = currentPrompt.question,
                style = TinyType.Section
            )
        }

        // Partner A Response (Boy)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TinySpace.xs)
        ) {
            Text(
                text = "${prefs.boyfriendName}'s reflection:",
                style = TinyType.Label
            )
            OutlinedTextField(
                value = answerA,
                onValueChange = { answerA = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.ui_write_your_thoughts), style = TinyType.Body.copy(color = TinyColors.InkMuted)) },
                maxLines = 3,
                shape = TinyFieldShape,
                colors = tinyTextFieldColors()
            )
        }

        // Partner B Response (Girl)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TinySpace.xs)
        ) {
            Text(
                text = "${prefs.girlfriendName}'s reflection:",
                style = TinyType.Label
            )
            OutlinedTextField(
                value = answerB,
                onValueChange = { answerB = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.ui_write_your_thoughts), style = TinyType.Body.copy(color = TinyColors.InkMuted)) },
                maxLines = 3,
                shape = TinyFieldShape,
                colors = tinyTextFieldColors()
            )
        }

        // Save Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TinySpace.xs),
            verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
        ) {
            TinyButton(
                text = stringResource(R.string.ui_save_our_daily_moment),
                onClick = {
                    val hasBoth = answerA.isNotBlank() && answerB.isNotBlank()
                    val updated = DailyMomentResponse(
                        promptId = currentPrompt.id,
                        dateString = todayStr,
                        boyAnswer = answerA.ifBlank { null },
                        girlAnswer = answerB.ifBlank { null },
                        isRevealed = revealPartnerAnswers || hasBoth,
                        completedTimestamp = if (hasBoth) System.currentTimeMillis() else null
                    )
                    prefs.saveDailyMomentResponse(updated)
                    savedResponse = updated
                    revealPartnerAnswers = updated.isRevealed
                    TinyUsWidgetProvider.updateAllWidgets(context)
                },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary
            )

            if (savedResponse.isAnsweredByBoy || savedResponse.isAnsweredByGirl) {
                Text(
                    text = if (revealPartnerAnswers) "Both reflections shared & unlocked on Bedside Notepad" else "Reflections preserved privately until both share",
                    style = TinyType.Caption.copy(color = if (revealPartnerAnswers) TinyColors.Sage else TinyColors.InkMuted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. TWO-PERSON MINI-GAMES DIALOG
// -----------------------------------------------------------------------------

@Composable
fun TwoPersonMiniGameDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(MiniGameType.WOULD_YOU_RATHER) }
    var questionIndex by remember { mutableIntStateOf(0) }

    val currentQuestions = remember(selectedType) {
        MiniGameCatalog.questions.filter { it.type == selectedType }
    }
    val currentQuestion = currentQuestions.getOrElse(questionIndex % currentQuestions.size.coerceAtLeast(1)) {
        MiniGameCatalog.questions.first()
    }

    var choiceAIndex by remember { mutableStateOf<Int?>(null) }
    var choiceBIndex by remember { mutableStateOf<Int?>(null) }
    var isRevealed by remember { mutableStateOf(false) }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("mini_games_dialog")
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_two_person_mini_games),
            subtitle = stringResource(R.string.ui_playful_1_minute_relationship_moments),
            icon = TinyIcons.MiniGames,
            onClose = onDismiss
        )

        // Game Type Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MiniGameType.values()) { type ->
                CategoryChip(
                    label = type.title,
                    isSelected = selectedType == type,
                    onClick = {
                        selectedType = type
                        questionIndex = 0
                        choiceAIndex = null
                        choiceBIndex = null
                        isRevealed = false
                    }
                )
            }
        }

        // Question Box
        TinyCard(spacing = TinySpace.sm) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
            ) {
                Text(
                    text = currentQuestion.prompt,
                    style = TinyType.Section,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(TinySpace.xs))

                // Choices Section for Partner A (Boy)
                Text(
                    text = "${prefs.boyfriendName}'s choice:",
                    style = TinyType.Label.copy(color = TinyColors.InkMuted)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    currentQuestion.options.forEachIndexed { idx, opt ->
                        GameOptionTile(
                            text = opt,
                            selected = choiceAIndex == idx,
                            onClick = { choiceAIndex = idx },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(TinySpace.xs))

                // Choices Section for Partner B (Girl)
                Text(
                    text = "${prefs.girlfriendName}'s choice:",
                    style = TinyType.Label.copy(color = TinyColors.InkMuted)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    currentQuestion.options.forEachIndexed { idx, opt ->
                        GameOptionTile(
                            text = opt,
                            selected = choiceBIndex == idx,
                            onClick = { choiceBIndex = idx },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // Reveal results
                if (isRevealed) {
                    Spacer(modifier = Modifier.height(TinySpace.xs))
                    val isMatch = choiceAIndex != null && choiceAIndex == choiceBIndex
                    Surface(
                        shape = TinyRadius.Medium,
                        color = if (isMatch) TinyColors.SageSoft else TinyColors.Muted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(TinySpace.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isMatch) "Match Made in Heaven!" else "Playful Perspectives!",
                                style = TinyType.Label.copy(color = if (isMatch) TinyColors.Sage else TinyColors.Ink)
                            )
                            val optA = choiceAIndex?.let { currentQuestion.options.getOrNull(it) } ?: "—"
                            val optB = choiceBIndex?.let { currentQuestion.options.getOrNull(it) } ?: "—"
                            Text(
                                text = "${prefs.boyfriendName}: \"$optA\" • ${prefs.girlfriendName}: \"$optB\"",
                                style = TinyType.Caption,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (!isRevealed) {
            TinyButton(
                text = stringResource(R.string.ui_reveal_answers),
                onClick = {
                    isRevealed = true
                    val round = MiniGameRound(
                        id = UUID.randomUUID().toString(),
                        questionId = currentQuestion.id,
                        type = selectedType,
                        prompt = currentQuestion.prompt,
                        options = currentQuestion.options,
                        boyChosenIndex = choiceAIndex,
                        girlChosenIndex = choiceBIndex,
                        isRevealed = true,
                        timestamp = System.currentTimeMillis()
                    )
                    prefs.saveMiniGameRound(round)
                    TinyUsWidgetProvider.updateAllWidgets(context)
                },
                enabled = choiceAIndex != null || choiceBIndex != null,
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary
            )
        } else {
            TinyButton(
                text = stringResource(R.string.ui_next_question),
                onClick = {
                    questionIndex = (questionIndex + 1) % currentQuestions.size.coerceAtLeast(1)
                    choiceAIndex = null
                    choiceBIndex = null
                    isRevealed = false
                },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary,
                icon = PixelIcons.Refresh
            )
        }
    }
}

/** One answer option in a mini-game round. Muted tile; selected = RoseSoft fill + Rose border. */
@Composable
private fun GameOptionTile(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = TinyRadius.Medium,
        color = if (selected) TinyColors.RoseSoft else TinyColors.Muted,
        contentColor = if (selected) TinyColors.Rose else TinyColors.Ink,
        border = BorderStroke(1.dp, if (selected) TinyColors.Rose else TinyColors.Line)
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(vertical = TinySpace.sm, horizontal = TinySpace.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TinyType.Label.copy(
                    color = if (selected) TinyColors.Rose else TinyColors.Ink,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 4. SHARED MOOD DIALOG
// -----------------------------------------------------------------------------

@Composable
fun SharedMoodDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentMoodState = remember { prefs.getPartnerMoodState() }

    var selectedMoodA by remember { mutableStateOf(currentMoodState.boyMood) }
    var isSharedA by remember { mutableStateOf(currentMoodState.boyMoodShared) }

    var selectedMoodB by remember { mutableStateOf(currentMoodState.girlMood) }
    var isSharedB by remember { mutableStateOf(currentMoodState.girlMoodShared) }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("shared_mood_dialog")
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_shared_mood),
            subtitle = stringResource(R.string.ui_a_gentle_whisper_of_how_you_feel_today),
            icon = TinyIcons.SharedMood,
            onClose = onDismiss
        )

        // Partner A Mood Section (Boy)
        PartnerMoodSection(
            partnerName = prefs.boyfriendName,
            selectedMood = selectedMoodA,
            onSelectMood = { selectedMoodA = it },
            isShared = isSharedA,
            onToggleShared = { isSharedA = it }
        )

        // Partner B Mood Section (Girl)
        PartnerMoodSection(
            partnerName = prefs.girlfriendName,
            selectedMood = selectedMoodB,
            onSelectMood = { selectedMoodB = it },
            isShared = isSharedB,
            onToggleShared = { isSharedB = it }
        )

        TinyButton(
            text = if (com.example.FeatureFlags.PARTNER_SYNC) "Share With Each Other" else "Save Our Moods",
            onClick = {
                prefs.setPartnerMood("boy", selectedMoodA, isSharedA)
                prefs.setPartnerMood("girl", selectedMoodB, isSharedB)
                TinyUsWidgetProvider.updateAllWidgets(context)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            style = TinyButtonStyle.Primary
        )
    }
}

@Composable
private fun PartnerMoodSection(
    partnerName: String,
    selectedMood: SharedMoodType,
    onSelectMood: (SharedMoodType) -> Unit,
    isShared: Boolean,
    onToggleShared: (Boolean) -> Unit
) {
    TinyCard(padding = TinySpace.md, spacing = TinySpace.sm) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$partnerName's feeling:",
                style = TinyType.Label,
                modifier = Modifier.weight(1f)
            )
            if (com.example.FeatureFlags.PARTNER_SYNC) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isShared) "Shared" else "Private",
                    style = TinyType.Micro.copy(color = if (isShared) TinyColors.Sage else TinyColors.InkMuted)
                )
                Spacer(modifier = Modifier.width(TinySpace.sm))
                Switch(
                    checked = isShared,
                    onCheckedChange = onToggleShared,
                    colors = tinySwitchColors()
                )
            }
        }

        // 5 Mood Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(TinySpace.xs),
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            SharedMoodType.values().forEach { mood ->
                val isSelected = selectedMood == mood
                Surface(
                    onClick = { onSelectMood(mood) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = TinyRadius.Medium,
                    color = if (isSelected) TinyColors.RoseSoft else TinyColors.Muted,
                    contentColor = if (isSelected) TinyColors.Rose else TinyColors.Ink,
                    border = BorderStroke(1.dp, if (isSelected) TinyColors.Rose else TinyColors.Line)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .padding(vertical = TinySpace.sm, horizontal = 2.dp)
                    ) {
                        Icon(
                            TinyIcons.mood(mood),
                            contentDescription = null,
                            tint = if (isSelected) TinyColors.Rose else TinyColors.InkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(TinySpace.xs))
                        Text(
                            text = mood.displayName,
                            style = TinyType.Micro.copy(
                                color = if (isSelected) TinyColors.Rose else TinyColors.Ink,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 5. LONG-DISTANCE MODE SIGNALS DIALOG
// -----------------------------------------------------------------------------

@Composable
fun LongDistanceSheet(
    prefs: PreferencesManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var savedSignals by remember { mutableStateOf(prefs.getLongDistanceSignals()) }
    var customNote by remember { mutableStateOf("") }
    var selectedSignalType by remember { mutableStateOf(LongDistanceSignalType.SEND_HEART) }

    TinyDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("long_distance_sheet"),
        widthFraction = 0.94f,
        maxHeight = 680.dp
    ) {
        TinyDialogHeader(
            title = stringResource(R.string.ui_long_distance_signals),
            subtitle = stringResource(R.string.ui_send_tender_asynchronous_love_across_the),
            icon = TinyIcons.LongDistance,
            onClose = onDismiss
        )

        // 6 Signal Types
        TinyCard(spacing = TinySpace.md) {
            Text(
                text = stringResource(R.string.ui_choose_a_signal_to_send),
                style = TinyType.Label
            )

            val signalsList = LongDistanceSignalType.values()
            Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    signalsList.take(3).forEach { sig ->
                        SignalTypeChip(
                            sig = sig,
                            isSelected = selectedSignalType == sig,
                            onClick = { selectedSignalType = sig },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    signalsList.drop(3).forEach { sig ->
                        SignalTypeChip(
                            sig = sig,
                            isSelected = selectedSignalType == sig,
                            onClick = { selectedSignalType = sig },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customNote,
                onValueChange = { customNote = it },
                placeholder = { Text(stringResource(R.string.ui_add_a_warm_note_optional), style = TinyType.Body.copy(color = TinyColors.InkMuted)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                shape = TinyFieldShape,
                colors = tinyTextFieldColors()
            )

            TinyButton(
                text = stringResource(R.string.ui_send_signal),
                onClick = {
                    val newSignal = LongDistanceSignal(
                        id = UUID.randomUUID().toString(),
                        sender = "boy",
                        type = selectedSignalType,
                        note = customNote.ifBlank { null },
                        timestamp = System.currentTimeMillis()
                    )
                    prefs.sendLongDistanceSignal(newSignal)
                    savedSignals = prefs.getLongDistanceSignals()
                    customNote = ""
                    TinyUsWidgetProvider.updateAllWidgets(context)
                },
                modifier = Modifier.fillMaxWidth(),
                style = TinyButtonStyle.Primary,
                icon = PixelIcons.Send
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
        ) {
            Text(
                text = stringResource(R.string.ui_recent_signals_history),
                style = TinyType.Section
            )

            if (savedSignals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.ui_no_signals_yet_send_your_first_signal_to),
                        style = TinyType.Caption,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TinySpace.sm)
                ) {
                    items(savedSignals) { signal ->
                        val senderDisplayName = if (signal.sender == "boy") prefs.boyfriendName else prefs.girlfriendName
                        SignalHistoryCard(signal, senderDisplayName)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalTypeChip(
    sig: LongDistanceSignalType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = TinyRadius.Medium,
        color = if (isSelected) TinyColors.RoseSoft else TinyColors.Muted,
        contentColor = if (isSelected) TinyColors.Rose else TinyColors.Ink,
        border = BorderStroke(1.dp, if (isSelected) TinyColors.Rose else TinyColors.Line)
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = TinySpace.xs, vertical = TinySpace.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                TinyIcons.signal(sig),
                contentDescription = null,
                tint = if (isSelected) TinyColors.Rose else TinyColors.InkMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(TinySpace.xs))
            Text(
                text = sig.title,
                style = TinyType.Micro.copy(
                    color = if (isSelected) TinyColors.Rose else TinyColors.Ink,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SignalHistoryCard(signal: LongDistanceSignal, senderDisplayName: String) {
    TinyCard(padding = TinySpace.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TinyIconBadge(TinyIcons.signal(signal.type), size = 36.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(TinySpace.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${signal.type.title} from $senderDisplayName",
                    style = TinyType.Label
                )
                if (!signal.note.isNullOrBlank()) {
                    Text(
                        text = "\"${signal.note}\"",
                        style = TinyType.Caption.copy(fontStyle = FontStyle.Italic)
                    )
                }
            }
        }
    }
}
