package com.example.games

/** How far a planted seed has grown (a crop's BLOOM is "ripe"). */
enum class PlotStage { EMPTY, SEED, SPROUT, BUD, BLOOM }

/** Who a spot of soil belongs to (plan 09, E2): each of them has one, and one is shared. */
enum class PlotOwner { BOY, GIRL, SHARED, ANYONE }

enum class SeedKind { FLOWER, CROP, HERB }

/**
 * Something to plant. Flowers are picked for bouquets; crops and herbs are harvested into the
 * pantry as [produce]. [seasons] are when it can go into the meadow (WINTER, SPRING, SUMMER,
 * AUTUMN); [inPots] means it also grows in the sunroom pots, the winter greenhouse, all year.
 */
class Seed(
    val id: String,
    val kind: SeedKind,
    val seasons: Set<String>,
    val produce: Ingredient? = null,
    val inPots: Boolean = false
)

object Seeds {
    private val WARM = setOf("SPRING", "SUMMER", "AUTUMN")

    val ALL: List<Seed> = listOf(
        // Flowers, for bouquets, whenever it isn't winter.
        Seed("sunflower", SeedKind.FLOWER, WARM),
        Seed("tulip", SeedKind.FLOWER, WARM),
        Seed("daisy", SeedKind.FLOWER, WARM),
        Seed("lavender", SeedKind.FLOWER, WARM),
        Seed("forget_me_not", SeedKind.FLOWER, WARM),
        // Crops, each in its season (plan 09, E1).
        Seed("strawberry", SeedKind.CROP, setOf("SPRING"), Ingredient.STRAWBERRY),
        Seed("peas", SeedKind.CROP, setOf("SPRING"), Ingredient.PEAS),
        Seed("tomato", SeedKind.CROP, setOf("SUMMER"), Ingredient.TOMATO),
        Seed("basil", SeedKind.CROP, setOf("SUMMER"), Ingredient.BASIL, inPots = true),
        Seed("pumpkin", SeedKind.CROP, setOf("AUTUMN"), Ingredient.PUMPKIN),
        Seed("apple", SeedKind.CROP, setOf("AUTUMN"), Ingredient.APPLE),
        // Herbs only grow in the sunroom pots, so there's always something to grow in winter.
        Seed("mint", SeedKind.HERB, emptySet(), Ingredient.MINT, inPots = true)
    )

    fun byId(id: String?): Seed? = ALL.firstOrNull { it.id == id }

    /** What can go into a meadow plot in [season]: nothing in winter (the sunroom pots are for that). */
    fun forMeadow(season: String): List<Seed> = ALL.filter { season in it.seasons }

    /** What grows in the sunroom pots, all year. */
    fun forPots(): List<Seed> = ALL.filter { it.inPots }
}

/**
 * One spot of soil (a meadow plot or a sunroom pot). [flower] is the seed planted (named for the
 * flowers it first held). [waterings] counts the different days it was watered (by hand or by
 * rain); each one grows it a stage. [lastWatered] is that day (epoch day), so a second watering the
 * same day does nothing more. On the shared plot, [sharedBy] is who has watered it on [sharedDay]:
 * it only grows once both of them have.
 */
data class Plot(
    val flower: String? = null,
    val waterings: Int = 0,
    val lastWatered: Long = -1L,
    val sharedBy: Set<String> = emptySet(),
    val sharedDay: Long = -1L
) {
    val stage: PlotStage
        get() = when {
            flower == null -> PlotStage.EMPTY
            waterings <= 0 -> PlotStage.SEED
            waterings == 1 -> PlotStage.SPROUT
            waterings < GardenPlots.WATERINGS_TO_BLOOM -> PlotStage.BUD
            else -> PlotStage.BLOOM
        }

    val seed: Seed? get() = Seeds.byId(flower)
}

/**
 * Garden care (plan 07, C5; plan 09, E1-E2): three plots at the front of the meadow (his, a
 * shared one, hers) and two pots in the sunroom. Plant a seed, water it once a day, and over a few
 * waterings it grows. Days without water only mean it waits; nothing wilts. Rain waters the
 * meadow. Picked flowers are held until there are enough for a bouquet; ripe crops and herbs go to
 * the pantry. Immutable, like ProgressState, so it saves and restores as a whole.
 */
data class GardenPlots(
    val plots: List<Plot> = List(SPOTS) { Plot() },
    /** Flowers picked and held for the next bouquet. */
    val stems: List<String> = emptyList()
) {
    /** The garden after planting [seed] at [index]; unchanged if the spot isn't empty or the seed can't grow there. */
    fun plant(index: Int, seed: String): GardenPlots {
        val plot = plots.getOrNull(index) ?: return this
        val s = Seeds.byId(seed) ?: return this
        if (plot.flower != null) return this
        if (isPot(index) && !s.inPots) return this
        if (!isPot(index) && s.seasons.isEmpty()) return this
        return copy(plots = plots.replace(index, Plot(seed)))
    }

    /**
     * The garden after [by] waters spot [index] on [day] (epoch day). Once a day per spot, and only
     * while it's still growing. The shared plot needs both of them that day: the first watering is
     * remembered, the second makes it grow. Watering without saying who (or rain) just waters.
     */
    fun water(index: Int, day: Long, by: PlotOwner? = null): GardenPlots {
        val plot = plots.getOrNull(index) ?: return this
        if (!plot.canWater(day)) return this
        if (ownerOf(index) == PlotOwner.SHARED && (by == PlotOwner.BOY || by == PlotOwner.GIRL)) {
            val today = if (plot.sharedDay == day) plot.sharedBy else emptySet()
            if (by.name in today) return this
            val both = today + by.name
            val grown = if (both.size >= 2) plot.copy(waterings = plot.waterings + 1, lastWatered = day) else plot
            return copy(plots = plots.replace(index, grown.copy(sharedBy = both, sharedDay = day)))
        }
        return copy(plots = plots.replace(index, plot.copy(waterings = plot.waterings + 1, lastWatered = day)))
    }

    /** Rain on [day] waters every growing meadow plot that hasn't had water that day (the pots are indoors). */
    fun rain(day: Long): GardenPlots =
        copy(plots = plots.mapIndexed { i, p -> if (!isPot(i) && p.canWater(day)) p.copy(waterings = p.waterings + 1, lastWatered = day) else p })

    /**
     * Picks the flower at [index] if it's in bloom. Returns the garden after, and the bouquet
     * (the held stems) if this flower made one, otherwise null. Crops are harvested instead.
     */
    fun pick(index: Int): Pair<GardenPlots, List<String>?> {
        val plot = plots.getOrNull(index) ?: return this to null
        val flower = plot.flower
        if (plot.stage != PlotStage.BLOOM || flower == null || plot.seed?.kind != SeedKind.FLOWER) return this to null
        val held = stems + flower
        val emptied = plots.replace(index, Plot())
        return if (held.size >= BOUQUET_SIZE) copy(plots = emptied, stems = emptyList()) to held
        else copy(plots = emptied, stems = held) to null
    }

    /** Harvests the ripe crop or herb at [index]: the garden after, and what goes to the pantry (or null). */
    fun harvest(index: Int): Pair<GardenPlots, Ingredient?> {
        val plot = plots.getOrNull(index) ?: return this to null
        val produce = plot.seed?.produce
        if (plot.stage != PlotStage.BLOOM || produce == null) return this to null
        return copy(plots = plots.replace(index, Plot())) to produce
    }

    /** Whether [by] watering spot [index] on [day] would count (on the shared plot, if they haven't yet today). */
    fun needsWater(index: Int, day: Long, by: PlotOwner? = null): Boolean {
        val plot = plots.getOrNull(index) ?: return false
        if (!plot.canWater(day)) return false
        if (ownerOf(index) == PlotOwner.SHARED && by != null && plot.sharedDay == day) return by.name !in plot.sharedBy
        return true
    }

    /** On the shared plot, who has watered it today but is still waiting for the other (null otherwise). */
    fun waitingOnOther(index: Int, day: Long): PlotOwner? {
        val plot = plots.getOrNull(index) ?: return null
        if (ownerOf(index) != PlotOwner.SHARED || plot.sharedDay != day || plot.sharedBy.size != 1 || plot.lastWatered == day) return null
        return PlotOwner.valueOf(plot.sharedBy.first())
    }

    private fun Plot.canWater(day: Long) = flower != null && stage != PlotStage.BLOOM && lastWatered != day

    private fun List<Plot>.replace(index: Int, plot: Plot) = mapIndexed { i, p -> if (i == index) plot else p }

    companion object {
        /** The meadow plots: his (0), shared (1) and hers (2). */
        const val PLOTS = 3
        /** The sunroom pots, after the meadow plots (indices 3 and 4). */
        const val POTS = 2
        const val SPOTS = PLOTS + POTS
        /** Waterings (on different days) from seed to bloom: a few days, at the couple's pace. */
        const val WATERINGS_TO_BLOOM = 3
        const val BOUQUET_SIZE = 3
        /** The flower seeds (ids shared with the meadow's keepsake plants, for their colours). */
        val SEEDS: List<String> = Seeds.ALL.filter { it.kind == SeedKind.FLOWER }.map { it.id }

        fun isPot(index: Int) = index >= PLOTS

        fun ownerOf(index: Int): PlotOwner = when (index) {
            0 -> PlotOwner.BOY
            1 -> PlotOwner.SHARED
            2 -> PlotOwner.GIRL
            else -> PlotOwner.ANYONE
        }
    }
}
