package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.example.data.BirthdayStore
import com.example.data.CoupleLifeStore
import com.example.data.InMemoryKeyValueStorage
import com.example.data.LetterKind
import com.example.data.Partner
import com.example.engine.GameText
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
import kotlin.random.Random

/** Renders the couple-life screens (plan 09, C) to PNGs when COUPLE_LIFE_PREVIEW_DIR is set. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoupleLifePreviewTest {
    private fun render(name: String, content: @Composable ColumnScope.() -> Unit) {
        runBlocking { GameText.load() }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            TinySurface(modifier = Modifier.width(380.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
        view.draw(android.graphics.Canvas(image))
        assertTrue(image.width > 100 && image.height > 100)
        val dir = System.getenv("COUPLE_LIFE_PREVIEW_DIR") ?: return
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun decider() = render("1_decider") {
        DinnerDeciderPanel(CoupleLifeStore(InMemoryKeyValueStorage()), "Bean", "Sprout", true, { _, _ -> }, {}, {}, Random(2), boyFirst = true)
    }

    @Test fun jar() = render("2_jar") {
        val store = CoupleLifeStore(InMemoryKeyValueStorage()).apply { repeat(11) { addThankYou(it % 2 == 0, "thanks $it") } }
        ThankYouJarPanel(store, "Bean", "Sprout", { _, _, _ -> }, {})
    }

    @Test fun openWhen() = render("3_open_when") {
        val store = BirthdayStore(InMemoryKeyValueStorage()).apply {
            seal(Partner.GIRL, LetterKind.OPEN_WHEN, "I'm right here.", "Bean", occasion = "when you miss me")
            seal(Partner.GIRL, LetterKind.OPEN_WHEN, "Count my snores.", "Bean", occasion = "when you can't sleep")
            seal(Partner.BOY, LetterKind.OPEN_WHEN, "You did so well.", "Sprout", occasion = "when you're proud of yourself")
        }
        OpenWhenPanel(store, "Bean", "Sprout", {})
    }

    @Test fun bench() = render("4_bench") {
        MakeUpBenchPanel(CoupleLifeStore(InMemoryKeyValueStorage()), "Bean", "Sprout", {}, {}, {})
    }
}
