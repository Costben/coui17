// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MarqueeTextLayoutTest {

    /**
     * An overflowing [MarqueeText] must span its full available width and anchor the first copy at
     * [MarqueeTextDefaults.ScrollStartLocation] (not center the oversized scrolling Row, which
     * previously pushed the first glyph to a negative X offset).
     */
    @Test
    fun overflowingMarqueeStartsAtStartLocationAndFillsWidth() = runComposeUiTest {
        val sample = "0123456789 This marquee text is deliberately much wider than a 240dp box"
        val startLocation = 10.dp
        val boxWidth = 240.dp
        mainClock.autoAdvance = false
        setContent {
            MarqueeText(
                text = sample,
                modifier = Modifier
                    .width(boxWidth)
                    .testTag("marquee"),
                startLocation = startLocation,
                scrollDelayMillis = 60_000L,
            )
        }
        mainClock.advanceTimeByFrame()

        val containerBounds = onNodeWithTag("marquee").fetchSemanticsNode().boundsInRoot
        val expectedWidthPx = with(density) { boxWidth.toPx() }
        assertEquals(expectedWidthPx, containerBounds.width, 1f, "marquee container must span its full width")

        val copies = onAllNodesWithText(sample).fetchSemanticsNodes()
        assertEquals(2, copies.size, "overflowing marquee must render two copies for seamless looping")

        val expectedStartPx = containerBounds.left + with(density) { startLocation.toPx() }
        val firstCopyLeft = copies.minOf { it.positionInRoot.x }
        assertTrue(
            abs(firstCopyLeft - expectedStartPx) <= 2f,
            "first copy must anchor at startLocation ($expectedStartPx), got $firstCopyLeft",
        )
    }

    @Test
    fun fillMaxWidthMarqueeUsesParentWidth() = runComposeUiTest {
        val sample = "A very long marquee line that overflows the entire width of the parent container smoothly"
        mainClock.autoAdvance = false
        setContent {
            MarqueeText(
                text = sample,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("marquee-full"),
                scrollDelayMillis = 60_000L,
            )
        }
        mainClock.advanceTimeByFrame()

        val bounds = onNodeWithTag("marquee-full").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width > 300f, "fillMaxWidth marquee must span the full parent row, got ${bounds.width}")
    }
}
