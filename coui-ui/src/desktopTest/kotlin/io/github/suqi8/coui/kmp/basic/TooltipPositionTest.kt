// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TooltipPositionTest {
    private val window = IntSize(400, 800)
    private val bubble = IntSize(200, 60)

    private fun provider(position: TooltipAnchorPosition = TooltipAnchorPosition.Below): TooltipPopupPositionProvider {
        lateinit var result: PopupPositionProvider
        runSkikoComposeUiTest(size = Size(10f, 10f), density = Density(1f)) {
            setContent { result = TooltipDefaults.rememberTooltipPositionProvider(position) }
            waitForIdle()
        }
        return result as TooltipPopupPositionProvider
    }

    @Test
    fun aTooltipAtTheRightEdgeKeepsTheBubbleMarginFromTheWindowEdge() {
        val anchor = IntRect(left = 360, top = 100, right = 400, bottom = 140)
        val x = provider().calculatePosition(anchor, window, LayoutDirection.Ltr, bubble).x
        assertEquals(window.width - bubble.width - 16, x)
    }

    @Test
    fun aTooltipAtTheLeftEdgeKeepsTheBubbleMarginFromTheWindowEdge() {
        val anchor = IntRect(left = 0, top = 100, right = 40, bottom = 140)
        val x = provider().calculatePosition(anchor, window, LayoutDirection.Ltr, bubble).x
        assertEquals(16, x)
    }

    @Test
    fun aBubbleWiderThanTheMarginsAllowIsCentred() {
        val wide = IntSize(390, 60)
        val anchor = IntRect(left = 360, top = 100, right = 400, bottom = 140)
        val x = provider().calculatePosition(anchor, window, LayoutDirection.Ltr, wide).x
        assertEquals((window.width - wide.width) / 2, x)
    }

    @Test
    fun transformOriginPointsAtAnchorOnFacingEdge() {
        // Right-edge anchor with tooltip Below -> pivot sits on top edge (y = 0) near right end.
        val belowProvider = provider(TooltipAnchorPosition.Below)
        val rightAnchor = IntRect(left = 340, top = 100, right = 380, bottom = 140)
        val belowOffset = belowProvider.calculatePosition(rightAnchor, window, LayoutDirection.Ltr, bubble)
        val belowPivot = belowProvider.placementState.transformOrigin!!
        assertEquals(0f, belowPivot.pivotFractionY)
        assertEquals((360f - belowOffset.x) / bubble.width, belowPivot.pivotFractionX, 0.001f)

        // Anchor near bottom edge with Below preferred -> flips to Above, pivot moves to bottom edge (y = 1).
        val flipProvider = provider(TooltipAnchorPosition.Below)
        val bottomAnchor = IntRect(left = 180, top = 750, right = 220, bottom = 790)
        flipProvider.calculatePosition(bottomAnchor, window, LayoutDirection.Ltr, bubble)
        assertEquals(TooltipAnchorPosition.Above, flipProvider.placementState.resolvedPosition)
        assertEquals(1f, flipProvider.placementState.transformOrigin!!.pivotFractionY)

        // Left / Right placements pivot at x = 1 / x = 0 respectively.
        val leftProvider = provider(TooltipAnchorPosition.Left)
        val midAnchor = IntRect(left = 240, top = 300, right = 280, bottom = 340)
        leftProvider.calculatePosition(midAnchor, window, LayoutDirection.Ltr, bubble)
        assertEquals(1f, leftProvider.placementState.transformOrigin!!.pivotFractionX)
        assertEquals(0.5f, leftProvider.placementState.transformOrigin!!.pivotFractionY, 0.01f)

        val rightProvider = provider(TooltipAnchorPosition.Right)
        val leftAnchor = IntRect(left = 20, top = 300, right = 60, bottom = 340)
        rightProvider.calculatePosition(leftAnchor, window, LayoutDirection.Ltr, bubble)
        assertEquals(0f, rightProvider.placementState.transformOrigin!!.pivotFractionX)
        assertEquals(0.5f, rightProvider.placementState.transformOrigin!!.pivotFractionY, 0.01f)
    }

    @Test
    fun endOnRightEdgeAnchorFlipsToLeftWhenRoomFitsWithinEdgeMargin() {
        val endProvider = provider(TooltipAnchorPosition.End)
        val rightAnchor = IntRect(left = 300, top = 200, right = 340, bottom = 240)
        val offset = endProvider.calculatePosition(rightAnchor, window, LayoutDirection.Ltr, bubble)
        assertEquals(TooltipAnchorPosition.Left, endProvider.placementState.resolvedPosition)
        assertEquals(rightAnchor.left - bubble.width - 8, offset.x)
    }

    @Test
    fun sidePlacementFallsBackVerticallyWhenNeitherSideFitsWithinEdgeMargin() {
        val wideBubble = IntSize(260, 80)
        val centeredAnchor = IntRect(left = 180, top = 200, right = 220, bottom = 240)
        for (preferred in listOf(TooltipAnchorPosition.Start, TooltipAnchorPosition.End)) {
            val sideProvider = provider(preferred)
            val offset = sideProvider.calculatePosition(centeredAnchor, window, LayoutDirection.Ltr, wideBubble)
            assertEquals(
                TooltipAnchorPosition.Below,
                sideProvider.placementState.resolvedPosition,
                "Wide bubble with no room on Left or Right must fall back vertically for $preferred",
            )
            assertEquals(centeredAnchor.bottom + 8, offset.y)
        }
    }
}
