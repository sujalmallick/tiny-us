package com.example

import com.example.data.IosUserDefaultsStorage
import com.example.data.PreferencesManager
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The app's saved data on iOS storage (NSUserDefaults), read back by a fresh PreferencesManager. */
class IosSavedDataTest {
    @Test
    fun savedDataSurvivesAReopen() {
        val suite = "tinyus.test.saved"
        NSUserDefaults.standardUserDefaults.removePersistentDomainForName(suite)
        val storage = IosUserDefaultsStorage(NSUserDefaults(suiteName = suite))
        val prefs = PreferencesManager(storage)
        prefs.boyfriendName = "Bean"
        prefs.buttonGlassIntensity = 0.4f
        prefs.addLoveNote("See you at the pier", "Sprout")
        prefs.markAppOpenedToday()

        val reopened = PreferencesManager(IosUserDefaultsStorage(NSUserDefaults(suiteName = suite)))
        assertEquals("Bean", reopened.boyfriendName)
        assertEquals(0.4f, reopened.buttonGlassIntensity)
        val note = reopened.getLoveNotes().first()
        assertEquals("See you at the pier", note.text)
        assertTrue(note.date.isNotBlank())
        assertEquals(36, note.id.length)
        assertTrue(reopened.lastOpenedDate.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
        NSUserDefaults.standardUserDefaults.removePersistentDomainForName(suite)
    }
}
