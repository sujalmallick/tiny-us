package com.example.data

/** A flower that joins the meadow for good once the couple has visited enough days. */
data class KeepsakePlant(
    val id: String,
    val name: String,
    val emoji: String,
    val petal: Long,
    val center: Long
)

/** One bloom the garden has earned. [index] is its order of appearance (0-based). */
data class GardenBloom(val index: Int, val plant: KeepsakePlant, val visitDay: Int, val isGolden: Boolean)

/**
 * A garden that never punishes: growth is counted in *total* days visited (never streaks),
 * nothing ever wilts or disappears, and time away is only met with a warm welcome back.
 */
object GardenGrowth {
    /** Visit day on which the first keepsake plant blooms (the day after the meadow is full). */
    const val FIRST_BLOOM_DAY = 15
    const val DAYS_BETWEEN_BLOOMS = 5
    /** After every keepsake plant has bloomed, a golden bloom appears this often. */
    const val DAYS_BETWEEN_GOLDEN_BLOOMS = 30
    /** Days away before the garden says hello again. */
    const val WELCOME_BACK_AFTER_DAYS = 3

    val keepsakePlants: List<KeepsakePlant> = listOf(
        KeepsakePlant("sunflower", "Sunflower", "🌻", 0xFFFFC93C, 0xFF7A4B1E),
        KeepsakePlant("lavender", "Lavender", "💜", 0xFFB497E7, 0xFF7B5EA7),
        KeepsakePlant("tulip", "Tulip", "🌷", 0xFFFF6F91, 0xFFFFD166),
        KeepsakePlant("forget_me_not", "Forget-me-not", "💙", 0xFF7EC8F2, 0xFFFFE066),
        KeepsakePlant("daisy", "Daisy", "🌼", 0xFFFFFFFF, 0xFFFFC93C),
        KeepsakePlant("rose", "Little Rose", "🌹", 0xFFE63946, 0xFF9D0208),
        KeepsakePlant("poppy", "Poppy", "🌺", 0xFFFF7F50, 0xFF2B2D42),
        KeepsakePlant("bluebell", "Bluebell", "🔔", 0xFF5E7CE2, 0xFFDDE5FF),
        KeepsakePlant("marigold", "Marigold", "🧡", 0xFFFF9F1C, 0xFFB5651D),
        KeepsakePlant("cosmos", "Cosmos", "🌸", 0xFFF7A1C4, 0xFFFFD166),
        KeepsakePlant("lily", "Lily of the Valley", "🤍", 0xFFFFF8F0, 0xFF95D5B2),
        KeepsakePlant("moonflower", "Moonflower", "🌙", 0xFFE0E7FF, 0xFFFFF3B0)
    )

    private val goldenPlant = KeepsakePlant("golden", "Golden Bloom", "✨", 0xFFFFD700, 0xFFFFF3B0)

    /** Meadow stage 0..6 (unchanged from the original growth curve). */
    fun stageFor(visitDays: Int): Int = when {
        visitDays <= 1 -> 0 // Bare soil with tiny sprout specks
        visitDays == 2 -> 1 // Clover patches & green sprouts
        visitDays in 3..4 -> 2 // Small floral buds appearing
        visitDays in 5..6 -> 3 // Blooming wildflowers
        visitDays in 7..9 -> 4 // Lush flower bushes & butterflies
        visitDays in 10..13 -> 5 // Blossom tree sapling
        else -> 6 // Paradise blooming tree, golden sparkles, ladybugs
    }

    private val lastKeepsakeDay get() = FIRST_BLOOM_DAY + DAYS_BETWEEN_BLOOMS * (keepsakePlants.size - 1)

    /** Visit day on which bloom number [index] appears. */
    fun bloomDay(index: Int): Int =
        if (index < keepsakePlants.size) FIRST_BLOOM_DAY + DAYS_BETWEEN_BLOOMS * index
        else lastKeepsakeDay + DAYS_BETWEEN_GOLDEN_BLOOMS * (index - keepsakePlants.size + 1)

    /** Every bloom earned after [visitDays] total days. Only ever grows as visitDays grows. */
    fun bloomsFor(visitDays: Int): List<GardenBloom> {
        val result = ArrayList<GardenBloom>()
        var index = 0
        while (bloomDay(index) <= visitDays) {
            val golden = index >= keepsakePlants.size
            result += GardenBloom(index, if (golden) goldenPlant else keepsakePlants[index], bloomDay(index), golden)
            index++
        }
        return result
    }

    /** Visit days still needed for the next bloom (always at least 1). */
    fun daysUntilNextBloom(visitDays: Int): Int {
        var index = 0
        while (bloomDay(index) <= visitDays) index++
        return bloomDay(index) - visitDays
    }

    /**
     * A gentle greeting after time away, or null if the gap is short. Never mentions loss,
     * because nothing was lost.
     */
    fun welcomeBackMessage(daysAway: Int, catName: String): String? = when {
        daysAway < WELCOME_BACK_AFTER_DAYS -> null
        daysAway < 14 -> "$catName kept the garden watered while you were away 🌱"
        else -> "Welcome back! The garden saved every flower for you 🌼"
    }
}
