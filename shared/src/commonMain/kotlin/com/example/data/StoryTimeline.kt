package com.example.data

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class StoryKind { MILESTONE, MEMORY, LETTER, PHOTO, DREAM, ADVENTURE, DAILY_MOMENT, GARDEN }

data class StoryEntry(
    val id: String,
    val kind: StoryKind,
    /** Null when the date can't be known (old free-text labels like "Quiet Night"). */
    val date: LocalDate?,
    val title: String,
    val body: String = "",
    /** Plain key the UI maps to an icon (see storyIcon in the app). */
    val iconKey: String,
    val imagePath: String? = null,
    /** Milestones still ahead (only the next one is ever included). */
    val isUpcoming: Boolean = false
)

/** Entries of one month, or the undated section when [year] is null. */
data class StorySection(val year: Int?, val month: Int?, val entries: List<StoryEntry>)

/** Everything the story is built from. All lists may be empty. */
data class StoryInput(
    val anniversary: LocalDate?,
    val memories: List<MemoryItem> = emptyList(),
    val letters: List<LoveNoteItem> = emptyList(),
    val photos: List<PolaroidMemory> = emptyList(),
    val dreams: List<DreamEntry> = emptyList(),
    val adventures: List<DateAdventure> = emptyList(),
    val dailyMoments: List<DailyMomentResponse> = emptyList(),
    val promptText: (String) -> String? = { null },
    /** Calendar date each earned bloom first appeared, by bloom index. */
    val gardenBloomDates: Map<Int, LocalDate> = emptyMap(),
    /** "Little firsts" the couple earned (id to date and title), shown as milestones. */
    val littleFirsts: List<Triple<String, LocalDate, String>> = emptyList(),
    /** Birthday parties (plan 09, A), from [StoryTimeline.birthdayEntries]. */
    val birthdays: List<StoryEntry> = emptyList()
)

/**
 * Builds "Our Story": one chronological scrapbook from memories, letters, photos, dreams,
 * adventures, daily moments, garden blooms and relationship milestones.
 */
object StoryTimeline {

    fun build(input: StoryInput, today: LocalDate, timeZone: TimeZone = TimeZone.currentSystemDefault()): List<StoryEntry> {
        fun fromEpoch(ms: Long?): LocalDate? =
            ms?.takeIf { it > 0L }?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date }

        val entries = ArrayList<StoryEntry>()

        input.memories.forEach { m ->
            entries += StoryEntry(
                id = "memory:${m.id}", kind = StoryKind.MEMORY,
                // The date the couple typed is when it happened; createdAt is only a fallback.
                date = parseLooseDate(m.date) ?: fromEpoch(m.createdAt),
                title = m.title, body = m.note, iconKey = m.iconType.ifBlank { "heart" }
            )
        }
        // Only letters the couple wrote; the bundled sample letters aren't part of their story.
        input.letters.filter { it.isCustom }.forEach { n ->
            entries += StoryEntry(
                id = "letter:${n.id}", kind = StoryKind.LETTER,
                date = fromEpoch(n.createdAt) ?: parseLooseDate(n.date),
                title = n.author.ifBlank { "A letter" }, body = n.text, iconKey = "letter"
            )
        }
        input.photos.forEach { p ->
            entries += StoryEntry(
                id = "photo:${p.id}", kind = StoryKind.PHOTO,
                date = fromEpoch(captureMillis(p)) ?: parseLooseDate(p.date),
                title = p.title, body = p.sceneName, iconKey = "photo", imagePath = p.imagePath
            )
        }
        input.dreams.forEach { d ->
            entries += StoryEntry(
                id = "dream:${d.id}", kind = StoryKind.DREAM, date = fromEpoch(d.timestamp),
                title = "A shared dream", body = d.text, iconKey = "dream"
            )
        }
        input.adventures.filter { it.status == AdventureStatus.COMPLETED }.forEach { a ->
            entries += StoryEntry(
                id = "adventure:${a.id}", kind = StoryKind.ADVENTURE, date = fromEpoch(a.completedTimestamp),
                title = a.title, body = a.description, iconKey = "adventure"
            )
        }
        input.dailyMoments.filter { it.isAnsweredByBoy || it.isAnsweredByGirl }.forEach { r ->
            entries += StoryEntry(
                id = "moment:${r.dateString}", kind = StoryKind.DAILY_MOMENT,
                date = parseLooseDate(r.dateString) ?: fromEpoch(r.completedTimestamp),
                title = input.promptText(r.promptId) ?: "Daily moment",
                body = listOfNotNull(r.boyAnswer, r.girlAnswer).filter { it.isNotBlank() }.joinToString("\n"),
                iconKey = "moment"
            )
        }
        // Sorted by index (toSortedMap is JVM-only; this compiles on iOS too).
        input.gardenBloomDates.entries.sortedBy { it.key }.forEach { (index, date) ->
            val bloom = GardenGrowth.bloomsFor(GardenGrowth.bloomDay(index)).getOrNull(index) ?: return@forEach
            entries += StoryEntry(
                id = "garden:$index", kind = StoryKind.GARDEN, date = date,
                title = if (bloom.isGolden) "A golden bloom in our garden" else "${bloom.plant.name} bloomed in our garden",
                iconKey = if (bloom.isGolden) "golden" else "garden"
            )
        }
        input.anniversary?.let { entries += milestones(it, today) }
        input.littleFirsts.forEach { (id, date, title) ->
            entries += StoryEntry(id = "first:$id", kind = StoryKind.MILESTONE, date = date, title = title, iconKey = "first")
        }
        entries += input.birthdays

        // Chronological; undated entries last, keeping their original order.
        return entries.withIndex()
            .sortedWith(compareBy<IndexedValue<StoryEntry>>({ it.value.date == null }, { it.value.date }, { kindOrder(it.value.kind) }, { it.index }))
            .map { it.value }
    }

    /**
     * One entry per birthday party, titled by [titleFor] (for example "Sprout's birthday"), with
     * the sealed letter that was opened at it as the body. The wish stays private and isn't shown.
     */
    fun birthdayEntries(store: BirthdayStore, titleFor: (BirthdayRecord) -> String): List<StoryEntry> {
        val letters = store.letters().associateBy { it.id }
        return store.records().mapNotNull { r ->
            val date = Birthdays.parse(r.date) ?: return@mapNotNull null
            StoryEntry(
                id = "birthday:${r.partner}:${r.date}", kind = StoryKind.MILESTONE, date = date,
                title = titleFor(r), body = r.letterId?.let { letters[it]?.body }.orEmpty(), iconKey = "birthday"
            )
        }
    }

    fun groupByMonth(entries: List<StoryEntry>): List<StorySection> {
        val dated = entries.filter { it.date != null }
            .groupBy { it.date!!.year to it.date.monthNumber }
            .map { (ym, list) -> StorySection(ym.first, ym.second, list) }
        val undated = entries.filter { it.date == null }
        return if (undated.isEmpty()) dated else dated + StorySection(null, null, undated)
    }

    /** Day 1, round-number days and yearly anniversaries up to today, plus the next one ahead. */
    fun milestones(start: LocalDate, today: LocalDate): List<StoryEntry> {
        if (start > today) return emptyList()
        val candidates = ArrayList<Pair<LocalDate, String>>()
        candidates += start to "The day our story began"
        listOf(100, 365, 500, 1000, 1500, 2000, 2500, 3000, 3650, 5000, 10000).forEach { day ->
            if (day != 365 && day != 3650) candidates += start.plus(day - 1, DateTimeUnit.DAY) to "Day $day together"
        }
        for (year in 1..60) {
            candidates += start.plus(DatePeriod(years = year)) to if (year == 1) "Our first anniversary" else "$year years together"
        }
        val sorted = candidates.sortedBy { it.first }
        val past = sorted.filter { it.first <= today }
        val next = sorted.firstOrNull { it.first > today }
        return past.map { (d, title) -> StoryEntry("milestone:$d", StoryKind.MILESTONE, d, title, iconKey = "milestone") } +
            listOfNotNull(next?.let { (d, title) -> StoryEntry("milestone:$d", StoryKind.MILESTONE, d, title, iconKey = "upcoming", isUpcoming = true) })
    }

    /** Polaroid images are saved as "pol_<epochMillis>_<id>.png"; use that as the capture time. */
    fun captureMillis(photo: PolaroidMemory): Long? =
        Regex("""pol_(\d{10,})_""").find(photo.imagePath)?.groupValues?.get(1)?.toLongOrNull()

    private val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    /**
     * Reads the date strings the app has written over time: "2026-10-01", "Sep 28, 2026",
     * "September 28, 2026", "28 Sep 2026". Labels without a year ("Today", "Sep 3") return null.
     */
    fun parseLooseDate(text: String): LocalDate? {
        val t = text.trim()
        Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""").find(t)?.let { m ->
            val (y, mo, d) = m.destructured
            return safeDate(y.toInt(), mo.toInt(), d.toInt())
        }
        Regex("""^([A-Za-z]+)\.?\s+(\d{1,2}),?\s+(\d{4})$""").find(t)?.let { m ->
            val (mon, d, y) = m.destructured
            val month = monthIndex(mon) ?: return null
            return safeDate(y.toInt(), month, d.toInt())
        }
        Regex("""^(\d{1,2})\s+([A-Za-z]+)\.?,?\s+(\d{4})$""").find(t)?.let { m ->
            val (d, mon, y) = m.destructured
            val month = monthIndex(mon) ?: return null
            return safeDate(y.toInt(), month, d.toInt())
        }
        return null
    }

    private fun monthIndex(name: String): Int? =
        name.lowercase().take(3).let { abbr -> months.indexOf(abbr).takeIf { it >= 0 }?.plus(1) }

    private fun safeDate(y: Int, m: Int, d: Int): LocalDate? = runCatching { LocalDate(y, m, d) }.getOrNull()

    private fun kindOrder(kind: StoryKind) = if (kind == StoryKind.MILESTONE) 0 else 1

}
