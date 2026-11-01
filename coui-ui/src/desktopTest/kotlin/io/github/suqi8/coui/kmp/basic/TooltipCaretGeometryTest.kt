// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `COUIToolTips` draws the bubble indicator from two vector drawables; these pin the traced outline
 * to their path data and keep the bubble inset consistent with the arrow height.
 */
class TooltipCaretGeometryTest {

    private val epsilon = 0.05f

    private fun List<Offset>.bounds(): FloatArray = floatArrayOf(
        minOf { it.x },
        minOf { it.y },
        maxOf { it.x },
        maxOf { it.y },
    )

    @Test
    fun theCentredArrowFillsItsDrawableAndRestsOnTheBubbleEdge() {
        val outline = TooltipCaretGeometry.outline(edge = false, mirror = false)
        val bounds = outline.bounds()
        assertEquals(0f, bounds[0], epsilon)
        assertEquals(TooltipCaretGeometry.MIDDLE_WIDTH, bounds[2], epsilon)
        assertEquals(0f, bounds[1], epsilon)
        assertEquals(TooltipCaretGeometry.MIDDLE_HEIGHT, bounds[3], epsilon)
        // Both feet sit on the base line, which is the only straight run between them.
        val feet = outline.filter { abs(it.y - TooltipCaretGeometry.MIDDLE_HEIGHT) < epsilon }
        assertTrue(feet.size >= 2, "the arrow base must run along the bubble edge")
    }

    @Test
    fun theCentredArrowApexIsOnTheCentreLine() {
        val outline = TooltipCaretGeometry.outline(edge = false, mirror = false)
        val apex = outline.minBy { it.y }
        assertEquals(TooltipCaretGeometry.MIDDLE_APEX, apex.x, 0.5f)
        assertEquals(0f, apex.y, epsilon)
    }

    @Test
    fun theEdgeArrowHasAFlatSideOnTheRight() {
        val outline = TooltipCaretGeometry.outline(edge = true, mirror = false)
        val bounds = outline.bounds()
        assertEquals(TooltipCaretGeometry.EDGE_WIDTH, bounds[2], epsilon)
        assertEquals(TooltipCaretGeometry.EDGE_HEIGHT, bounds[3], epsilon)
        // The wall runs from the rounded tip corner down to the base without any x drift.
        val wall = outline.filter { abs(it.x - TooltipCaretGeometry.EDGE_WIDTH) < epsilon }
        assertTrue(wall.size >= 2, "the edge arrow must keep a vertical wall")
        assertTrue(wall.maxOf { it.y } - wall.minOf { it.y } > 8f, "the wall spans most of the height")
    }

    @Test
    fun mirroringFlipsTheArrowAroundItsOwnCentre() {
        val straight = TooltipCaretGeometry.outline(edge = true, mirror = false)
        val mirrored = TooltipCaretGeometry.outline(edge = true, mirror = true)
        assertEquals(straight.size, mirrored.size)
        straight.forEachIndexed { index, point ->
            assertEquals(
                TooltipCaretGeometry.EDGE_WIDTH - point.x,
                mirrored[index].x,
                epsilon,
                "point $index",
            )
            assertEquals(point.y, mirrored[index].y, epsilon, "point $index")
        }
        // The mirrored arrow keeps its apex, which is what makes the tip point at the anchor.
        val apex = mirrored.minBy { it.y }
        assertEquals(TooltipCaretGeometry.EDGE_WIDTH - TooltipCaretGeometry.EDGE_APEX, apex.x, 0.5f)
    }

    @Test
    fun theBubbleIsInsetSoBothArrowHeightsOverlapTheEdge() {
        val padding = TooltipDefaults.CaretPadding.value
        val embed = TooltipDefaults.CaretEmbedDistance.value
        assertEquals(TooltipDefaults.CaretEdgeHeight.value, padding + embed, 0.001f)
        assertTrue(padding < TooltipDefaults.CaretHeight.value, "the centred arrow must overlap too")
    }
}
