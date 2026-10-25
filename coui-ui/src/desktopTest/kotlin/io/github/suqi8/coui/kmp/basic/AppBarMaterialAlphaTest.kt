// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The material alpha of a scroll-driven app bar must move continuously with the content and the
 * collapse animation. It used to snap to `1` as soon as the bar left its expanded position and back
 * to `0` on the last frame of the expand animation, which flashed the material on and off.
 */
class AppBarMaterialAlphaTest {

    private val density = Density(1f)
    private val tolerance = 1e-4f

    private fun assertAlpha(expected: Float, actual: Float) {
        assertEquals(expected, actual, tolerance, "expected alpha $expected, got $actual")
    }

    @Test
    fun restingBarDrawsNoMaterial() {
        assertAlpha(0f, appBarMaterialAlpha(contentOffset = 0f, collapsedFraction = 0f, density = density))
    }

    @Test
    fun partlyCollapsedBarFollowsTheCollapseInsteadOfSnappingToOpaque() {
        assertAlpha(0.25f, appBarMaterialAlpha(contentOffset = 0f, collapsedFraction = 0.25f, density = density))
        assertAlpha(0.02f, appBarMaterialAlpha(contentOffset = 0f, collapsedFraction = 0.02f, density = density))
    }

    @Test
    fun contentDisplacementRampsOverThirtyDp() {
        assertAlpha(0.5f, appBarMaterialAlpha(contentOffset = -15f, collapsedFraction = 0f, density = density))
        assertAlpha(1f, appBarMaterialAlpha(contentOffset = -45f, collapsedFraction = 0f, density = density))
    }

    @Test
    fun collapsedBarKeepsItsMaterialWhenTheOffsetReadsZero() {
        assertAlpha(1f, appBarMaterialAlpha(contentOffset = 0f, collapsedFraction = 1f, density = density))
    }

    @Test
    fun theLargerInputWins() {
        assertAlpha(0.6f, appBarMaterialAlpha(contentOffset = -18f, collapsedFraction = 0.3f, density = density))
    }
}
