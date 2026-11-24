// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class, ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BannerLockEmptyListDetailTest {

    @Test
    fun bannerScaleCurveMatchesCouiScalePageTransformer() {
        assertEquals(1f, bannerPageScale(0f), 1e-4f)
        assertEquals(BANNER_MIN_PAGE_SCALE, bannerPageScale(-1f), 1e-4f)
        assertEquals(BANNER_MIN_PAGE_SCALE, bannerPageScale(1f), 1e-4f)
        assertEquals(BANNER_MIN_PAGE_SCALE, bannerPageScale(-2f), 1e-4f)
        assertEquals(BANNER_MIN_PAGE_SCALE, bannerPageScale(2f), 1e-4f)
        assertEquals(0.925f, bannerPageScale(0.5f), 1e-4f)
        assertEquals(0.925f, bannerPageScale(-0.5f), 1e-4f)

        assertEquals(0.5f, bannerPagePivotX(0f), 1e-4f)
        assertEquals(1f, bannerPagePivotX(-1f), 1e-4f)
        assertEquals(0f, bannerPagePivotX(1f), 1e-4f)
        assertEquals(1f, bannerPagePivotX(-2f), 1e-4f)
        assertEquals(0f, bannerPagePivotX(2f), 1e-4f)
    }

    @Test
    fun bannerCenterCardIsTallerThanPeekingSideCards() = runSkikoComposeUiTest(
        size = Size(360f, 180f),
        density = Density(1f),
    ) {
        val centerColor = Color(0xFF0000FF)
        val sideColor = Color(0xFFFF0000)
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    val state = rememberPagerState(initialPage = 1, pageCount = { 3 })
                    Banner(
                        pageCount = 3,
                        autoScrollIntervalMillis = 0L,
                        autoScrollEnabled = false,
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        itemColor = sideColor,
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (page == 1) centerColor else sideColor),
                        )
                    }
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val centerHeight = columnSpanHeight(pixels, x = 180) { it.blue > 0.7f && it.red < 0.3f }
        val leftHeight = columnSpanHeight(pixels, x = 6) { it.red > 0.7f && it.blue < 0.3f }
        val rightHeight = columnSpanHeight(pixels, x = 353) { it.red > 0.7f && it.blue < 0.3f }

        assertTrue(centerHeight >= 150, "center card should span near full 160px height, was $centerHeight")
        assertTrue(leftHeight in 120 until centerHeight - 10, "left peeking card should be scaled smaller than center: left=$leftHeight center=$centerHeight")
        assertTrue(rightHeight in 120 until centerHeight - 10, "right peeking card should be scaled smaller than center: right=$rightHeight center=$centerHeight")
        assertTrue(abs(leftHeight - rightHeight) <= 2, "left ($leftHeight) and right ($rightHeight) peeking cards should match symmetrically")
    }

    @Test
    fun lockScreenPwdInputFollowsSegmentCapsule() = runSkikoComposeUiTest(
        size = Size(1200f, 360f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    LockScreenPwdInput(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.width(240.dp),
                        scene = LockScreenPwdInputScene.Desktop,
                        nextIcon = null,
                        colors = LockScreenPwdInputDefaults.colors(LockScreenPwdInputScene.Desktop).copy(
                            containerColor = Color.Black,
                            borderColor = Color.Transparent,
                            upperInnerShadowColor = Color.Transparent,
                            lowerInnerShadowColor = Color.Transparent,
                        ),
                    )
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        assertTrue(
            fit.rmsSegment < fit.rmsCircle,
            "password field must follow the continuous-curvature segment capsule: segment=${fit.rmsSegment} circle=${fit.rmsCircle}",
        )
        assertTrue(fit.rmsSegment < 1.5, "segment residual ${fit.rmsSegment}px is too large")
    }

    @Test
    fun lockScreenPwdInputDesktopInnerShadowWrapsAroundCapsuleWithoutTopStripe() = runSkikoComposeUiTest(
        size = Size(840f, 240f),
        density = Density(3f),
    ) {
        val backdrop = Color(0xFF1B1B1B)
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backdrop),
                    contentAlignment = Alignment.Center,
                ) {
                    LockScreenPwdInput(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.width(240.dp),
                        scene = LockScreenPwdInputScene.Desktop,
                        nextIcon = null,
                    )
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        // Pill is 720x144px centred inside 840x240px: left=60, right=780, top=48, bottom=192.
        val centerX = 420
        val centerY = 120
        val topInnerLum = pixels[centerX, 50].luminance()
        val centerLum = pixels[centerX, centerY].luminance()
        val bottomInnerLum = pixels[centerX, 189].luminance()
        val leftTipInnerLum = pixels[64, centerY].luminance()
        val outsideCornerLum = pixels[61, 49].luminance()

        // Pixels outside the continuous-curvature capsule corner must stay pure backdrop.
        assertEquals(backdrop.luminance(), outsideCornerLum, 0.005f)
        // Desktop glass pill uses a transparent background (matching COUILockScreenPwdInputLayout
        // when blur/effects are active), so the center stays close to the backdrop rather than
        // stacking a bright #33FFFFFF slab underneath the inner shadow.
        assertTrue(
            centerLum < backdrop.luminance() + 0.03f,
            "desktop pill center should stay transparent over backdrop: center=$centerLum backdrop=${backdrop.luminance()}",
        )
        // Blurred stroke inner shadow must wrap around the curved left tip, not vanish like a 1D
        // vertical gradient.
        assertTrue(
            leftTipInnerLum > centerLum + 0.01f,
            "curved end tip should receive wrap-around inner shadow glow: tip=$leftTipInnerLum center=$centerLum",
        )
        // Top inner edge must be a soft diffused glass rim rather than a harsh white bar.
        assertTrue(
            topInnerLum > centerLum && topInnerLum < 0.20f,
            "top inner glow should be soft and diffused, not a harsh white bar: top=$topInnerLum center=$centerLum",
        )
        // Bottom inner edge receives the dy=-8 soft diffuse glow without an abrupt bright shelf.
        assertTrue(
            bottomInnerLum > centerLum && bottomInnerLum < 0.20f,
            "bottom inner glow should be soft and diffused: bottom=$bottomInnerLum center=$centerLum",
        )
    }

    @Test
    fun lockScreenPwdInputDesktopNextPressEmitsOutwardLightClippedToPill() = runSkikoComposeUiTest(
        size = Size(900f, 240f),
        density = Density(3f),
    ) {
        val backdrop = Color(0xFF1B1B1B)
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backdrop),
                    contentAlignment = Alignment.Center,
                ) {
                    LockScreenPwdInput(
                        value = "123456",
                        onValueChange = {},
                        modifier = Modifier.width(260.dp),
                        scene = LockScreenPwdInputScene.Desktop,
                        minCount = 4,
                    )
                }
            }
        }
        waitForIdle()
        val before = onRoot().captureToImage().toPixelMap()
        // Row is 780x144px centred inside 900x240px: left=60, top=48, bottom=192.
        // Next button is 144px wide at x=696..840; gap is 36px at x=660..696; pill is x=60..660.
        val pillRightInnerX = 635
        val centerY = 120
        val gapX = 678
        val beforeRightLum = before[pillRightInnerX, centerY].luminance()

        mainClock.autoAdvance = false
        onNodeWithContentDescription(LockScreenPwdInputDefaults.NextDescription).performTouchInput {
            down(center)
        }
        mainClock.advanceTimeBy(180L)
        waitForIdle()
        val duringPress = onRoot().captureToImage().toPixelMap()
        val pressedRightLum = duringPress[pillRightInnerX, centerY].luminance()
        val gapLum = duringPress[gapX, centerY].luminance()

        assertTrue(
            pressedRightLum > beforeRightLum + 0.05f,
            "pressing next button should sweep outward light across the right end of the pill: before=$beforeRightLum during=$pressedRightLum",
        )
        assertEquals(
            backdrop.luminance(),
            gapLum,
            0.005f,
            "outward light sweep must stay clipped inside the pill capsule and never bleed into the gap",
        )
    }

    @Test
    fun lockScreenPwdInputSettingHasFlatCardFillWithoutInnerShadow() = runSkikoComposeUiTest(
        size = Size(840f, 240f),
        density = Density(3f),
    ) {
        setContent {
            COUITheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF2F2F2)),
                    contentAlignment = Alignment.Center,
                ) {
                    LockScreenPwdInput(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.width(240.dp),
                        scene = LockScreenPwdInputScene.Setting,
                        nextIcon = null,
                    )
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val centerLum = pixels[420, 120].luminance()
        val topInnerLum = pixels[420, 52].luminance()
        val bottomInnerLum = pixels[420, 188].luminance()
        val leftTipInnerLum = pixels[66, 120].luminance()

        assertEquals(1f, centerLum, 0.01f)
        assertEquals(centerLum, topInnerLum, 0.01f)
        assertEquals(centerLum, bottomInnerLum, 0.01f)
        assertEquals(centerLum, leftTipInnerLum, 0.01f)
    }

    @Test
    fun emptyStateUnboundedHeightIncludesVerticalPadding() = runSkikoComposeUiTest(
        size = Size(360f, 300f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Box(modifier = Modifier.fillMaxWidth()) {
                    EmptyState(
                        title = "No results",
                        subtitle = "Try a different keyword",
                    )
                }
            }
        }
        waitForIdle()
        val titleBounds = onNodeWithText("No results").getUnclippedBoundsInRoot()
        assertTrue(
            titleBounds.top >= 20.dp,
            "unbounded EmptyState must include vertical padding instead of flush top edge: top=${titleBounds.top}",
        )
    }

    @Test
    fun listDetailViewSwitchesBetweenSinglePaneAndSplitMode() = runSkikoComposeUiTest(
        size = Size(700f, 300f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Box(modifier = Modifier.width(360.dp).height(240.dp)) {
                    ListDetailView(
                        mainContent = { Text("CompactList") },
                        detailContent = { Text("CompactDetail") },
                        detailVisible = false,
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText("CompactList").assertIsDisplayed()
        onNodeWithText("CompactDetail").assertDoesNotExist()
    }

    @Test
    fun listDetailViewSplitModeShowsBothPanesSideBySide() = runSkikoComposeUiTest(
        size = Size(700f, 300f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Box(modifier = Modifier.width(640.dp).height(240.dp)) {
                    ListDetailView(
                        mainContent = { Text("SplitList") },
                        detailContent = { Text("SplitDetail") },
                        emptyPageContent = { Text("SplitEmpty") },
                        detailVisible = false,
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText("SplitList").assertIsDisplayed()
        onNodeWithText("SplitEmpty").assertIsDisplayed()
        onNodeWithText("SplitDetail").assertDoesNotExist()
    }

    private fun columnSpanHeight(pixels: PixelMap, x: Int, predicate: (Color) -> Boolean): Int {
        var top = Int.MAX_VALUE
        var bottom = -1
        for (y in 0 until pixels.height) {
            if (predicate(pixels[x, y])) {
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        return if (bottom >= top) bottom - top + 1 else 0
    }

    private data class CornerFit(val rmsCircle: Double, val rmsSegment: Double)

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
        return CornerFit(rms(measured, circle), rms(measured, segment))
    }

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
}
