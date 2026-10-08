package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AmbientAudio
import com.example.engine.CozyProps
import com.example.engine.PixelGrid
import com.example.engine.WorldViewport
import com.example.scene.CozyPropLayout
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The cozy pixel props: their art, where taps land, and their tap animations. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CozyPropsTest {

    private lateinit var engine: SceneEngine

    /** Stage sizes the low-res renderer produces on narrow, typical and wide screens. */
    private val stages = listOf(720f to 960f, 720f to 1080f, 800f to 1200f)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("tiny_us_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        engine = SceneEngine(audio = AmbientAudio(context), onOpenLoveNotes = {}, onOpenMemories = {})
    }

    // ------------------------------------------------------------------ art

    @Test
    fun `every prop grid has art and its runs cover exactly the filled pixels`() {
        val grids = mapOf(
            "footbridge" to CozyProps.footbridge, "bicycle" to CozyProps.bicycle, "fence" to CozyProps.flowerFence(28),
            "basket" to CozyProps.hangingBasket, "petBed" to CozyProps.petBed, "fishToy" to CozyProps.fishToy,
            "farBirdhouse" to CozyProps.farBirdhouse, "farSignpost" to CozyProps.farSignpost, "farClothesline" to CozyProps.farClothesline
        )
        for ((name, grid) in grids) {
            assertTrue("$name is empty", grid.filledCount() > grid.width)
            // No dark outlines: every edge pixel was given its material's deeper shade.
            for (y in 0 until grid.height) for (x in 0 until grid.width) {
                assertTrue("$name still has an outline pixel at ($x, $y)", grid[x, y] != 'k')
            }
            val runPixels = runPixelCount(grid)
            assertEquals("$name runs must cover each filled pixel once", grid.filledCount(), runPixels)
        }
    }

    @Test
    fun `props take the scene's light so they darken at night like everything else`() {
        val red = androidx.compose.ui.graphics.Color(0xFFC4505E)
        fun luma(c: androidx.compose.ui.graphics.Color) = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
        assertEquals(red, CozyProps.outdoorLight(isNight = false, isSunset = false).apply(red))
        assertTrue(luma(CozyProps.outdoorLight(isNight = true, isSunset = false).apply(red)) < luma(red) * 0.75f)
        assertTrue(luma(CozyProps.indoorLight(isNight = true).apply(red)) < luma(red))
        // Lamps keep rooms brighter than the outdoors at night.
        assertTrue(luma(CozyProps.indoorLight(true).apply(red)) > luma(CozyProps.outdoorLight(true, false).apply(red)))
    }

    @Test
    fun `far props are smaller than the close ones`() {
        assertTrue(CozyProps.farBirdhouse.height < CozyProps.hangingBasket.height)
        assertTrue(CozyProps.farClothesline.width < 30)
        assertTrue(CozyProps.farSignpost.height <= 12)
    }

    @Test
    fun `flower fence keeps its posts and bushes at any width`() {
        for (w in listOf(16, 28, 40)) {
            val fence = CozyProps.flowerFence(w)
            assertEquals(w, fence.width)
            assertTrue("first post cap", fence[1, 1] != '.')
            assertTrue("rails must end on a post", fence[w - 3, 1] != '.')
            assertEquals('g', fence[0, 15]) // ground line
        }
    }

    private fun runPixelCount(grid: PixelGrid): Int {
        var n = 0
        for (y in 0 until grid.height) {
            var x = 0
            while (x < grid.width) {
                val ch = grid[x, y]
                if (ch == '.') { x++; continue }
                var end = x + 1
                while (end < grid.width && grid[end, y] == ch) end++
                n += end - x
                x = end
            }
        }
        return n
    }

    // ------------------------------------------------------------------ taps land on the art

    @Test
    fun `taps on each prop hit it on every stage size`() {
        for ((cw, ch) in stages) {
            val p = WorldViewport.pixelScale(cw)
            val bike = CozyPropLayout.walkBicycle(cw, ch)
            assertTrue("bicycle $cw x $ch", CozyPropLayout.hitWalkBicycle(Offset(bike.x, bike.y - 9f * p), cw, ch, p))

            val creekTop = CozyPropLayout.walkCreekTop(ch, p)
            assertTrue("creek $cw x $ch", CozyPropLayout.hitWalkCreek(Offset(cw * 0.85f, creekTop + 3f * p), cw, ch, p))

            val fence = CozyPropLayout.meadowFence(cw, ch)
            assertTrue("fence $cw x $ch", CozyPropLayout.hitMeadowFence(Offset(fence.x, fence.y - 8f * p), cw, ch, p))

            val hook = CozyPropLayout.sunroomBasketHook(cw, ch, p)
            assertTrue("basket $cw x $ch", CozyPropLayout.hitSunroomBasket(Offset(hook.x, hook.y + 9f * p), cw, ch, p))
        }
    }

    @Test
    fun `the glowing mushrooms keep their own taps inside the creek`() {
        for ((cw, ch) in stages) {
            val p = WorldViewport.pixelScale(cw)
            val tap = Offset(CozyPropLayout.walkMushrooms(cw), CozyPropLayout.walkCreekTop(ch, p) + 3f * p)
            assertFalse(CozyPropLayout.hitWalkCreek(tap, cw, ch, p))
        }
    }

    @Test
    fun `the basket stays clear of the skylight band except where it hangs`() {
        val (cw, ch) = 720f to 1080f
        val p = WorldViewport.pixelScale(cw)
        // A skylight tap well away from the basket is not taken by it.
        assertFalse(CozyPropLayout.hitSunroomBasket(Offset(cw * 0.60f, ch * 0.22f), cw, ch, p))
    }

    @Test
    fun `the bicycle sits on the path between the telescope and the streetlamp`() {
        val (cw, ch) = 720f to 1080f
        val bike = CozyPropLayout.walkBicycle(cw, ch)
        assertTrue(bike.x > cw * 0.45f && bike.x < cw * 0.65f)
        assertEquals(ch * 0.68f, bike.y, 0.01f)
    }

    @Test
    fun `pet bed and fish toy sit on the living room's front row, clear of the walls`() {
        for ((cw, ch) in stages) {
            val p = WorldViewport.pixelScale(cw)
            val bed = CozyPropLayout.petBed(cw, ch, p)
            val toy = CozyPropLayout.fishToy(cw, ch, p)
            assertTrue(bed.y < ch && toy.y < ch)
            assertEquals(bed.y, toy.y, 0.01f)
            assertTrue("bed and toy must not overlap", bed.x - toy.x > (CozyProps.petBed.width + CozyProps.fishToy.width) / 2f * p - 4f * p)
        }
    }

    // ------------------------------------------------------------------ tap animations

    @Test
    fun `each prop tap plays once, ignores repeat taps and then settles`() {
        val taps = listOf<Pair<() -> Unit, () -> Float>>(
            { engine.onTouchCreek(300f, 900f) } to { engine.creekRippleTimer },
            { engine.onTouchBicycle(400f, 700f) } to { engine.bicycleBellTimer },
            { engine.onTouchFlowerFence(500f, 700f) } to { engine.fenceRustleTimer },
            { engine.onTouchHangingBasket(200f, 260f) } to { engine.basketSwayTimer }
        )
        for ((tap, timer) in taps) {
            assertEquals(0f, timer())
            tap()
            val started = timer()
            assertTrue(started > 1f)
            tap()
            assertEquals("a repeat tap must not restart or stack", started, timer(), 0.0001f)
        }
        repeat(200) { engine.update(1f / 60f, 720f, 1080f) }
        for ((_, timer) in taps) assertEquals("animation must settle by itself", 0f, timer(), 0.0001f)
    }

    @Test
    fun `changing scene resets the prop animations`() {
        engine.onTouchCreek(300f, 900f)
        engine.onTouchBicycle(400f, 700f)
        engine.onTouchFlowerFence(500f, 700f)
        engine.onTouchHangingBasket(200f, 260f)
        engine.loadScene(SceneType.SUNROOM)
        assertEquals(0f, engine.creekRippleTimer)
        assertEquals(0f, engine.bicycleBellTimer)
        assertEquals(0f, engine.fenceRustleTimer)
        assertEquals(0f, engine.basketSwayTimer)
    }

    @Test
    fun `prop progress runs from 0 to 1 and is negative when idle`() {
        assertEquals(-1f, engine.propProgress(0f, 2f), 0f)
        assertEquals(0f, engine.propProgress(2f, 2f), 0.0001f)
        assertEquals(0.75f, engine.propProgress(0.5f, 2f), 0.0001f)
    }
}
