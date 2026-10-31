// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchBarCapsuleShapeTest {

    /**
     * Port of MaterialShadowEdge.kt's smoothSDF_D and sdCapsule to Kotlin for math verification.
     */
    private fun smoothAbs(x: Float, k: Float): Float = sqrt(x * x + k * k)

    private fun smoothMaxSqrt(a: Float, b: Float, k: Float): Float {
        val d = a - b
        return 0.5f * (a + b + sqrt(d * d + k * k))
    }

    private fun sdCapsule(pLocalX: Float, pLocalY: Float, halfX: Float, halfY: Float): Float {
        val isVertical = halfX < halfY
        val hsX = if (isVertical) halfY else halfX
        val hsY = if (isVertical) halfX else halfY
        val plX = if (isVertical) pLocalY else pLocalX
        val plY = if (isVertical) pLocalX else pLocalY

        val qx = smoothAbs(plX, 1.0f)
        val qy = smoothAbs(plY, 1.0f)
        val r = hsY
        val a = max(hsX - r, 0.0f)

        val scale = r * 0.0125f
        val kMaxEff = 26.6f * scale
        val xBandEff = 70.0f * scale
        val yBandEff = 60.0f * scale

        val s = qx - a
        val xHard = max(s, 0.0f)
        val xSoft = if (kMaxEff > 0.0f) smoothMaxSqrt(s, 0.0f, kMaxEff) else xHard
        val wx = 1.0f - ((abs(s) - 0.0f) / max(xBandEff, 1e-3f)).coerceIn(0.0f, 1.0f)
        val wy = ((qy - (r - max(yBandEff, 1e-3f))) / max(yBandEff, 1e-3f)).coerceIn(0.0f, 1.0f)
        val w = (wx * wy).coerceIn(0.0f, 1.0f)
        val x = xHard * (1.0f - w) + xSoft * w
        return sqrt(x * x + qy * qy) - r
    }

    private fun powFast(x: Float, n: Float): Float = kotlin.math.exp(kotlin.math.ln(max(x, 1e-8f)) * n)

    private fun lpNorm(vx: Float, vy: Float, n: Float): Float {
        val ax = abs(vx)
        val ay = abs(vy)
        val m = max(ax, ay)
        if (m < 1e-8f) return 0.0f
        val ux = ax / m
        val uy = ay / m
        val s = powFast(ux, n) + powFast(uy, n)
        return m * kotlin.math.exp(kotlin.math.ln(max(s, 1e-6f)) / n)
    }

    private fun sdRoundRect(pLocalX: Float, pLocalY: Float, halfX: Float, halfY: Float, r: Float, n: Float): Float {
        val qx = abs(pLocalX) - halfX + r
        val qy = abs(pLocalY) - halfY + r
        val nClamped = max(n, 2.0f)
        val dCorner = lpNorm(max(qx, 0.0f), max(qy, 0.0f), nClamped)
        return if (qx > 0.0f && qy > 0.0f) dCorner - r else max(qx, qy) - r
    }

    private fun couiSegmentCapsuleDelta(pLocalX: Float, pLocalY: Float, halfX: Float, halfY: Float, r: Float): Float {
        if (halfX <= halfY || r < halfY - 0.001f) {
            return 0.0f
        }
        val radiusR = halfY
        val s = halfX - abs(pLocalX)
        val tail = min(1.5f, halfX / radiusR)
        val s0 = 0.2250555f * radiusR
        val s1 = tail * radiusR
        if (s <= s0 || s >= s1) {
            return 0.0f
        }
        val u = (s - s0) / (s1 - s0)
        val uFactor = u * u * (1.0f - u) * (1.0f - u)
        return uFactor * (0.003318f + u * (1.529298f - 1.888829f * u)) * radiusR
    }

    /**
     * Rim distance function from MaterialShadowEdge.kt.
     */
    private fun rimShaderDistance(
        pLocalX: Float,
        pLocalY: Float,
        halfX: Float,
        halfY: Float,
        corner: Float,
        weight: Float,
        applyDelta: Boolean,
    ): Float {
        var d = if (weight < 2.0f) {
            if (applyDelta && halfX > halfY && corner >= halfY - 0.001f) {
                // If wide capsule with continuous curve, evaluate sdRoundRect(2.0) or sdCapsule?
                sdRoundRect(pLocalX, pLocalY, halfX, halfY, corner, 2.0f)
            } else if (weight > 1.005f) {
                val t = ((2.0f - weight) / (2.0f - 1.0f)).coerceIn(0.0f, 1.0f)
                val dG2 = sdRoundRect(pLocalX, pLocalY, halfX, halfY, corner, 2.0f)
                val dCap = sdCapsule(pLocalX, pLocalY, halfX, halfY)
                dG2 * (1.0f - t) + dCap * t
            } else {
                sdCapsule(pLocalX, pLocalY, halfX, halfY)
            }
        } else {
            sdRoundRect(pLocalX, pLocalY, halfX, halfY, corner, weight)
        }
        if (applyDelta) {
            d += couiSegmentCapsuleDelta(pLocalX, pLocalY, halfX, halfY, corner)
        }
        return d
    }

    @Test
    fun rimShaderDistanceTracksSegmentCapsuleWithinThreshold() {
        val radius = 20f
        val halfW = 100f

        // 1. Wide capsule with delta: error to addSegmentCapsule contour must be <= 0.005R
        val path = Path().apply { addSegmentCapsule(Rect(0f, 0f, halfW * 2f, radius * 2f)) }
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val sampleCount = 2000
        var maxErrorWithDelta = 0f
        var maxErrorWithoutDelta = 0f

        for (i in 0..sampleCount) {
            val pos = measure.getPosition(i * length / sampleCount)
            val xLocal = pos.x - halfW
            val yLocal = pos.y - radius
            val distWith = abs(rimShaderDistance(xLocal, yLocal, halfW, radius, radius, 1.0f, applyDelta = true))
            val distWithout = abs(rimShaderDistance(xLocal, yLocal, halfW, radius, radius, 1.0f, applyDelta = false))
            if (distWith > maxErrorWithDelta) maxErrorWithDelta = distWith
            if (distWithout > maxErrorWithoutDelta) maxErrorWithoutDelta = distWithout
        }

        println("[rim-test] maxErrorWithDelta=${maxErrorWithDelta / radius}R, maxErrorWithoutDelta=${maxErrorWithoutDelta / radius}R")

        // Pre-fix check: without delta, rim distance function error against addSegmentCapsule exceeds 0.005R
        assertTrue(
            maxErrorWithoutDelta > 0.005f * radius,
            "Without delta, rim distance function deviates from segment capsule by >0.005R (was ${maxErrorWithoutDelta / radius}R)",
        )

        // Post-fix requirement: with delta, error is <= 0.005R
        assertTrue(
            maxErrorWithDelta <= 0.005f * radius,
            "With delta, rim error must be <= 0.005R (was ${maxErrorWithDelta / radius}R)",
        )

        // 2. Pure circle (w == h): delta must be exactly 0
        val circleDelta = couiSegmentCapsuleDelta(10f, 10f, 20f, 20f, 20f)
        assertEquals(0.0f, circleDelta, 1e-6f, "Circular particles (w == h) must produce 0 delta")

        // 3. Small corner radius (corner < halfH): delta must be exactly 0
        val smallRadiusDelta = couiSegmentCapsuleDelta(50f, 10f, 100f, 20f, 8f)
        assertEquals(0.0f, smallRadiusDelta, 1e-6f, "Small corner radius (r < halfH) must produce 0 delta")
    }

    @Test
    fun renderedSearchBarInputTracesSegmentCapsuleEnd() {
        val density = Density(4f)
        runSkikoComposeUiTest(size = Size(1200f, 260f), density = density) {
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF808080)))
                        InputField(
                            query = "",
                            onQueryChange = {},
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            modifier = Modifier.size(240.dp, 40.dp),
                            colors = SearchBarDefaults.searchBarColors(
                                backgroundColor = Color(0xFF101010),
                            ),
                            stroke = null,
                            glowColor = null,
                        )
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
                    val p = pixels[x, y]
                    if (p.red < 0.3f && p.green < 0.3f && p.blue < 0.3f) {
                        if (x < left) left = x
                        if (x > right) right = x
                        if (y < top) top = y
                        if (y > bottom) bottom = y
                    }
                }
            }
            val width = (right - left + 1).toFloat()
            val height = (bottom - top + 1).toFloat()
            assertTrue(width in 958f..962f, "search bar width $width")
            assertTrue(height in 158f..162f, "search bar height $height")
            val radius = height / 2f

            val background = pixels[0, 0].red
            val fill = pixels[left + width.toInt() / 2, top + height.toInt() / 2].red

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
            println("RENDERED SEARCH BAR RMS segment=$segmentRms circle=$circleRms")

            // The rendered input field MUST fit segment capsule better than a circular stadium
            assertTrue(segmentRms < circleRms, "Rendered search bar must trace segment capsule: segment=$segmentRms circle=$circleRms")
        }
    }

    @Test
    fun fillAndRimShareTheSameContourWithinThreshold() {
        val density = Density(1f)
        val width = 240f
        val height = 40f
        val radius = height / 2f
        val halfW = width / 2f

        // Outline path from CapsuleOutlineShape (the fill shape)
        val fillOutline = CapsuleOutlineShape().createOutline(Size(width, height), androidx.compose.ui.unit.LayoutDirection.Ltr, density)
        assertTrue(fillOutline is androidx.compose.ui.graphics.Outline.Generic)
        val fillPath = fillOutline.path

        // Top edge of the fill path at column x
        fun fillTopEdgeY(x: Float): Float {
            val slice = Path().apply { addRect(Rect(x - 0.05f, -1f, x + 0.05f, height + 1f)) }
            val bounds = Path.combine(androidx.compose.ui.graphics.PathOperation.Intersect, fillPath, slice).getBounds()
            return if (bounds.isEmpty) 0f else bounds.top
        }

        // Rim contour y from rimShaderDistance(x, y) = 0 using binary search
        fun rimTopEdgeY(x: Float): Float {
            val xLocal = x - halfW
            var yOutside = -2f // outside the top edge (d > 0)
            var yInside = radius // inside the shape (d < 0)
            for (iter in 0 until 24) {
                val yMid = (yOutside + yInside) / 2f
                val yLocal = yMid - radius
                val d = rimShaderDistance(xLocal, yLocal, halfW, radius, radius, 1.0f, applyDelta = true)
                if (d > 0f) {
                    yOutside = yMid
                } else {
                    yInside = yMid
                }
            }
            return (yOutside + yInside) / 2f
        }

        // Compare columns across transition segment x in [width - 1.5R, width - R]
        val columns = generateSequence(width - 1.5f * radius) { it + 0.5f }
            .takeWhile { it <= width - radius }
            .toList()

        var maxDiff = 0f
        for (col in columns) {
            val yFill = fillTopEdgeY(col)
            val yRim = rimTopEdgeY(col)
            val diff = abs(yFill - yRim)
            if (diff > maxDiff) maxDiff = diff
        }

        println("[contour-test] maxDiff across transition segment: ${maxDiff / radius}R ($maxDiff px)")
        assertTrue(
            maxDiff <= 0.005f * radius,
            "Fill and rim contours must match within <= 0.005R across transition (was ${maxDiff / radius}R)",
        )
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

    private fun topEdgeY(path: Path, x: Float, height: Float): Float {
        val slice = Path().apply { addRect(Rect(x - 0.05f, -1f, x + 0.05f, height + 1f)) }
        val bounds = Path.combine(androidx.compose.ui.graphics.PathOperation.Intersect, path, slice).getBounds()
        return if (bounds.isEmpty) 0f else bounds.top
    }
}
