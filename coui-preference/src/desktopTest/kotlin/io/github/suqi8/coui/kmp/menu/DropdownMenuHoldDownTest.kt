// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A dropdown menu row is a popup anchor: opening it presses the row, and the press has to be
 * released as the menu starts closing, not after its exit animation settles.
 *
 * The rows are hosted in a [Scaffold], which carries the popup host the overlay popups render into.
 */
@OptIn(ExperimentalTestApi::class)
class DropdownMenuHoldDownTest {

    @Test
    fun overlayDropdownMenuSupportsCascadingSubmenu() = runSkikoComposeUiTest(
        size = Size(500f, 600f),
        density = Density(1f),
    ) {
        var childClicks = 0
        setContent {
            COUITheme {
                Scaffold {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        OverlayDropdownMenu(
                            entries = listOf(
                                DropdownEntry(
                                    items = listOf(
                                        DropdownItem(text = "Pinned"),
                                        DropdownItem(
                                            text = "Submenu",
                                            children = listOf(
                                                DropdownItem(
                                                    text = "Child Option 1",
                                                    onClick = { childClicks++ },
                                                ),
                                                DropdownItem(text = "Child Option 2"),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                            title = "Cascading Row",
                        )
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithText("Cascading Row").performClick()
        waitForIdle()

        onNodeWithText("Submenu").assertIsDisplayed()
        onNodeWithText("Submenu").performClick()
        waitForIdle()

        val childNodes = onAllNodesWithText("Child Option 1")
        assertEquals(2, childNodes.fetchSemanticsNodes().size, "Submenu must be open with placed and probe nodes")
        val placedChild = if (childNodes.fetchSemanticsNodes()[0].boundsInRoot.left > 0f) childNodes[0] else childNodes[1]
        placedChild.assertIsDisplayed()
        placedChild.performClick()
        waitForIdle()

        assertEquals(1, childClicks, "The child row has to report its click")
    }

    @Test
    fun dismissReleasesRowHoldDownWithMenuExit() = runSkikoComposeUiTest(
        size = Size(500f, 600f),
        density = Density(1f),
    ) {
        var expanded = false
        setContent {
            COUITheme {
                Scaffold {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        OverlayDropdownMenu(
                            entries = listOf(
                                DropdownEntry(
                                    items = listOf(
                                        DropdownItem(text = "Select me", onClick = {}),
                                    ),
                                ),
                            ),
                            title = "Dismiss Row",
                            onExpandedChange = { expanded = it },
                        )
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithText("Dismiss Row").performClick()
        waitForIdle()
        assertTrue(expanded, "The row has to open its menu")

        onNodeWithText("Select me").performClick()
        waitForIdle()

        assertFalse(expanded, "Selecting a row has to start the dismissal")
        onNodeWithText("Select me").assertDoesNotExist()
    }
}
