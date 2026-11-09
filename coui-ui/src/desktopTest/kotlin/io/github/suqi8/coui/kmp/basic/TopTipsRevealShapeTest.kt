// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class TopTipsRevealShapeTest {
    private val density = Density(1f)
    private val banner = Size(288f, 76f)

    @Test
    fun theClipGrowsFromTheTopEdgeWhileTheBannerKeepsItsWidth() {
        // COUIDefaultTopTips.calculateClipHeight: the outline is the banner's silhouette at the
        // animated height, anchored to the top edge.
        val half = TopTipsRevealShape(topTipsSingleLineShape(), 0.5f)
            .createOutline(banner, LayoutDirection.Ltr, density).bounds
        assertEquals(0f, half.top)
        assertEquals(38f, half.bottom, 0.01f)
        assertEquals(288f, half.width, 0.01f)

        val full = TopTipsRevealShape(topTipsSingleLineShape(), 1f)
            .createOutline(banner, LayoutDirection.Ltr, density).bounds
        assertEquals(76f, full.bottom, 0.01f)
    }
}
