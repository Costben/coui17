// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.LocalBlurEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val WIDTH_PX = 200
private const val HEIGHT_PX = 240

/** Stands in for the sheet's glass surface: a dark neutral, so any light veil would show up. */
private val PanelColor = Color(0xFF303030)

/** The first body child: a saturated colour distinguishable from the panel in every channel. */
private val FirstColor = Color(0xFFC00000)

/** The second body child. */
private val SecondColor = Color(0xFF0000C0)

/**
 * The scrollable sheet body must stack its children and, once scrolled, only fade its content into
 * the sheet's own surface. ColorOS 17 never blurs or tints scrolled-under panel content, and a
 * blurred copy of the body's top rows read as a bright grey bar smeared over the first child (a
 * title, a card's lit rim), so every pixel outside the fading edge must be the content itself.
 */
@OptIn(ExperimentalTestApi::class)
class BottomSheetScrollBlurTest {

    @Test
    fun multiChildContentStacksVertically() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(1f),
    ) {
        setContent { Scene(dark = false, scrollFraction = 0f) }
        waitForIdle()

        val first = onNodeWithTag("first").getBoundsInRoot()
        val second = onNodeWithTag("second").getBoundsInRoot()
        assertEquals(0.dp, first.top)
        assertEquals(first.bottom, second.top, "the second child must start below the first one")
    }

    @Test
    fun restingBodyIsUntouched() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(1f),
    ) {
        setContent { Scene(dark = false, scrollFraction = 0f) }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        assertClose(FirstColor, pixels[WIDTH_PX / 2, 0], "top edge at rest")
        assertClose(FirstColor, pixels[WIDTH_PX / 2, 20], "band at rest")
    }

    @Test
    fun scrolledBandNeverWashesOutInLightMode() = assertNoWash(dark = false)

    @Test
    fun scrolledBandNeverWashesOutInDarkMode() = assertNoWash(dark = true)

    private fun assertNoWash(dark: Boolean) = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(1f),
    ) {
        setContent { Scene(dark = dark, scrollFraction = 1f) }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        // The very top dissolves into the panel itself: the first pixel row is sampled half a pixel
        // into the linear fade, so it may keep at most one row's worth of the content.
        val fadeRows = BottomSheetDefaults.TopScrollFadeHeight.value
        val top = pixels[WIDTH_PX / 2, 0]
        assertTrue(
            kotlin.math.abs(top.red - PanelColor.red) <= kotlin.math.abs(FirstColor.red - PanelColor.red) / fadeRows + TOLERANCE &&
                kotlin.math.abs(top.green - PanelColor.green) <= TOLERANCE &&
                kotlin.math.abs(top.blue - PanelColor.blue) <= TOLERANCE,
            "top edge (dark=$dark): expected about $PanelColor, got $top",
        )
        // Inside the band the pixels are a mix of panel and content, never lighter than either:
        // a veil of any light colour would lift the green and blue channels above the panel.
        val bandPx = BottomSheetDefaults.TopScrollFadeHeight.value.roundToInt()
        for (y in 0 until bandPx) {
            val pixel = pixels[WIDTH_PX / 2, y]
            assertTrue(pixel.red <= maxOf(PanelColor.red, FirstColor.red) + TOLERANCE, "red at y=$y: $pixel")
            assertTrue(pixel.green <= PanelColor.green + TOLERANCE, "green at y=$y (dark=$dark): $pixel")
            assertTrue(pixel.blue <= PanelColor.blue + TOLERANCE, "blue at y=$y (dark=$dark): $pixel")
        }
        // Below the band the content is back at full strength.
        assertClose(FirstColor, pixels[WIDTH_PX / 2, bandPx + 4], "below the band (dark=$dark)")
    }

    @Test
    fun slightScrollNeverSmearsTheFirstChild() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(1f),
    ) {
        // A few pixels of scroll: the fade is only a quarter of its full length.
        setContent { Scene(dark = true, scrollFraction = 0.25f) }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        val fadePx = (BottomSheetDefaults.TopScrollFadeHeight.value * 0.25f).roundToInt()
        for (y in fadePx + 1 until 60) {
            assertClose(FirstColor, pixels[WIDTH_PX / 2, y], "first child at y=$y")
        }
    }

    @Test
    fun brightRowsAreNeverBlurredIntoABand() = assertNoSmear(dark = true)

    @Test
    fun brightRowsAreNeverBlurredIntoABandInLightMode() = assertNoSmear(dark = false)

    private fun assertNoSmear(dark: Boolean) = runSkikoComposeUiTest(
        size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()),
        density = Density(1f),
    ) {
        setContent { Scene(dark = dark, scrollFraction = 1f, rimRow = true) }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        val fadePx = BottomSheetDefaults.TopScrollFadeHeight.value.roundToInt()
        // The white rim row sits inside the fading edge. Without any blur its light never spreads:
        // the rows around it stay a plain mix of the panel and the red content.
        for (y in 0 until 60) {
            if (y in RimRows) continue
            val pixel = pixels[WIDTH_PX / 2, y]
            assertTrue(pixel.green <= PanelColor.green + TOLERANCE, "green at y=$y (dark=$dark): $pixel")
            assertTrue(pixel.blue <= PanelColor.blue + TOLERANCE, "blue at y=$y (dark=$dark): $pixel")
            if (y > fadePx) assertClose(FirstColor, pixel, "sharp content at y=$y (dark=$dark)")
        }
    }
}

/** A two-pixel-tall white row inside the fading edge, standing in for a card's lit rim or text. */
private val RimRows = 10..11

@Composable
private fun Scene(dark: Boolean, scrollFraction: Float, rimRow: Boolean = false) {
    COUITheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalBlurEnabled provides true) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(PanelColor),
            ) {
                BottomSheetScrollableBody(
                    topScrollBlur = true,
                    scrollFraction = { scrollFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                ) {
                    Box(
                        Modifier
                            .testTag("first")
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(FirstColor)
                            .drawBehind {
                                if (rimRow) {
                                    drawRect(
                                        color = Color.White,
                                        topLeft = Offset(0f, RimRows.first.toFloat()),
                                        size = Size(size.width, (RimRows.last - RimRows.first + 1).toFloat()),
                                    )
                                }
                            },
                    )
                    Box(
                        Modifier
                            .testTag("second")
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(SecondColor),
                    )
                }
            }
        }
    }
}

private const val TOLERANCE = 3f / 255f

private fun assertClose(expected: Color, actual: Color, label: String) {
    val close = kotlin.math.abs(expected.red - actual.red) <= TOLERANCE &&
        kotlin.math.abs(expected.green - actual.green) <= TOLERANCE &&
        kotlin.math.abs(expected.blue - actual.blue) <= TOLERANCE
    assertTrue(close, "$label: expected $expected, got $actual")
}
