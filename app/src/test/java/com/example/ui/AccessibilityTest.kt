package com.example.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio
import com.example.engine.WorldCamera
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Plan 06, I3: every tappable thing on the main screen has a name a screen reader can say, and a
 * touch target at least 48 dp square (Android's accessibility minimum). Checked in the scenes
 * with the most buttons and the fewest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-port-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    @After
    fun resumeTicker() {
        frameTickerPaused = false
    }

    private fun problemsOnMainScreen(scene: SceneType): List<String> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PreferencesManager(context).apply {
            isOnboardingCompleted = true
            boyfriendName = "Leo"
            girlfriendName = "Mia"
        }
        val metrics = context.resources.displayMetrics
        val camera = WorldCamera.forScreen(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat(), scene, pixelRenderer = true)
        val engine = SceneEngine(audio = AmbientAudio().apply { isEnabled = false }, onOpenLoveNotes = {}, onOpenMemories = {}).apply {
            updateNames("Leo", "Mia")
            loadScene(scene)
            updateAtmosphereMode("DAY")
            weatherDriftEnabled = false
            repeat(150) { update(1f / 60f, camera.worldW, camera.worldH) }
        }
        frameTickerPaused = true
        rule.mainClock.autoAdvance = false
        rule.setContent { MainScreen(targetAtmosphere = "DAY", previewEngine = engine) }
        rule.mainClock.advanceTimeBy(200)

        val minPx = 48f * metrics.density - 1f
        val problems = mutableListOf<String>()
        val nodes = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        // The title pill and the row of buttons, at least: an empty list would pass vacuously.
        if (nodes.size < 5) problems += "only ${nodes.size} tap targets found"
        for (node in nodes) {
            val config = node.config
            val label = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty() +
                config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
            val bounds = node.touchBoundsInRoot
            if (label.isBlank()) problems += "unlabelled tap target at $bounds"
            if (bounds.width < minPx || bounds.height < minPx) {
                problems += "tap target '$label' is ${bounds.width / metrics.density} x ${bounds.height / metrics.density} dp"
            }
        }
        return problems
    }

    @Test
    fun mainScreenInTheMeadow() {
        val problems = problemsOnMainScreen(SceneType.FLOWER)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun mainScreenInTheLoftWithItsExtraButton() {
        val problems = problemsOnMainScreen(SceneType.COZY_LOFT)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
