package com.example.ui

import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.graphics.Color
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
    TOGETHER(Res.string.story_filter_together, setOf(StoryKind.ADVENTURE, StoryKind.DAILY_MOMENT, StoryKind.DREAM, StoryKind.MOMENT_TOGETHER))
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
    val prompts = DailyPromptCatalog.allPrompts.associateBy { it.id }
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
        coupleLife = StoryTimeline.coupleLifeEntries(
            com.example.data.CoupleLifeStore(prefs.storage),
            phonesDownTitle = { com.example.engine.GameText.get(com.example.resources.Res.string.pd_story, it) },
            makeUpTitle = com.example.engine.GameText.get(com.example.resources.Res.string.bench_story),
            jarTitle = com.example.engine.GameText.get(com.example.resources.Res.string.first_thank_you_jar)
        ),
        festivals = StoryTimeline.festivalEntries(
            com.example.data.FestivalStore(prefs.storage),
            com.example.data.Festivals.isSouthern(androidx.compose.ui.text.intl.Locale.current.region)
        ) { f, picks -> festivalStoryTitle(f, picks) },
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
    // The collection book (plan 09, H).
    var showCollection by remember { mutableStateOf(false) }
    // A friend's secret, opened again from the book (plan 09, I).
    var secret by remember { mutableStateOf<com.example.data.Friend?>(null) }
    val collection = remember(prefs) { com.example.data.CollectionStore(prefs.storage) }
    val story by produceState<List<StoryEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { loadStory(prefs, polaroids, progress) { GameText.get(it) } }
    }
    var filter by remember { mutableStateOf(StoryFilter.ALL) }
    // The "Little firsts" page (plan 07, B3), shown in place of the timeline.
    var showFirsts by remember { mutableStateOf(openLittleFirsts) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    secret?.let { FriendSecretDialog(it, prefs.boyfriendName, prefs.girlfriendName, onDismiss = { secret = null }) }
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
                        selected = !showFirsts && !showKeepsakes && !showCollection && f == filter,
                        onClick = { showFirsts = false; showKeepsakes = false; showCollection = false; filter = f; scope.launch { listState.scrollToItem(0) } }
                    )
                }
                TinyChip(
                    text = stringResource(Res.string.little_firsts),
                    selected = showFirsts,
                    onClick = { showFirsts = true; showKeepsakes = false; showCollection = false },
                    icon = PixelIcons.AutoAwesome
                )
                TinyChip(
                    text = stringResource(Res.string.keepsakes),
                    selected = showKeepsakes,
                    onClick = { showKeepsakes = true; showFirsts = false; showCollection = false },
                    icon = PixelIcons.CardGiftcard
                )
                TinyChip(
                    text = stringResource(Res.string.collection_book),
                    selected = showCollection,
                    onClick = { showCollection = true; showFirsts = false; showKeepsakes = false },
                    icon = PixelIcons.AutoStories
                )
            }

            TinyDivider()

            val all = story
            when {
                showFirsts -> LittleFirstsPage(progress)
                showCollection -> CollectionBookPage(
                    progress, collection, prefs.boyfriendName, prefs.girlfriendName,
                    together = prefs.anniversaryDate, daysTogether = prefs.getDaysTogether(),
                    onOpenSecret = { secret = it }
                )
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
    "MOON_JELLY" -> Res.string.keepsake_catch_moon_jelly
    "GLOW_SQUID" -> Res.string.keepsake_catch_glow_squid
    "RAIN_TROUT" -> Res.string.keepsake_catch_rain_trout
    "ICE_COD" -> Res.string.keepsake_catch_ice_cod
    "BLOSSOM_KOI" -> Res.string.keepsake_catch_blossom_koi
    "PEARL" -> Res.string.keepsake_catch_pearl
    "HEART_SHELL" -> Res.string.keepsake_catch_heart_shell
    "SEA_GLASS_HEART" -> Res.string.keepsake_catch_sea_glass_heart
    "BOUQUET" -> Res.string.keepsake_bouquet
    "THANK_YOU" -> Res.string.keepsake_thank_you_jar
    "BLOSSOM_PICNIC" -> Res.string.keepsake_festival_picnic
    "LANTERN_NIGHT" -> Res.string.keepsake_festival_lantern
    "GIFT_EXCHANGE" -> Res.string.keepsake_festival_gift
    "VISIT" -> Res.string.keepsake_fox_visit
    "BAO" -> Res.string.keepsake_secret_bao
    "LEO" -> Res.string.keepsake_secret_leo
    "PIP" -> Res.string.keepsake_secret_pip
    "BALL" -> Res.string.keepsake_fox_ball
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

/**
 * The collection book (plan 09, H): a page for each kind of thing, how many are found, and for each
 * one the little story of the first time (who, where, when). The rest are shadows with a hint; the
 * rare ones stay "???" until someone finds them. It opens on "Found you": the two of them.
 */
@Composable
fun CollectionBookPage(
    progress: com.example.progress.ProgressState,
    store: com.example.data.CollectionStore,
    boyName: String,
    girlName: String,
    /** The day they got together (yyyy-MM-dd), and how many days it's been. */
    together: String? = null,
    daysTogether: Long = 0,
    /** null opens "Found you". */
    initialPage: com.example.data.CollectionPage? = null,
    /** false draws "Found you" still. */
    animate: Boolean = true,
    /** Opens a friend's secret again (plan 09, I); null leaves the rows still. */
    onOpenSecret: ((com.example.data.Friend) -> Unit)? = null
) {
    var page by remember { mutableStateOf(initialPage) }
    val entries = page?.let { com.example.data.CollectionBook.page(it) }.orEmpty()
    val found = entries.count { com.example.data.CollectionBook.isFound(it.key, progress) }
    LazyColumn(
        contentPadding = PaddingValues(start = TinySpace.lg, end = TinySpace.lg, top = TinySpace.md, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(TinySpace.sm),
        modifier = Modifier.fillMaxSize().testTag("collection_book_page")
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TinySpace.sm)
            ) {
                TinyChip(
                    text = stringResource(Res.string.found_you),
                    selected = page == null,
                    onClick = { page = null },
                    icon = PixelIcons.Favorite
                )
                com.example.data.CollectionPage.entries.forEach { p ->
                    TinyChip(text = stringResource(collectionPageName(p)), selected = p == page, onClick = { page = p })
                }
            }
        }
        if (page == null) {
            foundYouItems(progress, boyName, girlName, together, daysTogether, animate)
            return@LazyColumn
        }
        item {
            Text(
                stringResource(Res.string.collection_found_count, found, entries.size),
                style = TinyType.Caption,
                modifier = Modifier.padding(vertical = TinySpace.xs).testTag("collection_count")
            )
        }
        items(entries, key = { it.key }) { entry ->
            val isFound = com.example.data.CollectionBook.isFound(entry.key, progress)
            val secret = com.example.data.Friend.entries.firstOrNull { it.secretKey == entry.key }
            CollectionRow(
                entry, isFound, if (isFound) store.first(entry.key) else null, boyName, girlName,
                onClick = if (isFound && secret != null && onOpenSecret != null) { { onOpenSecret(secret) } } else null
            )
        }
    }
}

/**
 * "Found you": the page about the two of them. A heart beats between their names while little
 * hearts drift up around it, and below are love lines that open one by one as the book fills.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.foundYouItems(
    progress: com.example.progress.ProgressState,
    boyName: String,
    girlName: String,
    together: String?,
    daysTogether: Long,
    animate: Boolean
) {
    val found = com.example.data.CollectionBook.foundCount(progress)
    val total = com.example.data.CollectionBook.ENTRIES.size
    item(key = "found_you_card") { FoundYouCard(boyName, girlName, together, daysTogether, animate) }
    item(key = "found_you_count") {
        Text(
            stringResource(Res.string.found_you_count, found, total),
            style = TinyType.Caption,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = TinySpace.xs).testTag("found_you_count")
        )
    }
    item(key = "found_you_lines") {
        val lines = GameText.array(Res.array.found_you_lines)
        val open = com.example.data.CollectionBook.foundYouLinesOpen(found).coerceAtMost(lines.size)
        Column(verticalArrangement = Arrangement.spacedBy(TinySpace.sm)) {
            lines.take(open).forEach { FoundYouLine(it) }
            com.example.data.CollectionBook.foundYouNextIn(found)?.let { more ->
                if (open < lines.size) FoundYouLine(stringResource(Res.string.found_you_next, more), locked = true)
            }
        }
    }
}

@Composable
private fun FoundYouCard(boyName: String, girlName: String, together: String?, daysTogether: Long, animate: Boolean) {
    // Still (one moment of it) for the JVM renders, which can't settle a never-ending animation.
    val beatAndDrift = if (animate) androidx.compose.animation.core.rememberInfiniteTransition(label = "found_you") else null
    val drift = beatAndDrift?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(9000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "found_you_drift"
    )?.value ?: 0.35f
    val beat = beatAndDrift?.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(650, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "found_you_beat"
    )?.value ?: 1f
    val since = together?.let { runCatching { kotlinx.datetime.LocalDate.parse(it) }.getOrNull() }
        ?.let { DateText.format(it, "d MMM yyyy") }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(TinyRadius.Large)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(TinyColors.RoseSoft, FOUND_YOU_BLUSH)))
            .border(2.dp, TinyColors.Blush, TinyRadius.Large)
            .testTag("found_you_card")
    ) {
        Canvas(Modifier.matchParentSize()) {
            // Little hearts drifting up, each on its own path, fading in and out.
            val p = 3.dp.toPx()
            for (i in 0 until 14) {
                val rise = (i * 0.071f + drift * (0.7f + (i % 4) * 0.12f)) % 1f
                val sway = kotlin.math.sin((rise * 2f + i * 0.37f) * kotlin.math.PI.toFloat()) * 8.dp.toPx()
                val x = ((i * 0.381f) % 1f) * (size.width - 8 * p) + 2 * p + sway
                val y = size.height * (1.05f - rise * 1.1f)
                val alpha = kotlin.math.sin(rise * kotlin.math.PI.toFloat()) * 0.5f
                val scale = if (i % 3 == 0) 1.5f else 1f
                pixelHeart(this, x, y, p * scale, FOUND_YOU_HEARTS[i % FOUND_YOU_HEARTS.size].copy(alpha = alpha))
            }
            // The big heart between them, beating.
            val big = 4.dp.toPx() * beat
            pixelHeart(this, size.width / 2f - 2.5f * big, 26.dp.toPx() - 2.5f * big, big, TinyColors.Rose)
            pixelHeart(this, size.width / 2f - 1.5f * big + big * 0.2f, 26.dp.toPx() - 2.5f * big + big * 0.2f, big * 0.25f, Color.White.copy(alpha = 0.7f))
        }
        Column(
            Modifier.fillMaxWidth().padding(start = TinySpace.lg, end = TinySpace.lg, top = 58.dp, bottom = TinySpace.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(Res.string.found_you),
                style = TinyType.Display.copy(color = TinyColors.Rose),
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(Res.string.found_you_names, boyName, girlName),
                style = TinyType.Section,
                modifier = Modifier.padding(top = TinySpace.xs)
            )
            if (since != null) {
                Text(stringResource(Res.string.found_you_since, since), style = TinyType.Caption, modifier = Modifier.padding(top = TinySpace.sm))
            }
            if (daysTogether > 0) {
                val d = daysTogether.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                Text(pluralStringResource(Res.plurals.found_you_days, d, d), style = TinyType.Caption)
            }
        }
    }
}

@Composable
private fun FoundYouLine(text: String, locked: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(TinyRadius.Medium)
            .background(if (locked) TinyColors.Muted else TinyColors.Card)
            .border(1.dp, if (locked) TinyColors.Line else TinyColors.Blush, TinyRadius.Medium)
            .padding(TinySpace.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(15.dp)) {
            pixelHeart(this, 0f, 0f, size.width / 5f, if (locked) TinyColors.InkMuted.copy(alpha = 0.35f) else TinyColors.Rose)
        }
        Spacer(Modifier.width(TinySpace.md))
        Text(
            text,
            style = if (locked) TinyType.Caption
            else TinyType.Body.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = TinyColors.Ink)
        )
    }
}

private val FOUND_YOU_BLUSH = Color(0xFFFFDCE3)
private val FOUND_YOU_HEARTS = listOf(Color(0xFFE88AA8), Color(0xFFFF8FAB), Color(0xFFAD4760), Color(0xFFFFB5C2))

@Composable
private fun CollectionRow(
    entry: com.example.data.CollectionEntry,
    isFound: Boolean,
    first: com.example.data.FirstFind?,
    boyName: String,
    girlName: String,
    onClick: (() -> Unit)? = null
) {
    val mystery = !isFound && entry.hidden
    val title = if (mystery) stringResource(Res.string.collection_mystery) else stringResource(collectionName(entry.key))
    val detail = when {
        isFound && first != null -> firstFindLine(first, boyName, girlName)
        isFound -> stringResource(Res.string.collection_before_book)
        else -> stringResource(entry.hint?.let(::collectionHintText) ?: if (mystery) Res.string.collection_hint_mystery else Res.string.collection_not_yet)
    }
    TinyCard(padding = TinySpace.md, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shadow = TinyColors.InkMuted.copy(alpha = 0.45f)
            Canvas(Modifier.size(36.dp).background(if (isFound) TinyColors.RoseSoft else TinyColors.Muted, PixelCircleShape)) {
                if (mystery) return@Canvas
                val silhouette = if (isFound) null else shadow
                val sprite = keepsakeSprite(entry.key)
                if (sprite != null) {
                    val p = kotlin.math.floor(size.width / (maxOf(sprite.width, sprite.height) + 3))
                    com.example.games.CozySprites.draw(
                        this, sprite, (size.width - sprite.width * p) / 2f, (size.height - sprite.height * p) / 2f, p,
                        com.example.games.CozySprites.BOUQUET_COLORS, silhouette = silhouette
                    )
                } else {
                    val p = size.width / 9f
                    drawKeepsake(this, entry.key, 2.5f * p, 7f * p, p, silhouette)
                }
            }
            Spacer(Modifier.width(TinySpace.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = TinyType.BodyStrong.copy(color = if (isFound) TinyColors.Ink else TinyColors.InkMuted))
                Text(detail, style = TinyType.Caption, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** "Sprout found this first, on the pier, at night, in the rain, 3 Oct 2026". */
@Composable
private fun firstFindLine(f: com.example.data.FirstFind, boyName: String, girlName: String): String {
    val who = when (f.by) {
        "BOY" -> if (f.forPartner) stringResource(Res.string.collection_first_for, boyName, girlName) else stringResource(Res.string.collection_first_by, boyName)
        "GIRL" -> if (f.forPartner) stringResource(Res.string.collection_first_for, girlName, boyName) else stringResource(Res.string.collection_first_by, girlName)
        else -> stringResource(Res.string.collection_first_together)
    }
    val place = collectionPlace(f.scene)?.let { stringResource(it) }
    val time = when (f.phase) {
        "MORNING" -> Res.string.collection_when_morning
        "AFTERNOON" -> Res.string.collection_when_afternoon
        "SUNSET" -> Res.string.collection_when_sunset
        "NIGHT" -> Res.string.collection_when_night
        else -> null
    }?.let { stringResource(it) }
    val weather = when (f.weather) {
        "RAIN" -> Res.string.collection_weather_rain
        "SNOW" -> Res.string.collection_weather_snow
        "SAKURA" -> Res.string.collection_weather_sakura
        "AUTUMN" -> Res.string.collection_weather_autumn
        else -> null
    }?.let { stringResource(it) }
    val date = if (f.epochDay > 0) DateText.format(kotlinx.datetime.LocalDate.fromEpochDays(f.epochDay.toInt()), "d MMM yyyy") else null
    return listOfNotNull(who, place, time, weather, date).joinToString(", ")
}

private fun collectionPlace(scene: String): StringResource? = when (scene) {
    "FLOWER" -> Res.string.collection_place_flower
    "UNDER_TREE" -> Res.string.collection_place_tree
    "COOKING" -> Res.string.collection_place_kitchen
    "SLEEP", "LOOKING" -> Res.string.collection_place_home
    "WALK" -> Res.string.collection_place_walk
    "MOMO_STALL" -> Res.string.collection_place_stall
    "EVENING_RIDE" -> Res.string.collection_place_ride
    "COZY_LOFT" -> Res.string.collection_place_loft
    "RAINY_CAFE" -> Res.string.collection_place_cafe
    "SUNROOM" -> Res.string.collection_place_sunroom
    "CAMPFIRE" -> Res.string.collection_place_campfire
    "SEASIDE_PIER" -> Res.string.collection_place_pier
    else -> null
}

private fun collectionPageName(p: com.example.data.CollectionPage): StringResource = when (p) {
    com.example.data.CollectionPage.FINDS -> Res.string.collection_page_finds
    com.example.data.CollectionPage.SEA -> Res.string.collection_page_sea
    com.example.data.CollectionPage.KITCHEN -> Res.string.collection_page_kitchen
    com.example.data.CollectionPage.GARDEN -> Res.string.collection_page_garden
    com.example.data.CollectionPage.MOMENTS -> Res.string.collection_page_moments
}

private fun collectionHintText(h: com.example.data.CollectionHint): StringResource = when (h) {
    com.example.data.CollectionHint.SPRING -> Res.string.collection_hint_spring
    com.example.data.CollectionHint.SUMMER -> Res.string.collection_hint_summer
    com.example.data.CollectionHint.AUTUMN -> Res.string.collection_hint_autumn
    com.example.data.CollectionHint.WINTER -> Res.string.collection_hint_winter
    com.example.data.CollectionHint.NIGHT -> Res.string.collection_hint_night
    com.example.data.CollectionHint.RAIN -> Res.string.collection_hint_rain
    com.example.data.CollectionHint.FOR_HER -> Res.string.collection_hint_for_her
    com.example.data.CollectionHint.FRIDAY -> Res.string.collection_hint_friday
    com.example.data.CollectionHint.FRIEND -> Res.string.collection_hint_friend
}

/** The name in the book: a keepsake's name, or a crop's. */
private fun collectionName(key: String): StringResource =
    if (key.startsWith("crop:")) {
        com.example.games.Ingredient.entries.firstOrNull { it.name == key.substringAfter(":") }
            ?.let { com.example.scene.CozyGames.ingredientName(it) } ?: Res.string.keepsake_something
    } else keepsakeName(key)

/** How a festival reads in Our Story: the picnic's flowers, the lanterns, the gifts exchanged. */
private fun festivalStoryTitle(f: com.example.data.Festival, picks: com.example.data.FestivalPicks): String {
    val flowers = GameText.array(Res.array.festival_flowers)
    fun gift(id: String) = if (id == HANDMADE_CARD) GameText.get(Res.string.fest_gift_handmade) else GameText.get(keepsakeName(id))
    return when (f) {
        com.example.data.Festival.BLOSSOM_PICNIC -> GameText.get(
            Res.string.fest_story_picnic,
            flowers.getOrElse(picks.boy.toIntOrNull() ?: -1) { "" },
            flowers.getOrElse(picks.girl.toIntOrNull() ?: -1) { "" }
        )
        com.example.data.Festival.LANTERN_NIGHT -> GameText.get(Res.string.fest_story_lantern)
        com.example.data.Festival.GIFT_EXCHANGE -> GameText.get(Res.string.fest_story_gift, gift(picks.boy), gift(picks.girl))
    }
}
