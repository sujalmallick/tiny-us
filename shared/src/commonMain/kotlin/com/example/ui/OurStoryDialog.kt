package com.example.ui

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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DailyPromptCatalog
import com.example.data.PreferencesManager
import com.example.data.StoryEntry
import com.example.data.StoryInput
import com.example.data.StoryKind
import com.example.data.StorySection
import com.example.data.StoryTimeline
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyRadius
import com.example.ui.theme.TinySpace
import com.example.ui.theme.TinyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun loadStory(
    prefs: PreferencesManager,
    polaroids: PolaroidPhotos,
    progress: com.example.progress.ProgressState = com.example.progress.ProgressState(),
    titleOf: (StringResource) -> String = { "" }
): List<StoryEntry> {
    val today = com.example.data.CoupleDates.today()
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
            Triple(id, kotlinx.datetime.LocalDate.fromEpochDays(day.toInt()), titleOf(first.title))
        },
        // Birthday parties (plan 09, A), with the sealed letter opened at each.
        birthdays = StoryTimeline.birthdayEntries(com.example.data.BirthdayStore(prefs.storage)) { r ->
            val name = if (r.partner == com.example.data.Partner.BOY) prefs.boyfriendName else prefs.girlfriendName
            if (r.age != null) com.example.engine.GameText.get(com.example.resources.Res.string.bday_turned_age, name, r.age)
            else com.example.engine.GameText.get(com.example.resources.Res.string.bday_story_title, name)
        }
    )
    return StoryTimeline.build(input, today)
}

@Composable
fun OurStoryDialog(
    prefs: PreferencesManager,
    polaroids: PolaroidPhotos,
    progressStore: com.example.progress.ProgressStore,
    onDismiss: () -> Unit,
    openLittleFirsts: Boolean = false,
    /** Gives a keepsake from one partner to the other (plan 07, D3); null hides the giving. */
    onGiveKeepsake: ((item: String, fromBoy: Boolean) -> Unit)? = null
) {
    var progress by remember { mutableStateOf(progressStore.load()) }
    var showKeepsakes by remember { mutableStateOf(false) }
    val story by produceState<List<StoryEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { loadStory(prefs, polaroids, progress) { GameText.get(it) } }
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
        DateText.format(kotlinx.datetime.LocalDate(section.year!!, section.month!!, 1), "LLLL yyyy")
    }
    Text(
        title,
        style = TinyType.Section.copy(fontSize = 17.sp, lineHeight = 22.sp),
        modifier = Modifier.padding(top = TinySpace.xl, bottom = TinySpace.sm).semantics { heading() }
    )
}

@Composable
private fun StoryRow(entry: StoryEntry, polaroids: PolaroidPhotos) {
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
                DateText.mediumDate(it)
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
                val thumb by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
                    value = withContext(Dispatchers.Default) { polaroids.loadThumbnail(path) }
                }
                thumb?.let {
                    Spacer(Modifier.height(6.dp))
                    Image(
                        bitmap = it,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(TinyRadius.Medium)
                    )
                }
            }
        }
    }
}

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
fun LittleFirstsPage(progress: com.example.progress.ProgressState) {
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
            val date = DateText.format(kotlinx.datetime.LocalDate.fromEpochDays((progress.firsts[first.id] ?: 0L).toInt()), "d MMM yyyy")
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
fun keepsakeName(item: String): StringResource = when (item.substringAfter(":")) {
    "WILDFLOWER" -> Res.string.keepsake_wildflower
    "RED_LEAF" -> Res.string.keepsake_red_leaf
    "LOVE_NOTE" -> Res.string.keepsake_love_note
    "SEASHELL" -> Res.string.keepsake_seashell
    "STAR_PEBBLE" -> Res.string.keepsake_star_pebble
    "MOCHI_TOY" -> Res.string.keepsake_mochi_toy
    // Cooking, fishing and the garden (plan 07, C3-C5).
    "pancakes" -> Res.string.keepsake_dish_pancakes
    "soup" -> Res.string.keepsake_dish_soup
    "dumplings" -> Res.string.keepsake_dish_dumplings
    "cookies" -> Res.string.keepsake_dish_cookies
    "tea" -> Res.string.keepsake_dish_tea
    "tomato_soup" -> Res.string.keepsake_dish_tomato_soup
    "strawberry_pancakes" -> Res.string.keepsake_dish_strawberry_pancakes
    "pumpkin_pie" -> Res.string.keepsake_dish_pumpkin_pie
    "herb_tea" -> Res.string.keepsake_dish_herb_tea
    "apple_crumble" -> Res.string.keepsake_dish_apple_crumble
    "pea_soup" -> Res.string.keepsake_dish_pea_soup
    "MINNOW" -> Res.string.keepsake_catch_minnow
    "CARP" -> Res.string.keepsake_catch_carp
    "OLD_BOOT" -> Res.string.keepsake_catch_old_boot
    "BOTTLE" -> Res.string.keepsake_catch_bottle
    "GOLDEN_FISH" -> Res.string.keepsake_catch_golden_fish
    "BOUQUET" -> Res.string.keepsake_bouquet
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
    val kinds = listOf("discovery:", "garden:", "dish:", "catch:")
    val kept = progress.keepsakes.filter { (k, v) -> v > 0 && kinds.any { k.startsWith(it) } }
        .entries.sortedWith(compareBy({ e -> kinds.indexOfFirst { e.key.startsWith(it) } }, { it.key }))
        .associate { it.key to it.value }
    val recipes = progress.seenSet(com.example.progress.Seen.RECIPES).size
    LazyColumn(
        contentPadding = PaddingValues(start = TinySpace.lg, end = TinySpace.lg, top = TinySpace.md, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(TinySpace.sm),
        modifier = Modifier.fillMaxSize().testTag("keepsakes_page")
    ) {
        if (kept.isEmpty()) {
            item { Text(stringResource(Res.string.keepsakes_empty), style = TinyType.Body.copy(color = TinyColors.InkMuted)) }
        }
        if (recipes > 0) {
            item {
                Text(
                    stringResource(Res.string.recipe_book, recipes, com.example.games.Recipes.ALL.size),
                    style = TinyType.Caption,
                    modifier = Modifier.testTag("recipe_book")
                )
            }
        }
        items(kept.keys.toList(), key = { it }) { item ->
            val count = kept[item] ?: 0
            TinyCard(padding = TinySpace.md) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(36.dp).background(TinyColors.Muted, PixelCircleShape)) {
                        val sprite = keepsakeSprite(item)
                        if (sprite != null) {
                            // The bigger pixel art for dishes, catches and bouquets, centred.
                            val p = kotlin.math.floor(size.width / (maxOf(sprite.width, sprite.height) + 3))
                            com.example.games.CozySprites.draw(
                                this, sprite, (size.width - sprite.width * p) / 2f, (size.height - sprite.height * p) / 2f, p,
                                com.example.games.CozySprites.BOUQUET_COLORS
                            )
                        } else {
                            val p = size.width / 9f
                            drawKeepsake(this, item, 2.5f * p, 7f * p, p)
                        }
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
