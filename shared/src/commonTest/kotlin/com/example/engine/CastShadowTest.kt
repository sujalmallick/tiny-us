package com.example.engine

import com.example.scene.WeatherType
import kotlin.math.roundToInt
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CastShadowTest {

    @AfterTest
    fun resetLight() {
        SceneLight.current = CastLight()
    }

    private fun shape(light: CastLight, w: Int = 10, h: Int = 26, x: Float = 100f) = castShadowShape(light, w, h, x)

    @Test
    fun morningShadowsPointRightAndSunsetShadowsLeft() {
        val morning = shape(CastLight(sunProgress = 0.1f))
        val sunset = shape(CastLight(sunProgress = 0.9f))
        assertTrue(morning.tipDx > 0)
        assertTrue(sunset.tipDx < 0)
        assertEquals(morning.tipDx, -sunset.tipDx)
    }

    @Test
    fun noonShadowsAreShortAndUnderfoot() {
        val noon = shape(CastLight(sunProgress = 0.5f))
        val evening = shape(CastLight(sunProgress = 0.95f))
        assertEquals(0, noon.tipDx)
        assertEquals(2, noon.rows)
        assertTrue(evening.rows > noon.rows)
    }

    @Test
    fun tallerThingsThrowLongerShadows() {
        val light = CastLight(sunProgress = 0.9f)
        assertTrue(shape(light, h = 40).tipDx < shape(light, h = 10).tipDx)
    }

    @Test
    fun noSunShadowsInRainSnowOrAtNight() {
        assertFalse(shape(CastLight(weather = WeatherType.RAIN)).visible)
        assertFalse(shape(CastLight(weather = WeatherType.SNOW)).visible)
        assertFalse(shape(CastLight(night = true)).visible)
        assertTrue(shape(CastLight(weather = WeatherType.SAKURA)).visible)
    }

    @Test
    fun firelightThrowsShadowsAwayFromTheFire() {
        val fire = CastLight(night = true, lampX = 500f)
        assertTrue(shape(fire, x = 300f).tipDx < 0)
        assertTrue(shape(fire, x = 700f).tipDx > 0)
    }

    @Test
    fun indoorsTheShadowIsShortAndSoft() {
        val room = shape(CastLight(outdoor = false, night = true, weather = WeatherType.RAIN))
        assertTrue(room.visible)
        assertEquals(2, room.rows)
        assertTrue(room.tipDx in 1..4)
    }

    @Test
    fun drawsOnTheGridBelowTheGroundLineOnly() {
        val scope = RecordingDrawScope()
        val p = 4f
        drawCastShadow(scope, centerX = 200f, groundY = 400f, widthPx = 10, p = p, heightPx = 26, light = CastLight(sunProgress = 0.9f))
        assertTrue(scope.rects.isNotEmpty())
        for (r in scope.rects) {
            assertEquals(0f, r.topLeft.x % p)
            assertEquals(0f, r.topLeft.y % p)
            assertEquals(p, r.size.height)
            assertEquals(0f, r.size.width % p)
            // Never above where it touches the ground
            assertTrue(r.topLeft.y >= 400f)
        }
    }

    @Test
    fun aSunsetShadowStretchesLeftAndItsFarEndDithers() {
        val scope = RecordingDrawScope()
        val p = 1f
        drawCastShadow(scope, centerX = 200f, groundY = 100f, widthPx = 10, p = p, heightPx = 26, light = CastLight(sunProgress = 0.9f))
        val minX = scope.rects.minOf { it.topLeft.x }
        val maxX = scope.rects.maxOf { it.topLeft.x + it.size.width }
        assertTrue(minX < 200f - 12f, "reaches well to the left: $minX")
        assertTrue(maxX <= 206f, "barely past the right of the footprint: $maxX")
        // The last row is a checkerboard of single pixels
        val lastRow = scope.rects.maxOf { it.topLeft.y }
        val far = scope.rects.filter { it.topLeft.y == lastRow }
        assertTrue(far.all { it.size.width == p })
        assertTrue(far.all { ((it.topLeft.x.roundToInt() + lastRow.roundToInt()) and 1) == 0 })
    }

    @Test
    fun drawsWithTheSceneLightByDefault() {
        val scope = RecordingDrawScope()
        SceneLight.current = CastLight(night = true)
        drawCastShadow(scope, 100f, 100f, 10, 2f)
        assertTrue(scope.rects.isEmpty())
        SceneLight.current = CastLight(sunProgress = 0.2f)
        drawCastShadow(scope, 100f, 100f, 10, 2f)
        assertTrue(scope.rects.isNotEmpty())
        assertTrue(scope.rects.all { it.color == CastShadowColor })
    }
}
