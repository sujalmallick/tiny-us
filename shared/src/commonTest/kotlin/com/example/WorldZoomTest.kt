package com.example

import com.example.engine.WorldCamera
import com.example.engine.WorldViewport
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldZoomTest {
    @AfterTest
    fun unlimited() {
        WorldViewport.maxZoom = Int.MAX_VALUE
    }

    @Test
    fun withoutALimitTheStageFillsTheWidth() {
        // A 12.9-inch iPad in portrait, 2048 x 2732 pixels: the 144-pixel stage enlarged 14 times.
        val camera = WorldCamera.forScreen(2048f, 2732f, pixelRenderer = true)
        assertEquals(14, camera.zoom)
        assertEquals(147, camera.gameW)
    }

    @Test
    fun aLimitShowsMoreOfTheSceneAtASmallerSize() {
        WorldViewport.maxZoom = 8 // 4 points per game pixel on a 2x iPad
        val camera = WorldCamera.forScreen(2048f, 2732f, pixelRenderer = true)
        assertEquals(8, camera.zoom)
        assertEquals(256, camera.gameW)
        assertTrue(camera.stageH >= camera.stageW, "a portrait iPad still gets a tall stage")
        assertTrue(camera.stageH * camera.zoom <= 2732)
    }

    @Test
    fun phonesAreUnderTheLimit() {
        WorldViewport.maxZoom = 12 // 4 points per game pixel on a 3x iPhone
        assertEquals(8, WorldCamera.forScreen(1170f, 2532f, pixelRenderer = true).zoom)
    }
}
