// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/** Pixel tests at the production bar call sites, including the accessory's expansion layer. */
@OptIn(ExperimentalTestApi::class)
class FloatingBarSpotLightRenderTest {
    @Test
    fun menuLightStaysInsideThePressedMaterialCapsule() = runSkikoComposeUiTest(size = Size(360f, 160f), density = Density(1f)) {
        setContent { LightScene(accessory = true) }
        waitForIdle()
        val rest = onNodeWithTag("menu").fetchSemanticsNode().boundsInRoot
        onNodeWithTag("menu").performTouchInput { down(center) }
        waitForIdle()
        val bounds = onNodeWithTag("menu").fetchSemanticsNode().boundsInRoot
        val scale = bounds.width / rest.width
        val pixels = onRoot().captureToImage().toPixelMap()
        var insideLight = 0
        var outsideLight = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val dx = abs(x + 0.5f - bounds.center.x) / scale - 19f
                val dy = abs(y + 0.5f - bounds.center.y) / scale
                val distance = sqrt(max(dx, 0f) * max(dx, 0f) + dy * dy) - 28f
                if (pixels[x, y].red > 0.025f) {
                    if (distance < -2f) insideLight++
                    if (distance > 1.5f) outsideLight++
                }
            }
        }
        assertTrue(insideLight > 20, "The menu light must be visible")
        assertTrue(outsideLight == 0, "Menu light must share the material's circular corners; $outsideLight pixels escape")
        onNodeWithTag("menu").performTouchInput { up() }
    }

    @Test
    fun accessoryLightStaysInsideThePressedCircle() = runSkikoComposeUiTest(size = Size(360f, 160f), density = Density(1f)) {
        setContent { LightScene(accessory = true) }
        waitForIdle()
        onNodeWithTag("accessory").performTouchInput { down(center) }
        waitForIdle()
        assertLightInsideAccessory()
        onNodeWithTag("accessory").performTouchInput { up() }
    }

    @Test
    fun accessoryLightFollowsDiagonalDragAndDeformation() = runSkikoComposeUiTest(size = Size(360f, 160f), density = Density(1f)) {
        setContent { LightScene(accessory = true) }
        waitForIdle()
        onNodeWithTag("accessory").performTouchInput {
            down(center)
            moveBy(Offset(160f, -80f), delayMillis = 160)
        }
        waitForIdle()
        assertLightInsideAccessory()
        onNodeWithTag("accessory").performTouchInput { up() }
    }

    @Test
    fun plainCapsuleBackgroundShrinksWithItsContent() = runSkikoComposeUiTest(size = Size(360f, 160f), density = Density(1f)) {
        setContent { LightScene(accessory = false) }
        waitForIdle()
        val restBounds = onNodeWithTag("menu").fetchSemanticsNode().boundsInRoot
        val restWidth = capsuleWidth(restBounds.center.y.toInt())
        onNodeWithTag("menu").performTouchInput { down(center) }
        waitForIdle()
        val pressedBounds = onNodeWithTag("menu").fetchSemanticsNode().boundsInRoot
        val pressedWidth = capsuleWidth(pressedBounds.center.y.toInt())
        val scale = pressedBounds.width / restBounds.width
        assertTrue(scale < 0.99f, "The content must shrink while pressed")
        assertTrue(abs(pressedWidth - restWidth * scale) <= 2f, "Background must share content scale: rest=$restWidth pressed=$pressedWidth scale=$scale")
        onNodeWithTag("menu").performTouchInput { up() }
        waitForIdle()
        assertTrue(abs(capsuleWidth(restBounds.center.y.toInt()) - restWidth) <= 1)
    }
}

@Composable
private fun LightScene(accessory: Boolean) {
    COUITheme(colors = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Green), contentAlignment = Alignment.BottomCenter) {
            if (accessory) {
                BottomFloatingAccessoryNavigationBar(
                    containerColor = Color.Blue,
                    backdrop = null,
                    startAccessory = { Box(Modifier.fillMaxSize().testTag("accessory")) },
                ) {
                    Box(Modifier.size(70.dp, 48.dp).testTag("menu"))
                }
            } else {
                BottomFloatingNavigationBar(containerColor = Color.Blue, backdrop = null) {
                    Box(Modifier.size(70.dp, 48.dp).testTag("menu"))
                }
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.assertLightInsideAccessory() {
    val bounds = onNodeWithTag("accessory").fetchSemanticsNode().boundsInRoot
    val pixels = onRoot().captureToImage().toPixelMap()
    var insideLight = 0
    var outsideLight = 0
    for (y in 0 until pixels.height) {
        for (x in 0 until (bounds.right + 12f).toInt()) {
            val dx = (x + 0.5f - bounds.center.x) / (bounds.width / 2f)
            val dy = (y + 0.5f - bounds.center.y) / (bounds.height / 2f)
            val distance = dx * dx + dy * dy
            if (pixels[x, y].red > 0.025f) {
                if (distance < 0.8f) insideLight++
                if (distance > 1.08f) outsideLight++
            }
        }
    }
    assertTrue(insideLight > 20, "The test must exercise a visible spotlight")
    assertTrue(outsideLight == 0, "Light must follow the transformed circle; $outsideLight pixels escape $bounds")
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.capsuleWidth(y: Int): Int {
    val pixels = onRoot().captureToImage().toPixelMap()
    return (0 until pixels.width).count { x -> pixels[x, y].blue > 0.8f }
}
