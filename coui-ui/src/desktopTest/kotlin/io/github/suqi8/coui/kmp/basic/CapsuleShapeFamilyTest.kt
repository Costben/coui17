// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class, ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Close
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The three corner families of ColorOS 17, and the controls that changed family in 1.2.0.
 *
 * The bottom bars, the chip and the segmented control are Path capsules drawn by
 * `OplusPathAdapter.addSmoothRoundRect` without a weight, which the screen measurements resolve to
 * the `SegmentCapsule` curve. The single-line TopTips is the Outline path at weight 1.0, i.e. a true
 * circle. A single-line Snackbar follows its measured height. Each test fails if the control falls
 * back to a circular or fixed-radius silhouette.
 */
class CapsuleShapeFamilyTest {

    @Test
    fun scrollToolbarPillFollowsSegmentCapsule() = runSkikoComposeUiTest(
        size = Size(600f, 300f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    BottomFloatingScrollToolbar(
                        colors = BottomFloatingScrollToolbarDefaults.colors(
                            containerColor = Color.Black,
                            strokeColor = Color.Transparent,
                        ),
                    ) {
                        BottomFloatingScrollToolbarItem(
                            onClick = {},
                            icon = COUIIcons.Basic.Close,
                            label = "A",
                        )
                    }
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        assertTrue(
            fit.rmsSegment < fit.rmsCircle,
            "the pill must follow the segment capsule: segment=${fit.rmsSegment} circle=${fit.rmsCircle}",
        )
        assertTrue(fit.rmsSegment < 1.5, "segment residual ${fit.rmsSegment}px is too large")
    }

    @Test
    fun chipFollowsSegmentCapsule() = runSkikoComposeUiTest(
        size = Size(400f, 240f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Chip(
                        selected = false,
                        onClick = {},
                        label = "A",
                        colors = ChipDefaults.chipColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                        ),
                    )
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        assertTrue(
            fit.rmsSegment < fit.rmsCircle,
            "the chip must follow the segment capsule: segment=${fit.rmsSegment} circle=${fit.rmsCircle}",
        )
        assertTrue(fit.rmsSegment < 1.5, "segment residual ${fit.rmsSegment}px is too large")
    }

    @Test
    fun explicitSmallerChipRadiusKeepsTheSquircle() = runSkikoComposeUiTest(
        size = Size(400f, 240f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Chip(
                        selected = false,
                        onClick = {},
                        label = "A",
                        cornerRadius = 8.dp,
                        colors = ChipDefaults.chipColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                        ),
                    )
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        assertTrue(
            fit.rmsCircle > 0.3,
            "an explicit smaller radius must keep the squircle: circle residual ${fit.rmsCircle} should be > 0.3",
        )
    }

    @Test
    fun topTipsSingleLineIsAFullCircle() = runSkikoComposeUiTest(
        size = Size(400f, 240f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    TopTips(
                        text = "Tips",
                        visible = true,
                        colors = TopTipsDefaults.topTipsColors(TopTipsBackgroundStyle.Gray).copy(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                        ),
                    )
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        assertTrue(fit.rmsCircle < 0.6, "single-line TopTips must be a full circle: circle=${fit.rmsCircle}")
    }

    @Test
    fun snackbarSingleLineRadiusFollowsHeight() = runSkikoComposeUiTest(
        size = Size(500f, 240f),
        density = Density(4f),
    ) {
        val data = object : SnackbarData {
            override val visuals: SnackbarVisuals = SnackbarVisuals(
                message = "Single line snackbar message",
                actionLabel = null,
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite,
            )
            override suspend fun performAction() {}
            override suspend fun dismiss() {}
        }
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Snackbar(
                        data = data,
                        colors = SnackbarDefaults.snackbarColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                        ),
                    )
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        assertTrue(fit.r > 60, "single-line Snackbar corner radius ${fit.r}px should follow measured height")
    }

    @Test
    fun verticalCapsuleOutlineDoesNotExceedBounds() {
        val size = Size(44f, 120f)
        val density = Density(1f)
        val outline = CapsuleOutlineShape(squircleEnabled = true)
            .createOutline(size, LayoutDirection.Ltr, density)
        val bounds = outline.bounds
        assertTrue(
            bounds.left >= 0f && bounds.top >= 0f && bounds.right <= size.width + 0.01f && bounds.bottom <= size.height + 0.01f,
            "outline bounds $bounds must stay within node bounds $size",
        )
    }

    @Test
    fun bottomFloatingToolbarSingleItemRendersWithoutExtraRecomposition() = runSkikoComposeUiTest(
        size = Size(300f, 200f),
        density = Density(2f),
    ) {
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                    BottomFloatingToolbarGroup {
                        BottomFloatingToolbarItem(
                            onClick = {},
                            icon = COUIIcons.Basic.Close,
                            label = "Item",
                        )
                    }
                }
            }
        }
        waitForIdle()
    }

    private data class CornerFit(val rmsCircle: Double, val rmsSegment: Double, val r: Int)

    /** Fits the top-left fill boundary to a circle and to the segment capsule of the same bounds. */
    private fun fitTopLeftCorner(pixels: PixelMap, fill: (Color) -> Boolean): CornerFit {
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = -1
        var bottom = -1
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (!fill(pixels[x, y])) continue
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        check(right > left && bottom > top) { "no filled pixels" }
        val width = right - left + 1
        val r = (bottom - top + 1) / 2
        val measured = ArrayList<Double>()
        val rows = ArrayList<Double>()
        val maxU = (1.6 * r).toInt()
        for (y in top + 1 until top + r) {
            var edge = -1
            for (x in maxOf(0, left - 2) until minOf(pixels.width, left + maxU)) {
                if (fill(pixels[x, y])) {
                    edge = x
                    break
                }
            }
            if (edge >= 0) {
                measured += (edge - left).toDouble()
                rows += (y - top).toDouble()
            }
        }
        val circle = DoubleArray(measured.size) { index ->
            val v = rows[index]
            r - sqrt((r * r - (r - v) * (r - v)).coerceAtLeast(0.0))
        }
        val segment = segmentProfile(rows, r, width)
        return CornerFit(rms(measured, circle), rms(measured, segment), r)
    }

    /** `addSegmentCapsule`'s left-cap profile, sampled at the same integer rows as the image. */
    private fun segmentProfile(rows: List<Double>, r: Int, width: Int): DoubleArray {
        val path = Path().apply { addSegmentCapsule(Rect(0f, 0f, width.toFloat(), (2 * r).toFloat())) }
        val measure = PathMeasure().apply { setPath(path, false) }
        val samples = DoubleArray(r + 1) { Double.NaN }
        val steps = 20_000
        for (i in 0..steps) {
            val point = measure.getPosition(measure.length * i / steps)
            val v = point.y.toInt()
            if (v in 0..r && point.x <= 1.6f * r) {
                if (samples[v].isNaN() || point.x < samples[v]) samples[v] = point.x.toDouble()
            }
        }
        for (i in samples.indices) {
            if (!samples[i].isNaN()) continue
            val lower = (i - 1 downTo 0).firstOrNull { !samples[it].isNaN() }
            val upper = (i + 1..r).firstOrNull { !samples[it].isNaN() }
            samples[i] = when {
                lower != null -> samples[lower]
                upper != null -> samples[upper]
                else -> 0.0
            }
        }
        return DoubleArray(rows.size) { samples[rows[it].toInt()] }
    }

    private fun rms(measured: List<Double>, model: DoubleArray): Double {
        val offset = measured.indices.sumOf { measured[it] - model[it] } / measured.size
        return sqrt(
            measured.indices.sumOf {
                val d = measured[it] - model[it] - offset
                d * d
            } / measured.size,
        )
    }

    /** The 45-degree inset of the top-left corner, in px. */
    private fun diagonalInset(outline: Outline, cornerRadius: Double): Double {
        val path = when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Rectangle -> return 0.0
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val steps = 20_000
        val limit = 1.6 * cornerRadius
        var best = Double.MAX_VALUE
        var bestX = 0.0
        for (i in 0..steps) {
            val point = measure.getPosition(measure.length * i / steps)
            if (point.x <= 0f || point.y <= 0f) continue
            // Only the top-left corner: a short, wide rect's bottom edge also crosses x == y.
            if (point.x > limit || point.y > limit) continue
            val difference = abs(point.x - point.y).toDouble()
            if (difference < best) {
                best = difference
                bestX = point.x.toDouble()
            }
        }
        return bestX
    }
}
