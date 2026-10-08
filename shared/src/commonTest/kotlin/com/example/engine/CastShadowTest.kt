package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.example.scene.WeatherType
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class CastShadowRecordingScope : DrawScope {
    val rects = mutableListOf<RectRecord>()

    override val density: Float = 1f
    override val fontScale: Float = 1f
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val size: Size = Size(2000f, 2000f)
    override val drawContext: DrawContext
        get() = throw UnsupportedOperationException()

    override fun drawRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) {
        rects.add(RectRecord(color, topLeft, size))
    }

    override fun drawRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawLine(
        color: Color,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: androidx.compose.ui.graphics.StrokeCap,
        pathEffect: androidx.compose.ui.graphics.PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: androidx.compose.ui.graphics.StrokeCap,
        pathEffect: androidx.compose.ui.graphics.PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawOval(
        color: Color,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawOval(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: Offset,
        size: Size,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: androidx.compose.ui.graphics.StrokeCap,
        pathEffect: androidx.compose.ui.graphics.PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float,
        cap: androidx.compose.ui.graphics.StrokeCap,
        pathEffect: androidx.compose.ui.graphics.PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawRoundRect(
        color: Color,
        topLeft: Offset,
        size: Size,
        cornerRadius: androidx.compose.ui.geometry.CornerRadius,
        style: DrawStyle,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()

    override fun drawRoundRect(
        brush: Brush,
        topLeft: Offset,
        size: Size,
        cornerRadius: androidx.compose.ui.geometry.CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode
    ) = throw UnsupportedOperationException()
}

class CastShadowTest {

    @Test
    fun testMorningProgressZeroPointsRightAndIsLong() {
        val geom = calculateCastShadowGeometry(
            widthPx = 14,
            sunProgress = 0.0f,
            isOutdoor = true,
            isNight = false,
            weather = WeatherType.SUNNY
        )
        assertTrue(geom.active, "Cast shadow must be active on sunny morning")
        assertTrue(geom.dxPx > 0, "Morning shadow must point right (positive dx): dx=${geom.dxPx}")
        assertEquals(4, geom.lengthRows, "Morning shadow must be maximal length (4 rows)")
    }

    @Test
    fun testNoonProgressHalfIsCenteredAndShort() {
        val geom = calculateCastShadowGeometry(
            widthPx = 14,
            sunProgress = 0.5f,
            isOutdoor = true,
            isNight = false,
            weather = WeatherType.SUNNY
        )
        assertTrue(geom.active, "Cast shadow must be active at noon")
        assertEquals(0, geom.dxPx, "Noon shadow must be centered under feet (dx == 0)")
        assertEquals(2, geom.lengthRows, "Noon shadow must be shortest (2 rows)")
    }

    @Test
    fun testSunsetProgressOnePointsLeftAndIsLong() {
        val geom = calculateCastShadowGeometry(
            widthPx = 14,
            sunProgress = 1.0f,
            isOutdoor = true,
            isNight = false,
            weather = WeatherType.SUNNY
        )
        assertTrue(geom.active, "Cast shadow must be active on sunny sunset")
        assertTrue(geom.dxPx < 0, "Sunset shadow must point left (negative dx): dx=${geom.dxPx}")
        assertEquals(4, geom.lengthRows, "Sunset shadow must be maximal length (4 rows)")
    }

    @Test
    fun testNoneInRain() {
        val geom = calculateCastShadowGeometry(
            widthPx = 14,
            sunProgress = 0.3f,
            isOutdoor = true,
            isNight = false,
            weather = WeatherType.RAIN
        )
        assertFalse(geom.active, "Cast shadow must be suppressed in rain")
        assertEquals(0, geom.lengthRows, "Rain must have 0 rows")

        val scope = CastShadowRecordingScope()
        drawCastShadow(scope, 100f, 200f, 14, 4f, sunProgress = 0.3f, weather = WeatherType.RAIN)
        assertEquals(0, scope.rects.size, "No rects must be drawn in rain")
    }

    @Test
    fun testNoneInSnow() {
        val geom = calculateCastShadowGeometry(
            widthPx = 14,
            sunProgress = 0.3f,
            isOutdoor = true,
            isNight = false,
            weather = WeatherType.SNOW
        )
        assertFalse(geom.active, "Cast shadow must be suppressed in overcast snow")
        assertEquals(0, geom.lengthRows, "Snow must have 0 rows")
    }

    @Test
    fun testNoneFromSunAtNight() {
        val geom = calculateCastShadowGeometry(
            widthPx = 14,
            sunProgress = 0.5f,
            isOutdoor = true,
            isNight = true,
            weather = WeatherType.SUNNY,
            localLightX = null
        )
        assertFalse(geom.active, "No sun or moon cast shadow at night")
        assertEquals(0, geom.lengthRows)

        val scope = CastShadowRecordingScope()
        drawCastShadow(scope, 100f, 200f, 14, 4f, isOutdoor = true, isNight = true)
        assertEquals(0, scope.rects.size, "No rects must be drawn from sun at night")
    }

    @Test
    fun testNightLocalLightCastsAwayFromLight() {
        // Campfire at X = 200, Character at X = 300 (to the right)
        val rightGeom = calculateCastShadowGeometry(
            widthPx = 14,
            isOutdoor = true,
            isNight = true,
            localLightX = 200f,
            centerX = 300f
        )
        assertTrue(rightGeom.active, "Local light at night must cast shadow")
        assertTrue(rightGeom.dxPx > 0, "Character to right of fire must cast shadow right")
        assertEquals(2, rightGeom.lengthRows, "Local light shadow is short (2 rows)")

        // Character at X = 100 (to the left of fire)
        val leftGeom = calculateCastShadowGeometry(
            widthPx = 14,
            isOutdoor = true,
            isNight = true,
            localLightX = 200f,
            centerX = 100f
        )
        assertTrue(leftGeom.active)
        assertTrue(leftGeom.dxPx < 0, "Character to left of fire must cast shadow left")
        assertEquals(2, leftGeom.lengthRows)
    }

    @Test
    fun testIndoorsSubtleShort() {
        val geom = calculateCastShadowGeometry(
            widthPx = 16,
            isOutdoor = false,
            isNight = false
        )
        assertTrue(geom.active, "Indoor cast shadow is active")
        assertEquals(2, geom.lengthRows, "Indoor shadow is short (2 rows)")
    }

    @Test
    fun testGridSnappingAndGroundBoundary() {
        val scope = CastShadowRecordingScope()
        val p = 4f
        val groundY = 200f
        drawCastShadow(
            scope = scope,
            centerX = 103.7f,
            groundY = groundY,
            widthPx = 14,
            p = p,
            sunProgress = 0.2f,
            isOutdoor = true,
            isNight = false
        )

        assertTrue(scope.rects.isNotEmpty(), "Rects must be drawn")
        for (rect in scope.rects) {
            // Must snap to pixel grid
            val remX = rect.topLeft.x % p
            val remY = rect.topLeft.y % p
            assertEquals(0f, remX, 0.001f, "TopLeft X must be snapped to p: ${rect.topLeft.x}")
            assertEquals(0f, remY, 0.001f, "TopLeft Y must be snapped to p: ${rect.topLeft.y}")

            // Size must be whole multiples of p
            assertEquals(0f, rect.size.width % p, 0.001f, "Width must be multiple of p")
            assertEquals(p, rect.size.height, 0.001f, "Height must be 1 pixel row (p)")

            // Ground boundary: row must be at or below groundY
            assertTrue(rect.topLeft.y >= (groundY / p).roundToInt() * p, "Shadow must not climb above groundY")
        }
    }

    @Test
    fun testClampingToPierDeck() {
        val scope = CastShadowRecordingScope()
        val p = 5f
        val groundY = 300f
        val minGroundY = 305f // clamp out the top row
        drawCastShadow(
            scope = scope,
            centerX = 100f,
            groundY = groundY,
            widthPx = 14,
            p = p,
            sunProgress = 0.0f,
            minGroundY = minGroundY
        )
        for (rect in scope.rects) {
            assertTrue(rect.topLeft.y >= minGroundY, "Rect y (${rect.topLeft.y}) must be >= minGroundY ($minGroundY)")
        }
    }
}
