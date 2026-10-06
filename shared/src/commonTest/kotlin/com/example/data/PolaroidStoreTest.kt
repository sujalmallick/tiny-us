package com.example.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolaroidStoreTest {
    private fun photo(id: String) = PolaroidMemory(id, "Title $id", "Oct 6, 2026", "6:42 PM", "Meadow", "MEADOW", "/photos/$id.png")

    @Test
    fun newestFirstAndAtMostSixty() {
        val store = PolaroidStore(InMemoryKeyValueStorage())
        repeat(65) { store.savePolaroid(photo("p$it")) }
        val list = store.getPolaroids()
        assertEquals(60, list.size)
        assertEquals("p64", list.first().id)
        assertEquals("p5", list.last().id)
    }

    @Test
    fun deletingRemovesTheEntryAndItsImage() {
        val deleted = mutableListOf<String>()
        val store = PolaroidStore(InMemoryKeyValueStorage()) { deleted += it }
        store.savePolaroid(photo("a"))
        store.savePolaroid(photo("b"))
        store.deletePolaroid("a")
        assertEquals(listOf("b"), store.getPolaroids().map { it.id })
        assertEquals(listOf("/photos/a.png"), deleted)
    }

    @Test
    fun titlesFitTheSceneAndDoNotRepeatSoon() {
        val store = PolaroidStore(InMemoryKeyValueStorage())
        val titles = List(20) { store.pickTitle("SEASIDE_PIER") }
        assertEquals(20, titles.toSet().size)
        val known = PolaroidStore.SCENE_TITLES.getValue("SEASIDE_PIER") + PolaroidStore.TIME_TITLES_DAY + PolaroidStore.UNIVERSAL_TITLES
        assertTrue(titles.all { it in known })
    }
}
