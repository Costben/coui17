// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

private const val OVERLAY_TAG = "app-bar-blur-overlay"

private const val SURFACE_WIDTH_DP = 240f
private const val SURFACE_HEIGHT_DP = 200f

/** The bar the overlay is anchored to: the material's ramp reaches alpha 0 on its bottom edge. */
private const val BAR_HEIGHT_DP = 80f

/** The flat layer the overlay samples, and therefore the colour its clear band must fall back to. */
private val BackdropColor = Color(0xFF0000FF)

/** The app bar content laid over the material; distinguishable from both surface and backdrop. */
private val ContentColor = Color(0xFFFF00FF)

private val TestDensity = Density(1f)

/** The node the caller sizes: the bar plus the extra band [AppBarBlurOverlay] paints past it. */
private val OverlayHeight = BAR_HEIGHT_DP.dp + AppBarBlurOverlayDefaults.ExtraHeight

private val ExtraHeightPx = with(TestDensity) { AppBarBlurOverlayDefaults.ExtraHeight.toPx() }

/**
 * The overlay's material ramps its alpha from full strength at the node's top edge down to nothing
 * at the bar's bottom edge, and stays transparent over the extra band the caller added below the
 * bar. The surface colour is white and the flat backdrop under the sampled column is blue, so the
 * red channel *is* the ramp: 255 where the material is opaque, 0 where the ramp has run out.
 */
@OptIn(ExperimentalTestApi::class)
class AppBarBlurOverlayRenderTest {

    @Test
    fun overlayRampsDownToItsBottomBandAndLeavesItClear() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = TestDensity,
    ) {
        setContent { OverlayScene(scrollFraction = 1f, withBackdrop = true) }
        waitForIdle()

        val overlay = overlayBounds()
        val pixels = onRoot().captureToImage().toPixelMap()
        val x = pixels.width / 4

        val top = pixels[x, overlay.top.roundToInt() + 2]
        assertTrue(redChannel(top) >= 200, "the node's top edge is the painted surface, got $top")

        val lastRed = assertRampDoesNotClimb(pixels, x, overlay)
        assertTrue(lastRed <= 4, "the ramp must run out by the node's bottom edge, got $lastRed")

        val bandTop = (overlay.bottom - ExtraHeightPx).roundToInt() + 2
        for (y in bandTop until overlay.bottom.roundToInt()) {
            val pixel = pixels[x, y]
            assertTrue(
                maxChannelDelta(pixel, BackdropColor) <= 2,
                "the extra band must show the raw backdrop: y=$y got ${pixel.argbHex()}",
            )
        }
    }

    /**
     * Control for [overlayRampsDownToItsBottomBandAndLeavesItClear]: with no [LocalBackdrop] the
     * overlay paints the ramp alone. The ramp is a property of the surface drawable rather than of
     * the blur, so it has to survive the fallback path unchanged.
     */
    @Test
    fun withoutABackdropTheOverlayPaintsTheSameRamp() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = TestDensity,
    ) {
        setContent { OverlayScene(scrollFraction = 1f, withBackdrop = false) }
        waitForIdle()

        val overlay = overlayBounds()
        val pixels = onRoot().captureToImage().toPixelMap()
        val x = pixels.width / 4

        val top = pixels[x, overlay.top.roundToInt() + 2]
        assertTrue(redChannel(top) >= 200, "the node's top edge is the painted surface, got $top")

        val lastRed = assertRampDoesNotClimb(pixels, x, overlay)
        assertTrue(lastRed <= 4, "the ramp must run out by the node's bottom edge, got $lastRed")

        val bandTop = (overlay.bottom - ExtraHeightPx).roundToInt() + 2
        for (y in bandTop until overlay.bottom.roundToInt()) {
            val pixel = pixels[x, y]
            assertTrue(
                maxChannelDelta(pixel, BackdropColor) <= 2,
                "the extra band must stay transparent: y=$y got ${pixel.argbHex()}",
            )
        }
    }

    /**
     * `scrollDrivenAlphaEnabled`: at zero scroll the native overlay's alpha is 0, which makes the
     * whole view invisible. Nothing may be drawn over the backdrop across the entire node.
     */
    @Test
    fun zeroScrollFractionDrawsNothing() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = TestDensity,
    ) {
        setContent { OverlayScene(scrollFraction = 0f, withBackdrop = true) }
        waitForIdle()

        val overlay = overlayBounds()
        val pixels = onRoot().captureToImage().toPixelMap()

        var sampled = 0
        for (y in overlay.top.roundToInt() + 1 until overlay.bottom.roundToInt() step 3) {
            for (x in overlay.left.roundToInt() + 1 until overlay.right.roundToInt() step 4) {
                val pixel = pixels[x, y]
                assertTrue(
                    maxChannelDelta(pixel, BackdropColor) <= 2,
                    "at alpha 0 nothing may be drawn: ($x, $y) got ${pixel.argbHex()}",
                )
                sampled++
            }
        }
        assertTrue(sampled > 100, "the sampled grid must cover the node, sampled=$sampled")
    }

    /**
     * The material's scroll alpha belongs to the material, not to the app bar. `AppBarBlurOverlay`
     * is the hollowed-out bar's surface, so the content the caller lays on it — the title and the
     * actions — must stay readable at rest, when the material has not faded in yet.
     */
    @Test
    fun contentStaysVisibleAtZeroScroll() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = TestDensity,
    ) {
        setContent {
            val backdrop = rememberLayerBackdrop {
                drawRect(Color.Black)
                drawContent()
            }
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Canvas(Modifier.fillMaxSize()) { drawRect(BackdropColor) }
                }
                CompositionLocalProvider(LocalBackdrop provides backdrop) {
                    AppBarBlurOverlay(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(OverlayHeight)
                            .testTag(OVERLAY_TAG),
                        containerColor = Color.White,
                        scrollFraction = 0f,
                    ) {
                        Canvas(Modifier.fillMaxSize()) { drawRect(ContentColor) }
                    }
                }
            }
        }
        waitForIdle()

        val overlay = overlayBounds()
        val pixel = onRoot().captureToImage().toPixelMap()[
            overlay.left.roundToInt() + 4,
            overlay.top.roundToInt() + 4,
        ]
        assertTrue(
            maxChannelDelta(pixel, ContentColor) <= 2,
            "the content must not ride the material's alpha: got ${pixel.argbHex()}",
        )
    }
}

/**
 * A flat [BackdropColor] layer recorded as the backdrop, with the overlay pinned across the top of
 * the surface. The overlay's material fades to alpha 0 over its bottom band, so whatever shows
 * there is the raw backdrop.
 */
@Composable
private fun OverlayScene(scrollFraction: Float, withBackdrop: Boolean) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            Canvas(Modifier.fillMaxSize()) { drawRect(BackdropColor) }
        }
        val overlay = Modifier
            .fillMaxWidth()
            .height(OverlayHeight)
            .testTag(OVERLAY_TAG)
        if (withBackdrop) {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                AppBarBlurOverlay(
                    modifier = overlay,
                    containerColor = Color.White,
                    scrollFraction = scrollFraction,
                )
            }
        } else {
            AppBarBlurOverlay(
                modifier = overlay,
                containerColor = Color.White,
                backdrop = null,
                scrollFraction = scrollFraction,
            )
        }
    }
}

/** The overlay node's bounds in the captured image. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.overlayBounds(): Rect = onNodeWithTag(OVERLAY_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

private fun Color.argbHex(): String = "#%08X".format(toArgb())

/**
 * Walks the sampled column from the node's top edge down to its bottom edge, asserting the alpha
 * ramp never climbs, and returns the red channel of the last row.
 *
 * The material's stops are piecewise linear and the backdrop under the column is flat, so the only
 * thing that may vary down the column is the ramp; the one-unit slack absorbs compositing rounding.
 */
private fun assertRampDoesNotClimb(pixels: PixelMap, x: Int, overlay: Rect): Int {
    val firstY = overlay.top.roundToInt() + 1
    var previousRed = redChannel(pixels[x, firstY])
    for (y in firstY + 1 until overlay.bottom.roundToInt()) {
        val red = redChannel(pixels[x, y])
        assertTrue(
            red <= previousRed + 1,
            "the ramp must not climb: y=$y red=$red follows red=$previousRed at x=$x",
        )
        previousRed = red
    }
    return previousRed
}

private fun redChannel(color: Color): Int = color.toArgb() shr 16 and 0xFF

/** Largest per-channel distance between two pixels, alpha excluded. */
private fun maxChannelDelta(a: Color, b: Color): Int {
    val aArgb = a.toArgb()
    val bArgb = b.toArgb()
    return maxOf(
        abs((aArgb shr 16 and 0xFF) - (bArgb shr 16 and 0xFF)),
        abs((aArgb shr 8 and 0xFF) - (bArgb shr 8 and 0xFF)),
        abs((aArgb and 0xFF) - (bArgb and 0xFF)),
    )
}
