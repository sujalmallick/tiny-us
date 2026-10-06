package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.engine.GameText
import com.example.games.TinyDecks
import com.example.games.TinyGame
import com.example.progress.ProgressState
import com.example.resources.Res
import com.example.resources.tiny_guess_me
import com.example.resources.tiny_this_or_that
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/** Plan 09, B: one Tiny Games round on one phone, with the picks hidden between turns. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TinyGamesDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var prefs: PreferencesManager
    private val reveals = mutableListOf<Boolean>()
    private var finished: Pair<TinyGame, Int>? = null

    @Before
    fun setup() {
        runBlocking { GameText.load() }
        prefs = PreferencesManager(ApplicationProvider.getApplicationContext<android.content.Context>())
        prefs.boyfriendName = "Bean"
        prefs.girlfriendName = "Sprout"
    }

    private fun shot(@Suppress("UNUSED_PARAMETER") name: String) = Unit // screens are rendered by TinyGamesPreviewTest

    @Test
    fun `both decks have sixty questions`() {
        assertEquals(60, TinyDecks.parse("tot_", GameText.array(Res.array.tiny_this_or_that)).size)
        assertEquals(60, TinyDecks.parse("gm_", GameText.array(Res.array.tiny_guess_me)).size)
    }

    @Test
    fun `a this-or-that round, passing the phone between picks`() {
        compose.setContent {
            // The panel without its Dialog window, so the screens can be captured on the JVM.
            TinySurface(modifier = androidx.compose.ui.Modifier.width(360.dp)) {
                androidx.compose.foundation.layout.Column(
                    androidx.compose.ui.Modifier.padding(20.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    TinyGamesPanel(
                        prefs = prefs,
                        progress = ProgressState(),
                        photos = emptyList(),
                        onReveal = { _, _, _, _, match -> reveals += match },
                        onRoundFinished = { game, score, _ -> finished = game to score },
                        onDismiss = {},
                        random = Random(3)
                    )
                }
            }
        }
        shot("1_pick_game")
        compose.onNodeWithTag("tg_pick_this_or_that").performClick()
        repeat(5) { q ->
            // Each pick waits behind "pass the phone", so the other's choice stays hidden.
            compose.onNodeWithTag("tg_pass_phone").assertIsDisplayed()
            assertTrue(compose.onAllNodesWithTagCount("tg_option_0") == 0)
            if (q == 0) shot("2_pass_phone")
            compose.onNodeWithTag("tg_ready").performClick()
            if (q == 0) shot("3_question")
            compose.onNodeWithTag("tg_option_0").performClick()
            compose.onNodeWithTag("tg_pass_phone").assertIsDisplayed()
            compose.onNodeWithTag("tg_ready").performClick()
            compose.onNodeWithTag("tg_option_${if (q < 3) 0 else 1}").performClick()
            compose.onNodeWithTag("tg_reveal").assertExists()
            if (q == 0) shot("4_reveal")
            compose.onNodeWithTag("tg_next").performClick()
        }
        compose.onNodeWithTag("tg_result").assertExists()
        shot("5_result")
        assertEquals(listOf(true, true, true, false, false), reveals)
        assertEquals(TinyGame.THIS_OR_THAT to 3, finished)
    }

    @Test
    fun `the story quiz waits until there is enough story`() {
        compose.setContent {
            androidx.compose.foundation.layout.Column {
                TinyGamesPanel(prefs = prefs, progress = ProgressState(), photos = emptyList(), onReveal = { _, _, _, _, _ -> }, onRoundFinished = { _, _, _ -> }, onDismiss = {})
            }
        }
        compose.onNodeWithTag("tg_pick_story_quiz").performClick()
        // Still on the picker: nothing started.
        compose.onNodeWithTag("tg_pick_this_or_that").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagCount(tag: String): Int =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().size
}
