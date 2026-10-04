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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
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
    fun leftLightRetainsItsOwnerAndDisappearsFarOutside() = runSkikoComposeUiTest(size = Size(1000f, 320f), density = Density(2f)) {
        setContent { TerritoryScene() }
        waitForIdle()
        checkTerritoryExitAndReturn("left", direction = 1f)
    }

    @Test
    fun rightLightRetainsItsOwnerAndDisappearsFarOutside() = runSkikoComposeUiTest(size = Size(1000f, 320f), density = Density(2f)) {
        setContent { TerritoryScene() }
        waitForIdle()
        checkTerritoryExitAndReturn("right", direction = -1f)
    }

    @Test
    fun leftGestureDoesNotLightTheMenuOrRightAccessory() = runSkikoComposeUiTest(size = Size(1000f, 320f), density = Density(2f)) {
        setContent { TerritoryScene() }
        waitForIdle()
        val start = onNodeWithTag("left").fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { down(start) }
        waitForIdle()
        assertTrue(lightEnergy("left") > 0.02f)
        for (target in listOf("middle1", "right", "middle0", "left")) {
            val point = onNodeWithTag(target).fetchSemanticsNode().boundsInRoot.center
            onRoot().performTouchInput { moveTo(point, delayMillis = 160) }
            waitForIdle()
            assertOtherTerritoriesUnlit("left")
        }
        assertTrue(lightEnergy("left") > 0.02f, "Returning must restore the original owner's light")
        onRoot().performTouchInput { up() }
    }

    @Test
    fun lightMovesBetweenMenuItemsWithinOneCapsule() = runSkikoComposeUiTest(size = Size(1000f, 320f), density = Density(2f)) {
        setContent { TerritoryScene() }
        waitForIdle()
        val start = onNodeWithTag("middle0").fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { down(start) }
        waitForIdle()
        assertTrue(lightEnergy("middle0") > lightEnergy("middle2") + 0.01f)
        val target = onNodeWithTag("middle2").fetchSemanticsNode().boundsInRoot.center
        onRoot().performTouchInput { moveTo(target, delayMillis = 160) }
        waitForIdle()
        assertTrue(lightEnergy("middle2") > lightEnergy("middle0") + 0.01f, "The light must follow movement inside the capsule")
        assertTrue(lightEnergy("left") < 0.002f && lightEnergy("right") < 0.002f)
        onRoot().performTouchInput { up() }
    }

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
        val decoration = decorationAtCurrentPose()
        var insideLight = 0
        var outsideLight = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val dx = abs(x + 0.5f - bounds.center.x) / scale - 19f
                val dy = abs(y + 0.5f - bounds.center.y) / scale
                val distance = sqrt(max(dx, 0f) * max(dx, 0f) + dy * dy) - 28f
                if (pixels[x, y].red - decoration[x, y].red > 0.025f) {
                    if (distance < -2f) insideLight++
                    if (distance > 1.5f && pixels[x, y].red - decoration[x, y].red > 0.025f) outsideLight++
                }
            }
        }
        assertTrue(insideLight > 20, "The menu light must be visible")
        assertTrue(outsideLight == 0, "Menu light must share the material's circular corners; $outsideLight pixels escape")
        onRoot().performTouchInput { up() }
    }

    @Test
    fun accessoryLightStaysInsideThePressedCircle() = runSkikoComposeUiTest(size = Size(360f, 160f), density = Density(1f)) {
        setContent { LightScene(accessory = true) }
        waitForIdle()
        onNodeWithTag("accessory").performTouchInput { down(center) }
        waitForIdle()
        assertLightInsideAccessory()
        onRoot().performTouchInput { up() }
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
        onRoot().performTouchInput { up() }
    }

    @Test
    fun accessoryBarWrapsItsContentInsteadOfFillingTheParentHeight() = runSkikoComposeUiTest(size = Size(360f, 640f), density = Density(1f)) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color.Green), contentAlignment = Alignment.BottomCenter) {
                    BottomFloatingAccessoryNavigationBar(
                        modifier = Modifier.testTag("bar-root"),
                        containerColor = Color.Blue,
                        backdrop = null,
                        startAccessory = { Box(Modifier.size(56.dp)) },
                    ) {
                        Box(Modifier.size(70.dp, 48.dp))
                    }
                }
            }
        }
        waitForIdle()
        val bounds = onNodeWithTag("bar-root").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.height <= 96f, "The accessory host must wrap its 88dp content band, not fill the 640dp parent: $bounds")
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
        onRoot().performTouchInput { up() }
        waitForIdle()
        assertTrue(abs(capsuleWidth(restBounds.center.y.toInt()) - restWidth) <= 1)
    }
}

@Composable
private fun TerritoryScene() {
    COUITheme(colors = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Green), contentAlignment = Alignment.BottomCenter) {
            BottomFloatingAccessoryNavigationBar(
                containerColor = Color.Blue,
                backdrop = null,
                startAccessory = { Box(Modifier.fillMaxSize().testTag("left")) },
                endAccessory = { Box(Modifier.fillMaxSize().testTag("right")) },
            ) {
                repeat(3) { Box(Modifier.size(70.dp, 48.dp).testTag("middle$it")) }
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.checkTerritoryExitAndReturn(owner: String, direction: Float) {
    val start = onNodeWithTag(owner).fetchSemanticsNode().boundsInRoot.center
    onRoot().performTouchInput { down(start) }
    waitForIdle()
    val initial = lightEnergy(owner)
    assertTrue(initial > 0.02f, "The owner must light up on down")
    val near = start + Offset(direction * 120f, 0f)
    onRoot().performTouchInput { moveTo(near, delayMillis = 160) }
    waitForIdle()
    assertTrue(lightEnergy(owner) > 0.005f, "Part of the halo must remain after a nearby exit")
    assertOtherTerritoriesUnlit(owner)
    val far = Offset(if (direction > 0f) 995f else 5f, start.y)
    onRoot().performTouchInput { moveTo(far, delayMillis = 160) }
    waitForIdle()
    assertTrue(lightEnergy(owner) < 0.002f, "A far-away light must stop intersecting its original container")
    assertOtherTerritoriesUnlit(owner)
    onRoot().performTouchInput { moveTo(start, delayMillis = 160) }
    waitForIdle()
    assertTrue(lightEnergy(owner) > initial * 0.5f, "The same gesture must light its owner again on return")
    assertOtherTerritoriesUnlit(owner)
    onRoot().performTouchInput { up() }
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.assertOtherTerritoriesUnlit(owner: String) {
    for (tag in listOf("left", "middle0", "middle1", "middle2", "right")) {
        if (tag != owner) assertTrue(lightEnergy(tag) < 0.002f, "$tag must not acquire the $owner gesture's light")
    }
}

/** Mean red on a blue body: shadow and geometry changes cannot impersonate illumination. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.lightEnergy(tag: String): Float {
    val bounds = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
    val pixels = onRoot().captureToImage().toPixelMap()
    var sum = 0f
    var count = 0
    for (y in bounds.top.toInt() until bounds.bottom.toInt()) {
        for (x in bounds.left.toInt() until bounds.right.toInt()) {
            val dx = (x + 0.5f - bounds.center.x) / (bounds.width / 2f)
            val dy = (y + 0.5f - bounds.center.y) / (bounds.height / 2f)
            if (dx * dx + dy * dy < 0.64f) {
                sum += pixels[x, y].red
                count++
            }
        }
    }
    return sum / count.coerceAtLeast(1)
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
    val decoration = decorationAtCurrentPose()
    var insideLight = 0
    var outsideLight = 0
    for (y in 0 until pixels.height) {
        for (x in 0 until (bounds.right + 12f).toInt()) {
            val dx = (x + 0.5f - bounds.center.x) / (bounds.width / 2f)
            val dy = (y + 0.5f - bounds.center.y) / (bounds.height / 2f)
            val distance = dx * dx + dy * dy
            if (pixels[x, y].red - decoration[x, y].red > 0.025f) {
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

/** Reference decoration at captured production bounds; no pointer light is present in this scene. */
@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.decorationAtCurrentPose(): PixelMap {
    val accessory = onNodeWithTag("accessory").fetchSemanticsNode().boundsInRoot
    val menu = onNodeWithTag("menu").fetchSemanticsNode().boundsInRoot
    val barHeight = BottomFloatingNavigationBarDefaults.BarHeight.value
    val accessorySize = BottomFloatingAccessoryNavigationBarDefaults.AccessorySize.value
    val menuWidth = 70f + BottomFloatingNavigationBarDefaults.ItemInnerPadding.value * 2f
    fun shape(bounds: Rect, width: Float, height: Float, scaleX: Float, scaleY: Float): MetaBallParticle = MetaBallParticle(
        centerX = bounds.center.x.dp,
        centerY = bounds.center.y.dp,
        width = width.dp,
        height = height.dp,
        cornerRadius = (height / 2f).dp,
        scaleX = scaleX,
        scaleY = scaleY,
    )
    val shapes = listOf(
        shape(menu, menuWidth, barHeight, menu.width / 70f, menu.height / 48f),
        shape(accessory, accessorySize, accessorySize, accessory.width / accessorySize, accessory.height / accessorySize),
    )
    setContent {
        COUITheme(colors = darkColorScheme()) {
            Box(Modifier.fillMaxSize().background(Color.Green)) {
                MetaBallSurface(
                    particles = shapes,
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Blue,
                    backdrop = null,
                    rimColor = Color.White.copy(alpha = 0.8f),
                    rimBottomScale = 1f,
                    bottomGlowColor = Color.White.copy(alpha = 66f / 255f),
                )
            }
        }
    }
    waitForIdle()
    return onRoot().captureToImage().toPixelMap()
}
