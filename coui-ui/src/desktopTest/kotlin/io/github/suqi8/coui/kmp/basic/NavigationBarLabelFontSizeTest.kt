// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `COUINavigationView` sizes its labels through
 * `COUIChangeTextUtil.getSuitableFontSize(size, fontScale, 2)`: below a font scale of 1.15 the label
 * keeps its size, above it the label reaches 1.15x its dp size and stops. In `sp` that is
 * `size * 1.15 / fontScale`, because `sp` already carries the font scale.
 */
class NavigationBarLabelFontSizeTest {

    private val tolerance = 1e-4f

    @Test
    fun defaultFontScaleKeepsTheSize() {
        assertEquals(10f, cappedLabelFontSizeSp(10f, 1f), tolerance)
        assertEquals(14f, cappedLabelFontSizeSp(14f, 1f), tolerance)
    }

    @Test
    fun upToTheCapTheSizeIsUntouched() {
        assertEquals(10f, cappedLabelFontSizeSp(10f, 0.85f), tolerance)
        assertEquals(10f, cappedLabelFontSizeSp(10f, 1f), tolerance)
        assertEquals(10f, cappedLabelFontSizeSp(10f, 1.15f), tolerance)
    }

    @Test
    fun aboveTheCapTheRenderedSizeStopsAtOnePointOneFive() {
        val fontScale = 2f
        val sp = cappedLabelFontSizeSp(10f, fontScale)
        assertEquals(5.75f, sp, tolerance)
        // What the label actually renders at: sp x font scale.
        assertEquals(10f * 1.15f, sp * fontScale, tolerance)
    }

    @Test
    fun theLargestSystemFontStillFitsTheCell() {
        // The 10dp label over the largest font scale renders at 11.5dp, not 20dp.
        val rendered = cappedLabelFontSizeSp(10f, 2f) * 2f
        assertEquals(11.5f, rendered, tolerance)
    }
}
