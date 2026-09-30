// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private const val BUTTON_TAG = "highlighted-icon-button"

/** Room for the 40dp button and the light theme's halo, which reaches 15.5dp past its edge. */
private val CanvasSize = Size(160f, 160f)

/**
 * A saturated fill the button's translucent disc stands out against. The default (light) scheme
 * paints #f9f9f9 at 78% over it, leaving a red channel of ≈194 where the disc is and 0 where it is
 * not — the halo included, since it only darkens the blue it is drawn over.
 */
private val BackgroundColor = Color(0xFF0000FF)

/**
 * The press feedback of [HighlightedIconButton] is a scale on the whole node, so it is read straight
 * off the rendered disc: the fill's width across the button's centre line shrinks by the pressed
 * ratio and comes back to its resting width once the pointer is lifted.
 */
@OptIn(ExperimentalTestApi::class)
class HighlightedIconButtonTest {

    @Test
    fun theNodeScalesDownWhilePressedAndReturnsToFullSizeWhenReleased() = runSkikoComposeUiTest(
        size = CanvasSize,
        density = Density(1f),
    ) {
        setContent { Scene() }
        waitForIdle()

        val rest = fillWidthPx()
        assertTrue(rest > 0, "the button must be drawn at rest")

        onNodeWithTag(BUTTON_TAG).performTouchInput { down(center) }
        waitForIdle()
        val pressed = fillWidthPx()
        assertTrue(
            pressed <= rest - 2,
            "the pressed control scales to ${HighlightedIconButtonDefaults.PressedScale}, so the " +
                "disc must narrow: rest=$rest px, pressed=$pressed px",
        )

        onNodeWithTag(BUTTON_TAG).performTouchInput { up() }
        waitForIdle()
        val released = fillWidthPx()
        assertTrue(
            abs(released - rest) <= 1,
            "the released control must spring back to 1f: rest=$rest px, released=$released px",
        )
    }
}

@Composable
private fun Scene() {
    Box(
        Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        HighlightedIconButton(
            onClick = {},
            modifier = Modifier.align(Alignment.Center).testTag(BUTTON_TAG),
        ) {}
    }
}

/** The width of the run of fill pixels across the button's horizontal centre line, in pixels. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.fillWidthPx(): Int {
    val image = onRoot().captureToImage()
    val pixels = image.toPixelMap()
    val row = image.height / 2
    var first = -1
    var last = -1
    for (x in 0 until image.width) {
        if (isFill(pixels[x, row])) {
            if (first < 0) first = x
            last = x
        }
    }
    val width = if (first < 0) 0 else last - first + 1
    println("[highlightedIconButton] row=$row of ${image.width}x${image.height} fillWidth=$width")
    return width
}

/** True for the button's translucent near-white disc; nothing else on the scene lifts red this far. */
private fun isFill(color: Color): Boolean = (color.toArgb() shr 16 and 0xFF) > 100
