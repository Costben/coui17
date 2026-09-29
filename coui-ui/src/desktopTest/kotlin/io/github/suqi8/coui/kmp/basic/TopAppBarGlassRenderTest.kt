// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val APP_BAR_TAG = "glass-app-bar"

/** Short enough that the title glyphs stop well before the left sample column. */
private const val APP_BAR_TITLE = "G"

private const val SURFACE_WIDTH_DP = 240f
private const val SURFACE_HEIGHT_DP = 120f

/** The recorded backdrop is split down the middle: blue on the left half, red on the right half. */
private val BackdropLeftColor = Color(0xFF0000FF)
private val BackdropRightColor = Color(0xFFFF0000)

/**
 * An opaque flat cover drawn on top of the backdrop layer and below the app bar. Whatever the bar
 * shows over it either came from the backdrop or is flat — the cover itself carries no split.
 */
private val CoverColor = Color(0xFF00FF00)

/**
 * The app bar paints its `color` on its own when no backdrop is published, and samples the backdrop
 * (blurred, tinted by that same `color` at the ColorOS background alpha) when one is. A flat cover
 * sits between the recorded layer and the bar, so a left/right difference inside the bar can only
 * come from the sampling path: a bar that failed to sample, sampled an empty layer, or simply
 * composited its own translucent fill over the cover would come out uniform.
 */
@OptIn(ExperimentalTestApi::class)
class TopAppBarGlassRenderTest {

    @Test
    fun appBarSamplesTheLayerBehindItInsteadOfPaintingAFlatColor() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { GlassScene(withBackdrop = true) }
        waitForIdle()

        val (left, right) = captureAppBarSamples()

        assertNotEquals(left, right, "the bar must pick up the blue/red split of the backdrop")
        assertTrue(
            maxChannelDelta(left, right) >= 8,
            "left/right must differ on some channel by at least 8, got $left vs $right",
        )
        assertNotEquals(Color.White.toArgb(), left.toArgb(), "the left sample must not be flat white")
        assertNotEquals(Color.White.toArgb(), right.toArgb(), "the right sample must not be flat white")
    }

    /**
     * Control: with no [LocalBackdrop] the bar is expected to fall back to its plain fill, which
     * makes the assertions above meaningful — the sampled row really is inside the bar.
     */
    @Test
    fun withoutABackdropTheAppBarPaintsOneFlatColor() = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = Density(1f),
    ) {
        setContent { GlassScene(withBackdrop = false) }
        waitForIdle()

        val (left, right) = captureAppBarSamples()

        assertEquals(left.toArgb(), right.toArgb(), "the plain fill must be uniform across the bar")
        assertTrue(
            channelDistanceToWhite(left) <= 2,
            "the plain fill is expected to be white, got $left",
        )
    }
}

@Composable
private fun GlassScene(withBackdrop: Boolean) {
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
            Canvas(Modifier.fillMaxSize()) {
                val half = size.width / 2f
                drawRect(BackdropLeftColor, size = Size(half, size.height))
                drawRect(
                    color = BackdropRightColor,
                    topLeft = Offset(half, 0f),
                    size = Size(size.width - half, size.height),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(CoverColor),
        )
        if (withBackdrop) {
            CompositionLocalProvider(LocalBackdrop provides backdrop) { GlassAppBar() }
        } else {
            GlassAppBar()
        }
    }
}

@Composable
private fun GlassAppBar() {
    SmallTopAppBar(
        title = APP_BAR_TITLE,
        modifier = Modifier.fillMaxWidth().testTag(APP_BAR_TAG),
        color = Color.White,
        defaultWindowInsetsPadding = false,
    )
}

/**
 * Reads the two comparison pixels: the same row across the middle of the bar, at a quarter and
 * three quarters of the surface width.
 */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.captureAppBarSamples(): Pair<Color, Color> {
    val bar = onNodeWithTag(APP_BAR_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val image = onRoot().captureToImage()
    val pixels = image.toPixelMap()
    val sampleY = ((bar.top + bar.bottom) / 2f).roundToInt()
    val leftX = image.width / 4
    val rightX = image.width * 3 / 4
    val left = pixels[leftX, sampleY]
    val right = pixels[rightX, sampleY]
    println(
        "[glass] surface=${image.width}x${image.height} bar=$bar y=$sampleY " +
            "left($leftX)=${left.argbHex()} right($rightX)=${right.argbHex()}",
    )
    assertTrue(
        bar.top <= sampleY && sampleY < bar.bottom && leftX >= bar.left && rightX <= bar.right,
        "the sample points must sit inside the bar, bar=$bar y=$sampleY left=$leftX right=$rightX",
    )
    return left to right
}

private fun Color.argbHex(): String = "#%08X".format(toArgb())

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

private fun channelDistanceToWhite(color: Color): Int {
    val argb = color.toArgb()
    return maxOf(
        255 - (argb shr 16 and 0xFF),
        255 - (argb shr 8 and 0xFF),
        255 - (argb and 0xFF),
    )
}
