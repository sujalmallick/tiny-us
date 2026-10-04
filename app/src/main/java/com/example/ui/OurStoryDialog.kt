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
import androidx.compose.ui.res.stringResource
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

private enum class StoryFilter(val labelRes: Int, val kinds: Set<StoryKind>?) {
    ALL(R.string.story_filter_all, null),
    MILESTONES(R.string.story_filter_milestones, setOf(StoryKind.MILESTONE, StoryKind.GARDEN)),
    MEMORIES(R.string.story_filter_memories, setOf(StoryKind.MEMORY)),
    LETTERS(R.string.story_filter_letters, setOf(StoryKind.LETTER)),
    PHOTOS(R.string.story_filter_photos, setOf(StoryKind.PHOTO)),
    TOGETHER(R.string.story_filter_together, setOf(StoryKind.ADVENTURE, StoryKind.DAILY_MOMENT, StoryKind.DREAM))
}

/** Reads everything the app has stored and builds the story. Runs off the main thread. */
internal fun loadStory(prefs: PreferencesManager, polaroids: PolaroidManager): List<StoryEntry> {
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
        }.toMap()
    )
    return StoryTimeline.build(input, today)
}

@Composable
fun OurStoryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val polaroids = remember { PolaroidManager(context) }
    val story by produceState<List<StoryEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadStory(prefs, polaroids) }
    }
    var filter by remember { mutableStateOf(StoryFilter.ALL) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(TinyColors.Paper).statusBarsPadding().testTag("our_story_dialog")) {
            Row(
                Modifier.fillMaxWidth().padding(start = TinySpace.xl, end = TinySpace.xs, top = TinySpace.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.story_title), style = TinyType.Display, modifier = Modifier.semantics { heading() })
                    val count = story?.count { !it.isUpcoming && it.kind != StoryKind.MILESTONE } ?: 0
                    Text(
                        stringResource(R.string.story_subtitle, prefs.getDaysTogether(), count),
                        style = TinyType.Caption,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TinyCloseButton(
                    onClick = onDismiss,
                    testTag = "our_story_close",
                    contentDescription = stringResource(R.string.action_close)
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
                        selected = f == filter,
                        onClick = { filter = f; scope.launch { listState.scrollToItem(0) } }
                    )
                }
            }

            TinyDivider()

            val all = story
            when {
                all == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.story_loading), style = TinyType.Body.copy(color = TinyColors.InkMuted))
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
        stringResource(R.string.story_undated)
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
                    if (entry.isUpcoming) stringResource(R.string.story_upcoming, dateText) else dateText,
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
        Text(stringResource(R.string.story_empty_title), style = TinyType.Title, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.story_empty_body), style = TinyType.Body.copy(color = TinyColors.InkMuted), textAlign = TextAlign.Center)
    }
}
