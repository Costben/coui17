// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The sub menu shares the main menu's leading edge and must stay inside the window: a sub menu can
 * be wider than the main menu, and the main menu can sit flush against the trailing edge. In RTL the
 * leading edge is the right one, so the two directions are mirror images rather than the same rect.
 *
 * Vertical placement is checked too: the tapped row is mapped through [PRIMARY_SHRUNK_SCALE] (0.9)
 * before the overlap is subtracted, and both window edges clamp the result.
 */
class CascadingSecondaryRectTest {

    private val available = IntRect(left = 0, top = 0, right = 1080, bottom = 2400)
    private val secondarySize = IntSize(width = 360, height = 900)

    @Test
    fun ltrKeepsPrimaryLeadingEdge() {
        // primary right edge = 1000, sub menu left follows the primary's left edge at 650.
        val rect = placement(
            primary = IntRect(650, 800, 1000, 1400),
            layoutDirection = LayoutDirection.Ltr,
        )
        assertEquals(650, rect.left)
        assertEquals(1010, rect.right)
    }

    @Test
    fun ltrClampsASubMenuWiderThanTheMainMenuIntoTheWindow() {
        // The main menu is flush against the right edge; 800 + 360 would leave the 1080px window.
        val rect = placement(
            primary = IntRect(800, 800, 1080, 1400),
            layoutDirection = LayoutDirection.Ltr,
        )
        assertEquals(720, rect.left)
        assertEquals(1080, rect.right)
    }

    @Test
    fun rtlSharesThePrimaryTrailingEdge() {
        // Mirror of the LTR case: the sub menu's right edge follows the primary's right edge.
        val primary = IntRect(650, 800, 1000, 1400)
        val rect = placement(primary = primary, layoutDirection = LayoutDirection.Rtl)
        assertEquals(640, rect.left)
        assertEquals(1000, rect.right)
    }

    @Test
    fun rtlClampsASubMenuWiderThanTheMainMenuIntoTheWindow() {
        // Mirror of the flush-right case: the sub menu would start at -80.
        val rect = placement(
            primary = IntRect(0, 800, 280, 1400),
            layoutDirection = LayoutDirection.Rtl,
        )
        assertEquals(0, rect.left)
        assertEquals(360, rect.right)
    }

    @Test
    fun topMapsTheAnchorThroughTheShrinkRatioThenSubtractsTheOverlap() {
        // primary.top 800, anchor.top 1000: 800 + 0.9 * 200 - 54 = 926.
        val rect = placement(
            primary = IntRect(650, 800, 1000, 1400),
            anchor = IntRect(650, 1000, 1000, 1100),
            verticalOverlapPx = 54,
        )
        assertEquals(926, rect.top)
        assertEquals(1826, rect.bottom)
    }

    @Test
    fun topIsPinnedToTheBottomWhenTheSubMenuOverflows() {
        val rect = placement(
            primary = IntRect(650, 2200, 1000, 2380),
            anchor = IntRect(650, 2200, 1000, 2300),
            verticalOverlapPx = 54,
        )
        assertEquals(1500, rect.top)
        assertEquals(2400, rect.bottom)
    }

    @Test
    fun topIsClampedToTheAvailableTop() {
        val narrow = IntRect(left = 0, top = 1000, right = 1080, bottom = 2400)
        val rect = placement(
            primary = IntRect(650, 1000, 1000, 1400),
            anchor = IntRect(650, 1000, 1000, 1100),
            verticalOverlapPx = 54,
            bounds = narrow,
        )
        assertEquals(1000, rect.top)
        assertEquals(1900, rect.bottom)
    }

    private fun placement(
        primary: IntRect,
        anchor: IntRect = IntRect(primary.left, primary.top + 200, primary.right, primary.top + 300),
        verticalOverlapPx: Int = 0,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        bounds: IntRect = available,
    ): IntRect = computeSecondaryRect(
        primary = primary,
        anchor = anchor,
        secondarySize = secondarySize,
        available = bounds,
        layoutDirection = layoutDirection,
        verticalOverlapPx = verticalOverlapPx,
    )
}
