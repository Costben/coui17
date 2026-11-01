// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `COUIPopupMenuRootView.handleMainAreaDelayedDismissOnTouch`: with a submenu open, a drag that stays
 * inside the main menu hides the submenu only after `MAIN_AREA_DISMISS_DELAY_MS` (500ms), so the
 * finger can still slide back into the submenu or simply lift. A plain tap on the main menu keeps
 * collapsing at once, as before.
 */
@OptIn(ExperimentalTestApi::class)
class CascadingMainAreaDismissTest {
    @Test
    fun aDragInTheMainMenuCollapsesTheSubmenuAfterTheGracePeriod() = runMenu { menu ->
        menu.expand()
        menu.dragMainMenu()

        menu.clock.advanceTimeBy(520)
        menu.clock.advanceTimeBy(700)

        assertEquals(0, menu.submenuRowCount(), "The main-area drag must collapse the submenu after 500ms")
    }

    @Test
    fun slidingBackIntoTheSubmenuKeepsItOpen() = runMenu { menu ->
        menu.expand()
        val submenuRow = menu.submenuRowBounds()

        menu.clock.autoAdvance = false
        onRoot().performTouchInput { down(menu.mainMenuPoint) }
        onRoot().performTouchInput { moveBy(Offset(0f, MAIN_MENU_SLIDE)) }
        // Sliding into the submenu leaves the main-menu-exclusive area and disarms the runnable.
        onRoot().performTouchInput { moveTo(submenuRow.center) }
        menu.clock.advanceTimeBy(1_200)

        assertEquals(2, menu.submenuRowCount(), "The submenu must survive a slide back into it")

        onRoot().performTouchInput { up() }
        assertEquals(0, menu.dismissRequests, "Only the submenu is at stake, not the popup")
    }

    @Test
    fun aTapOnTheMainMenuStillCollapsesWithoutWaiting() = runMenu { menu ->
        menu.expand()
        val point = menu.mainMenuPoint

        onRoot().performTouchInput { click(point) }
        menu.clock.advanceTimeBy(900)

        assertEquals(0, menu.submenuRowCount(), "A tap on the main menu still collapses at once")
        assertEquals(0, menu.dismissRequests, "A tap on the main menu collapses the submenu, not the popup")
    }
}

private const val SUBMENU_ROW = "Child A"

/** Vertical travel of the main-area drag; comfortably past the touch slop and inside the menu. */
private const val MAIN_MENU_SLIDE = 48f

private const val TRIGGER_ROW = "More"

/**
 * The cascading layout composes the secondary twice: the measurement probe and the placed slot. Both
 * carry the same semantics, so an open submenu is two matching rows and a closed one none.
 */
private fun SkikoComposeUiTest.submenuRowCount(): Int = onAllNodesWithText(SUBMENU_ROW).fetchSemanticsNodes().size

private class MenuHarness(private val test: SkikoComposeUiTest) {
    val clock get() = test.mainClock
    var dismissRequests = 0

    /** A point inside the main menu and outside the submenu: the first row of the primary card. */
    val mainMenuPoint: Offset
        get() = test.onNodeWithText("Local").fetchSemanticsNode().boundsInRoot.center

    /** The placed submenu row, not the probe (which sits at the origin). */
    fun submenuRowBounds() = test.onAllNodesWithText(SUBMENU_ROW).fetchSemanticsNodes()
        .maxBy { it.boundsInRoot.top }
        .boundsInRoot

    fun submenuRowCount(): Int = test.submenuRowCount()

    fun expand() {
        test.onNodeWithText(TRIGGER_ROW).performClick()
        test.mainClock.advanceTimeBy(600)
        assertEquals(2, submenuRowCount(), "Tapping the trigger must open its submenu")
        test.mainClock.autoAdvance = false
    }

    /** Drags far enough inside the main menu to arm the 500ms dismiss. */
    fun dragMainMenu() {
        val point = mainMenuPoint
        test.onRoot().performTouchInput { down(point) }
        test.onRoot().performTouchInput { moveBy(Offset(0f, MAIN_MENU_SLIDE)) }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun runMenu(block: SkikoComposeUiTest.(MenuHarness) -> Unit) = runSkikoComposeUiTest(
    size = Size(420f, 760f),
    density = Density(1f),
) {
    val harness = MenuHarness(this)
    setContent { CascadingMenu(onDismissRequest = { harness.dismissRequests++ }) }
    waitForIdle()
    block(harness)
}

@Composable
private fun CascadingMenu(onDismissRequest: () -> Unit) {
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
                                    DropdownItem(text = "Share"),
                                    DropdownItem(text = "Edit"),
                                    DropdownItem(
                                        text = TRIGGER_ROW,
                                        children = listOf(
                                            DropdownItem(text = SUBMENU_ROW),
                                            DropdownItem(text = "Child B"),
                                        ),
                                    ),
                                    DropdownItem(text = "Delete"),
                                ),
                            ),
                        ),
                        onDismissRequest = onDismissRequest,
                        alignment = PopupPositionProvider.Align.BottomStart,
                    )
                }
            }
        }
    }
}
