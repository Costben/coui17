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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

private const val CANVAS_WIDTH_DP = 240f
private const val CANVAS_HEIGHT_DP = 120f

/** The plain fill the blob is drawn over, so anything the blob does not cover reads green. */
private val BackgroundColor = Color(0xFF00FF00)

/** The blob body, far from both the background and the (disabled) rim. */
private val BlobColor = Color(0xFF0000FF)

private val CanvasSize = Size(CANVAS_WIDTH_DP, CANVAS_HEIGHT_DP)

private val CapsuleCenterX = 60.dp
private val CapsuleCenterY = 60.dp
private val CapsuleWidth = 80.dp
private val CapsuleHeight = 40.dp

private val ParticleDiameter = 40.dp

/** 2dp between the capsule's right edge (x=100) and the circle's left edge (x=102). */
private val MergedCircleCenterX = 122.dp

/**
 * 4dp — one dp past the measured merge threshold. The smooth minimum folds with radius
 * `extension * factorX = 4 * 1.5 = 6px`, which bridges gaps below `6 / 2 = 3dp`, so a 4dp gap must
 * still read as background.
 */
private val PastThresholdCircleCenterX = 124.dp

/** 40dp between the capsule's right edge (x=100) and the circle's left edge (x=140). */
private val SeparatedCircleCenterX = 160.dp

private val CapsuleRightEdge = CapsuleCenterX + CapsuleWidth / 2

private fun circleLeftEdge(circleCenterX: Dp): Dp = circleCenterX - ParticleDiameter / 2

/** Midpoint of the horizontal gap between the capsule's right edge and the circle's left edge. */
private fun gapMidpointX(circleCenterX: Dp): Dp = (CapsuleRightEdge + circleLeftEdge(circleCenterX)) / 2

/**
 * The melt ball's union is computed per pixel by a smooth minimum over each silhouette's signed
 * distance, so "do these two shapes read as one blob" is a question only the rendered pixels can
 * answer. Every scene uses the same capsule and the same 40dp circle and differs only in the
 * circle's horizontal centre; the sample point sits on the centre line, halfway across the gap
 * between the two outlines, so it falls inside the union exactly when the fields actually merged.
 *
 * A flat green fill sits under the surface, and the blob is an opaque blue body with no rim, so the
 * sample is unambiguous: blue means the merged field bridges the gap, green means it does not.
 *
 * That makes the sample a shader-liveness probe too: the fallback path draws the silhouettes as a
 * hard union with no bridging at all, so it reads background at every positive gap. Only a gap
 * below the measured 3dp threshold tells "the shader ran and merged" from "the shader was skipped".
 */
@OptIn(ExperimentalTestApi::class)
class MetaBallSurfaceRenderTest {

    @Test
    fun silhouettesThatMeetMeltIntoOneBlob() = runSkikoComposeUiTest(
        size = CanvasSize,
        density = Density(1f),
    ) {
        setContent { MetaBallScene(MergedCircleCenterX) }
        waitForIdle()

        val pixel = sampleGapMidpoint(MergedCircleCenterX)

        assertTrue(
            greenChannel(pixel) <= 40,
            "the gap midpoint must be inside the blob, so the background must not show: $pixel. " +
                "Green here means nothing bridged the 2dp gap: either the melt ball shader did not " +
                "compile and MetaBallSurface fell back to the unmerged silhouettes, or the " +
                "smooth-min radius no longer reaches past half the gap.",
        )
        assertTrue(
            blueChannel(pixel) >= 200,
            "the gap midpoint must read as the blob's blue body: $pixel",
        )
    }

    /**
     * Characterises the measured merge threshold: the fold radius is
     * `extension * factorX = 4 * 1.5 = 6px`, so the fields bridge gaps below `6 / 2 = 3dp` and a
     * 4dp gap stays background. The threshold lives between the 2dp scene above and this one — a
     * wider or narrower fold shows up here first.
     */
    @Test
    fun aGapJustPastTheThresholdStaysSeparate() = runSkikoComposeUiTest(
        size = CanvasSize,
        density = Density(1f),
    ) {
        setContent { MetaBallScene(PastThresholdCircleCenterX) }
        waitForIdle()

        val pixel = sampleGapMidpoint(PastThresholdCircleCenterX)

        assertTrue(
            greenChannel(pixel) >= 200,
            "a 4dp gap is past the 3dp merge threshold, so the midpoint must be background: $pixel",
        )
        assertTrue(
            blueChannel(pixel) <= 40,
            "a 4dp gap is past the 3dp merge threshold, so the midpoint must not be the blob: $pixel",
        )
    }

    @Test
    fun silhouettesThatAreApartStaySeparate() = runSkikoComposeUiTest(
        size = CanvasSize,
        density = Density(1f),
    ) {
        setContent { MetaBallScene(SeparatedCircleCenterX) }
        waitForIdle()

        val pixel = sampleGapMidpoint(SeparatedCircleCenterX)

        assertTrue(
            greenChannel(pixel) >= 200,
            "the gap midpoint must still be background, so the blob must not reach it: $pixel",
        )
        assertTrue(
            blueChannel(pixel) <= 40,
            "the gap midpoint must not read as the blob's blue body: $pixel",
        )
    }

    @Test
    fun theTwoStatesSampleVisiblyDifferentColors() {
        val merged = renderGapMidpoint(MergedCircleCenterX)
        val separated = renderGapMidpoint(SeparatedCircleCenterX)

        assertTrue(
            maxChannelDelta(merged, separated) >= 60,
            "merged=$merged and separated=$separated must be visibly different",
        )
    }
}

@Composable
private fun MetaBallScene(circleCenterX: Dp) {
    Box(
        Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        MetaBallSurface(
            particles = listOf(
                MetaBallParticle(
                    centerX = CapsuleCenterX,
                    centerY = CapsuleCenterY,
                    width = CapsuleWidth,
                    height = CapsuleHeight,
                ),
                MetaBallParticle.circle(
                    centerX = circleCenterX,
                    centerY = CapsuleCenterY,
                    diameter = ParticleDiameter,
                ),
            ),
            modifier = Modifier.fillMaxSize(),
            color = BlobColor,
            backdrop = null,
            rimColor = null,
        )
    }
}

/** Renders [MetaBallScene] for one circle position and returns its gap-midpoint pixel. */
@OptIn(ExperimentalTestApi::class)
private fun renderGapMidpoint(circleCenterX: Dp): Color {
    var sample: Color? = null
    runSkikoComposeUiTest(size = CanvasSize, density = Density(1f)) {
        setContent { MetaBallScene(circleCenterX) }
        waitForIdle()
        sample = sampleGapMidpoint(circleCenterX)
    }
    return checkNotNull(sample)
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.sampleGapMidpoint(circleCenterX: Dp): Color {
    val x = with(density) { gapMidpointX(circleCenterX).roundToPx() }
    val y = with(density) { CapsuleCenterY.roundToPx() }
    val image = onRoot().captureToImage()
    val pixel = image.toPixelMap()[x, y]
    println(
        "[metaball] circleX=${circleCenterX.value} " +
            "gap=${circleLeftEdge(circleCenterX).value - CapsuleRightEdge.value} " +
            "sample=($x,$y) pixel=${pixel.argbHex()}",
    )
    return pixel
}

private fun Color.argbHex(): String = "#%08X".format(toArgb())

private fun greenChannel(color: Color): Int = color.toArgb() shr 8 and 0xFF

private fun blueChannel(color: Color): Int = color.toArgb() and 0xFF

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
