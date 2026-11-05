// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Checks [SlideView]'s geometry against ColorOS 17 Calendar's custom-reminder row on a
 * 3.875 px/dp device: the row spans x 62..1178 (1116px) and the delete action is 80dp, 310px.
 */
class SlideViewMathTest {
    private val rowStart = 62f
    private val rowWidth = 1116f
    private val holder = 310f
    private val widths = listOf(holder)

    @Test
    fun openDeleteActionStartsWhereTheDeviceFillStarts() {
        // Device: the red fill of the open row starts at x = 868.
        assertEquals(868f, rowStart + slideMenuItemStart(0, holder, rowWidth, holder, widths), 0.5f)
    }

    @Test
    fun deleteKeepsTheLabelCentredInTheWideningFill() {
        // Device: when the delete has slid the content out, the label is centred at x = 619.5.
        val start = slideMenuItemStart(0, rowWidth, rowWidth, holder, widths)
        assertEquals(620f, rowStart + start + holder / 2f, 1f)
        // Half-way through, the label stays centred in the visible fill.
        val scroll = 600f
        val centre = slideMenuItemStart(0, scroll, rowWidth, holder, widths) + holder / 2f
        assertEquals(rowWidth - scroll / 2f, centre, 0.01f)
    }

    @Test
    fun actionsRunFromTheEndEdgeInwards() {
        val two = listOf(240f, 200f)
        assertEquals(rowWidth - 240f, slideMenuItemStart(0, 440f, rowWidth, 440f, two), 0.01f)
        assertEquals(rowWidth - 440f, slideMenuItemStart(1, 440f, rowWidth, 440f, two), 0.01f)
        assertEquals(0, menuIndexAt(300f, two))
        assertEquals(1, menuIndexAt(100f, two))
        assertEquals(-1, menuIndexAt(500f, two))
    }

    @Test
    fun releaseFollowsTheFlingThenTheHalfWayPoint() {
        assertEquals(SlideRelease.Open, slideReleaseTarget(100f, holder, -1500f, false, 116f, 186f))
        assertEquals(SlideRelease.Close, slideReleaseTarget(300f, holder, 1500f, false, 116f, 186f))
        assertEquals(SlideRelease.Open, slideReleaseTarget(160f, holder, 0f, false, 116f, 186f))
        assertEquals(SlideRelease.Close, slideReleaseTarget(150f, holder, 0f, false, 116f, 186f))
        // Without fast delete a long slide only opens the menu.
        assertEquals(SlideRelease.Open, slideReleaseTarget(900f, holder, -3000f, false, 116f, 186f))
    }

    @Test
    fun fastDeleteNeedsDistanceOrAFlingPastTheQuickSlop() {
        assertEquals(SlideRelease.Delete, slideReleaseTarget(holder + 190f, holder, 0f, true, 116f, 186f))
        assertEquals(SlideRelease.Open, slideReleaseTarget(holder + 120f, holder, 0f, true, 116f, 186f))
        assertEquals(SlideRelease.Delete, slideReleaseTarget(holder + 120f, holder, -1500f, true, 116f, 186f))
        // The fast-delete branch ignores a closing fling.
        assertEquals(SlideRelease.Open, slideReleaseTarget(200f, holder, 3000f, true, 116f, 186f))
    }
}
