// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

private val CanvasSize = Size(240f, 120f)

/** The plain fill the bar is drawn over, so anything the bar does not cover reads green. */
private val BackgroundColor = Color(0xFF00FF00)

/**
 * A 24-unit icon whose path fills the whole viewport, so every pixel of the icon's box is painted.
 * That makes "is this sample inside the icon" a question about the icon's *box*, not about the
 * artwork's silhouette.
 */
private val FullBoxIcon: ImageVector = ImageVector.Builder(
    name = "FullBox",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(0f, 0f)
        lineTo(24f, 0f)
        lineTo(24f, 24f)
        lineTo(0f, 24f)
        close()
    }
}.build()

/**
 * `COUIBottomFloatingNavigationBar` draws its large-icon item through a bar-level `ViewGroupOverlay`
 * rather than in the item itself, precisely so the 40dp icon can hang 14dp above the item's top edge
 * without the bar clipping it.
 *
 * This port draws it inside the item instead, offset by `-(LargeIconOverflow + IconTopMargin)`. That
 * is only equivalent while nothing in the chain clips its children — the glass surface
 * (`couiGlassSurface` → `textureBlur` → `drawBackdrop`) is a background draw, not a clip, and the
 * capsule's `Row` carries no `clip` modifier. These two scenes are the differential proof: the same
 * bar and the same icon, once as a 24dp icon that stays inside the capsule and once as a 40dp one
 * that overflows it, sampled in the band above the capsule's top edge.
 */
@OptIn(ExperimentalTestApi::class)
class BottomFloatingNavigationBarLargeIconRenderTest {

    @Test
    fun theLargeIconOverflowsTheCapsule() = runSkikoComposeUiTest(
        size = CanvasSize,
        density = Density(1f),
    ) {
        setContent { BarScene(largeIcon = true) }
        waitForIdle()

        val pixel = sample(OVERFLOW_SAMPLE_X, OVERFLOW_SAMPLE_Y)
        assertTrue(
            greenChannel(pixel) < 200,
            "the 40dp icon is drawn 14dp above the item, so the band above the capsule must not be " +
                "the background: $pixel. Green here means the overflow was clipped away.",
        )
    }

    @Test
    fun aRegularIconStaysInsideTheCapsule() = runSkikoComposeUiTest(
        size = CanvasSize,
        density = Density(1f),
    ) {
        setContent { BarScene(largeIcon = false) }
        waitForIdle()

        val pixel = sample(OVERFLOW_SAMPLE_X, OVERFLOW_SAMPLE_Y)
        assertTrue(
            greenChannel(pixel) >= 200,
            "without the large-icon style nothing reaches above the capsule, so this sample must be " +
                "the background: $pixel",
        )
    }

    @Test
    fun theCapsuleItselfIsStillDrawn() = runSkikoComposeUiTest(
        size = CanvasSize,
        density = Density(1f),
    ) {
        setContent { BarScene(largeIcon = true) }
        waitForIdle()

        // The capsule sits MenuToScreenNaviDistance above the 88dp box's bottom edge, so it spans
        // y = 24dp .. 80dp.
        val pixel = sample(OVERFLOW_SAMPLE_X, CAPSULE_SAMPLE_Y)
        assertTrue(
            greenChannel(pixel) < 200,
            "the capsule must cover the bar's centre line: $pixel",
        )
    }

    @OptIn(ExperimentalTestApi::class)
    private fun SkikoComposeUiTest.sample(x: Float, y: Float): Color {
        val px = with(density) { x.toInt() }
        val py = with(density) { y.toInt() }
        val image = onRoot().captureToImage()
        val pixel = image.toPixelMap()[px, py]
        println("[largeIcon] sample=($px,$py) pixel=${"#%08X".format(pixel.toArgb())}")
        return pixel
    }
}

/**
 * The bar's layout, in dp. The 56dp capsule sits
 * [BottomFloatingNavigationBarDefaults.MenuToScreenNaviDistance] above the bottom of the 88dp box, so
 * it spans 24dp..80dp. The 48dp item is centred in the capsule and starts at 28dp; the icon slot's
 * natural top is 28 + [BottomFloatingNavigationBarDefaults.IconTopMargin] = 32dp, and the large-icon
 * style offsets it by `-(LargeIconOverflow + IconTopMargin)` = -18dp, leaving the 40dp icon at
 * 14dp..54dp.
 *
 * 20dp is therefore inside the icon and above the capsule's 24dp top edge — the overflow band.
 */
private const val OVERFLOW_SAMPLE_Y = 20f

/** Between the capsule's top edge (24dp) and its bottom edge (80dp). */
private const val CAPSULE_SAMPLE_Y = 44f

/** The bar fills the canvas width and holds a single item, so the icon is centred horizontally. */
private const val OVERFLOW_SAMPLE_X = 120f

@Composable
private fun BarScene(largeIcon: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        BottomFloatingNavigationBar(
            backdrop = null,
            containerColor = CapsuleColor,
        ) {
            BottomFloatingNavigationBarItem(
                selected = true,
                onClick = {},
                icon = FullBoxIcon,
                label = "item",
                largeIcon = largeIcon,
            )
        }
    }
}

/** An opaque fill far from the green background, so a covered sample is unambiguous. */
private val CapsuleColor = Color(0xFF0000FF)

private fun greenChannel(color: Color): Int = color.toArgb() shr 8 and 0xFF
