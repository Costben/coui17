// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A default 44dp button has a 22dp radius — exactly half its height — so its ends sit at the capsule
 * radius. ColorOS 17 draws those ends as a circular arc joined to a 1.5-radius easing tail, neither a
 * circular stadium nor the n = 2.5 superellipse of the popup panel, and the button has to trace it.
 */
@OptIn(ExperimentalTestApi::class)
class ButtonCapsuleShapeTest {

    @Test
    fun theDefaultButtonOutlineFollowsTheSegmentCapsuleCurve() {
        val width = 120f
        val height = 44f
        val radius = height / 2f
        val actual = outlinePath(CapsuleOutlineShape(cornerRadius = 22.dp), Size(width, height))
        val segment = Path().apply { addSegmentCapsule(Rect(0f, 0f, width, height)) }
        val circular = Path().apply { addSquircleRect(width, height, radius) }
        val superellipse = Path().apply { addSquircleRect(width, height, radius, capsuleCircleBlend = false) }

        val toSegment = columnRms(actual, segment, width, height, radius)
        val toCircle = columnRms(actual, circular, width, height, radius)
        val toSuperellipse = columnRms(actual, superellipse, width, height, radius)
        println("BUTTON OUTLINE RMS segment=$toSegment circle=$toCircle superellipse=$toSuperellipse")

        assertTrue(toSegment < 0.05f, "the button outline left the system capsule curve by $toSegment")
        assertTrue(toCircle > 0.05f, "the outline should not be a circular stadium: $toCircle")
        assertTrue(
            toSuperellipse > toCircle,
            "n=2.5 must fit worse than a circle: superellipse=$toSuperellipse circle=$toCircle",
        )

        // The easing tail is what separates the curve from both models: in x in [R, 1.5R] measured
        // from the tip the top edge dips below the straight line instead of meeting it directly.
        val band = transitionBandMean(actual, width, height, radius)
        val segmentBand = transitionBandMean(segment, width, height, radius)
        println("BUTTON TRANSITION MEAN actual=$band segment=$segmentBand")
        assertTrue(band > 0.03f, "the tail must leave the straight edge, mean dip $band")
        assertTrue(abs(band - segmentBand) < 0.02f, "actual=$band segment=$segmentBand")
    }

    @Test
    fun anExplicitSmallRadiusStaysOnTheSquircle() {
        val width = 120f
        val height = 44f
        val actual = outlinePath(CapsuleOutlineShape(cornerRadius = 8.dp), Size(width, height))
        val squircle = Path().apply { addSquircleRect(width, height, 8f) }
        val segment = Path().apply { addSegmentCapsule(Rect(0f, 0f, width, height)) }

        val toSquircle = columnRms(actual, squircle, width, height, 8f)
        val toSegment = columnRms(actual, segment, width, height, height / 2f)
        println("SMALL RADIUS RMS squircle=$toSquircle segment=$toSegment")
        assertTrue(toSquircle < 0.05f, "a small radius must keep the squircle: $toSquircle")
        assertTrue(toSegment > 0.5f, "a small radius must not take the capsule curve: $toSegment")
    }

    @Test
    fun aNarrowCapsuleStaysWithinItsBoundsWithoutSelfIntersection() {
        // The button's default minWidth (58dp) against a 44dp height is narrower than 3R, so the
        // easing tail has to shorten; the outline must still be a single closed, convex contour
        // inside its bounds.
        val width = 58f
        val height = 44f
        val path = outlinePath(CapsuleOutlineShape(cornerRadius = 22.dp), Size(width, height))
        val bounds = path.getBounds()
        assertEquals(0f, bounds.left, 0.01f)
        assertEquals(0f, bounds.top, 0.01f)
        assertEquals(width, bounds.right, 0.01f)
        assertEquals(height, bounds.bottom, 0.01f)
        assertTrue(path.containsPoint(Offset(width / 2f, height / 2f)), "the capsule must have a fill")
        assertTrue(!path.crossesItself(), "the shortened tail must not fold the outline")
    }

    @Test
    fun disablingTheSquircleFallsBackToAStandardCapsule() {
        val outline = CapsuleOutlineShape(cornerRadius = 22.dp, squircleEnabled = false)
            .createOutline(Size(120f, 44f), LayoutDirection.Ltr, DensityStub)
        assertTrue(outline is Outline.Rounded, "a disabled squircle falls back to a rounded outline")
        assertEquals(22f, outline.roundRect.topLeftCornerRadius.x, 1e-3f)
    }

    /**
     * Guards the wiring: the rendered [Button] has to use the same outline, not merely a shape that
     * happens to be correct in isolation.
     */
    @Test
    fun aRenderedButtonTracesTheSegmentCapsuleEnd() {
        val density = Density(4f)
        runSkikoComposeUiTest(size = Size(600f, 260f), density = density) {
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF808080)))
                        Button(
                            onClick = {},
                            modifier = Modifier.size(120.dp, 44.dp),
                            colors = ButtonDefaults.buttonColors(
                                color = Color(0xFF101010),
                                disabledColor = Color(0xFF101010),
                                contentColor = Color.White,
                                disabledContentColor = Color.White,
                            ),
                            edgeStroke = null,
                            spotLight = null,
                            touchMotionEnabled = false,
                        ) {}
                    }
                }
            }
            waitForIdle()

            val pixels = onRoot().captureToImage().toPixelMap()
            var left = pixels.width
            var top = pixels.height
            var right = -1
            var bottom = -1
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) {
                    if (pixels[x, y].isButtonFill()) {
                        if (x < left) left = x
                        if (x > right) right = x
                        if (y < top) top = y
                        if (y > bottom) bottom = y
                    }
                }
            }
            val width = (right - left + 1).toFloat()
            val height = (bottom - top + 1).toFloat()
            assertTrue(width in 478f..482f, "button width $width")
            assertTrue(height in 174f..178f, "button height $height")
            val radius = height / 2f

            val background = pixels[0, 0].red
            val fill = pixels[left + width.toInt() / 2, top + height.toInt() / 2].red

            // Boundary position from the first partially covered pixel: coverage is the fraction of
            // that row's height the fill reaches, so the edge sits 1 - coverage below its top.
            fun renderedTop(localX: Float): Float {
                val column = left + localX.toInt()
                for (y in top..top + height.toInt()) {
                    val value = pixels[column, y].red
                    if (value < background - 0.001f) {
                        val coverage = ((background - value) / (background - fill)).coerceIn(0f, 1f)
                        return (y - top) + 1f - coverage
                    }
                }
                return 0f
            }

            val segment = Path().apply { addSegmentCapsule(Rect(0f, 0f, width, height)) }
            val circular = Path().apply { addSquircleRect(width, height, radius) }
            val segmentRms = profileRms(::renderedTop, segment, width, height, radius)
            val circleRms = profileRms(::renderedTop, circular, width, height, radius)
            println("RENDERED BUTTON RMS segment=$segmentRms circle=$circleRms")
            assertTrue(segmentRms < circleRms, "rendered segment=$segmentRms circle=$circleRms")

            val bandMean = (0..24).map { step ->
                renderedTop(width - radius - step * (radius * 0.5f / 24f))
            }.average().toFloat()
            println("RENDERED TRANSITION MEAN $bandMean")
            assertTrue(bandMean > 0.2f, "the rendered end must dip off the straight edge: $bandMean")
        }
    }

    /**
     * The old `squircleSurface` masked the *content* to the silhouette as well as filling it, so a
     * button whose content paints its full bounds stayed inside the capsule. The fill path must not
     * regress that: the corner squares of the node are page background, not content colour.
     */
    @Test
    fun fullBleedContentIsClippedToTheCapsule() {
        runSkikoComposeUiTest(size = Size(600f, 260f), density = Density(4f)) {
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF808080)))
                        Button(
                            onClick = {},
                            modifier = Modifier.size(120.dp, 44.dp),
                            colors = ButtonDefaults.buttonColors(
                                color = Color(0xFF101010),
                                disabledColor = Color(0xFF101010),
                                contentColor = Color.White,
                                disabledContentColor = Color.White,
                            ),
                            insideMargin = PaddingValues(0.dp),
                            edgeStroke = null,
                            spotLight = null,
                            touchMotionEnabled = false,
                        ) {
                            Box(Modifier.fillMaxSize().background(Color(0xFF00FF00)))
                        }
                    }
                }
            }
            waitForIdle()

            val pixels = onRoot().captureToImage().toPixelMap()
            var left = pixels.width
            var top = pixels.height
            var right = -1
            var bottom = -1
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) {
                    if (!pixels[x, y].isPageBackground()) {
                        if (x < left) left = x
                        if (x > right) right = x
                        if (y < top) top = y
                        if (y > bottom) bottom = y
                    }
                }
            }
            val width = right - left + 1
            val height = bottom - top + 1
            assertTrue(width in 478..482, "button width $width")
            assertTrue(height in 174..178, "button height $height")

            val centre = pixels[left + width / 2, top + height / 2]
            assertTrue(centre.isContentGreen(), "the content was not drawn: $centre")

            val corners = listOf(
                pixels[left + 2, top + 2],
                pixels[right - 2, top + 2],
                pixels[left + 2, bottom - 2],
                pixels[right - 2, bottom - 2],
            )
            println("FULL-BLEED CORNERS $corners")
            corners.forEach {
                assertTrue(it.isPageBackground(), "content leaked past the capsule corner: $it")
            }
        }
    }

    private fun outlinePath(shape: Shape, size: Size): Path {
        val outline = shape.createOutline(size, LayoutDirection.Ltr, DensityStub)
        assertTrue(outline is Outline.Generic, "the capsule outline must be a path")
        return outline.path
    }

    /** RMS of the difference between the two paths' top edge, over the right capsule end. */
    private fun columnRms(a: Path, b: Path, width: Float, height: Float, radius: Float): Float {
        val columns = generateSequence(width - 2f * radius) { it + 0.25f }
            .takeWhile { it <= width - 0.5f }
            .toList()
        val sum = columns.sumOf { x ->
            val difference = (topEdgeY(a, x, height) - topEdgeY(b, x, height)).toDouble()
            difference * difference
        }
        return sqrt(sum / columns.size).toFloat()
    }

    private fun profileRms(
        topAt: (Float) -> Float,
        model: Path,
        width: Float,
        height: Float,
        radius: Float,
    ): Float {
        val columns = generateSequence(width - 1.5f * radius) { it + 0.5f }
            .takeWhile { it <= width - 1f }
            .toList()
        val sum = columns.sumOf { x ->
            val difference = (topAt(x) - topEdgeY(model, x, height)).toDouble()
            difference * difference
        }
        return sqrt(sum / columns.size).toFloat()
    }

    /**
     * Mean top-edge dip over x in [R, 1.5R] measured from the right tip — the easing tail the system
     * capsule has and both circular and n = 2.5 ends do not.
     */
    private fun transitionBandMean(path: Path, width: Float, height: Float, radius: Float): Float {
        val values = (0..40).map { index ->
            val x = width - radius - index * (radius * 0.5f / 40f)
            topEdgeY(path, x, height)
        }
        return values.average().toFloat()
    }

    /** Topmost y of [path] at column [x], read from a thin vertical slice of the path. */
    private fun topEdgeY(path: Path, x: Float, height: Float): Float {
        val slice = Path().apply { addRect(Rect(x - 0.05f, -1f, x + 0.05f, height + 1f)) }
        val bounds = Path.combine(PathOperation.Intersect, path, slice).getBounds()
        return if (bounds.isEmpty) 0f else bounds.top
    }

    private fun Path.containsPoint(point: Offset): Boolean {
        val cell = Path().apply { addRect(Rect(point.x, point.y, point.x + 0.1f, point.y + 0.1f)) }
        return !Path.combine(PathOperation.Intersect, this, cell).getBounds().isEmpty
    }

    /** Whether any two non-adjacent segments of the sampled outline cross. */
    private fun Path.crossesItself(): Boolean {
        val measure = PathMeasure().apply { setPath(this@crossesItself, false) }
        val length = measure.length
        val count = (length / 0.5f).toInt().coerceAtLeast(8)
        val points = List(count) { measure.getPosition(it * length / count) }
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % count]
            for (j in i + 2 until count) {
                if (i == 0 && j == count - 1) continue
                if (segmentsCross(a, b, points[j], points[(j + 1) % count])) return true
            }
        }
        return false
    }

    private fun segmentsCross(a: Offset, b: Offset, c: Offset, d: Offset): Boolean {
        fun cross(o: Offset, p: Offset, q: Offset): Float = (p.x - o.x) * (q.y - o.y) - (p.y - o.y) * (q.x - o.x)
        val d1 = cross(c, d, a)
        val d2 = cross(c, d, b)
        val d3 = cross(a, b, c)
        val d4 = cross(a, b, d)
        return ((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
            ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
    }

    private fun Color.isButtonFill(): Boolean = red < 0.3f && green < 0.3f && blue < 0.3f

    /** The 0xFF808080 page behind the test button, with a little room for antialiasing. */
    private fun Color.isPageBackground(): Boolean = abs(red - 0.50196f) < 0.04f && abs(green - 0.50196f) < 0.04f && abs(blue - 0.50196f) < 0.04f

    /** The 0xFF00FF00 full-bleed content. */
    private fun Color.isContentGreen(): Boolean = green > 0.5f && red < 0.3f && blue < 0.3f

    private object DensityStub : Density {
        override val density: Float = 1f
        override val fontScale: Float = 1f
    }
}
