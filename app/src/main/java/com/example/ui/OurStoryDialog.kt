package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.DailyPromptCatalog
import com.example.data.PolaroidManager
import com.example.data.PreferencesManager
import com.example.data.StoryEntry
import com.example.data.StoryInput
import com.example.data.StoryKind
import com.example.data.StorySection
import com.example.data.StoryTimeline
import com.example.ui.theme.DeepRose
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.example.ui.theme.PixelCircleShape
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.StringResource
import com.example.engine.GameText

private enum class StoryFilter(val labelRes: StringResource, val kinds: Set<StoryKind>?) {
    ALL(Res.string.story_filter_all, null),
    MILESTONES(Res.string.story_filter_milestones, setOf(StoryKind.MILESTONE, StoryKind.GARDEN)),
    MEMORIES(Res.string.story_filter_memories, setOf(StoryKind.MEMORY)),
    LETTERS(Res.string.story_filter_letters, setOf(StoryKind.LETTER)),
    PHOTOS(Res.string.story_filter_photos, setOf(StoryKind.PHOTO)),
    TOGETHER(Res.string.story_filter_together, setOf(StoryKind.ADVENTURE, StoryKind.DAILY_MOMENT, StoryKind.DREAM))
}

/** Reads everything the app has stored and builds the story. Runs off the main thread. */
internal fun loadStory(
    prefs: PreferencesManager,
    polaroids: PolaroidManager,
    progress: com.example.progress.ProgressState = com.example.progress.ProgressState(),
    titleOf: (StringResource) -> String = { "" }
): List<StoryEntry> {
    val today = kotlinx.datetime.LocalDate.parse(java.time.LocalDate.now().toString())
    val anniversary = runCatching { kotlinx.datetime.LocalDate.parse(prefs.anniversaryDate) }.getOrNull()
    val prompts = DailyPromptCatalog.defaultPrompts.associateBy { it.id }
    val input = StoryInput(
        anniversary = anniversary,
        memories = prefs.getMemories(),
        letters = prefs.getLoveNotes(),
        photos = polaroids.getPolaroids(),
        dreams = prefs.getDreamEntries(),
        adventures = prefs.getDateAdventures(),
        dailyMoments = prefs.getDailyMomentResponses().values.toList(),
        promptText = { id -> prompts[id]?.question },
        gardenBloomDates = prefs.getGardenBloomDates().mapNotNull { (i, d) ->
            runCatching { kotlinx.datetime.LocalDate.parse(d) }.getOrNull()?.let { i to it }
        }.toMap(),
        littleFirsts = progress.firsts.mapNotNull { (id, day) ->
            val first = com.example.progress.LittleFirsts.byId(id) ?: return@mapNotNull null
            Triple(id, kotlinx.datetime.LocalDate.parse(java.time.LocalDate.ofEpochDay(day).toString()), titleOf(first.title))
        }
    )
    return StoryTimeline.build(input, today)
}

@Composable
fun OurStoryDialog(
    onDismiss: () -> Unit,
    openLittleFirsts: Boolean = false,
    /** Gives a keepsake from one partner to the other (plan 07, D3); null hides the giving. */
    onGiveKeepsake: ((item: String, fromBoy: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val polaroids = remember { PolaroidManager(context) }
    val progressStore = remember {
        com.example.progress.ProgressStore(
            context.getSharedPreferences(com.example.progress.ProgressStore.PREFS_FILE, android.content.Context.MODE_PRIVATE)
        )
    }
    var progress by remember { mutableStateOf(progressStore.load()) }
    var showKeepsakes by remember { mutableStateOf(false) }
    val story by produceState<List<StoryEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadStory(prefs, polaroids, progress) { GameText.get(it) } }
    }
    var filter by remember { mutableStateOf(StoryFilter.ALL) }
    // The "Little firsts" page (plan 07, B3), shown in place of the timeline.
    var showFirsts by remember { mutableStateOf(openLittleFirsts) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(TinyColors.Paper).statusBarsPadding().testTag("our_story_dialog")) {
            Row(
                Modifier.fillMaxWidth().padding(start = TinySpace.xl, end = TinySpace.xs, top = TinySpace.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.story_title), style = TinyType.Display, modifier = Modifier.semantics { heading() })
                    val count = story?.count { !it.isUpcoming && it.kind != StoryKind.MILESTONE } ?: 0
                    Text(
                        stringResource(Res.string.story_subtitle, prefs.getDaysTogether(), count),
                        style = TinyType.Caption,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TinyCloseButton(
                    onClick = onDismiss,
                    testTag = "our_story_close",
                    contentDescription = stringResource(Res.string.action_close)
                )
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = TinySpace.lg, vertical = TinySpace.sm),
                horizontalArrangement = Arrangement.spacedBy(TinySpace.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoryFilter.values().forEach { f ->
                    TinyChip(
                        text = stringResource(f.labelRes),
                        selected = !showFirsts && !showKeepsakes && f == filter,
                        onClick = { showFirsts = false; showKeepsakes = false; filter = f; scope.launch { listState.scrollToItem(0) } }
                    )
                }
                TinyChip(
                    text = stringResource(Res.string.little_firsts),
                    selected = showFirsts,
                    onClick = { showFirsts = true; showKeepsakes = false },
                    icon = PixelIcons.AutoAwesome
                )
                TinyChip(
                    text = stringResource(Res.string.keepsakes),
                    selected = showKeepsakes,
                    onClick = { showKeepsakes = true; showFirsts = false },
                    icon = PixelIcons.CardGiftcard
                )
            }

            TinyDivider()

            val all = story
            when {
                showFirsts -> LittleFirstsPage(progress)
                showKeepsakes -> KeepsakesPage(progress, prefs.boyfriendName, prefs.girlfriendName, onGiveKeepsake?.let { give ->
                    { item: String, fromBoy: Boolean ->
                        give(item, fromBoy)
                        progress = progressStore.load()
                    }
                })
                all == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.story_loading), style = TinyType.Body.copy(color = TinyColors.InkMuted))
                }
                else -> {
                    val visible = filter.kinds?.let { kinds -> all.filter { it.kind in kinds } } ?: all
                    if (visible.none { !it.isUpcoming }) {
                        EmptyStory()
                    } else {
                        val sections = StoryTimeline.groupByMonth(visible)
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = TinySpace.lg, end = TinySpace.lg, bottom = 32.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            sections.forEach { section ->
                                item(key = "h:${section.year}-${section.month}") { SectionHeader(section) }
                                items(section.entries, key = { it.id }) { entry -> StoryRow(entry, polaroids) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: StorySection) {
    val title = if (section.year == null || section.month == null) {
        stringResource(Res.string.story_undated)
    } else {
        java.time.YearMonth.of(section.year!!, section.month!!)
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
    }
    Text(
        title,
        style = TinyType.Section.copy(fontSize = 17.sp, lineHeight = 22.sp),
        modifier = Modifier.padding(top = TinySpace.xl, bottom = TinySpace.sm).semantics { heading() }
    )
}

@Composable
private fun StoryRow(entry: StoryEntry, polaroids: PolaroidManager) {
    var expanded by remember { mutableStateOf(false) }
    // IntrinsicSize.Min lets the rail line stretch to the card's height.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 64.dp)) {
        // Timeline rail with the entry's badge
        Box(Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(TinyColors.Line))
            Box(
                Modifier.padding(top = 10.dp).size(30.dp)
                    .background(if (entry.kind == StoryKind.MILESTONE) TinyColors.RoseSoft else TinyColors.Card, PixelCircleShape)
                    .border(1.dp, TinyColors.Line, PixelCircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(TinyIcons.story(entry.iconKey), contentDescription = null, tint = TinyColors.Rose, modifier = Modifier.size(16.dp)) }
        }
        Spacer(Modifier.width(TinySpace.sm))
        val isRibbon = entry.kind == StoryKind.MILESTONE || entry.kind == StoryKind.GARDEN
        Column(
            Modifier.weight(1f).padding(vertical = 6.dp)
                .clip(TinyRadius.Large)
                .background(if (isRibbon) TinyColors.RoseSoft.copy(alpha = if (entry.isUpcoming) 0.45f else 1f) else TinyColors.Card)
                .border(1.dp, TinyColors.Line, TinyRadius.Large)
                .clickable(enabled = entry.body.isNotBlank()) { expanded = !expanded }
                .padding(TinySpace.md),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val dateText = entry.date?.let {
                java.time.LocalDate.of(it.year, it.monthNumber, it.dayOfMonth)
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }
            if (dateText != null) {
                Text(
                    if (entry.isUpcoming) stringResource(Res.string.story_upcoming, dateText) else dateText,
                    style = TinyType.Micro
                )
            }
            Text(entry.title, style = TinyType.BodyStrong)
            if (entry.body.isNotBlank()) {
                Text(
                    entry.body,
                    style = TinyType.Body.copy(color = TinyColors.InkMuted),
                    maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis
                )
            }
            entry.imagePath?.let { path ->
                val thumb by produceState<Bitmap?>(null, path) {
                    value = withContext(Dispatchers.IO) { loadThumbnail(path) }
                }
                thumb?.let {
                    Spacer(Modifier.height(6.dp))
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(TinyRadius.Medium)
                    )
                }
            }
        }
    }
}

/** Loads a small version of a polaroid so a long story stays light on memory. */
private fun loadThumbnail(path: String): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 360) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()

@Composable
private fun EmptyStory() {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        TinyIconBadge(TinyIcons.OurStory, size = 64.dp, iconSize = 30.dp)
        Spacer(Modifier.height(TinySpace.lg))
        Text(stringResource(Res.string.story_empty_title), style = TinyType.Title, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(Res.string.story_empty_body), style = TinyType.Body.copy(color = TinyColors.InkMuted), textAlign = TextAlign.Center)
    }
}

/**
 * The "Little firsts" page (plan 07, B3): the ones earned, newest first, with their dates; then the
 * rest as gentle hints, so there's something to look for without a checklist feel.
 */
@Composable
internal fun LittleFirstsPage(progress: com.example.progress.ProgressState) {
    val all = com.example.progress.LittleFirsts.ALL
    val earned = all.filter { it.id in progress.firsts }.sortedByDescending { progress.firsts[it.id] }
    val ahead = all.filter { it.id !in progress.firsts }
    LazyColumn(
        contentPadding = PaddingValues(start = TinySpace.lg, end = TinySpace.lg, top = TinySpace.md, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(TinySpace.sm),
        modifier = Modifier.fillMaxSize().testTag("little_firsts_page")
    ) {
        item {
            Text(
                stringResource(Res.string.little_firsts_count, earned.size, all.size),
                style = TinyType.Caption,
                modifier = Modifier.padding(bottom = TinySpace.xs)
            )
        }
        items(earned, key = { it.id }) { first ->
            val date = java.time.LocalDate.ofEpochDay(progress.firsts[first.id] ?: 0L)
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
            LittleFirstRow(
                title = stringResource(first.title),
                detail = stringResource(Res.string.little_first_earned_on, date) +
                    (if (first.reward != null) " \u00b7 " + stringResource(Res.string.little_first_reward_short) else ""),
                earned = true
            )
        }
        items(ahead, key = { it.id }) { first ->
            LittleFirstRow(title = stringResource(Res.string.little_first_not_yet), detail = stringResource(first.hint), earned = false)
        }
    }
}

@Composable
private fun LittleFirstRow(title: String, detail: String, earned: Boolean) {
    TinyCard(padding = TinySpace.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TinyIconBadge(
                icon = PixelIcons.AutoAwesome,
                size = 36.dp,
                iconSize = 20.dp,
                tint = if (earned) TinyColors.Rose else TinyColors.InkMuted,
                background = if (earned) TinyColors.RoseSoft else TinyColors.Muted
            )
            Spacer(Modifier.width(TinySpace.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = TinyType.BodyStrong.copy(color = if (earned) TinyColors.Ink else TinyColors.InkMuted))
                Text(detail, style = TinyType.Caption, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** The name of a keepsake, for the page and the gift message. */
internal fun keepsakeName(item: String): StringResource = when (item.substringAfter(":")) {
    "WILDFLOWER" -> Res.string.keepsake_wildflower
    "RED_LEAF" -> Res.string.keepsake_red_leaf
    "LOVE_NOTE" -> Res.string.keepsake_love_note
    "SEASHELL" -> Res.string.keepsake_seashell
    "STAR_PEBBLE" -> Res.string.keepsake_star_pebble
    "MOCHI_TOY" -> Res.string.keepsake_mochi_toy
    else -> Res.string.keepsake_something
}

/**
 * The keepsake box (plan 07, D3): what the two of them have found, how many, and a way for either
 * to give one to the other. Gifts already given sit on the kitchen shelf.
 */
@Composable
private fun KeepsakesPage(
    progress: com.example.progress.ProgressState,
    boyName: String,
    girlName: String,
    onGive: ((item: String, fromBoy: Boolean) -> Unit)?
) {
    val kept = progress.keepsakes.filter { it.key.startsWith("discovery:") && it.value > 0 }.toSortedMap()
    LazyColumn(
        contentPadding = PaddingValues(start = TinySpace.lg, end = TinySpace.lg, top = TinySpace.md, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(TinySpace.sm),
        modifier = Modifier.fillMaxSize().testTag("keepsakes_page")
    ) {
        if (kept.isEmpty()) {
            item { Text(stringResource(Res.string.keepsakes_empty), style = TinyType.Body.copy(color = TinyColors.InkMuted)) }
        }
        items(kept.keys.toList(), key = { it }) { item ->
            val count = kept[item] ?: 0
            TinyCard(padding = TinySpace.md) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(36.dp).background(TinyColors.Muted, PixelCircleShape)) {
                        val p = size.width / 9f
                        drawKeepsake(this, item, 2.5f * p, 7f * p, p)
                    }
                    Spacer(Modifier.width(TinySpace.md))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(keepsakeName(item)), style = TinyType.BodyStrong)
                        Text(pluralStringResource(Res.plurals.keepsake_count, count, count), style = TinyType.Caption)
                    }
                }
                if (onGive != null && item in com.example.progress.Gifts.GIVEABLE) {
                    Row(Modifier.padding(top = TinySpace.sm), horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
                        TinyButton(
                            text = stringResource(Res.string.keepsake_give, boyName, girlName),
                            onClick = { onGive(item, true) },
                            style = TinyButtonStyle.Outline,
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        TinyButton(
                            text = stringResource(Res.string.keepsake_give, girlName, boyName),
                            onClick = { onGive(item, false) },
                            style = TinyButtonStyle.Outline,
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        if (progress.shelf.isNotEmpty()) {
            item { Text(stringResource(Res.string.keepsakes_on_shelf, progress.shelf.size), style = TinyType.Caption, modifier = Modifier.padding(top = TinySpace.sm)) }
        }
    }
}
