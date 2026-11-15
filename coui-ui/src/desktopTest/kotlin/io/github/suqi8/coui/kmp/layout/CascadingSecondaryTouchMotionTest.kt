// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.overlay.OverlayNavigationPopupMenu
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The expanded secondary menu owns the same `COUITouchMotion` rubber-band translation and
 * area-preserving deformation as the primary card across both axes, and springs back on release.
 */
@OptIn(ExperimentalTestApi::class)
class CascadingSecondaryTouchMotionTest {

    @Test
    fun draggingSecondaryHorizontallyAndVerticallyDeformsAndBouncesBack() = runMenu {
        expandSubmenu()
        val rest = submenuRowBounds()

        // Horizontal drag: submenu stretches horizontally, compresses vertically, shifts left.
        mainClock.autoAdvance = false
        onRoot().performTouchInput {
            down(rest.center)
            repeat(6) { moveBy(Offset(-25f, 0f), delayMillis = 16) }
        }
        mainClock.advanceTimeBy(180)
        val stretchedX = submenuRowBounds()
        assertTrue(
            stretchedX.width > rest.width + 1f,
            "Horizontal drag should stretch secondary width: ${rest.width} -> ${stretchedX.width}",
        )
        assertTrue(
            stretchedX.height < rest.height - 0.5f,
            "Horizontal drag should compress secondary height: ${rest.height} -> ${stretchedX.height}",
        )
        assertTrue(
            stretchedX.center.x < rest.center.x - 1f,
            "Horizontal drag should rubber-band secondary left: ${rest.center.x} -> ${stretchedX.center.x}",
        )

        onRoot().performTouchInput { up() }
        mainClock.advanceTimeBy(1_000)
        val afterX = submenuRowBounds()
        assertTrue(
            abs(afterX.width - rest.width) < 0.5f && abs(afterX.center.x - rest.center.x) < 0.5f,
            "Secondary should bounce back after horizontal release: rest=$rest, after=$afterX",
        )

        // Vertical drag: submenu stretches vertically, compresses horizontally, shifts down.
        onRoot().performTouchInput {
            down(rest.center)
            repeat(6) { moveBy(Offset(0f, 25f), delayMillis = 16) }
        }
        mainClock.advanceTimeBy(180)
        val stretchedY = submenuRowBounds()
        assertTrue(
            stretchedY.height > rest.height + 1f,
            "Vertical drag should stretch secondary height: ${rest.height} -> ${stretchedY.height}",
        )
        assertTrue(
            stretchedY.width < rest.width - 0.5f,
            "Vertical drag should compress secondary width: ${rest.width} -> ${stretchedY.width}",
        )
        assertTrue(
            stretchedY.center.y > rest.center.y + 1f,
            "Vertical drag should rubber-band secondary down: ${rest.center.y} -> ${stretchedY.center.y}",
        )

        onRoot().performTouchInput { up() }
        mainClock.advanceTimeBy(1_000)
        val afterY = submenuRowBounds()
        assertTrue(
            abs(afterY.height - rest.height) < 0.5f && abs(afterY.center.y - rest.center.y) < 0.5f,
            "Secondary should bounce back after vertical release: rest=$rest, after=$afterY",
        )
    }

    @Test
    fun tappingSubmenuLeafStillFiresSelection() {
        var clicked = 0
        runMenu(onLeafClick = { clicked++ }) {
            expandSubmenu()
            val leafCenter = submenuRowBounds().center
            onRoot().performTouchInput { click(leafCenter) }
            mainClock.advanceTimeBy(200)
            assertEquals(1, clicked, "Tapping a submenu leaf must still trigger its click handler")
        }
    }

    private fun SkikoComposeUiTest.expandSubmenu() {
        onNodeWithText(TRIGGER_ROW).performClick()
        mainClock.advanceTimeBy(600)
        assertEquals(2, onAllNodesWithText(SUBMENU_ROW).fetchSemanticsNodes().size)
    }

    /** The placed submenu row, not the measurement probe sitting at the origin. */
    private fun SkikoComposeUiTest.submenuRowBounds() = onAllNodesWithText(SUBMENU_ROW)
        .fetchSemanticsNodes()
        .maxBy { it.boundsInRoot.top }
        .boundsInRoot

    private fun runMenu(
        onLeafClick: () -> Unit = {},
        block: SkikoComposeUiTest.() -> Unit,
    ) = runSkikoComposeUiTest(
        size = Size(420f, 760f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Scaffold {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomStart) {
                        Box(Modifier.size(56.dp)) {
                            OverlayNavigationPopupMenu(
                                expanded = true,
                                entries = listOf(
                                    DropdownEntry(
                                        items = listOf(
                                            DropdownItem(text = "Local"),
                                            DropdownItem(text = "Export"),
                                            DropdownItem(
                                                text = TRIGGER_ROW,
                                                children = listOf(
                                                    DropdownItem(text = SUBMENU_ROW, onClick = onLeafClick),
                                                    DropdownItem(text = "Child B"),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                                onDismissRequest = {},
                                alignment = PopupPositionProvider.Align.BottomStart,
                                collapseOnSelection = false,
                            )
                        }
                    }
                }
            }
        }
        waitForIdle()
        block()
    }

    private companion object {
        const val TRIGGER_ROW = "More"
        const val SUBMENU_ROW = "Child A"
    }
}
