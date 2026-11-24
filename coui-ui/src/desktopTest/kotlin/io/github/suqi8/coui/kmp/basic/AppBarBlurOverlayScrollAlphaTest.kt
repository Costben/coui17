// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The scroll alpha ramp is a piecewise-linear function of the content's displacement, and the
 * floor overload re-maps that ramp into `minAlpha..1`. Both are pure arithmetic, so they are tested
 * without a composition.
 */
class AppBarBlurOverlayScrollAlphaTest {

    private val density = Density(1f)
    private val tolerance = 1e-4f

    private fun assertAlpha(expected: Float, actual: Float) {
        assertEquals(expected, actual, tolerance, "expected alpha $expected, got $actual")
    }

    @Test
    fun alphaRampsOverThirtyDp() {
        assertAlpha(0f, AppBarBlurOverlayDefaults.scrollAlpha(0f, density))
        assertAlpha(0.5f, AppBarBlurOverlayDefaults.scrollAlpha(15f, density))
        assertAlpha(1f, AppBarBlurOverlayDefaults.scrollAlpha(30f, density))
        assertAlpha(1f, AppBarBlurOverlayDefaults.scrollAlpha(60f, density))
    }

    @Test
    fun floorHoldsTheAlphaAtRest() {
        assertAlpha(0.55f, AppBarBlurOverlayDefaults.scrollAlpha(0f, density, minAlpha = 0.55f))
        assertAlpha(0.775f, AppBarBlurOverlayDefaults.scrollAlpha(15f, density, minAlpha = 0.55f))
        assertAlpha(1f, AppBarBlurOverlayDefaults.scrollAlpha(30f, density, minAlpha = 0.55f))
    }

    @Test
    fun negativeScrollKeepsTheRestingAlpha() {
        assertAlpha(0f, AppBarBlurOverlayDefaults.scrollAlpha(-20f, density))
        assertAlpha(
            0.55f,
            AppBarBlurOverlayDefaults.scrollAlpha(-20f, density, minAlpha = 0.55f),
        )
    }

    @Test
    fun outOfRangeFloorIsClamped() {
        assertAlpha(0f, AppBarBlurOverlayDefaults.scrollAlpha(0f, density, minAlpha = -1f))
        assertAlpha(0.5f, AppBarBlurOverlayDefaults.scrollAlpha(15f, density, minAlpha = -1f))
        assertAlpha(1f, AppBarBlurOverlayDefaults.scrollAlpha(0f, density, minAlpha = 2f))
    }

    @Test
    fun delayedRampHoldsZeroThroughStartOffset() {
        assertAlpha(0f, AppBarBlurOverlayDefaults.scrollAlpha(0f, density, startOffset = 22.dp, rampDistance = 56.dp))
        assertAlpha(0f, AppBarBlurOverlayDefaults.scrollAlpha(22f, density, startOffset = 22.dp, rampDistance = 56.dp))
        assertAlpha(0.5f, AppBarBlurOverlayDefaults.scrollAlpha(50f, density, startOffset = 22.dp, rampDistance = 56.dp))
        assertAlpha(1f, AppBarBlurOverlayDefaults.scrollAlpha(78f, density, startOffset = 22.dp, rampDistance = 56.dp))
        assertAlpha(1f, AppBarBlurOverlayDefaults.scrollAlpha(120f, density, startOffset = 22.dp, rampDistance = 56.dp))
    }

    @Test
    fun twoRowScrollAlphaSkipsChipBottomAndContentGap() {
        // SecondRowBottom (6dp) + default contentGap (16dp) = 22dp deadzone, 56dp ramp.
        assertAlpha(0f, TwoRowTopAppBarDefaults.scrollAlpha(0f, density))
        assertAlpha(0f, TwoRowTopAppBarDefaults.scrollAlpha(22f, density))
        assertAlpha(0.5f, TwoRowTopAppBarDefaults.scrollAlpha(50f, density))
        assertAlpha(1f, TwoRowTopAppBarDefaults.scrollAlpha(78f, density))
        // Custom 8dp contentGap -> 14dp deadzone (6dp + 8dp).
        assertAlpha(0f, TwoRowTopAppBarDefaults.scrollAlpha(14f, density, contentGap = 8.dp))
        assertAlpha(0.5f, TwoRowTopAppBarDefaults.scrollAlpha(42f, density, contentGap = 8.dp))
        assertAlpha(1f, TwoRowTopAppBarDefaults.scrollAlpha(70f, density, contentGap = 8.dp))
    }
}
