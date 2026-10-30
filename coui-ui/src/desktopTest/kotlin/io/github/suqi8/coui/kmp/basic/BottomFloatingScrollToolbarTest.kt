// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals

class BottomFloatingScrollToolbarTest {
    private fun target(itemStart: Int, scroll: Int) = scrollToolbarRevealTarget(
        itemStart = itemStart,
        itemWidth = ITEM,
        scroll = scroll,
        viewport = VIEWPORT,
        padding = PADDING,
    )

    @Test
    fun anItemNearTheEndRevealsTheNextOne() {
        // The item at 800 is near the end, and the one after it ends at 1000 past the visible 884.
        assertEquals(1000 + PADDING - VIEWPORT, target(itemStart = 800, scroll = 0))
    }

    @Test
    fun anItemNearTheStartRevealsThePreviousOne() {
        // The item at 400 sits near the start at scroll 300, and the one before it starts at 300.
        assertEquals(300 - PADDING, target(itemStart = 400, scroll = 300))
    }

    @Test
    fun aVisibleNeighbourLeavesTheListAlone() {
        assertEquals(0, target(itemStart = 300, scroll = 0))
        assertEquals(120, target(itemStart = 600, scroll = 120))
    }

    @Test
    fun theFirstItemNeverAsksForANegativeScroll() {
        assertEquals(0, target(itemStart = PADDING, scroll = 40))
    }

    private companion object {
        const val ITEM = 100
        const val VIEWPORT = 1000
        const val PADDING = 16
    }
}
