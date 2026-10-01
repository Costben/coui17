// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.ui.unit.Density
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The panel drag curve, its release velocity and the hand-up bounce are pure arithmetic and are the
 * parts a device recording cannot pin down: a wrong sign only shows up at the extremes of a drag,
 * and a wrong velocity scale only shows up as a slightly different overshoot. They are checked here
 * against `TouchMotionRubberBand.apply` / `TouchMotionHandUpHelper.computeInitialVelocity` /
 * `TouchMotionHandUpSpringResolver.resolve` directly, on the real functions the popup calls.
 *
 * [PANEL_MAX_STRETCH_PX] is the popup tier's `iolMaxStretchDistance` (28dp) at the 620dpi device the
 * gesture recordings come from — 1dp = 3.875px, so 28dp = 108.5px.
 */
class CascadingPanelDragTest {

    private val maxStretch = PANEL_MAX_STRETCH_PX

    // region panelDragRubber

    @Test
    fun rubberBandIsZeroAtRest() {
        assertEquals(0f, panelDragRubber(0f, maxStretch))
    }

    @Test
    fun rubberBandIsFiniteEverywhere() {
        val samples = listOf(
            0f, 1f, -1f,
            1000f, -1000f,
            1160f, -1160f,
            2170f, -2170f,
            // The old signed-denominator form divided by zero at -max / curveRatio == -2170px and
            // diverged past it; anything beyond that must stay bounded, not flip sign to infinity.
            2170.0001f, -2170.0001f,
            1e5f, -1e5f, 1e7f, -1e7f,
        )
        samples.forEach { distance ->
            val out = panelDragRubber(distance, maxStretch)
            assertTrue(out.isFinite(), "panelDragRubber($distance) = $out is not finite")
            assertTrue(
                abs(out) < maxStretch,
                "panelDragRubber($distance) = $out must stay inside the $maxStretch px asymptote",
            )
        }
    }

    @Test
    fun rubberBandIsOddSymmetric() {
        listOf(0f, 1f, 37f, 1000f, 1160f, 2170f, 5000f, 1e6f).forEach { distance ->
            assertEquals(
                -panelDragRubber(distance, maxStretch),
                panelDragRubber(-distance, maxStretch),
                "panelDragRubber must be odd: f(-$distance) != -f($distance)",
            )
        }
    }

    @Test
    fun rubberBandKeepsTheFingerDirection() {
        assertTrue(panelDragRubber(600f, maxStretch) > 0f)
        assertTrue(panelDragRubber(-600f, maxStretch) < 0f)
    }

    @Test
    fun rubberBandIsMonotonic() {
        val samples = listOf(
            -1e7f, -2170f, -1160f, -1000f, -100f, 0f, 100f, 1000f, 1160f, 2170f, 1e7f,
        )
        samples.zipWithNext { low, high ->
            val lowOut = panelDragRubber(low, maxStretch)
            val highOut = panelDragRubber(high, maxStretch)
            assertTrue(
                lowOut < highOut,
                "panelDragRubber must be monotonic: f($low) = $lowOut !< f($high) = $highOut",
            )
        }
    }

    @Test
    fun rubberBandMatchesTheNativeCurveAtDeviceScale() {
        // signum(d) * (1 - 1 / (abs(d) * 0.05 / 108.5 + 1)) * 108.5
        assertEquals(34.2265f, panelDragRubber(1000f, maxStretch), 1e-3f)
        assertEquals(37.7959f, panelDragRubber(1160f, maxStretch), 1e-3f)
        assertEquals(54.25f, panelDragRubber(2170f, maxStretch), 1e-3f)
        assertEquals(75.6620f, panelDragRubber(5000f, maxStretch), 1e-3f)
        // The regression the signed form produced: -1000px upward used to map to -92.3px.
        assertEquals(-34.2265f, panelDragRubber(-1000f, maxStretch), 1e-3f)
        assertEquals(-54.25f, panelDragRubber(-2170f, maxStretch), 1e-3f)
    }

    @Test
    fun rubberBandDegradesToZeroWithoutATravelBudget() {
        assertEquals(0f, panelDragRubber(1000f, 0f))
        assertEquals(0f, panelDragRubber(-1000f, 0f))
        assertEquals(0f, panelDragRubber(1000f, -1f))
    }

    // endregion

    // region panelHandUpInitialVelocity

    @Test
    fun handUpVelocityScalesTheFingerSpeedByTheTrackerScale() {
        // (1000 px/s * 0.3) / 108.5 px * 34.2265 px
        assertEquals(
            94.6354f,
            panelHandUpInitialVelocity(1000f, 34.2265f, maxStretch),
            1e-2f,
        )
    }

    @Test
    fun handUpVelocityIsClampedAtTheTierCeiling() {
        assertEquals(
            PANEL_MAX_HAND_UP_VELOCITY,
            panelHandUpInitialVelocity(1e6f, maxStretch, maxStretch),
        )
        assertEquals(
            -PANEL_MAX_HAND_UP_VELOCITY,
            panelHandUpInitialVelocity(-1e6f, maxStretch, maxStretch),
        )
    }

    @Test
    fun handUpVelocityCollapsesWhenTheFingerStopped() {
        assertEquals(0f, panelHandUpInitialVelocity(0f, maxStretch, maxStretch))
    }

    @Test
    fun handUpVelocityDegradesToZeroWithoutATravelBudget() {
        assertEquals(0f, panelHandUpInitialVelocity(1000f, 10f, 0f))
    }

    // endregion

    // region panelHandUpBounce

    @Test
    fun handUpBounceNeverDropsBelowTheAreaValue() {
        assertEquals(0.42f, panelHandUpBounce(0f, 0.42f))
        assertEquals(0.42f, panelHandUpBounce(-100f, 0.42f))
        assertEquals(0.5f, panelHandUpBounce(500f, 0.42f))
    }

    @Test
    fun handUpBounceSaturatesAtOne() {
        assertEquals(1f, panelHandUpBounce(PANEL_MAX_HAND_UP_VELOCITY, 0.42f))
        assertEquals(1f, panelHandUpBounce(-1e6f, 0.42f))
    }

    // endregion

    // region panelHandUpAreaBounce

    @Test
    fun areaBounceRampsBetweenTheReferenceSquares() {
        val density = Density(1f)
        val small = 56f * 56f
        val large = 100f * 100f
        assertEquals(0.42f, panelHandUpAreaBounce(0f, density), 1e-4f)
        assertEquals(0.42f, panelHandUpAreaBounce(small, density), 1e-4f)
        assertEquals(0.45f, panelHandUpAreaBounce(large, density), 1e-4f)
        assertEquals(0.45f, panelHandUpAreaBounce(large * 10f, density), 1e-4f)
    }

    @Test
    fun areaBounceIsMonotonicAndBounded() {
        val density = Density(1f)
        val samples = listOf(0f, 1000f, 3136f, 5000f, 7000f, 10000f, 20000f)
        samples.zipWithNext { low, high ->
            val lowOut = panelHandUpAreaBounce(low, density)
            val highOut = panelHandUpAreaBounce(high, density)
            assertTrue(lowOut <= highOut, "area bounce must not decrease: $low -> $lowOut, $high -> $highOut")
        }
        samples.forEach { area ->
            val out = panelHandUpAreaBounce(area, density)
            assertTrue(out in 0.42f..0.45f, "area bounce $out out of range for area $area")
        }
    }

    @Test
    fun areaBounceUsesTheDensityScaledReferenceSquares() {
        // At 620dpi the reference squares are 217px and 387.5px on a side, so a menu larger than
        // 100dp across lands on the large-area bounce even though its px area dwarfs the dp² value.
        val density = Density(3.875f)
        assertEquals(0.45f, panelHandUpAreaBounce(652f * 900f, density), 1e-4f)
        assertEquals(0.42f, panelHandUpAreaBounce(100f * 100f, density), 1e-4f)
    }

    // endregion

    // region PanelVelocityTracker

    @Test
    fun velocityTrackerReportsTheSteadyDragSpeed() {
        val tracker = PanelVelocityTracker()
        // 10ms apart, 10px per sample: 1000 px/s.
        (0 until 8).forEach { i -> tracker.add(i * 10L, i * 10f) }
        assertEquals(1000f, tracker.velocityPxPerSecond(), 1e-3f)
    }

    @Test
    fun velocityTrackerStaysZeroOnTheFirstSample() {
        val tracker = PanelVelocityTracker()
        tracker.add(0L, 0f)
        assertEquals(0f, tracker.velocityPxPerSecond())
        tracker.add(16L, 40f)
        assertEquals(2500f, tracker.velocityPxPerSecond(), 1e-3f)
    }

    @Test
    fun velocityTrackerForgetsSamplesPastTheHorizon() {
        val tracker = PanelVelocityTracker()
        tracker.add(0L, 0f)
        tracker.add(10L, 10f)
        // A 200ms gap puts both older samples outside the 100ms horizon.
        tracker.add(210L, 400f)
        assertEquals(0f, tracker.velocityPxPerSecond())
        tracker.add(220L, 410f)
        assertEquals(1000f, tracker.velocityPxPerSecond(), 1e-3f)
    }

    @Test
    fun velocityTrackerReportsNetTravelWhenTheFingerReverses() {
        val tracker = PanelVelocityTracker()
        (0 until 8).forEach { i -> tracker.add(i * 10L, i * 10f) }
        assertTrue(tracker.velocityPxPerSecond() > 0f)
        listOf(80L to 50f, 90L to 40f, 100L to 30f, 110L to 20f).forEach { (t, travel) ->
            tracker.add(t, travel)
        }
        assertTrue(tracker.velocityPxPerSecond() < 0f)
    }

    @Test
    fun velocityTrackerKeepsTheSampleCountBounded() {
        val tracker = PanelVelocityTracker(horizonMs = 10_000L, capacity = 3)
        (0 until 20).forEach { i -> tracker.add(i * 10L, i * 10f) }
        // Only the last three samples survive, so the window is the 20ms they span.
        assertEquals(1000f, tracker.velocityPxPerSecond(), 1e-3f)
    }

    @Test
    fun velocityTrackerResetsBetweenGestures() {
        val tracker = PanelVelocityTracker()
        (0 until 8).forEach { i -> tracker.add(i * 10L, i * 10f) }
        tracker.reset()
        assertEquals(0f, tracker.velocityPxPerSecond())
        tracker.add(0L, 0f)
        assertEquals(0f, tracker.velocityPxPerSecond())
    }

    // endregion

    private companion object {
        /** `iolMaxStretchDistance` (28dp) at the 620dpi gesture-recording device. */
        const val PANEL_MAX_STRETCH_PX = 108.5f

        /** `iolHandUpMaxInitialVelocity`. */
        const val PANEL_MAX_HAND_UP_VELOCITY = 1000f
    }
}
