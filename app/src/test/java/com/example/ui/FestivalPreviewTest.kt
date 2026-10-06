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
import com.example.data.BirthdayStore
import com.example.data.Festival
import com.example.data.FestivalStore
import com.example.data.InMemoryKeyValueStorage
import com.example.data.LetterKind
import com.example.data.Partner
import com.example.engine.GameText
import com.example.progress.ProgressState
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders each festival's first screen (plan 09, D) when FESTIVAL_PREVIEW_DIR is set. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FestivalPreviewTest {
    private fun render(festival: Festival, letters: BirthdayStore = BirthdayStore(InMemoryKeyValueStorage())) {
        runBlocking { GameText.load() }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            TinySurface(modifier = Modifier.width(380.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FestivalPanel(
                        festival, 2027, FestivalStore(InMemoryKeyValueStorage()), letters,
                        ProgressState(keepsakes = mapOf("discovery:SEASHELL" to 1, "dish:pancakes" to 2)),
                        "Bean", "Sprout", FestivalCallbacks(), {}
                    )
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
        view.draw(android.graphics.Canvas(image))
        assertTrue(image.width > 100 && image.height > 100)
        val dir = System.getenv("FESTIVAL_PREVIEW_DIR") ?: return
        File(dir).mkdirs()
        File(dir, "${festival.name.lowercase()}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun picnic() = render(Festival.BLOSSOM_PICNIC)

    @Test fun lanternNightWithLastYearsWishes() = render(Festival.LANTERN_NIGHT, BirthdayStore(InMemoryKeyValueStorage()).apply {
        seal(Partner.BOY, LetterKind.LANTERN, "That we finally see the sea together.", "Bean", opensOn = LocalDate(2026, 7, 14))
        seal(Partner.GIRL, LetterKind.LANTERN, "A little garden of our own.", "Sprout", opensOn = LocalDate(2026, 7, 14))
    })

    @Test fun giftExchange() = render(Festival.GIFT_EXCHANGE)
}
