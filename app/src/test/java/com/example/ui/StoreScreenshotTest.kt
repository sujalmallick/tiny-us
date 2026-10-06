package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import com.example.data.PolaroidManager
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.scene.WeatherType
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

/**
 * Google Play screenshots (plan 06, E1): the whole app screen, scene and buttons, drawn on the JVM
 * because there's no phone or emulator to capture from. Runs only when STORE_SHOTS_DIR is set
 * (use --rerun); STORE_SHOTS_ONLY=01_meadow_day,02_loft_night limits the shots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreScreenshotTest {

    private class Shot(val name: String, val scene: String, val atmosphere: String, val weather: String)

    private val shots = listOf(
        Shot("01_meadow_day", "FLOWER", "DAY", "SUNNY"),
        Shot("02_loft_night", "COZY_LOFT", "NIGHT", "SUNNY"),
        Shot("03_campfire_night", "CAMPFIRE", "NIGHT", "SUNNY"),
        Shot("04_rainy_cafe", "RAINY_CAFE", "DAY", "RAIN"),
        Shot("05_pier_sunset", "SEASIDE_PIER", "SUNSET", "SUNNY"),
        Shot("06_tree_sakura", "UNDER_TREE", "DAY", "SAKURA"),
        Shot("07_kitchen", "COOKING", "DAY", "AUTUMN"),
        Shot("08_stroll_snow", "WALK", "NIGHT", "SNOW")
    )

    private val outDir: File? get() = System.getenv("STORE_SHOTS_DIR")?.let { File(it) }

    /** Phone: 1080 x 2400. */
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-port-xxhdpi")
    fun phone() = renderAll("phone", polaroid = true)

    /** 7-inch tablet: 1200 x 1920. */
    @Test
    @Config(sdk = [34], qualifiers = "w600dp-h960dp-port-xhdpi")
    fun tablet7() = renderAll("tablet7")

    /** 10-inch tablet: 1600 x 2560. */
    @Test
    @Config(sdk = [34], qualifiers = "w800dp-h1280dp-port-xhdpi")
    fun tablet10() = renderAll("tablet10")

    /**
     * Plan 06, F2: the same screens in the en-XA pseudo-locale, where every string is about a
     * third longer, to spot text that would clip in a longer language. On the smallest common
     * phone width as well as the usual one.
     */
    @Test
    @Config(sdk = [34], qualifiers = "en-rXA-w360dp-h800dp-port-xxhdpi")
    fun phonePseudoLocale() = renderAll("pseudo_en_xa")

    @Test
    @Config(sdk = [34], qualifiers = "en-rXA-w320dp-h640dp-port-xhdpi")
    fun smallPhonePseudoLocale() = renderAll("pseudo_en_xa_small")

    private fun renderAll(device: String, polaroid: Boolean = false) {
        val root = outDir
        assumeTrue("Set STORE_SHOTS_DIR to render store screenshots", root != null)
        val dir = File(root, device).apply { mkdirs() }
        val only = System.getenv("STORE_SHOTS_ONLY")?.split(',')?.map { it.trim() }?.toSet()
        for (shot in shots) {
            if (only != null && shot.name !in only) continue
            val image = renderScreen(shot)
            save(image, File(dir, "${shot.name}.png"))
            if (polaroid && shot.name == "01_meadow_day") {
                // A Tiny Moment, as the heart button captures it: the scene on a polaroid card.
                val card = PolaroidManager(activityContext()).renderPolaroidCard(
                    sceneBitmap = image,
                    title = "Our little meadow",
                    date = "Oct 5, 2026",
                    time = "4:30 PM",
                    sceneName = "A Flower For You"
                )
                save(card, File(dir, "09_tiny_moment_polaroid.png"))
            }
        }
    }

    private fun activityContext() = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    private fun renderScreen(shot: Shot): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        PreferencesManager(activity).apply {
            isOnboardingCompleted = true
            boyfriendName = "Leo"
            girlfriendName = "Mia"
            soundEnabled = true
            atmosphereMode = shot.atmosphere
        }
        // Run the scene forward on the engine itself: the fade finishes, the weather fills the
        // sky and the couple gets going. (Letting the whole screen recompose for every one of
        // those frames takes minutes per shot in a JVM test.) The world size matches what the
        // screen's camera gives, with the button strips MainScreen reserves.
        val metrics = activity.resources.displayMetrics
        val scene = SceneType.valueOf(shot.scene)
        val camera = WorldCamera.forScreen(
            metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat(), scene, pixelRenderer = true,
            topReservePx = 58f * metrics.density, bottomReservePx = 70f * metrics.density
        )
        val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            updateNames("Leo", "Mia")
            loadScene(scene)
            updateAtmosphereMode(shot.atmosphere)
            weatherDriftEnabled = false
            weather = WeatherType.valueOf(shot.weather)
            repeat(SIM_FRAMES) { update(1f / 60f, camera.worldW, camera.worldH) }
            // STORE_SHOTS_COZY=1 opens the recipe card in the kitchen (or casts a line at the pier);
            // STORE_SHOTS_MESSAGE="..." shows that caption, to check where it sits.
            // STORE_SHOTS_COZY=picker opens the recipe picker with a little in the pantry, =seeds the
            // seed picker on the first meadow plot, =who the "who is watering?" card (plan 09, E).
            when (System.getenv("STORE_SHOTS_COZY")) {
                null -> Unit
                "picker" -> {
                    cozy.pantry = mapOf(com.example.games.Ingredient.TOMATO to 2, com.example.games.Ingredient.BASIL to 1)
                    cozy.openRecipePicker()
                }
                "seeds", "who" -> {
                    cozy.season = "SPRING"
                    val shared = System.getenv("STORE_SHOTS_COZY") == "who"
                    if (shared) cozy.garden = com.example.games.GardenPlots().plant(1, "peas")
                    val i = if (shared) 1 else 0
                    val p = com.example.engine.WorldViewport.pixelScale(camera.worldW)
                    val base = cozy.plotBase(i, camera.worldW, camera.worldH)
                    cozy.onGardenTap(base.x, base.y - 3f * p, camera.worldW, camera.worldH, p)
                }
                else -> {
                    cozy.startCooking(kotlin.random.Random(3))
                    cozy.startFishing()
                }
            }
            System.getenv("STORE_SHOTS_MESSAGE")?.let { msg ->
                showMessage(msg, duration = 10f)
                repeat(40) { update(1f / 60f, camera.worldW, camera.worldH) }
            }
        }
        frameTickerPaused = true
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            MainScreen(targetAtmosphere = shot.atmosphere, targetWeather = shot.weather, previewEngine = engine)
        }
        // Just long enough to lay the screen out and draw it.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(30))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(image))
        frameTickerPaused = false
        activity.finish()
        return image
    }

    private companion object {
        /** Six seconds of scene at 60 frames a second. */
        const val SIM_FRAMES = 360
    }

    private fun save(bmp: Bitmap, file: File) = file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
