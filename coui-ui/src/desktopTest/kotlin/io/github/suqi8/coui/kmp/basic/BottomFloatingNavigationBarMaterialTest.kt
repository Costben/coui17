// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `BaseBottomFloatingMenuView` dresses the capsule in `TYPE_FRAMEWORK_BOTTOM_BAR`:
 * `RenderEffect.createBlurEffect(25, 25)` in pixels, then two `createColorFilterEffect` layers with
 * layer 1 innermost. Reading the 25 as dp, or applying the layers in the other order, is what made the
 * capsule a flat grey smear instead of glass.
 */
@OptIn(ExperimentalTestApi::class)
class BottomFloatingNavigationBarMaterialTest {

    @Test
    fun theBlurMatchesTheNativeSigmaAtAnyDensity() {
        for (density in listOf(1f, 2.75f, 3.875f)) {
            var radius = 0f
            runSkikoComposeUiTest(size = Size(10f, 10f), density = Density(density)) {
                setContent { radius = BottomFloatingNavigationBarDefaults.blurRadius() }
                waitForIdle()
            }
            // RenderEffect's blur radius is a Skia sigma in pixels, so the native 25 is a 25px
            // sigma; textureBlur derives its sigma as 0.45 * px.
            val nativeSigma = BottomFloatingNavigationBarDefaults.NATIVE_BLUR_RADIUS_PX
            assertEquals(nativeSigma, 0.45f * radius * density, 0.01f, "sigma at density $density")
        }
    }

    @Test
    fun theLabelKeepsItsSizeWhenTheFontGrows() {
        fun labelWidth(fontScale: Float): Int {
            var width = 0
            runSkikoComposeUiTest(size = Size(200f, 100f), density = Density(1f, fontScale)) {
                setContent {
                    Row {
                        BottomFloatingNavigationBarItem(
                            selected = true,
                            onClick = {},
                            icon = ImageVector.Builder("Empty", 24.dp, 24.dp, 24f, 24f).build(),
                            label = "Label",
                        )
                    }
                }
                waitForIdle()
                width = onNodeWithText("Label").fetchSemanticsNode().size.width
            }
            return width
        }
        // applyNaviVerticalStyle sizes the title with dp2px(10f), which no font scale reaches.
        assertEquals(labelWidth(1f), labelWidth(1.5f))
    }

    @Test
    fun theLightLayersBrightenBeforeTheyRemapLightness() {
        assertEquals(
            listOf(BlurBlendMode.ColorDodge, BlurBlendMode.Luminosity),
            BottomFloatingNavigationBarDefaults.BlurColorsLight.map { it.mode },
        )
    }

    @Test
    fun theDarkLayersOverlayBeforeTheyRemapLightness() {
        assertEquals(
            listOf(BlurBlendMode.Overlay, BlurBlendMode.Luminosity),
            BottomFloatingNavigationBarDefaults.BlurColorsDark.map { it.mode },
        )
    }
}
