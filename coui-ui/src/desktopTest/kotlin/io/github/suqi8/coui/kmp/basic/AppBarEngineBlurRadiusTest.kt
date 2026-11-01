// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `AppBarBlurParams` carries HWUI radii (30 for the gradient's start, 50 for the uniform blur), and
 * HWUI converts a radius to a sigma with `0.57735`. COUI's engine derives its sigma from the radius
 * it is given with `0.45`, so the same platform radius has to be converted on the way in. Pure
 * arithmetic, so no composition is needed.
 */
class AppBarEngineBlurRadiusTest {

    private val tolerance = 1e-3f
    private val hwuiRadiusToSigma = 0.57735f
    private val engineSigmaScale = 0.45f

    /** The engine's sigma for a platform radius: radius x 0.57735, then the engine's own slope. */
    private fun engineSigma(hwuiRadiusPx: Float): Float = appBarEngineBlurRadiusPx(hwuiRadiusPx) * engineSigmaScale

    @Test
    fun platformTokensKeepTheFrameworkValues() {
        assertEquals(30f, AppBarGlassDefaults.GradientBlurRadiusPx)
        assertEquals(0f, AppBarGlassDefaults.GradientEndBlurRadiusPx)
        assertEquals(50f, AppBarGlassDefaults.BlurRadiusPx)
        assertEquals(3f, AppBarGlassDefaults.GradientBlurSpeed)
    }

    @Test
    fun engineRadiusReproducesThePlatformSigma() {
        assertEquals(30f * hwuiRadiusToSigma, engineSigma(30f), tolerance)
        assertEquals(50f * hwuiRadiusToSigma, engineSigma(50f), tolerance)
        assertEquals(17.3205f, engineSigma(30f), tolerance)
        assertEquals(28.8675f, engineSigma(50f), tolerance)
    }

    @Test
    fun gradientRadiusStaysOnThePlatformsDownscaleLevel() {
        // sigma^2 of 17.32 is 300, inside the 4x level (90.25..400) the native radius selects. The
        // previous value of 150 was a sigma of 150, i.e. 22500, three levels coarser.
        val sigma = engineSigma(AppBarGlassDefaults.GradientBlurRadiusPx)
        assertEquals(true, sigma * sigma in 90.25f..400f)
    }

    @Test
    fun zeroRadiusStaysZero() {
        assertEquals(0f, appBarEngineBlurRadiusPx(0f))
        assertEquals(0f, engineSigma(0f))
    }
}
