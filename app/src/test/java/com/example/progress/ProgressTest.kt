package com.example.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressTest {

    private fun run(vararg events: ProgressEvent, start: ProgressState = ProgressState(), day: Long = 20000): Pair<ProgressState, List<String>> {
        var s = start
        val earned = mutableListOf<String>()
        for (e in events) {
            val (next, firsts) = LittleFirsts.apply(s, e, day)
            s = next
            earned += firsts.map { it.id }
        }
        return s to earned
    }

    @Test
    fun aFirstIsEarnedOnceWithItsDateAndReward() {
        val (s, earned) = run(ProgressEvent.RainbowWish, ProgressEvent.RainbowWish)
        assertEquals(listOf("first_rainbow"), earned)
        assertEquals(2, s.count(Counter.RAINBOW_WISHES))
        assertEquals(20000L, s.firsts["first_rainbow"])
        assertTrue(Rewards.RAINBOW_SCARF in s.unlocked)
    }

    @Test
    fun fullMoonsCountOncePerNight() {
        val (s, earned) = run(ProgressEvent.FullMoonSeen(100), ProgressEvent.FullMoonSeen(100), ProgressEvent.FullMoonSeen(130))
        assertEquals(2, s.count(Counter.FULL_MOONS))
        assertEquals(listOf("first_full_moon"), earned)
    }

    @Test
    fun visitingEverySceneAndSeasonAddsUp() {
        val scenes = SceneType.values().map { ProgressEvent.SceneVisited(it.name) }
        val seasons = listOf("WINTER", "SPRING", "SUMMER", "AUTUMN").map { ProgressEvent.WeatherSeen("SUNNY", it) }
        val (_, earned) = run(*(scenes + seasons).toTypedArray())
        assertTrue("every_scene" in earned)
        assertTrue("four_seasons" in earned)
    }

    @Test
    fun discoveriesGoInTheKeepsakeBox() {
        val all = LittleFirsts.DISCOVERY_KINDS.map { ProgressEvent.DiscoveryFound(it) }
        val (s, earned) = run(*all.toTypedArray(), ProgressEvent.DiscoveryFound("SEASHELL"))
        assertEquals(2, s.keepsakes["discovery:SEASHELL"])
        assertTrue("first_discovery" in earned && "all_discoveries" in earned)
    }

    @Test
    fun daysTogetherOnlyGoUp() {
        val (s, earned) = run(ProgressEvent.DaysTogether(120), ProgressEvent.DaysTogether(3))
        assertEquals(120, s.count(Counter.DAYS_TOGETHER))
        assertTrue(earned.containsAll(listOf("days_7", "days_30", "days_100")))
        assertTrue(Rewards.STARRY_ROOM in s.unlocked)
    }

    @Test
    fun nothingIsEverTakenBack() {
        val (s, _) = run(ProgressEvent.SnowmanBuilt)
        // Even if a later state somehow no longer qualifies, an earned first and its reward stay.
        val (after, again) = LittleFirsts.apply(s.copy(counters = emptyMap()), ProgressEvent.LoveNote, 20001)
        assertTrue("first_snowman" in after.firsts)
        assertTrue(Rewards.SNOWMAN_BEANIE in after.unlocked)
        assertEquals(listOf("first_love_note"), again.map { it.id })
    }

    @Test
    fun mochiFondnessGrowsSlowlyAndNeverFalls() {
        // Lots of petting in one day only counts up to the daily cap.
        val (one, _) = run(*Array(40) { ProgressEvent.MochiCare(1, 100) })
        assertEquals(MochiFondness.DAILY_CAP, one.count(Counter.MOCHI_FONDNESS))
        // A new day, a little more.
        val (two, earned) = run(ProgressEvent.MochiCare(3, 101), ProgressEvent.MochiCare(3, 101), start = one)
        assertEquals(MochiFondness.DAILY_CAP + 6, two.count(Counter.MOCHI_FONDNESS))
        assertTrue(earned.isEmpty())
        // Enough days make a best friend, with the headband.
        var s = two
        val all = mutableListOf<String>()
        for (d in 102L..120L) {
            val (next, e) = run(ProgressEvent.MochiCare(12, d), start = s)
            s = next
            all += e
        }
        assertEquals(3, MochiFondness.level(s.count(Counter.MOCHI_FONDNESS)))
        assertTrue(all.containsAll(listOf("mochi_friendly", "mochi_cuddly", "mochi_best_friend")))
        assertTrue(Rewards.MOCHI_HEADBAND in s.unlocked)
    }

    @Test
    fun aGiftMovesAKeepsakeToTheShelf() {
        val (found, _) = run(ProgressEvent.DiscoveryFound("SEASHELL"), ProgressEvent.DiscoveryFound("SEASHELL"))
        val (given, earned) = run(ProgressEvent.GiftGiven("discovery:SEASHELL", fromBoy = true), start = found)
        assertEquals(1, given.keepsakes["discovery:SEASHELL"])
        assertEquals(listOf("discovery:SEASHELL"), given.shelf)
        assertEquals(listOf("first_gift"), earned)
        // Nothing left to give: nothing happens.
        val (none, _) = run(ProgressEvent.GiftGiven("discovery:STAR_PEBBLE", fromBoy = false), start = given)
        assertEquals(given, none)
    }

    @Test
    fun theStoreKeepsEverythingInTheBackedUpPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(ProgressStore.PREFS_FILE, Context.MODE_PRIVATE)
        val (s, _) = run(
            ProgressEvent.RainbowWish, ProgressEvent.ConstellationFound("TWO_HEARTS"),
            ProgressEvent.DiscoveryFound("SEASHELL"), ProgressEvent.Caught("SNOWFLAKE")
        )
        val withBest = s.bestScore("catch", 42)
        ProgressStore(prefs).save(withBest)
        assertEquals(withBest, ProgressStore(prefs).load())
        // The in-app backup copies tiny_us_prefs, and so the progress with it.
        assertTrue(ProgressStore.PREFS_FILE in com.example.data.backup.TinyBackup.PREF_FILES)
    }
}
