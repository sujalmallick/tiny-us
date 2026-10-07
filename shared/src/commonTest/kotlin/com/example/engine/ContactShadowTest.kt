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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class RectRecord(
    val color: Color,
    val topLeft: Offset,
    val size: Size
)

@Suppress("DEPRECATION")
class RecordingDrawScope : DrawScope {
    val rects = mutableListOf<RectRecord>()

    override val density: Float = 1f
    override val fontScale: Float = 1f
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val size: Size = Size(1000f, 1000f)
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
}

class ContactShadowTest {

    @Test
    fun doesNothingForInvalidWidthOrPixelSize() {
        val scope1 = RecordingDrawScope()
        drawContactShadow(scope1, 100f, 100f, widthPx = 1, p = 3f)
        assertEquals(0, scope1.rects.size)

        val scope2 = RecordingDrawScope()
        drawContactShadow(scope2, 100f, 100f, widthPx = 0, p = 3f)
        assertEquals(0, scope2.rects.size)

        val scope3 = RecordingDrawScope()
        drawContactShadow(scope3, 100f, 100f, widthPx = 20, p = 0f)
        assertEquals(0, scope3.rects.size)

        val scope4 = RecordingDrawScope()
        drawContactShadow(scope4, 100f, 100f, widthPx = 20, p = -1f)
        assertEquals(0, scope4.rects.size)
    }

    @Test
    fun isDeterministicAcrossRepeatedCalls() {
        val scopeA = RecordingDrawScope()
        val scopeB = RecordingDrawScope()
        drawContactShadow(scopeA, 123.4f, 234.5f, widthPx = 24, p = 3.5f)
        drawContactShadow(scopeB, 123.4f, 234.5f, widthPx = 24, p = 3.5f)

        assertEquals(scopeA.rects.size, scopeB.rects.size)
        for (i in scopeA.rects.indices) {
            assertEquals(scopeA.rects[i], scopeB.rects[i])
        }
    }

    @Test
    fun heightIsClampedBetweenTwoAndFourRows() {
        for (w in 2..50) {
            val scope = RecordingDrawScope()
            val p = 2f
            drawContactShadow(scope, 200f, 200f, widthPx = w, p = p)
            val distinctRows = scope.rects.map { (it.topLeft.y / p).toInt() }.distinct()
            assertTrue(distinctRows.size in 2..4, "Width $w produced row count ${distinctRows.size} not in 2..4")
        }
    }

    @Test
    fun coordinatesAndSizesAreGridSnappedToPixelSize() {
        val p = 3.2f
        val scope = RecordingDrawScope()
        drawContactShadow(scope, 157.3f, 89.1f, widthPx = 28, p = p)

        for (rect in scope.rects) {
            val leftUnits = rect.topLeft.x / p
            val topUnits = rect.topLeft.y / p
            val widthUnits = rect.size.width / p
            val heightUnits = rect.size.height / p

            assertEquals(kotlin.math.round(leftUnits), leftUnits, 0.001f, "X not snapped")
            assertEquals(kotlin.math.round(topUnits), topUnits, 0.001f, "Y not snapped")
            assertEquals(kotlin.math.round(widthUnits), widthUnits, 0.001f, "Width not multiple of p")
            assertEquals(1f, heightUnits, 0.001f, "Height should be 1 pixel row")
        }
    }

    @Test
    fun checkerboardDitheringOnOuterEdges() {
        val p = 4f
        val scope = RecordingDrawScope()
        drawContactShadow(scope, 100f, 100f, widthPx = 20, p = p)

        // All rects should use ContactShadowColor
        for (rect in scope.rects) {
            assertEquals(ContactShadowColor, rect.color)
        }

        // Must have at least some 1x1 checker pixels (width == p)
        val singlePixels = scope.rects.filter { it.size.width == p }
        assertTrue(singlePixels.isNotEmpty(), "Checkerboard outer pixels should be drawn as single 1x1 pixels")
    }
}
