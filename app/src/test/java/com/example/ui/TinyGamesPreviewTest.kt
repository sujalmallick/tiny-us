package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.example.data.PreferencesManager
import com.example.engine.GameText
import com.example.games.TinyGame
import com.example.games.TinyQuestion
import com.example.games.TinyRound
import com.example.progress.ProgressState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders each Tiny Games screen (plan 09, B) to PNGs when TINY_GAMES_PREVIEW_DIR is set. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TinyGamesPreviewTest {
    private val question = TinyQuestion("p", "A cabin in the mountains or a cottage by the sea?", listOf("Mountain cabin", "Seaside cottage"))
    private fun round(game: TinyGame = TinyGame.THIS_OR_THAT) = TinyRound(game, List(5) { question.copy(id = "p$it") })

    private fun render(name: String, previewRound: TinyRound?, covered: Boolean) {
        runBlocking { GameText.load() }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val prefs = PreferencesManager(activity).apply { boyfriendName = "Bean"; girlfriendName = "Sprout" }
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            TinySurface(modifier = Modifier.width(380.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TinyGamesPanel(
                        prefs = prefs, progress = ProgressState(best = mapOf(TinyGame.THIS_OR_THAT.progressId to 4)), photos = emptyList(),
                        onReveal = { _, _, _, _, _ -> }, onRoundFinished = { _, _, _ -> }, onDismiss = {},
                        previewRound = previewRound, previewCovered = covered
                    )
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
        view.draw(android.graphics.Canvas(image))
        assertTrue(image.width > 100 && image.height > 100)
        val dir = System.getenv("TINY_GAMES_PREVIEW_DIR") ?: return
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun picker() = render("1_pick_game", null, false)
    @Test fun passThePhone() = render("2_pass_phone", round(), true)
    @Test fun question() = render("3_question", round(), false)
    @Test fun reveal() = render("4_reveal", round().apply { pick(0); pick(0) }, false)
    @Test fun guessMe() = render("5_guess_me", round(TinyGame.GUESS_ME).apply { pick(1) }, false)
    @Test fun result() = render("6_result", round().apply {
        pick(0); pick(0)
        repeat(4) { next(); pick(0); pick(if (it < 2) 0 else 1) }
    }, true)
}
