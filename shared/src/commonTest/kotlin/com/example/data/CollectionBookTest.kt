package com.example.data

import com.example.games.Recipes
import com.example.games.Seeds
import com.example.progress.Gifts
import com.example.progress.ProgressEvent
import com.example.progress.ProgressState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionBookTest {

    @Test
    fun everyPageHasThingsAndNothingIsListedTwice() {
        val keys = CollectionBook.ENTRIES.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        for (p in CollectionPage.entries) assertTrue(CollectionBook.page(p).isNotEmpty(), "$p is empty")
        // Every recipe and every crop has its place in the book.
        for (r in Recipes.ALL) assertTrue("dish:${r.id}" in keys, r.id)
        for (s in Seeds.ALL) s.produce?.let { assertTrue("crop:${it.name}" in keys, it.name) }
    }

    @Test
    fun theRareOnesAreHiddenAndTheSeasonalOnesHaveHints() {
        val byKey = CollectionBook.ENTRIES.associateBy { it.key }
        for (k in listOf("catch:GOLDEN_FISH", "catch:GLOW_SQUID", "catch:PEARL", "catch:SEA_GLASS_HEART", "discovery:STAR_PEBBLE")) {
            assertTrue(byKey.getValue(k).hidden, k)
        }
        assertFalse(byKey.getValue("catch:MINNOW").hidden)
        assertEquals(CollectionHint.SPRING, byKey.getValue("catch:BLOSSOM_KOI").hint)
        assertEquals(CollectionHint.RAIN, byKey.getValue("catch:RAIN_TROUT").hint)
        assertEquals(CollectionHint.WINTER, byKey.getValue("catch:ICE_COD").hint)
        assertEquals(CollectionHint.NIGHT, byKey.getValue("catch:MOON_JELLY").hint)
        assertEquals(CollectionHint.FOR_HER, byKey.getValue("catch:HEART_SHELL").hint)
        assertEquals(CollectionHint.AUTUMN, byKey.getValue("discovery:RED_LEAF").hint)
        // A garden crop with a single season says so.
        val seasonal = Seeds.ALL.first { it.produce != null && it.seasons.size == 1 }
        assertTrue(byKey.getValue("crop:${seasonal.produce!!.name}").hint != null)
    }

    @Test
    fun foundFromEverySortOfProgress() {
        var p = ProgressState()
        assertTrue(CollectionBook.ENTRIES.none { CollectionBook.isFound(it.key, p) })
        p = p.record(ProgressEvent.DiscoveryFound("SEASHELL", "GIRL"))
            .record(ProgressEvent.FishCaught("PEARL"))
            .record(ProgressEvent.DishCooked("soup"))
            .record(ProgressEvent.FestivalCelebrated("LANTERN_NIGHT"))
            .record(ProgressEvent.ThankYouJarFilled)
        for (k in listOf("discovery:SEASHELL", "catch:PEARL", "dish:soup", "festival:LANTERN_NIGHT", "jar:THANK_YOU")) {
            assertTrue(CollectionBook.isFound(k, p), k)
        }
        assertFalse(CollectionBook.isFound("catch:SEASHELL", p)) // a shell from the sand isn't one from the sea
        // A crop counts once it's been harvested, even after it's been cooked away.
        val crop = Seeds.ALL.first { it.produce != null }.produce!!
        assertTrue(CollectionBook.isFound("crop:${crop.name}", p.copy(seen = p.seen + ("crops" to setOf(crop.name)))))
    }

    @Test
    fun givingAThingAwayDoesNotLoseItFromTheBook() {
        val p = ProgressState().keep(Gifts.BOUQUET).record(ProgressEvent.GiftGiven(Gifts.BOUQUET, fromBoy = true))
        assertEquals(0, p.keepsakes[Gifts.BOUQUET])
        assertTrue(CollectionBook.isFound(Gifts.BOUQUET, p))
    }

    @Test
    fun newlyFoundIsOnlyWhatTheEventBrought() {
        val before = ProgressState().record(ProgressEvent.FishCaught("MINNOW"))
        val after = before.record(ProgressEvent.FishCaught("MINNOW")).record(ProgressEvent.FishCaught("GOLDEN_FISH"))
        assertEquals(listOf("catch:GOLDEN_FISH"), CollectionBook.newlyFound(before, after))
        assertEquals(emptyList(), CollectionBook.newlyFound(after, after))
    }

    @Test
    fun foundYouOpensALineAsTheBookFillsAndTheLastWhenItIsFull() {
        val total = CollectionBook.ENTRIES.size
        assertEquals(1, CollectionBook.foundYouLinesOpen(0)) // the first line is there from the start
        assertEquals(3, CollectionBook.foundYouNextIn(0))
        assertEquals(2, CollectionBook.foundYouLinesOpen(3))
        assertEquals(1, CollectionBook.foundYouNextIn(7))
        assertEquals(CollectionBook.FOUND_YOU_STEPS.size - 1, CollectionBook.foundYouLinesOpen(total - 1))
        assertEquals(CollectionBook.FOUND_YOU_STEPS.size, CollectionBook.foundYouLinesOpen(total))
        assertNull(CollectionBook.foundYouNextIn(total))
        assertEquals(CollectionBook.FOUND_YOU_STEPS, CollectionBook.FOUND_YOU_STEPS.sorted())
        assertEquals(2, ProgressState().record(ProgressEvent.FishCaught("MINNOW")).record(ProgressEvent.ThankYouJarFilled)
            .let(CollectionBook::foundCount))
    }

    @Test
    fun theFirstFindIsKeptAndLaterOnesDoNotChangeIt() {
        val storage = InMemoryKeyValueStorage()
        val store = CollectionStore(storage)
        assertNull(store.first("catch:PEARL"))
        store.record(FirstFind("catch:PEARL", "BOY", true, "SEASIDE_PIER", "RAIN", "NIGHT", 20_000))
        store.record(FirstFind("catch:PEARL", "GIRL", false, "SUNROOM", "SUNNY", "MORNING", 20_100))
        store.record(FirstFind("discovery:SEASHELL", "GIRL", false, "WALK", "SUNNY", "AFTERNOON", 20_050))

        // A fresh store reads the same stories back.
        val again = CollectionStore(storage)
        assertEquals(FirstFind("catch:PEARL", "BOY", true, "SEASIDE_PIER", "RAIN", "NIGHT", 20_000), again.first("catch:PEARL"))
        assertEquals("GIRL", again.first("discovery:SEASHELL")?.by)
        assertEquals(2, again.all().size)
    }

    @Test
    fun aBrokenSaveReadsAsAnEmptyBook() {
        val storage = InMemoryKeyValueStorage()
        storage.putString("collection_first_finds_json", "not json")
        assertTrue(CollectionStore(storage).all().isEmpty())
    }
}
