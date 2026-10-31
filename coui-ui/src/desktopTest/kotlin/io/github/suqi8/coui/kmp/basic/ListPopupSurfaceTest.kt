// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.BezierGravityPathGeometry
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A popup that is not on the blur path must be an opaque panel with no material rim.
 *
 * `RoundFrameLayout.updateMaterialEffectsState()` clears `mShadowEdgeEnabled` and `mSpotLightEnabled`
 * as soon as `useBackgroundBlur()` is false, and `onDraw` then skips both drawables. The flat
 * fallback is `?couiColorBackgroundTop`, `#FFFFFF` / `#333333` with no alpha — so an opaque fill and
 * no rim, not a translucent panel wearing a white gradient edge.
 */
@OptIn(ExperimentalTestApi::class)
class ListPopupSurfaceTest {

    @Test
    fun withoutABackdropThePanelIsOpaqueAndHasNoRim() {
        runSkikoComposeUiTest(size = Size(WIDTH, HEIGHT), density = Density(1f)) {
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF9A9A9A)))
                        ListPopupContent(
                            enableGlass = false,
                            popupContentSize = IntSize(168, 88),
                            onPopupContentSizeChange = {},
                            fractionProgress = { 1f },
                            alphaProgress = { 1f },
                            pathProgress = { 0f },
                            gravityPath = ZERO_PATH,
                            popupLayoutPosition = BELOW_LEFT,
                            localTransformOrigin = TransformOrigin.Center,
                        ) {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
            waitForIdle()

            val pixels = onRoot().captureToImage().toPixelMap()
            val centreX = pixels.width / 2
            val centreY = pixels.height / 2
            val fill = pixels[centreX, centreY]

            // `coui_color_background_top` = #FFFFFF with alpha FF.
            assertEquals(1f, fill.alpha, 0.01f, "the flat popup fill is opaque")
            assertEquals(255, (fill.red * 255).toInt(), "and it is the full #FFFFFF")

            // No rim: the panel's top edge is the same colour as its interior, not brighter.
            val column = (centreY - 40..centreY).map { y -> (pixels[centreX, y].red * 255).toInt() }
            println("TOP EDGE COLUMN $column")
            assertTrue(
                column.max() - column.min() <= 1,
                "the flat popup must not paint a gradient rim: $column",
            )
        }
    }

    @Test
    fun withABackdropThePanelStaysTranslucent() {
        runSkikoComposeUiTest(size = Size(WIDTH, HEIGHT), density = Density(1f)) {
            setContent {
                COUITheme {
                    val backdrop = rememberLayerBackdrop { drawContent() }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.White))
                        CompositionLocalProvider(LocalBackdrop provides backdrop) {
                            ListPopupContent(
                                enableGlass = true,
                                popupContentSize = IntSize(168, 88),
                                onPopupContentSizeChange = {},
                                fractionProgress = { 1f },
                                alphaProgress = { 1f },
                                pathProgress = { 0f },
                                gravityPath = ZERO_PATH,
                                popupLayoutPosition = BELOW_LEFT,
                                localTransformOrigin = TransformOrigin.Center,
                            ) {
                                Box(Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
            waitForIdle()

            val pixels = onRoot().captureToImage().toPixelMap()
            val fill = (pixels[pixels.width / 2, pixels.height / 2].red * 255).toInt()
            println("GLASS FILL $fill")

            // A composed frame is always opaque, so translucency shows up as the page bleeding
            // through: `POPUP_GLASS_SURFACE_ALPHA` over a white page lands below the flat #FFFFFF
            // the solid path paints, but nowhere near the page itself.
            assertTrue(fill < 250, "the glass fill lets the page through: $fill")
            assertTrue(fill > 200, "but it still covers most of it: $fill")
        }
    }

    /**
     * A one-row menu is 56dp tall and carries COUI's fixed 28dp radius, so its corners are exactly
     * at the capsule radius. ColorOS keeps the n ≈ 2.5 superellipse there (measured on the PLK110:
     * a 168 x 56dp Settings overflow panel), it does not blend into a circular stadium or take the
     * capsule-button curve. The panel's own silhouette must follow the same curve.
     */
    @Test
    fun aSingleRowPanelKeepsTheSuperellipseAtTheCapsuleRadius() {
        runSkikoComposeUiTest(size = Size(320f, 200f), density = Density(1f)) {
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF808080)))
                        ListPopupContent(
                            enableGlass = false,
                            popupContentSize = IntSize(168, 56),
                            onPopupContentSizeChange = {},
                            fractionProgress = { 1f },
                            alphaProgress = { 1f },
                            pathProgress = { 0f },
                            gravityPath = ZERO_PATH,
                            popupLayoutPosition = BELOW_LEFT,
                            localTransformOrigin = TransformOrigin.Center,
                        ) {
                            Box(Modifier.size(168.dp, 56.dp))
                        }
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
                    if (pixels[x, y].isPanelFill()) {
                        if (x < left) left = x
                        if (x > right) right = x
                        if (y < top) top = y
                        if (y > bottom) bottom = y
                    }
                }
            }
            assertTrue(right - left + 1 in 166..170, "panel width ${right - left + 1}")
            assertTrue(bottom - top + 1 in 54..58, "panel height ${bottom - top + 1}")

            val radius = (bottom - top + 1) / 2f
            val profile = (2 until radius.toInt() - 1).mapNotNull { v ->
                val x = (left..left + radius.toInt() + 2).firstOrNull { pixels[it, top + v].isPanelFill() }
                    ?: return@mapNotNull null
                v.toFloat() to (x - left).toFloat()
            }
            assertTrue(profile.size > 20, "corner profile too short: ${profile.size}")

            fun rms(exponent: Float): Double = sqrt(
                profile.sumOf { (v, u) ->
                    val t = 1f - ((radius - v) / radius).pow(exponent)
                    val model = radius - radius * t.pow(1f / exponent)
                    (model - u).toDouble() * (model - u)
                } / profile.size,
            )
            val circle = rms(2f)
            val smooth = rms(2.5f)
            println("POPUP CORNER RMS circle=$circle smooth=$smooth profile=$profile")
            assertTrue(
                smooth < circle,
                "the capsule corner must stay on the superellipse: circle=$circle smooth=$smooth",
            )
        }
    }

    private fun Color.isPanelFill(): Boolean = red > 0.93f && green > 0.93f && blue > 0.93f

    private companion object {
        const val WIDTH = 400f
        const val HEIGHT = 300f
        val ZERO_PATH = BezierGravityPathGeometry(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val BELOW_LEFT = PopupLayoutPosition(showBelow = true, showAbove = false, isRightAligned = false)
    }
}
