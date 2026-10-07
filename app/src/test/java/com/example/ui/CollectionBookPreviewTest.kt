package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.example.data.CollectionPage
import com.example.data.CollectionStore
import com.example.data.FirstFind
import com.example.data.InMemoryKeyValueStorage
import com.example.engine.GameText
import com.example.progress.ProgressEvent
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

/** Renders the collection book's pages (plan 09, H) when COLLECTION_PREVIEW_DIR is set. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CollectionBookPreviewTest {
    private val progress = ProgressState()
        .record(ProgressEvent.DiscoveryFound("SEASHELL", "GIRL"))
        .record(ProgressEvent.DiscoveryFound("WILDFLOWER", "BOY"))
        .record(ProgressEvent.DiscoveryFound("LOVE_NOTE", "BOY"))
        .record(ProgressEvent.FishCaught("MINNOW"))
        .record(ProgressEvent.FishCaught("PEARL"))
        .record(ProgressEvent.FishCaught("RAIN_TROUT"))
        .record(ProgressEvent.DishCooked("pancakes"))
        .record(ProgressEvent.FestivalCelebrated("LANTERN_NIGHT"))

    private fun store() = CollectionStore(InMemoryKeyValueStorage()).apply {
        record(FirstFind("discovery:SEASHELL", "GIRL", false, "WALK", "SUNNY", "AFTERNOON", 20_729))
        record(FirstFind("discovery:WILDFLOWER", "BOY", false, "FLOWER", "SAKURA", "MORNING", 20_700))
        record(FirstFind("catch:PEARL", "BOY", true, "SEASIDE_PIER", "SUNNY", "NIGHT", 20_733))
        record(FirstFind("catch:RAIN_TROUT", "BOTH", false, "SEASIDE_PIER", "RAIN", "AFTERNOON", 20_731))
        record(FirstFind("festival:LANTERN_NIGHT", "BOTH", false, "SEASIDE_PIER", "SUNNY", "NIGHT", 20_649))
        // The minnow, love note and pancakes were found before the book began.
    }

    private fun render(page: CollectionPage?) {
        runBlocking { GameText.load() }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            TinySurface(modifier = Modifier.width(380.dp)) {
                Box(Modifier.height(820.dp)) {
                    CollectionBookPage(progress, store(), "Bean", "Sprout", together = "2024-02-14", daysTogether = 966, initialPage = page, animate = false)
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
        view.draw(android.graphics.Canvas(image))
        assertTrue(image.width > 100 && image.height > 100)
        val dir = System.getenv("COLLECTION_PREVIEW_DIR") ?: return
        File(dir).mkdirs()
        File(dir, "collection_${page?.name?.lowercase() ?: "found_you"}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun foundYou() = render(null)
    @Test fun finds() = render(CollectionPage.FINDS)
    @Test fun sea() = render(CollectionPage.SEA)
    @Test fun kitchen() = render(CollectionPage.KITCHEN)
    @Test fun garden() = render(CollectionPage.GARDEN)
    @Test fun moments() = render(CollectionPage.MOMENTS)
}
