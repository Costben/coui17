// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SECONDARY_SLIDER_TAG = "secondary-slider"

/**
 * The secondary progress layer added in 1.2.0. Its fraction math is normalized and clamped into
 * the value range, and the layer is painted behind the played track so only the part past the
 * thumb stays visible.
 */
@OptIn(ExperimentalTestApi::class)
class SliderSecondaryProgressTest {

    @Test
    fun fractionNormalizesAndClampsIntoTheValueRange() {
        assertEquals(0.5f, secondaryProgressFraction(0.5f, 0f..1f), 1e-6f)
        assertEquals(0f, secondaryProgressFraction(-1f, 0f..1f), 1e-6f)
        assertEquals(1f, secondaryProgressFraction(2f, 0f..1f), 1e-6f)
        assertEquals(0.75f, secondaryProgressFraction(150f, 0f..200f), 1e-6f)
        assertEquals(0.5f, secondaryProgressFraction(0f, -50f..50f), 1e-6f)
    }

    @Test
    fun theSecondaryLayerStaysVisiblePastThePrimaryValue() = runSkikoComposeUiTest(
        size = Size(240f, 60f),
        density = Density(1f),
    ) {
        setContent { SecondarySlider() }
        waitForIdle()

        val bounds = onNodeWithTag(SECONDARY_SLIDER_TAG).fetchSemanticsNode().boundsInRoot
        val pixels = onRoot().captureToImage().toPixelMap()
        val y = bounds.center.y.roundToInt()
        val primary = pixels[(bounds.left + bounds.width * 0.12f).roundToInt(), y]
        val secondary = pixels[(bounds.left + bounds.width * 0.45f).roundToInt(), y]
        val beyond = pixels[(bounds.left + bounds.width * 0.95f).roundToInt(), y]

        assertTrue(isRed(primary), "the played track must be red, got ${primary.toArgb().toString(16)}")
        assertTrue(isBlue(secondary), "the secondary tier must show past the thumb, got ${secondary.toArgb().toString(16)}")
        assertTrue(isWhite(beyond), "past the secondary end the background must show, got ${beyond.toArgb().toString(16)}")
    }
}

@Composable
private fun SecondarySlider() {
    COUITheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            Slider(
                value = 0.25f,
                onValueChange = {},
                secondaryProgress = 0.75f,
                colors = SliderDefaults.sliderColors(
                    foregroundColor = Color.Red,
                    backgroundColor = Color.Transparent,
                    thumbColor = Color.Transparent,
                    secondaryProgressColor = Color.Blue,
                ),
                modifier = Modifier.fillMaxWidth().testTag(SECONDARY_SLIDER_TAG),
            )
        }
    }
}

private fun isRed(color: Color): Boolean {
    val argb = color.toArgb()
    return (argb shr 16 and 0xFF) > 200 && (argb and 0xFF) < 60
}

private fun isBlue(color: Color): Boolean {
    val argb = color.toArgb()
    return (argb and 0xFF) > 200 && (argb shr 16 and 0xFF) < 60
}

private fun isWhite(color: Color): Boolean {
    val argb = color.toArgb()
    return (argb shr 16 and 0xFF) > 220 && (argb shr 8 and 0xFF) > 220 && (argb and 0xFF) > 220
}
