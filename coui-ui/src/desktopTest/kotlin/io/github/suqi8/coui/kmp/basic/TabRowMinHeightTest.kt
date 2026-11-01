// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `COUISegmentButtonLayout.onMeasure` measures a custom height as
 * `max(mCustomHeightPx, mMinHeightPx)` with `coui_segment_min_height` (28dp), so a segment row can
 * be shorter than its 40dp style height but never below 28dp.
 */
class TabRowMinHeightTest {

    @Test
    fun heightBelowTheFloorIsRaised() {
        assertEquals(28.dp, resolvedTabRowHeight(24.dp))
        assertEquals(28.dp, resolvedTabRowHeight(0.dp))
    }

    @Test
    fun heightAtOrAboveTheFloorIsKept() {
        assertEquals(28.dp, resolvedTabRowHeight(28.dp))
        assertEquals(TabRowDefaults.TabRowWithContourTinyHeight, resolvedTabRowHeight(TabRowDefaults.TabRowWithContourTinyHeight))
        assertEquals(40.dp, resolvedTabRowHeight(TabRowDefaults.TabRowHeight))
    }

    @Test
    fun theFloorIsTheNativeToken() {
        assertEquals(28.dp, TabRowDefaults.TabRowMinHeight)
    }
}
