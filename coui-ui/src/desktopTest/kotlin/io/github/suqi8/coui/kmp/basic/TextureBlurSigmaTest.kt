// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

private const val SIDE = 400

/**
 * Pins what `textureBlur`'s `blurRadius` actually delivers: a Gaussian of `0.45 x radius`
 * ([io.github.suqi8.coui.kmp.basic.TEXTURE_BLUR_SIGMA_SCALE]).
 *
 * Every caller that converts a platform token into an engine radius — the app bar's 30px HWUI
 * radius, the popup's compositor level — relies on that slope, so it is measured here on a step
 * edge rather than assumed. The 10-90% width of a Gaussian-blurred step is `2.563 sigma`.
 */
@OptIn(ExperimentalTestApi::class)
class TextureBlurSigmaTest {

    /** Measured sigma, in px, of a vertical white/black step blurred with [radiusPx]. */
    private fun sigmaFor(radiusPx: Float): Float {
        var sigma = Float.NaN
        runSkikoComposeUiTest(size = Size(SIDE.toFloat(), SIDE.toFloat()), density = Density(1f)) {
            setContent {
                val backdrop = rememberLayerBackdrop { drawContent() }
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(Color.White, size = Size(SIDE / 2f, SIDE.toFloat()))
                            drawRect(Color.Black, topLeft = Offset(SIDE / 2f, 0f), size = Size(SIDE / 2f, SIDE.toFloat()))
                        }
                    }
                    CompositionLocalProvider(LocalBackdrop provides backdrop) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .textureBlur(
                                    backdrop = backdrop,
                                    shape = RectangleShape,
                                    blurRadius = radiusPx,
                                ),
                        )
                    }
                }
            }
            waitForIdle()
            val pixels = onRoot().captureToImage().toPixelMap()
            val row = SIDE / 2
            val profile = FloatArray(SIDE) { x -> pixels[x, row].red }

            /** x where the profile crosses [level] on its way from white to black. */
            fun crossing(level: Float): Float {
                for (x in 1 until SIDE) {
                    if (profile[x - 1] >= level && profile[x] < level) {
                        val t = (profile[x - 1] - level) / (profile[x - 1] - profile[x])
                        return (x - 1) + t
                    }
                }
                return Float.NaN
            }

            sigma = (crossing(0.1f) - crossing(0.9f)) / 2.563f
        }
        return sigma
    }

    @Test
    fun engineDeliversTheNominalSigma() {
        // The three radii the library's callers convert: a small control radius, the popup's
        // converted compositor level, and a HWUI-scale radius.
        for (radius in listOf(20f, 55.56f, 160f)) {
            val expected = radius * 0.45f
            val measured = sigmaFor(radius)
            assertTrue(
                abs(measured - expected) / expected < 0.12f,
                "radius $radius: expected sigma $expected, measured $measured",
            )
        }
    }
}
