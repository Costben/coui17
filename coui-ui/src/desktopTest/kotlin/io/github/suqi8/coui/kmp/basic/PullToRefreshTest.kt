// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PullToRefreshTest {
    @Test
    fun indicatorIsTheCouiLoadingSizeWithoutText() {
        assertEquals(26.dp, PullToRefreshDefaults.circleSize)
        assertTrue(PullToRefreshDefaults.refreshTexts.isEmpty())
    }

    @Test
    fun dragFollowsTheNativeDampingAndResistance() {
        // PullLayout: offset += delta / (2.5 / (1 - offset / height)).
        assertEquals(40f, pullToRefreshDragStep(offset = 0f, delta = 100f, height = 1000f), 0.001f)
        assertEquals(520f, pullToRefreshDragStep(offset = 500f, delta = 100f, height = 1000f), 0.001f)
    }

    @Test
    fun dragStopsAtTheContainerEdges() {
        assertEquals(0f, pullToRefreshDragStep(offset = 10f, delta = -1000f, height = 1000f), 0.001f)
        assertEquals(1000f, pullToRefreshDragStep(offset = 1000f, delta = 100f, height = 1000f), 0.001f)
        assertEquals(10f, pullToRefreshDragStep(offset = 10f, delta = 100f, height = 0f), 0.001f)
    }

    @Test
    fun indicatorTurnsOnlyAfterItIsFullyRevealed() {
        // DefaultHeader: 270 at rest, a quarter turn between one and two indicator heights, then
        // half a degree per pixel past the threshold.
        assertEquals(270f, pullToRefreshIndicatorRotation(offset = 10f, begin = 26f, threshold = 52f), 0.001f)
        assertEquals(270f, pullToRefreshIndicatorRotation(offset = 26f, begin = 26f, threshold = 52f), 0.001f)
        assertEquals(315f, pullToRefreshIndicatorRotation(offset = 39f, begin = 26f, threshold = 52f), 0.001f)
        assertEquals(0f, pullToRefreshIndicatorRotation(offset = 52f, begin = 26f, threshold = 52f), 0.001f)
        assertEquals(10f, pullToRefreshIndicatorRotation(offset = 72f, begin = 26f, threshold = 52f), 0.001f)
    }

    @Test
    fun headerKeepsAnEmptyBandAboveTheContent() {
        // The indicator block is bottom-anchored, so without this it sits flush on the list.
        assertEquals(8.dp, PullToRefreshDefaults.HeaderBottomGap)
    }
}
