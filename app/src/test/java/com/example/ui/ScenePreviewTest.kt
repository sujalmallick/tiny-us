package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.FeatureFlags
import com.example.engine.AmbientAudio
import com.example.engine.TimeOfDayPhase
import com.example.engine.WorldCamera
import com.example.engine.WorldViewport
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real world frame offscreen (Plan 03, Phase 0) and checks the low-res renderer's grid.
 *
 * Set SCENE_PREVIEW_DIR to also write PNGs of every scene at day, sunset and night, drawn by
 * both renderers, for side-by-side review (SCENE_PREVIEW_SCENES=A,B limits the scenes; run with
 * --rerun, since Gradle doesn't see environment changes). The "classic" images keep the pixel
 * renderer's sprite sizes for the barista and Grandpa Bao.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScenePreviewTest {
    private val cw = 1080f
    private val ch = 2400f

    private fun engineFor(scene: SceneType, phase: TimeOfDayPhase, cw: Float = this.cw, ch: Float = this.ch) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            loadScene(scene)
            updateAtmosphereMode(if (phase == TimeOfDayPhase.NIGHT || phase == TimeOfDayPhase.SUNSET) phase.name else "DAY")
            // SCENE_PREVIEW_WEATHER=RAIN (or SNOW, SAKURA, AUTUMN) previews weather.
            // SCENE_PREVIEW_MOON=0.25 pins the moon's phase (0 new, 0.5 full).
            moonPhaseOverride = System.getenv("SCENE_PREVIEW_MOON")?.toFloat()
            // SCENE_PREVIEW_SPECIAL=DIWALI (or any SpecialDay) dresses the scene for that day.
            com.example.engine.SpecialDays.override = System.getenv("SCENE_PREVIEW_SPECIAL")?.let { com.example.engine.SpecialDay.valueOf(it) }
            System.getenv("SCENE_PREVIEW_WEATHER")?.let { name ->
                weatherDriftEnabled = false
                weather = com.example.scene.WeatherType.valueOf(name)
            }
            // Let the scene-change fade finish and the characters settle (two seconds of frames;
            // ten when previewing weather, so it has filled the sky).
            // SCENE_PREVIEW_FRAMES overrides the count (for example to let the birds arrive).
            // SCENE_PREVIEW_CATCH=1 starts a round of Catch together (with falling weather).
            if (System.getenv("SCENE_PREVIEW_CATCH") != null) startCatchGame()
            // SCENE_PREVIEW_COZY=cook (a dish served, in the kitchen), fish (the couple's line out at
            // the pier) or garden (the meadow's plots growing, one in bloom, a picked flower held).
            // Several at once, comma-separated; each only shows in its own scene.
            for (mode in System.getenv("SCENE_PREVIEW_COZY")?.split(',').orEmpty()) when (mode.trim()) {
                "cook" -> {
                    cozy.startCooking(kotlin.random.Random(2))
                    cozy.cooking.recipe?.ingredients?.forEach { cozy.tapIngredient(it) }
                    repeat(180) { update(1f / 60f, cw, ch) }
                }
                "fish" -> cozy.startFishing()
                "garden" -> cozy.garden = com.example.games.GardenPlots()
                    .plant(0, "tulip").water(0, 1).water(0, 2).water(0, 3)
                    .plant(1, "sunflower").water(1, 1)
                    .plant(2, "lavender")
                    .copy(stems = listOf("daisy"))
            }
            val frames = System.getenv("SCENE_PREVIEW_FRAMES")?.toInt() ?: if (System.getenv("SCENE_PREVIEW_WEATHER") != null) 600 else 120
            repeat(frames) { update(1f / 60f, cw, ch) }
            check(wipeAlpha == 0f)
            // SCENE_PREVIEW_GIFTS=1 fills the kitchen's keepsake shelf and shows Mochi's heart meter.
            if (System.getenv("SCENE_PREVIEW_GIFTS") != null) {
                keepsakeShelf = com.example.progress.Gifts.GIVEABLE
                mochiFondness = 75
                onTouchCat(cw, ch)
            }
            // SCENE_PREVIEW_STARS=2 shows the second constellation's puzzle with two stars connected.
            System.getenv("SCENE_PREVIEW_STARS")?.toInt()?.let { n ->
                val c = com.example.games.Constellations.ALL[n - 1]
                starPuzzle.start(c)
                val drift = skyDrift(sceneTime)
                repeat(2) { i -> starPuzzle.tap(starPuzzle.starX(c, i, drift) * cw, c.stars[i].second * ch, cw, ch, drift) }
            }
            // SCENE_PREVIEW_OUTFITS=7,2 dresses the girl and the boy in those wardrobe outfits;
            // two more numbers give their accessories (7,2,4,5).
            System.getenv("SCENE_PREVIEW_OUTFITS")?.split(',')?.map { it.trim().toInt() }?.let { v ->
                girl.outfitIndex = v[0]
                boy.outfitIndex = v[1]
                if (v.size >= 4) {
                    girl.accessoryIndex = v[2]
                    boy.accessoryIndex = v[3]
                }
            }
            // SCENE_PREVIEW_EARPHONES=1 puts the shared earphones on both of them.
            if (System.getenv("SCENE_PREVIEW_EARPHONES") != null) setEarphones(true)
            // SCENE_PREVIEW_THEME=STARRY_NIGHT sets the room theme.
            System.getenv("SCENE_PREVIEW_THEME")?.let { setRoomTheme(com.example.engine.RoomTheme.valueOf(it), announce = false) }
            // SCENE_PREVIEW_RAINBOW=1 shows the after-rain rainbow, part way through.
            if (System.getenv("SCENE_PREVIEW_RAINBOW") != null) rainbowTimer = com.example.scene.WeatherLayout.RAINBOW_SECONDS * 0.6f
            // SCENE_PREVIEW_HELD=MUG,BOOK:use puts those in the girl's and the boy's hands (":use" raises it).
            System.getenv("SCENE_PREVIEW_HELD")?.split(',')?.zip(listOf(girl, boy))?.forEach { (spec, who) ->
                val parts = spec.trim().split(':')
                who.hold(com.example.engine.HeldItem.valueOf(parts[0]), 60f, useSeconds = if (parts.size > 1) 30f else 0f)
                who.heldItemAge = 0.3f
            }
            // SCENE_PREVIEW_EMOTE=HEART,MUSIC_NOTE shows those emote bubbles over the girl and the boy.
            System.getenv("SCENE_PREVIEW_EMOTE")?.split(',')?.zip(listOf(girl, boy))?.forEach { (name, who) ->
                who.emote = com.example.engine.EmoteType.valueOf(name.trim())
                who.emoteTimer = 5f
            }
        }

    private fun render(cw: Float = this.cw, ch: Float = this.ch, block: DrawScope.() -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(cw.toInt(), ch.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(android.graphics.Canvas(bmp))
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(cw, ch), block)
        return bmp
    }

    @Test
    fun everySceneUsesThePixelRenderer() {
        assertTrue(FeatureFlags.PIXEL_RENDERER)
        for (scene in SceneType.values()) assertTrue(scene.name, engineFor(scene, TimeOfDayPhase.NIGHT).usesLowResRenderer)
    }

    @Test
    fun lowResFrameIsBuiltFromWholeGamePixels() {
        reallocateNightStars(42L)
        val scale = WorldViewport.gameScale(cw)
        assertEquals(5, scale)
        val engine = engineFor(SceneType.CAMPFIRE, TimeOfDayPhase.NIGHT)
        val buffer = LowResWorldBuffer()
        val screen = render { buffer.draw(this, scale) { drawWorldFrame(engine, lowRes = true) } }

        val frame = buffer.frame!!
        assertEquals(WorldViewport.gameWidth(cw), frame.width)
        assertEquals(WorldViewport.gameHeight(cw, ch), frame.height)

        // Every screen pixel equals the top-left pixel of its scale-by-scale block.
        var mismatches = 0
        for (y in 0 until ch.toInt()) for (x in 0 until cw.toInt()) {
            if (screen.getPixel(x, y) != screen.getPixel(x - x % scale, y - y % scale)) mismatches++
        }
        assertEquals("pixels that break the game-pixel grid", 0, mismatches)

        // The scene actually drew something: many distinct colours, nothing left transparent.
        val small = frame.asAndroidBitmap()
        val colours = HashSet<Int>()
        var transparent = 0
        for (y in 0 until small.height) for (x in 0 until small.width) {
            val c = small.getPixel(x, y)
            colours += c
            if (c ushr 24 == 0) transparent++
        }
        assertTrue("only ${colours.size} colours", colours.size > 40)
        assertEquals("transparent game pixels", 0, transparent)
    }

    @Test
    fun stagedFrameFillsTheScreenInWholeZoomBlocks() {
        reallocateNightStars(42L)
        val camera = WorldCamera.forScreen(cw, ch, pixelRenderer = true)
        val engine = engineFor(SceneType.SEASIDE_PIER, TimeOfDayPhase.NIGHT, camera.worldW, camera.worldH)
        val buffer = LowResWorldBuffer()
        val screen = render { buffer.drawStaged(this, camera) { drawWorldFrame(engine, lowRes = true) } }
        val z = camera.zoom
        var mismatches = 0
        var transparent = 0
        for (y in 0 until ch.toInt()) for (x in 0 until cw.toInt()) {
            val c = screen.getPixel(x, y)
            if (c != screen.getPixel(x - x % z, y - y % z)) mismatches++
            if (c ushr 24 == 0) transparent++
        }
        assertEquals("pixels that break the zoomed grid", 0, mismatches)
        assertEquals("screen pixels left empty above or below the stage", 0, transparent)
    }

    @Test
    fun lowResShapesHaveHardEdges() {
        // Off-grid shapes on black must come out as pure white or pure black game pixels.
        val buffer = LowResWorldBuffer()
        render {
            buffer.draw(this, 5) {
                drawRect(Color.Black, Offset.Zero, size)
                drawCircle(Color.White, 37.3f, Offset(101.7f, 98.2f))
                drawOval(Color.White, Offset(302.4f, 51.1f), Size(83.3f, 41.7f))
                drawLine(Color.White, Offset(12.2f, 400.4f), Offset(611.9f, 523.3f), strokeWidth = 7.3f)
                drawRoundRect(Color.White, Offset(640.6f, 700.3f), Size(97.1f, 61.9f), CornerRadius(14f))
            }
        }
        val frame = buffer.frame!!.asAndroidBitmap()
        val colours = HashSet<Int>()
        for (y in 0 until frame.height) for (x in 0 until frame.width) colours += frame.getPixel(x, y)
        assertEquals(setOf(android.graphics.Color.BLACK, android.graphics.Color.WHITE), colours)
    }

    @Test
    fun writesPreviewPngsWhenAsked() {
        val out = File(System.getenv("SCENE_PREVIEW_DIR") ?: return).apply { mkdirs() }
        // SCENE_PREVIEW_SIZE=720x960 renders at another canvas size.
        val (cw, ch) = System.getenv("SCENE_PREVIEW_SIZE")?.split('x')?.map { it.trim().toFloat() }
            ?.let { it[0] to it[1] } ?: (this.cw to this.ch)
        val only = System.getenv("SCENE_PREVIEW_SCENES")?.split(',')?.map { it.trim() }?.toSet()
        val phases = listOf(TimeOfDayPhase.AFTERNOON to "day", TimeOfDayPhase.SUNSET to "sunset", TimeOfDayPhase.NIGHT to "night")
        for (scene in SceneType.values()) {
            if (only != null && scene.name !in only) continue
            for ((phase, label) in phases) {
                reallocateNightStars(42L)
                val engine = engineFor(scene, phase, cw, ch)
                val classic = render(cw, ch) { drawWorldFrame(engine) }
                save(classic, File(out, "${scene.name.lowercase()}_${label}_classic.png"))
                classic.recycle()
                // Button strips as on a typical phone: status bar + top row, heart button + nav bar.
                val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
                val staged = engineFor(scene, phase, camera.worldW, camera.worldH)
                // Exactly what the app draws (stage, continued background, weather beyond the stage).
                val pixel = render(cw, ch) { drawWorld(staged, LowResWorldBuffer(), camera) }
                save(pixel, File(out, "${scene.name.lowercase()}_${label}_pixel.png"))
                pixel.recycle()
            }
        }
    }

    @Test
    fun writesDissolvePreviewWhenAsked() {
        val out = File(System.getenv("SCENE_PREVIEW_DIR") ?: return).apply { mkdirs() }
        val camera = WorldCamera.forScreen(cw, ch, SceneType.SEASIDE_PIER, pixelRenderer = true)
        val engine = engineFor(SceneType.SEASIDE_PIER, TimeOfDayPhase.AFTERNOON, camera.worldW, camera.worldH)
        for (wipe in listOf(0.45f, 0.75f)) {
            engine.wipeAlpha = wipe
            val frame = render { drawWorld(engine, LowResWorldBuffer(), camera) }
            save(frame, File(out, "dissolve_${(wipe * 100).toInt()}.png"))
            frame.recycle()
        }
    }

    private fun save(bmp: Bitmap, file: File) = file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
