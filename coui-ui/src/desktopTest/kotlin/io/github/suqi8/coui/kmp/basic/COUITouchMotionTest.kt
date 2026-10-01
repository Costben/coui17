// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class COUITouchMotionTest {
    @Test
    fun rubberBandRoundTripsTheRawOffset() {
        val raw = 42f
        val rendered = couiRubberBandApply(raw, curveRatio = 0.55f, maxDistance = 150f)
        val restored = couiRubberBandInverse(rendered, curveRatio = 0.55f, maxDistance = 150f)
        assertEquals(raw, restored, absoluteTolerance = 0.001f)
        assertTrue(abs(rendered) < raw)
    }

    @Test
    fun rubberBandPreservesDirectionAndCapsAtMaximum() {
        assertTrue(couiRubberBandApply(-42f, 0.55f, 150f) < 0f)
        assertTrue(abs(couiRubberBandApply(10_000f, 0.55f, 150f)) < 150f)
        assertEquals(0f, couiRubberBandApply(1f, 0f, 0f))
    }

    @Test
    fun deformationStretchesTheDominantAxisAndCompressesTheOther() {
        val scales = couiDeformationScales(
            dx = 28f,
            dy = 0f,
            spec = COUITouchMotionSpec(),
            width = 80f,
            height = 48f,
        )
        assertTrue(scales.x > 1f)
        assertTrue(scales.y < 1f)
    }

    @Test
    fun scaleWithKUsesNativeSmallAreaFastPath() {
        assertEquals(1.2f, couiScaleWithK(100f, 100f, 1.2f, 350000, 15))
        assertTrue(couiScaleWithK(300f, 80f, 1.2f, 350000, 15) >= 1.03f)
    }

    @Test
    fun pressScaleUsesAreaAndAspectChannels() {
        val compact = couiPressScale(56f, 56f, COUITouchMotionSpec())
        val capsule = couiPressScale(280f, 48f, COUITouchMotionSpec())
        assertTrue(compact in 0.85f..0.97f)
        assertTrue(capsule in 0.85f..1f)
    }

    @Test
    fun pressAreaUsesTheNativeCubicBezierEndpoint() {
        val spec = COUITouchMotionSpec()
        val atMinimum = couiPressScale(20f, 20f, spec)
        val nearMaximum = couiPressScale(149f, 149f, spec)
        assertEquals(spec.pressScaleMin, atMinimum, absoluteTolerance = 0.0001f)
        assertTrue(nearMaximum > atMinimum)
        assertTrue(nearMaximum < spec.pressScaleMax)
    }

    @Test
    fun pressBezierSolvesParametricXBeforeReadingY() {
        val midpoint = couiCubicBezierInterpolate(0.5f, 0f, 0f, 0f, 1f)
        assertEquals(0.8899f, midpoint, absoluteTolerance = 0.001f)
        assertEquals(0.8024f, couiCubicBezierInterpolate(0.5f, 0.25f, 0.1f, 0.25f, 1f), absoluteTolerance = 0.003f)
    }

    @Test
    fun handUpSpringInterpolatesAreaAndBoundsVelocityBounce() {
        val small = couiResolveHandUpSpring(
            widthPx = 56f,
            heightPx = 56f,
            density = 1f,
            pan = 28f,
            totalDistance = 28f,
            previousVelocity = 200f,
            baseResponse = 0.42f,
            baseBounce = 0.65f,
            largeAreaResponse = 0.45f,
            largeAreaBounce = 0.65f,
            smallAreaRefWidthDp = 56f,
            largeAreaRefWidthDp = 100f,
            bezierX1 = 0.55026454f,
            bezierY1 = 0f,
            bezierX2 = 0.8915344f,
            bezierY2 = 0.18783069f,
            maxInitialVelocity = 1000f,
            bounceGain = 1f,
        )
        val large = couiResolveHandUpSpring(
            widthPx = 100f,
            heightPx = 100f,
            density = 1f,
            pan = 28f,
            totalDistance = 28f,
            previousVelocity = 200f,
            baseResponse = 0.42f,
            baseBounce = 0.65f,
            largeAreaResponse = 0.45f,
            largeAreaBounce = 0.65f,
            smallAreaRefWidthDp = 56f,
            largeAreaRefWidthDp = 100f,
            bezierX1 = 0.55026454f,
            bezierY1 = 0f,
            bezierX2 = 0.8915344f,
            bezierY2 = 0.18783069f,
            maxInitialVelocity = 1000f,
            bounceGain = 1f,
        )
        assertEquals(0.42f, small.response, absoluteTolerance = 0.0001f)
        assertEquals(0.45f, large.response, absoluteTolerance = 0.0001f)
        assertEquals(0.65f, large.bounce, absoluteTolerance = 0.0001f)
        assertEquals(200f, large.initialVelocity, absoluteTolerance = 0.0001f)
    }

    @Test
    fun spotlightRadiusInterpolatesAndExtinguishesPastTheEndRadius() {
        assertEquals(100f, couiSpotlightRadius(100f, 500f, 1000f, 0f, false))
        assertEquals(300f, couiSpotlightRadius(100f, 500f, 1000f, 0.5f, false))
        assertEquals(1000f, couiSpotlightRadius(100f, 500f, 1000f, 0f, true))
        assertEquals(500f, couiSpotlightRadius(100f, 500f, 1000f, 1f, true))
    }

    @Test
    fun collectorAddsUnitScaleDeltasInsteadOfMultiplyingThem() {
        assertEquals(0.92f, couiTouchMotionScaleContribution(0.95f, 0.97f), absoluteTolerance = 0.0001f)
    }
}
