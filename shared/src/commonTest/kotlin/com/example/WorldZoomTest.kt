package com.example

import com.example.engine.WorldCamera
import com.example.engine.WorldViewport
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
        // A tall stage still (partial pixels at the edge round up: 2732 / 8 is 341.5).
        assertEquals(342, camera.stageH)
    }

    @Test
    fun phonesAreUnderTheLimit() {
        WorldViewport.maxZoom = 12 // 4 points per game pixel on a 3x iPhone
        assertEquals(8, WorldCamera.forScreen(1170f, 2532f, pixelRenderer = true).zoom)
    }
}
