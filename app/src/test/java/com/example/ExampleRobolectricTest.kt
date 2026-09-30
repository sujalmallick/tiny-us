package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.engine.AmbientAudio
import com.example.scene.SceneEngine
import com.example.scene.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Tiny Us", appName)
    }

    @Test
    fun `preferences stores boyfriend and girlfriend names`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        prefs.boyfriendName = "Alex"
        prefs.girlfriendName = "Sam"
        assertEquals("Alex", prefs.boyfriendName)
        assertEquals("Sam", prefs.girlfriendName)
    }

    @Test
    fun `scene engine initializes and loads scene`() {
        val audio = AmbientAudio().apply { isEnabled = false }
        val engine = SceneEngine(
            audio = audio,
            onOpenLoveNotes = {},
            onOpenMemories = {}
        )
        assertNotNull(engine.currentScene)
        engine.loadScene(SceneType.COOKING)
        assertEquals(SceneType.COOKING, engine.currentScene)
    }
}
