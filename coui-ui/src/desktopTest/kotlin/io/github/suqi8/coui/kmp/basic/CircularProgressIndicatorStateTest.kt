// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

private const val CIRCULAR_STATE_TAG = "circular-state"

/**
 * The `Error` / `Paused` states added to [CircularProgressIndicator] in 1.2.0: the error state
 * paints a filled error dot with an on-error glyph, the paused state a full accent ring with a
 * pause glyph. Both are static, so a single rendered frame is enough to read them.
 */
@OptIn(ExperimentalTestApi::class)
class CircularProgressIndicatorStateTest {

    @Test
    fun errorStateDrawsAFilledErrorDotWithAnOnErrorGlyph() = runSkikoComposeUiTest(
        size = Size(80f, 80f),
        density = Density(1f),
    ) {
        setContent { ErrorScene() }
        waitForIdle()

        val bounds = onNodeWithTag(CIRCULAR_STATE_TAG).fetchSemanticsNode().boundsInRoot
        val pixels = onRoot().captureToImage().toPixelMap()
        val centerX = bounds.center.x.roundToInt()
        val centerY = bounds.center.y.roundToInt()

        val glyph = pixels[centerX, centerY]
        val disc = pixels[centerX + bounds.width.roundToInt() / 5, centerY]

        assertTrue(isWhite(glyph), "the exclamation glyph must cover the center, got ${glyph.toArgb().toString(16)}")
        assertTrue(isRed(disc), "the error dot must fill around the glyph, got ${disc.toArgb().toString(16)}")
    }

    @Test
    fun pausedStateDrawsAFullRingAndAPauseGlyph() = runSkikoComposeUiTest(
        size = Size(80f, 80f),
        density = Density(1f),
    ) {
        setContent { PausedScene() }
        waitForIdle()

        val bounds = onNodeWithTag(CIRCULAR_STATE_TAG).fetchSemanticsNode().boundsInRoot
        val pixels = onRoot().captureToImage().toPixelMap()
        val centerX = bounds.center.x.roundToInt()
        val centerY = bounds.center.y.roundToInt()

        val center = pixels[centerX, centerY]
        val bar = pixels[centerX + 3, centerY]
        val ring = pixels[centerX + 13, centerY]

        assertTrue(isWhite(center), "the middle of the paused indicator stays empty, got ${center.toArgb().toString(16)}")
        assertTrue(isRed(bar), "the pause glyph must sit right of the center, got ${bar.toArgb().toString(16)}")
        assertTrue(isRed(ring), "the static ring must sit at the outer radius, got ${ring.toArgb().toString(16)}")
    }
}

@Composable
private fun ErrorScene() {
    COUITheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            CircularProgressIndicator(
                state = CircularProgressState.Error,
                colors = ProgressIndicatorDefaults.circularProgressIndicatorColors(
                    errorColor = Color.Red,
                    onErrorColor = Color.White,
                    backgroundColor = Color.Transparent,
                ),
                modifier = Modifier.testTag(CIRCULAR_STATE_TAG),
            )
        }
    }
}

@Composable
private fun PausedScene() {
    COUITheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            CircularProgressIndicator(
                state = CircularProgressState.Paused,
                colors = ProgressIndicatorDefaults.circularProgressIndicatorColors(
                    foregroundColor = Color.Red,
                    backgroundColor = Color.Transparent,
                ),
                modifier = Modifier.testTag(CIRCULAR_STATE_TAG),
            )
        }
    }
}

private fun isRed(color: Color): Boolean {
    val argb = color.toArgb()
    return (argb shr 16 and 0xFF) > 200 && (argb and 0xFF) < 60
}

private fun isWhite(color: Color): Boolean {
    val argb = color.toArgb()
    return (argb shr 16 and 0xFF) > 220 && (argb shr 8 and 0xFF) > 220 && (argb and 0xFF) > 220
}
