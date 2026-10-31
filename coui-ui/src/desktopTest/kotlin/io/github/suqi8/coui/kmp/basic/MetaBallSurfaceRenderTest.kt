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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
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
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun capsuleGlowFollowsItsLowerOutlineAcrossTheFullWidth() = runSkikoComposeUiTest(size = Size(800f, 480f), density = Density(4f)) {
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                MetaBallSurface(
                    particles = listOf(MetaBallParticle(100.dp, 60.dp, 140.dp, 40.dp, cornerRadius = 20.dp)),
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Blue,
                    backdrop = null,
                    rimColor = null,
                    bottomGlowColor = Color.White.copy(alpha = 66f / 255f),
                )
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val samples = listOf(220, 310, 400, 490, 580).map { pixels[it, 330].red }
        println("[bottom-halo-fade] capsule_lower_edge=$samples")
        assertTrue(samples.all { it > 0.02f }, "Directional fading must follow the capsule's surface normal and preserve the whole lower edge")
        assertTrue(samples.max() - samples.min() < 0.008f, "The straight lower edge must carry consistent glow")
    }

    @Test
    fun aStretchedCircleKeepsItsEllipticalOutline() = runSkikoComposeUiTest(size = CanvasSize, density = Density(1f)) {
        setContent {
            Box(Modifier.fillMaxSize().background(BackgroundColor)) {
                MetaBallSurface(
                    particles = listOf(MetaBallParticle.circle(60.dp, 60.dp, 40.dp).copy(scaleX = 1.5f)),
                    modifier = Modifier.fillMaxSize(),
                    color = BlobColor,
                    backdrop = null,
                    rimColor = null,
                )
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        assertTrue(greenChannel(pixels[85, 73]) >= 200, "a stadium would incorrectly fill this pixel")
        assertTrue(blueChannel(pixels[80, 60]) >= 200, "the stretched horizontal axis must remain filled")
    }

    @Test
    fun aDraggedParticleCanPaintOutsideTheRestingSurfaceBounds() = runSkikoComposeUiTest(size = CanvasSize, density = Density(1f)) {
        setContent {
            Box(Modifier.fillMaxSize().background(BackgroundColor)) {
                MetaBallSurface(
                    particles = listOf(MetaBallParticle.circle(40.dp, 50.dp, 40.dp)),
                    modifier = Modifier.size(80.dp, 40.dp),
                    color = BlobColor,
                    backdrop = null,
                    rimColor = null,
                )
            }
        }
        waitForIdle()
        val pixel = onRoot().captureToImage().toPixelMap()[40, 60]
        assertTrue(blueChannel(pixel) >= 200, "the moving particle must not be cut at the resting 40dp bottom")
    }

    /**
     * A member fading out over an opaque one must only fade its own exposed part. At x=88 the
     * circle's edge is nearer than the capsule's, yet the pixel lies inside the capsule, so it has to
     * keep the capsule's full body; at x=105 only the faded circle covers it, so it stays background.
     */
    @Test
    fun aFadingMemberNeverCutsIntoAnOpaqueOne() = runSkikoComposeUiTest(size = CanvasSize, density = Density(1f)) {
        setContent {
            Box(Modifier.fillMaxSize().background(BackgroundColor)) {
                MetaBallSurface(
                    particles = listOf(
                        MetaBallParticle(CapsuleCenterX, CapsuleCenterY, CapsuleWidth, CapsuleHeight),
                        MetaBallParticle.circle(90.dp, CapsuleCenterY, ParticleDiameter).copy(alpha = 0f),
                    ),
                    modifier = Modifier.fillMaxSize(),
                    color = BlobColor,
                    backdrop = null,
                    rimColor = null,
                )
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        assertTrue(blueChannel(pixels[88, 60]) >= 200, "the capsule must stay whole where the faded circle's edge is nearer")
        assertTrue(greenChannel(pixels[105, 60]) >= 200, "the circle's exposed part must take its own alpha")
    }

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

    @Test
    fun metaBallContinuousCurveMatchesSegmentCapsuleWithinThreshold() {
        val radius = 28f
        val halfW = 150f
        fun shaderDistance(x: Float, y: Float, continuousCurve: Boolean): Float {
            val qx = abs(x) - (halfW - radius)
            val qy = abs(y)
            var d = minOf(maxOf(qx, qy), 0f) + sqrt(maxOf(qx, 0f) * maxOf(qx, 0f) + maxOf(qy, 0f) * maxOf(qy, 0f)) - radius
            if (continuousCurve) {
                if (halfW > radius && radius >= radius - 0.001f) {
                    val s = halfW - abs(x)
                    val tail = minOf(1.5f, halfW / radius)
                    val s0 = 0.2250555f * radius
                    val s1 = tail * radius
                    if (s > s0 && s < s1) {
                        val u = (s - s0) / (s1 - s0)
                        val uFactor = u * u * (1f - u) * (1f - u)
                        d += uFactor * (0.003318f + u * (1.529298f - 1.888829f * u)) * radius
                    }
                }
            }
            return d
        }

        // Circular particles remain pure circles:
        val r = 28f
        val qx = 20f
        val qy = 20f
        val circleDist = minOf(maxOf(qx, qy), 0f) + sqrt(qx * qx + qy * qy) - r
        val expectedCircleDist = sqrt(20f * 20f + 20f * 20f) - 28f
        assertEquals(expectedCircleDist, circleDist, 1e-4f, "Circular particles must remain pure circles")

        // SegmentCapsule contour error:
        val path = Path().apply { addSegmentCapsule(Rect(0f, 0f, halfW * 2f, radius * 2f)) }
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val sampleCount = 2000
        var maxError = 0f
        var maxDipAtR = 0f
        for (i in 0..sampleCount) {
            val pos = measure.getPosition(i * length / sampleCount)
            val xLocal = pos.x - halfW
            val yLocal = pos.y - radius
            val dist = abs(shaderDistance(xLocal, yLocal, continuousCurve = true))
            if (dist > maxError) maxError = dist

            val s = halfW - abs(xLocal)
            if (abs(s - radius) < 0.2f && abs(yLocal) > radius * 0.5f) {
                val roundboxDist = abs(shaderDistance(xLocal, yLocal, continuousCurve = false))
                if (roundboxDist > maxDipAtR) maxDipAtR = roundboxDist
            }
        }
        println("[metaball-curve] maxError=${maxError / radius}R maxDipAtR=${maxDipAtR / radius}R")
        assertTrue(maxError <= 0.005f * radius, "Contour error $maxError must be <= 0.005R (${0.005f * radius})")
        assertTrue(abs(maxDipAtR / radius - 0.0134f) < 0.001f, "Dip at x=R should be ~0.0134R, was ${maxDipAtR / radius}")
    }

    @Test
    fun metaBallContinuousCurveRendersSegmentCapsuleCorner() = runSkikoComposeUiTest(
        size = Size(600f, 300f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                    MetaBallSurface(
                        particles = listOf(MetaBallParticle(75.dp, 35.dp, 120.dp, 56.dp, cornerRadius = 28.dp)),
                        modifier = Modifier.size(150.dp, 70.dp),
                        color = Color.Black,
                        backdrop = null,
                        rimColor = null,
                        blurColors = null,
                        continuousCurve = true,
                    )
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        println("[metaball-render] rmsSegment=${fit.rmsSegment} rmsCircle=${fit.rmsCircle}")
        assertTrue(fit.rmsSegment < fit.rmsCircle, "MetaBallSurface with continuousCurve must fit SegmentCapsule better than circle")
        assertTrue(fit.rmsSegment < 1.0, "segment residual ${fit.rmsSegment}px too large")
    }

    @Test
    fun metaBallWithoutContinuousCurveStaysStandardRoundBox() = runSkikoComposeUiTest(
        size = Size(600f, 300f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                    MetaBallSurface(
                        particles = listOf(MetaBallParticle(75.dp, 35.dp, 120.dp, 56.dp, cornerRadius = 28.dp)),
                        modifier = Modifier.size(150.dp, 70.dp),
                        color = Color.Black,
                        backdrop = null,
                        rimColor = null,
                        blurColors = null,
                        continuousCurve = false,
                    )
                }
            }
        }
        waitForIdle()
        val fit = fitTopLeftCorner(onRoot().captureToImage().toPixelMap()) { it.luminance() < 0.2f }
        println("[metaball-nocontrol] rmsSegment=${fit.rmsSegment} rmsCircle=${fit.rmsCircle}")
        assertTrue(fit.rmsCircle < fit.rmsSegment, "MetaBallSurface without continuousCurve must be a circle")
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

private data class CornerFit(val rmsCircle: Double, val rmsSegment: Double, val r: Int)

private fun fitTopLeftCorner(pixels: PixelMap, fill: (Color) -> Boolean): CornerFit {
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1
    for (y in 0 until pixels.height) {
        for (x in 0 until pixels.width) {
            if (!fill(pixels[x, y])) continue
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }
    check(right > left && bottom > top) { "no filled pixels" }
    val width = right - left + 1
    val r = (bottom - top + 1) / 2
    val measured = ArrayList<Double>()
    val rows = ArrayList<Double>()
    val maxU = (1.6 * r).toInt()
    for (y in top + 1 until top + r) {
        var edge = -1
        for (x in maxOf(0, left - 2) until minOf(pixels.width, left + maxU)) {
            if (fill(pixels[x, y])) {
                edge = x
                break
            }
        }
        if (edge >= 0) {
            measured += (edge - left).toDouble()
            rows += (y - top).toDouble()
        }
    }
    val circle = DoubleArray(measured.size) { index ->
        val v = rows[index]
        r - sqrt((r * r - (r - v) * (r - v)).coerceAtLeast(0.0))
    }
    val segment = segmentProfile(rows, r, width)
    return CornerFit(rms(measured, circle), rms(measured, segment), r)
}

private fun segmentProfile(rows: List<Double>, r: Int, width: Int): DoubleArray {
    val path = Path().apply { addSegmentCapsule(Rect(0f, 0f, width.toFloat(), (2 * r).toFloat())) }
    val measure = PathMeasure().apply { setPath(path, false) }
    val samples = DoubleArray(r + 1) { Double.NaN }
    val steps = 20_000
    for (i in 0..steps) {
        val point = measure.getPosition(measure.length * i / steps)
        val v = point.y.toInt()
        if (v in 0..r && point.x <= 1.6f * r) {
            if (samples[v].isNaN() || point.x < samples[v]) samples[v] = point.x.toDouble()
        }
    }
    for (i in samples.indices) {
        if (!samples[i].isNaN()) continue
        val lower = (i - 1 downTo 0).firstOrNull { !samples[it].isNaN() }
        val upper = (i + 1..r).firstOrNull { !samples[it].isNaN() }
        samples[i] = when {
            lower != null -> samples[lower]
            upper != null -> samples[upper]
            else -> 0.0
        }
    }
    return DoubleArray(rows.size) { samples[rows[it].toInt()] }
}

private fun rms(measured: List<Double>, model: DoubleArray): Double {
    val offset = measured.indices.sumOf { measured[it] - model[it] } / measured.size
    return sqrt(
        measured.indices.sumOf {
            val d = measured[it] - model[it] - offset
            d * d
        } / measured.size,
    )
}
