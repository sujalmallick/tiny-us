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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val PageCream = Color(0xFFFFF6EE)
private val Ink = Color(0xFF553D36)
private val Muted = Color(0xFF8A7568)
private val Rail = Color(0xFFEBD3C4)
private val CardFill = Color(0xFFFFFCF8)
private val RibbonPink = Color(0xFFFFE3E8)

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
        Column(Modifier.fillMaxSize().background(PageCream).statusBarsPadding().testTag("our_story_dialog")) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.story_title), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink)
                    val count = story?.count { !it.isUpcoming && it.kind != StoryKind.MILESTONE } ?: 0
                    Text(stringResource(R.string.story_subtitle, prefs.getDaysTogether(), count), fontSize = 13.sp, color = Muted)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("our_story_close")) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = Ink)
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoryFilter.values().forEach { f ->
                    val selected = f == filter
                    Box(
                        Modifier
                            .border(if (selected) 2.dp else 1.dp, if (selected) DeepRose else Rail, RoundedCornerShape(50))
                            .background(if (selected) DeepRose.copy(alpha = 0.08f) else CardFill, RoundedCornerShape(50))
                            .clickable { filter = f; scope.launch { listState.scrollToItem(0) } }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text(stringResource(f.labelRes), fontSize = 13.sp, color = if (selected) DeepRose else Ink) }
                }
            }

            val all = story
            when {
                all == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.story_loading), color = Muted)
                }
                else -> {
                    val visible = filter.kinds?.let { kinds -> all.filter { it.kind in kinds } } ?: all
                    if (visible.none { !it.isUpcoming }) {
                        EmptyStory()
                    } else {
                        val sections = StoryTimeline.groupByMonth(visible)
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
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
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Ink,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun StoryRow(entry: StoryEntry, polaroids: PolaroidManager) {
    var expanded by remember { mutableStateOf(false) }
    // IntrinsicSize.Min lets the rail line stretch to the card's height.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 64.dp)) {
        // Timeline rail with the entry's badge
        Box(Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Rail))
            Box(
                Modifier.padding(top = 10.dp).size(30.dp).background(if (entry.kind == StoryKind.MILESTONE) RibbonPink else CardFill, CircleShape)
                    .border(1.dp, Rail, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(entry.emoji, fontSize = 14.sp) }
        }
        Spacer(Modifier.width(8.dp))
        val isRibbon = entry.kind == StoryKind.MILESTONE || entry.kind == StoryKind.GARDEN
        Column(
            Modifier.weight(1f).padding(vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isRibbon) RibbonPink.copy(alpha = if (entry.isUpcoming) 0.45f else 1f) else CardFill)
                .border(1.dp, Rail, RoundedCornerShape(16.dp))
                .clickable(enabled = entry.body.isNotBlank()) { expanded = !expanded }
                .padding(12.dp)
        ) {
            val dateText = entry.date?.let {
                java.time.LocalDate.of(it.year, it.monthNumber, it.dayOfMonth)
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }
            if (dateText != null) {
                Text(
                    if (entry.isUpcoming) stringResource(R.string.story_upcoming, dateText) else dateText,
                    fontSize = 11.sp, color = Muted
                )
            }
            Text(entry.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (entry.body.isNotBlank()) {
                Text(
                    entry.body, fontSize = 13.sp, color = Ink.copy(alpha = 0.85f),
                    maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            entry.imagePath?.let { path ->
                val thumb by produceState<Bitmap?>(null, path) {
                    value = withContext(Dispatchers.IO) { loadThumbnail(path) }
                }
                thumb?.let {
                    Spacer(Modifier.height(8.dp))
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
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
        Text("📖", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.story_empty_title), fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.story_empty_body), fontSize = 13.sp, color = Muted)
    }
}
