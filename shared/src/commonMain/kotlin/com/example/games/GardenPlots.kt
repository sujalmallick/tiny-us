package com.example.games

/** How far a planted flower has grown. */
enum class PlotStage { EMPTY, SEED, SPROUT, BUD, BLOOM }

/**
 * One patch of soil. [waterings] counts the different days it was watered (by hand or by rain);
 * each one grows it a stage. [lastWatered] is that day (epoch day), so a second watering the same
 * day does nothing more.
 */
data class Plot(
    val flower: String? = null,
    val waterings: Int = 0,
    val lastWatered: Long = -1L
) {
    val stage: PlotStage
        get() = when {
            flower == null -> PlotStage.EMPTY
            waterings <= 0 -> PlotStage.SEED
            waterings == 1 -> PlotStage.SPROUT
            waterings < GardenPlots.WATERINGS_TO_BLOOM -> PlotStage.BUD
            else -> PlotStage.BLOOM
        }
}

/**
 * Garden care (plan 07, C5): a few patches of soil in the meadow. Plant a seed, water it once a
 * day, and over a few waterings it grows into a flower. Days without water only mean it waits;
 * nothing wilts. Rain waters it too. Picked flowers are held until there are enough for a bouquet,
 * which goes into the keepsake box (and can be given as a gift). Immutable, like ProgressState, so
 * it saves and restores as a whole.
 */
data class GardenPlots(
    val plots: List<Plot> = List(PLOTS) { Plot() },
    /** Flowers picked and held for the next bouquet. */
    val stems: List<String> = emptyList()
) {
    /** The garden after planting [flower] in plot [index]; unchanged if that plot isn't empty. */
    fun plant(index: Int, flower: String): GardenPlots {
        val plot = plots.getOrNull(index) ?: return this
        if (plot.flower != null || flower !in SEEDS) return this
        return copy(plots = plots.replace(index, Plot(flower)))
    }

    /**
     * The garden after watering plot [index] on [day] (epoch day). Once a day per plot, and only
     * while it's still growing; anything else leaves it as it was.
     */
    fun water(index: Int, day: Long): GardenPlots {
        val plot = plots.getOrNull(index) ?: return this
        if (!plot.canWater(day)) return this
        return copy(plots = plots.replace(index, plot.copy(waterings = plot.waterings + 1, lastWatered = day)))
    }

    /** Rain on [day] waters every growing plot that hasn't had water that day. */
    fun rain(day: Long): GardenPlots =
        copy(plots = plots.map { if (it.canWater(day)) it.copy(waterings = it.waterings + 1, lastWatered = day) else it })

    /**
     * Picks the flower in plot [index] if it's in bloom. Returns the garden after, and the bouquet
     * (the held stems) if this flower made one, otherwise null.
     */
    fun pick(index: Int): Pair<GardenPlots, List<String>?> {
        val plot = plots.getOrNull(index) ?: return this to null
        val flower = plot.flower
        if (plot.stage != PlotStage.BLOOM || flower == null) return this to null
        val held = stems + flower
        val emptied = plots.replace(index, Plot())
        return if (held.size >= BOUQUET_SIZE) copy(plots = emptied, stems = emptyList()) to held
        else copy(plots = emptied, stems = held) to null
    }

    /** Whether plot [index] would grow from a watering on [day]. */
    fun needsWater(index: Int, day: Long): Boolean = plots.getOrNull(index)?.canWater(day) == true

    private fun Plot.canWater(day: Long) = flower != null && stage != PlotStage.BLOOM && lastWatered != day

    private fun List<Plot>.replace(index: Int, plot: Plot) = mapIndexed { i, p -> if (i == index) plot else p }

    companion object {
        const val PLOTS = 3
        /** Waterings (on different days) from seed to bloom: a few days, at the couple's pace. */
        const val WATERINGS_TO_BLOOM = 3
        const val BOUQUET_SIZE = 3
        /** Seeds to choose from (ids shared with the meadow's keepsake plants, for their colours). */
        val SEEDS = listOf("sunflower", "tulip", "daisy", "lavender", "forget_me_not")
    }
}
