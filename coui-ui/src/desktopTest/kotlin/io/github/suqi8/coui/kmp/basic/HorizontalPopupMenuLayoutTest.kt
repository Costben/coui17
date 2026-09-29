// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ITEM_TAG_PREFIX = "horizontal-popup-item"

private fun itemTag(index: Int): String = "$ITEM_TAG_PREFIX-$index"

/** `coui-ui` does not depend on `coui-icons`, so the scene carries its own square. */
private val Icon: ImageVector = ImageVector.Builder(
    name = "horizontal-popup-test-icon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(0f, 0f)
        lineTo(24f, 0f)
        lineTo(24f, 24f)
        lineTo(0f, 24f)
        close()
    }
}.build()

/** A labelled item's text at 12sp, so a two-line label cannot fit the 40dp minimum height. */
private const val SHORT_LABEL = "Aa"

/**
 * Geometry of [HorizontalPopupMenu], pinned to the numbers
 * `MixedPopupListAdapter.configHorizontalItemLayout` produces on the native side.
 *
 * The scene puts the block in a [Box] and pins its width with a `Modifier.width`, so the block's
 * own intrinsic clamping (168dp .. 256dp, the popup window band) cannot move the target: every
 * expectation below is the algorithm applied to that width, at density 1 so dp and px coincide.
 */
@OptIn(ExperimentalTestApi::class)
class HorizontalPopupMenuLayoutTest {

    /**
     * Five icon-only items fill the popup's 256dp maximum width. `edge = 12`, `spacing = 8` and the
     * 256/5 = 51.2dp column leave `start`/`end` pairs of (12, 0), (9, 2), (6, 5), (2, 9), (0, 11),
     * i.e. every item keeps its 40dp square except the first, whose 39.2dp is truncated with the
     * column.
     */
    @Test
    fun iconOnlyItemsFillTheWindowWidthWithFortyDipSquares() = runSkikoComposeUiTest(
        size = Size(320f, 200f),
        density = Density(1f),
    ) {
        setContent { Block(width = 256.dp, itemCount = 5, labelled = false) }
        waitForIdle()

        val items = (0 until 5).map { itemBounds(it) }

        assertEquals(listOf(12f, 60f, 108f, 155f, 204f), items.map { it.left }, "column starts")
        assertEquals(listOf(39f, 40f, 40f, 40f, 40f), items.map { it.width }, "column widths")
        assertEquals(listOf(40f, 40f, 40f, 40f, 40f), items.map { it.height }, "icon-only height")
        assertEquals(1, items.map { it.top }.distinct().size, "one row only")
        assertTrue(
            items.last().right <= 256f,
            "the last item must stay inside the 256dp window, got ${items.last().right}",
        )
    }

    /**
     * One label switches the whole block to labelled metrics: three columns, the 52dp minimum
     * column width once the container can afford it, and a row at least `text_min_height` tall.
     */
    @Test
    fun labelsSwitchTheBlockToThreeColumnsAndTheFiftyTwoDipMinimum() = runSkikoComposeUiTest(
        size = Size(320f, 200f),
        density = Density(1f),
    ) {
        setContent { Block(width = 180.dp, itemCount = 3, labelled = true) }
        waitForIdle()

        val items = (0 until 3).map { itemBounds(it) }

        assertEquals(listOf(8f, 64f, 120f), items.map { it.left }, "three columns of 60dp")
        assertEquals(listOf(52f, 52f, 52f), items.map { it.width }, "min column width")
        assertEquals(1, items.map { it.top }.distinct().size, "one row only")
        assertTrue(
            items.all { it.height >= 40f },
            "a labelled item keeps the 40dp minimum height, got ${items.map { it.height }}",
        )
    }

    /**
     * Two icon-only items still occupy a three-column grid (`createMenuGridLayoutManager` floors the
     * span count at 3), so the pair keeps its 40dp squares instead of stretching across the block.
     */
    @Test
    fun anIconOnlyBlockKeepsItsThreeColumnFloor() = runSkikoComposeUiTest(
        size = Size(320f, 200f),
        density = Density(1f),
    ) {
        setContent { Block(width = 168.dp, itemCount = 2, labelled = false) }
        waitForIdle()

        val items = (0 until 2).map { itemBounds(it) }

        assertEquals(listOf(12f, 60f), items.map { it.left }, "the pair keeps the 3-column grid")
        assertEquals(listOf(40f, 40f), items.map { it.width }, "40dp squares, not stretched")
        assertEquals(listOf(40f, 40f), items.map { it.height }, "icon-only height")
    }

    /**
     * `setItemList` drops horizontal items past the column limit; Compose cannot un-compose them, so
     * the excess wraps onto a second grid row that keeps the first column's left edge.
     */
    @Test
    fun itemsPastTheColumnLimitWrapInsteadOfBeingDropped() = runSkikoComposeUiTest(
        size = Size(320f, 400f),
        density = Density(1f),
    ) {
        setContent { Block(width = 180.dp, itemCount = 4, labelled = true) }
        waitForIdle()

        val items = (0 until 4).map { itemBounds(it) }

        assertEquals(listOf(8f, 64f, 120f), items.take(3).map { it.left }, "first row")
        assertEquals(1, items.take(3).map { it.top }.distinct().size, "first row shares a top edge")
        assertEquals(8f, items[3].left, "the wrapped item keeps the first column's left edge")
        assertTrue(
            items[3].top >= items[0].bottom,
            "the wrapped item sits below the first row, got ${items[3].top} vs ${items[0].bottom}",
        )
    }
}

/**
 * A block of [itemCount] items, pinned to [width]. Half of the items carry a label once [labelled]
 * is set, which is what puts the block in labelled shape.
 */
@Composable
private fun Block(width: Dp, itemCount: Int, labelled: Boolean) {
    Box {
        HorizontalPopupMenu(modifier = Modifier.width(width)) {
            for (index in 0 until itemCount) {
                HorizontalPopupMenuItem(
                    icon = Icon,
                    label = if (labelled) SHORT_LABEL else null,
                    onClick = {},
                    modifier = Modifier.testTag(itemTag(index)),
                )
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.itemBounds(index: Int): Rect = onNodeWithTag(itemTag(index), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
