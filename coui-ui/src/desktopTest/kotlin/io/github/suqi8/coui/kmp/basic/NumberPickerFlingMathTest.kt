// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The wheel picker's throw uses `COUINumberPicker`'s own spline, not a Compose decay: the distance
 * and duration come from `getSplineFlingDistance` / `getSplineFlingDuration` with the framework's
 * scroll friction and `computeDeceleration(0.84f)`.
 */
class NumberPickerFlingMathTest {

    // A 3x device: mPpi = 3 * 160, mPhysicalCoeff = mPpi * 386.0878 * 0.84.
    private val density = 3f

    @Test
    fun splineDecelerationMatchesTheFramework() {
        val deceleration = splineDeceleration(5000f, density)
        // ln(5000 * 0.35 / (0.015 * 3 * 160 * 386.0878 * 0.84))
        assertEquals(-0.2886f, deceleration, 0.001f)
    }

    @Test
    fun splineFlingDistanceAndDurationMatchTheFramework() {
        assertEquals(1414.6f, splineFlingDistance(5000f, density), 3f)
        // The native runs the throw for 1.5x this duration.
        assertEquals(808.5f, splineFlingDuration(5000f, density), 3f)
    }

    @Test
    fun fasterReleasesThrowFurtherAndLonger() {
        val slow = splineFlingDistance(1000f, density)
        val fast = splineFlingDistance(4000f, density)
        assertTrue(fast > slow, "distance must grow with the release velocity")
        assertTrue(
            splineFlingDuration(4000f, density) > splineFlingDuration(1000f, density),
            "duration must grow with the release velocity",
        )
    }

    @Test
    fun aFullSpeedThrowClearsAboutTenRows() {
        // 45dp rows at 3x are 135px; the native truncates the spline distance to whole elements.
        val rowPx = 135f
        val distance = splineFlingDistance(5000f, density)
        val wholeRows = (distance - distance % rowPx) / rowPx
        assertEquals(10f, wholeRows)
    }
}

/** `COUIMarqueeTextView.checkAllCharactersDirections` decides the scroll direction from the text. */
class MarqueeTextDirectionTest {

    @Test
    fun leftToRightTextScrollsNormally() {
        assertFalse(containsRtlChar("Now playing: track one"))
        assertFalse(containsRtlChar("正在播放"))
        assertFalse(containsRtlChar(""))
    }

    @Test
    fun rightToLeftCharactersMirrorTheScroll() {
        assertTrue(containsRtlChar("שלום"))
        assertTrue(containsRtlChar("مرحبا"))
        assertTrue(containsRtlChar("Track · שלום"))
    }
}
