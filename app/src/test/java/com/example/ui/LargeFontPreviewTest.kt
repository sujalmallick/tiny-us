package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import com.example.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Plan 06, I2: the main screen and the pieces every dialog is built from, at the phone's larger
 * font sizes (1.3x and the maximum, 2x). Draws them so clipped or overlapping text can be spotted;
 * with A11Y_PREVIEW_DIR set it saves the pictures.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-port-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LargeFontPreviewTest {

    private val outDir: File? get() = System.getenv("A11Y_PREVIEW_DIR")?.let { File(it).apply { mkdirs() } }

    private fun render(name: String, fontScale: Float, content: @Composable () -> Unit): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) { content() }
        }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(30))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
        view.draw(android.graphics.Canvas(image))
        outDir?.let { d -> File(d, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        activity.finish()
        return image
    }

    @Test
    fun mainScreenAtLargeFonts() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        PreferencesManager(activity).apply {
            isOnboardingCompleted = true
            boyfriendName = "Leo"
            girlfriendName = "Mia"
        }
        val metrics = activity.resources.displayMetrics
        for (scene in listOf(SceneType.FLOWER, SceneType.COZY_LOFT)) {
            val camera = WorldCamera.forScreen(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat(), scene, pixelRenderer = true)
            val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
                updateNames("Leo", "Mia")
                loadScene(scene)
                updateAtmosphereMode("DAY")
                weatherDriftEnabled = false
                repeat(150) { update(1f / 60f, camera.worldW, camera.worldH) }
            }
            frameTickerPaused = true
            try {
                for (scale in listOf(1.3f, 2f)) {
                    val image = render("main_${scene.name.lowercase()}_x$scale", scale) {
                        MainScreen(targetAtmosphere = "DAY", previewEngine = engine)
                    }
                    assertTrue(image.width > 100)
                }
            } finally {
                frameTickerPaused = false
            }
        }
    }

    /** Plan 07, B3: the "Little firsts" page, with a few earned (and at 1.3x fonts). */
    @Test
    fun littleFirstsPage() {
        var progress = com.example.progress.ProgressState()
        var day = 20360L
        for (e in listOf(
            com.example.progress.ProgressEvent.DaysTogether(40),
            com.example.progress.ProgressEvent.RainbowWish,
            com.example.progress.ProgressEvent.ConstellationFound("constellation_1"),
            com.example.progress.ProgressEvent.LoveNote
        )) {
            progress = com.example.progress.LittleFirsts.apply(progress, e, day++).first
        }
        for (scale in listOf(1f, 1.3f)) {
            val image = render("little_firsts_x$scale", scale) {
                androidx.compose.foundation.layout.Box(Modifier.background(com.example.ui.theme.TinyColors.Paper)) {
                    LittleFirstsPage(progress)
                }
            }
            assertTrue(image.height > 100)
        }
    }

    @Test
    fun dialogPiecesAtLargeFonts() {
        for (scale in listOf(1.3f, 2f)) {
            val image = render("dialog_pieces_x$scale", scale) {
                TinySurface(modifier = Modifier.width(340.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TinyDialogHeader(
                            title = org.jetbrains.compose.resources.stringResource(Res.string.ui_tiny_care),
                            subtitle = org.jetbrains.compose.resources.stringResource(Res.string.ui_tiny_care_description),
                            icon = PixelIcons.Favorite,
                            onClose = {}
                        )
                        TinySectionHeader(title = "Make Us", subtitle = "Pick your looks", icon = PixelIcons.Palette)
                        TinyCard {
                            Text(org.jetbrains.compose.resources.stringResource(Res.string.ui_gift_guide_1), style = TinyType.Body)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TinyChip(text = org.jetbrains.compose.resources.stringResource(Res.string.ui_shared), selected = true, onClick = {})
                            TinyChip(text = org.jetbrains.compose.resources.stringResource(Res.string.ui_private), selected = false, onClick = {})
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TinyButton(text = org.jetbrains.compose.resources.stringResource(Res.string.ui_save_to_device), onClick = {}, icon = PixelIcons.Check)
                            TinyButton(text = org.jetbrains.compose.resources.stringResource(Res.string.ui_back), onClick = {}, style = TinyButtonStyle.Outline)
                        }
                    }
                }
            }
            assertTrue(image.height > 100)
        }
    }
}
