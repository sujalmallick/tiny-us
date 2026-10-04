package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import android.os.Looper
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PixelIcons
import com.example.ui.theme.TinyColors
import com.example.ui.theme.TinyType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the chrome components (pixel font, pixel frames, pixel icons) to check they draw.
 * Set SCENE_PREVIEW_DIR to also save the picture for review.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChromePreviewTest {
    @Test
    fun chromeRendersWithPixelFrameFontAndIcons() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = ComposeView(activity)
        activity.setContentView(view)
        view.setContent {
            TinySurface(modifier = Modifier.width(380.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TinyDialogHeader(title = "Our Story", subtitle = "Every little moment, in order", icon = PixelIcons.AutoStories, onClose = {})
                    TinySectionHeader(title = "Make Us", subtitle = "Pick your looks", icon = PixelIcons.Palette)
                    TinyCard {
                        Text("Privacy Lock", style = TinyType.Label)
                        Text("Keep Tiny Us behind your PIN or fingerprint. Body text stays in the regular font so longer text is easy to read.", style = TinyType.Body)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TinyChip(text = "Cozy", selected = true, onClick = {}, icon = PixelIcons.Favorite)
                        TinyChip(text = "Rainy", selected = false, onClick = {}, icon = PixelIcons.Cloud)
                        TinyTag(text = "New")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TinyButton(text = "Save", onClick = {}, icon = PixelIcons.Check)
                        TinyButton(text = "Backup", onClick = {}, style = TinyButtonStyle.Outline, icon = PixelIcons.Backup)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (icon in listOf(
                            PixelIcons.Settings, PixelIcons.Lock, PixelIcons.Mail, PixelIcons.PhotoCamera, PixelIcons.Headphones,
                            PixelIcons.WbSunny, PixelIcons.Bedtime, PixelIcons.Park, PixelIcons.LocalCafe, PixelIcons.Celebration
                        )) TinyIconBadge(icon = icon, size = 32.dp, iconSize = 24.dp)
                    }
                    Icon(PixelIcons.Favorite, contentDescription = null, tint = TinyColors.Rose)
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
        val image = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(0x2E, 0x21, 0x1D))
        view.draw(android.graphics.Canvas(image))
        assertTrue(image.width > 100 && image.height > 100)
        val dir = System.getenv("SCENE_PREVIEW_DIR") ?: return
        File(dir).mkdirs()
        File(dir, "chrome.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
