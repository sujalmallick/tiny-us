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
import org.junit.After
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

    @After
    fun tearDown() {
        TimeOfDayPhase.hourOverride = null
        celestialProgressOverride = null
        localDaySecondsOverride = null
        moonPhaseOverride = null
    }

    private fun engineFor(scene: SceneType, phase: TimeOfDayPhase, cw: Float = this.cw, ch: Float = this.ch) =
        SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            // Seed the world's randomness (birds, weather, ambient particles) so this scene renders
            // identically every run; without it a pixel-by-pixel before/after diff is swamped by
            // particles that happen to land in different spots each time.
            com.example.engine.WorldRandom.seed(0x5EED1L)
            // Autonomy has its own seedable random; point it at the same seeded stream so the
            // couple's routines, blinks and discovery beats land the same way every run.
            behaviorBrain.random = com.example.engine.WorldRandom.rng
            // The pier's gull/crab/barista beats have their own random; point it at the same stream.
            pierRng = com.example.engine.WorldRandom.rng
            loadScene(scene)
            updateAtmosphereMode(if (phase == TimeOfDayPhase.NIGHT || phase == TimeOfDayPhase.SUNSET) phase.name else "DAY")
            // Pin everything that otherwise follows the real clock, so repeat renders of the same
            // scene and phase land the sun/moon, wall clocks and sunbeams in the same place. The
            // before/after comparison then differs only where the art changed.
            val (sunProg, daySec) = when (phase) {
                TimeOfDayPhase.MORNING -> 0.12f to 7.5f * 3600f
                TimeOfDayPhase.AFTERNOON -> 0.45f to 14f * 3600f
                TimeOfDayPhase.SUNSET -> 0.9f to 18.5f * 3600f
                TimeOfDayPhase.NIGHT -> 0.62f to 22f * 3600f
            }
            celestialProgressOverride = sunProg
            localDaySecondsOverride = daySec
            TimeOfDayPhase.hourOverride = 21
            // Keep the weather from drifting to another type mid-preview (unless a specific weather
            // is being previewed, which turns drift off itself below).
            weatherDriftEnabled = false
            // SCENE_PREVIEW_WEATHER=RAIN (or SNOW, SAKURA, AUTUMN) previews weather.
            // SCENE_PREVIEW_MOON=0.25 pins the moon's phase (0 new, 0.5 full). Without it, pin a
            // full moon so repeat renders agree: otherwise the phase follows the real date and its
            // smooth glow shifts a sub-pixel amount between runs.
            moonPhaseOverride = System.getenv("SCENE_PREVIEW_MOON")?.toFloat() ?: 0.5f
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
            // SCENE_PREVIEW_REQUEST=TEA,girl starts that request at once (plan 07 D1), to see its bubble.
            System.getenv("SCENE_PREVIEW_REQUEST")?.split(',')?.let { (kind, who) ->
                startRequestForTest(com.example.scene.autonomy.RequestKind.valueOf(kind.trim()), if (who.trim() == "girl") girl else boy)
            }
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
                // Crops ripe in all three plots and herbs in both sunroom pots (plan 09, E).
                "crops" -> cozy.garden = listOf(0 to "tomato", 1 to "pumpkin", 2 to "strawberry", 3 to "mint", 4 to "basil")
                    .fold(com.example.games.GardenPlots()) { g, (i, seed) -> g.plant(i, seed).water(i, 1).water(i, 2).water(i, 3) }
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
            // SCENE_PREVIEW_COUPLE=jar shows the Thank-You Jar (11 thanks); =bench the Make-Up
            // Bench's clouds; =rainbow the rainbow as they clear (plan 09, C).
            System.getenv("SCENE_PREVIEW_COUPLE")?.let { mode ->
                val store = com.example.data.CoupleLifeStore(com.example.data.InMemoryKeyValueStorage())
                repeat(11) { store.addThankYou(it % 2 == 0, "thanks $it") }
                coupleLifeStore = store
                if (mode == "bench" || mode == "rainbow") {
                    startMakeUpBench()
                    repeat(240) { update(1f / 60f, cw, ch) }
                    if (mode == "rainbow") {
                        finishMakeUpBench(com.example.data.MakeUpChoice.HUG)
                        repeat(50) { update(1f / 60f, cw, ch) }
                    }
                }
            }
            // SCENE_PREVIEW_FESTIVAL=BLOSSOM_PICNIC (or LANTERN_NIGHT, GIFT_EXCHANGE) dresses the scene
            // for that festival (plan 09, D): crowns on, lanterns rising, or gifts under the tree.
            System.getenv("SCENE_PREVIEW_FESTIVAL")?.let { name ->
                val f = com.example.data.Festival.valueOf(name)
                com.example.data.Festivals.override = f
                festivalStore = com.example.data.FestivalStore(com.example.data.InMemoryKeyValueStorage())
                refreshFestival()
                when (f) {
                    com.example.data.Festival.BLOSSOM_PICNIC -> celebratePicnic(1, 2)
                    com.example.data.Festival.LANTERN_NIGHT -> { releaseLanterns(); repeat(120) { update(1f / 60f, cw, ch) } }
                    com.example.data.Festival.GIFT_EXCHANGE -> {
                        festivalStore?.savePicks(f, com.example.data.CoupleDates.today().year, com.example.data.FestivalPicks(boy = "card", girl = "card"))
                        refreshFestival()
                    }
                }
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

    /**
     * Plan 10, B: every scene with the couple at each of its spots (him at one, her at the next), in
     * the spot's pose, so props in front of or behind them can be checked. SCENE_SPOTS_DIR=dir.
     */
    @Test
    fun writesSpotSheetsWhenAsked() {
        val out = File(System.getenv("SCENE_SPOTS_DIR") ?: return).apply { mkdirs() }
        val only = System.getenv("SCENE_PREVIEW_SCENES")?.split(',')?.map { it.trim() }?.toSet()
        for (scene in SceneType.values()) {
            if (only != null && scene.name !in only) continue
            val spots = com.example.scene.autonomy.SceneSpots.forScene(scene)
            if (spots.isEmpty()) continue
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            val frames = spots.indices.map { i ->
                reallocateNightStars(42L)
                val engine = engineFor(scene, TimeOfDayPhase.AFTERNOON, camera.worldW, camera.worldH)
                engine.autonomyEnabled = false
                engine.cuddleProgress = 0f // at the spots they're apart, as the routine has them
                fun place(c: com.example.engine.PixelCharacter, spot: com.example.scene.autonomy.SceneSpot) {
                    c.worldX = spot.x
                    c.worldY = spot.y
                    c.pose = spot.pose
                    c.direction = if (spot.faceLeft) com.example.engine.Direction.LEFT else com.example.engine.Direction.RIGHT
                }
                place(engine.boy, spots[i])
                if (spots.size > 1) place(engine.girl, spots[(i + 1) % spots.size])
                // SCENE_SPOTS_MEAL=1: the kitchen frames show them sat down to eat instead.
                if (scene == SceneType.COOKING && System.getenv("SCENE_SPOTS_MEAL") != null) {
                    engine.boy.worldX = com.example.scene.CozyGames.TABLE_BOY_X; engine.boy.worldY = com.example.scene.CozyGames.TABLE_SEAT_Y
                    engine.girl.worldX = com.example.scene.CozyGames.TABLE_GIRL_X; engine.girl.worldY = com.example.scene.CozyGames.TABLE_SEAT_Y
                    engine.boy.pose = com.example.engine.CharacterPose.SIT; engine.girl.pose = com.example.engine.CharacterPose.SIT
                    engine.boy.direction = com.example.engine.Direction.RIGHT; engine.girl.direction = com.example.engine.Direction.LEFT
                }
                render(cw, ch) { drawWorld(engine, LowResWorldBuffer(), camera) }
            }
            // The lower part of each frame, where they stand, side by side at half size.
            val top = (ch * 0.30f).toInt()
            val h = (ch * 0.58f).toInt()
            val sheet = Bitmap.createBitmap((cw.toInt() / 2) * frames.size, h / 2, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(sheet)
            frames.forEachIndexed { i, f ->
                val part = Bitmap.createBitmap(f, 0, top, cw.toInt(), h)
                canvas.drawBitmap(Bitmap.createScaledBitmap(part, cw.toInt() / 2, h / 2, false), (i * cw.toInt() / 2).toFloat(), 0f, null)
                f.recycle()
            }
            save(sheet, File(out, "spots_${scene.name.lowercase()}.png"))
            File(out, "spots_${scene.name.lowercase()}.txt").writeText(
                spots.mapIndexed { i, sp -> "$i: him at ${sp.action} (${sp.pose}), her at ${spots[(i + 1) % spots.size].action}" }.joinToString("\n")
            )
        }
    }

    /**
     * Plan 10, C: Mochi in every scene where she is, then in her box and at her saucer after
     * walking over. MOCHI_PREVIEW_DIR=dir.
     */
    @Test
    fun writesMochiSheetWhenAsked() {
        val out = File(System.getenv("MOCHI_PREVIEW_DIR") ?: return).apply { mkdirs() }
        val frames = mutableListOf<Pair<String, Bitmap>>()
        fun frame(scene: SceneType, label: String, setup: (SceneEngine, Float, Float) -> Unit) {
            val camera = WorldCamera.forScreen(cw, ch, scene, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
            reallocateNightStars(42L)
            val engine = engineFor(scene, TimeOfDayPhase.AFTERNOON, camera.worldW, camera.worldH)
            engine.autonomyEnabled = false
            setup(engine, camera.worldW, camera.worldH)
            frames += label to render(cw, ch) { drawWorld(engine, LowResWorldBuffer(), camera) }
        }
        fun SceneEngine.runFor(seconds: Float, w: Float, h: Float, until: () -> Boolean = { false }) {
            var t = 0f
            while (t < seconds && !until()) { update(1f / 30f, w, h); t += 1f / 30f }
        }
        for (scene in SceneType.values()) frame(scene, scene.name.lowercase()) { _, _, _ -> }
        frame(SceneType.SLEEP, "in_the_box") { e, w, h ->
            e.onTouchCardboardBox(0f, 0f)
            e.runFor(30f, w, h) { e.mochiInBox }
            e.runFor(0.6f, w, h)
        }
        frame(SceneType.MOMO_STALL, "at_the_saucer") { e, w, h ->
            e.onTouchMilkSaucer(0f, 0f)
            e.runFor(30f, w, h) { e.catState != com.example.scene.CatState.WALK_FOLLOW }
        }
        val top = (ch * 0.30f).toInt()
        val hh = (ch * 0.58f).toInt()
        for ((label, f) in frames) {
            save(Bitmap.createBitmap(f, 0, top, cw.toInt(), hh), File(out, "mochi_$label.png"))
            f.recycle()
        }
    }

    /** Plan 10, D: the Friday fox arriving, at play, and the ball it leaves. FOX_PREVIEW_DIR=dir. */
    @Test
    fun writesFridayFoxWhenAsked() {
        val out = File(System.getenv("FOX_PREVIEW_DIR") ?: return).apply { mkdirs() }
        val camera = WorldCamera.forScreen(cw, ch, SceneType.FLOWER, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
        val w = camera.worldW
        val h = camera.worldH
        fun shot(name: String, today: kotlinx.datetime.LocalDate, until: (SceneEngine) -> Boolean) {
            com.example.data.FridayFox.todayOverride = today
            reallocateNightStars(42L)
            val engine = engineFor(SceneType.FLOWER, TimeOfDayPhase.AFTERNOON, w, h)
            engine.autonomyEnabled = false
            engine.foxStore = com.example.data.FoxStore(com.example.data.InMemoryKeyValueStorage()).apply { noteSeen(kotlinx.datetime.LocalDate(2026, 9, 1)) }
            var t = 0f
            while (t < 60f && !until(engine)) { engine.update(1f / 30f, w, h); t += 1f / 30f }
            val frame = render(cw, ch) { drawWorld(engine, LowResWorldBuffer(), camera) }
            save(Bitmap.createBitmap(frame, 0, (ch * 0.30f).toInt(), cw.toInt(), (ch * 0.58f).toInt()), File(out, "fox_$name.png"))
            frame.recycle()
        }
        val friday = kotlinx.datetime.LocalDate(2026, 10, 9)
        try {
            shot("arriving", friday) { it.foxVisit.active && it.foxVisit.x < 0.92f }
            shot("catch", friday) { it.foxVisit.ballFlying && it.foxVisit.ballT in 0.45f..0.6f && it.foxVisit.phase == com.example.scene.FoxVisit.Phase.PLAYING }
            shot("with_mochi", friday) { it.foxVisit.ballWithMochi }
            shot("ball_left", kotlinx.datetime.LocalDate(2026, 10, 11)) { it.sceneTime > 2f }
        } finally {
            com.example.data.FridayFox.todayOverride = null
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

    /** SCENE_PREVIEW_BIRTHDAY=dir renders the birthday surprise's beats (plan 09, A) as PNGs. */
    @Test
    fun writesBirthdayFilmstripWhenAsked() {
        val out = File(System.getenv("SCENE_PREVIEW_BIRTHDAY") ?: return).apply { mkdirs() }
        val camera = WorldCamera.forScreen(cw, ch, SceneType.SLEEP, pixelRenderer = true, topReservePx = 0.09f * ch, bottomReservePx = 0.10f * ch)
        val w = camera.worldW
        val h = camera.worldH
        val today = com.example.data.CoupleDates.today()
        val store = com.example.data.BirthdayStore(com.example.data.InMemoryKeyValueStorage())
        store.setBirthday(com.example.data.Partner.GIRL, kotlinx.datetime.LocalDate(1996, today.month, today.day).toString())
        val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            updateAtmosphereMode(System.getenv("SCENE_PREVIEW_PHASE") ?: "SUNSET")
            loadScene(SceneType.FLOWER)
            birthdayStore = store
            birthdayOverlayShowing = true
        }
        fun frames(seconds: Float) = repeat((seconds * 60).toInt()) { engine.update(1f / 60f, w, h) }
        fun shot(name: String) {
            val frame = render(cw, ch) {
                drawWorld(engine, LowResWorldBuffer(), camera)
                val dark = engine.birthdaySurprise.darkness
                if (dark > 0f) drawRect(Color(0xFF05070F).copy(alpha = 0.88f * dark))
            }
            save(frame, File(out, "birthday_$name.png"))
            frame.recycle()
        }
        frames(1.5f)
        shot("1_dark")
        engine.onBirthdayTap(w, h)
        frames(1.4f)
        shot("2_surprise")
        frames(3f)
        engine.onBirthdayTap(w, h)
        frames(0.3f)
        shot("3_candles")
        engine.onBirthdayTap(w, h)
        engine.onBirthdayTap(w, h)
        frames(1.2f)
        engine.submitBirthdayWish("")
        frames(0.5f)
        shot("4_gift")
        engine.onBirthdayTap(w, h)
        frames(3f)
        shot("5_party")
    }

    private fun save(bmp: Bitmap, file: File) = file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
