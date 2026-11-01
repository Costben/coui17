// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `COUIPopupListWindow` installs a `RegionBasedLongPressDetector` with a 650ms timeout on the popup
 * root. The library arms the same timer on the row under the finger: the long click fires once the
 * hold elapses with the finger still on that row, the following release no longer selects, leaving
 * the row or lifting early cancels, and a row without a long-click action is not a target at all.
 */
@OptIn(ExperimentalTestApi::class)
class PopupListItemLongPressTest {
    @Test
    fun holdingARowDeliversItsLongClickAndSuppressesTheRelease() = runRows { rows ->
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(rows.centerOf("row0")) }
        mainClock.advanceTimeBy(660)

        assertEquals(listOf("long 0"), rows.events, "The hold must fire the row's long click at 650ms")

        onRoot().performTouchInput { up() }
        assertEquals(listOf("long 0"), rows.events, "A long-clicked row must not also click on release")
    }

    @Test
    fun liftingBeforeTheTimeoutOnlyClicks() = runRows { rows ->
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(rows.centerOf("row0")) }
        mainClock.advanceTimeBy(600)
        assertEquals(emptyList<String>(), rows.events, "The long click must not fire before 650ms")

        onRoot().performTouchInput { up() }
        assertEquals(listOf("click 0"), rows.events, "A short hold stays a plain click")
    }

    @Test
    fun movingToAnotherRowCancelsTheLongPress() = runRows { rows ->
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(rows.centerOf("row0")) }
        onRoot().performTouchInput { moveTo(rows.centerOf("row1")) }
        mainClock.advanceTimeBy(700)

        assertEquals(emptyList<String>(), rows.events, "The detector cancels once the finger leaves the target row")

        onRoot().performTouchInput { up() }
        assertEquals(listOf("click 1"), rows.events, "The release still selects the row under the finger")
    }

    @Test
    fun aRowWithoutALongClickKeepsItsPlainTap() = runRows { rows ->
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(rows.centerOf("row2")) }
        mainClock.advanceTimeBy(700)
        assertEquals(emptyList<String>(), rows.events, "A row with no long-click action is not a long-press target")

        onRoot().performTouchInput { up() }
        assertEquals(listOf("click 2"), rows.events)
    }
}

/** Runs [block] against a three-row column; `row0` and `row1` carry long clicks, `row2` does not. */
@OptIn(ExperimentalTestApi::class)
private fun runRows(block: SkikoComposeUiTest.(Rows) -> Unit) = runSkikoComposeUiTest(
    size = Size(260f, 420f),
    density = Density(1f),
) {
    val rows = Rows(this)
    setContent { RowsContent(rows) }
    waitForIdle()
    block(rows)
}

private class Rows(private val test: SkikoComposeUiTest) {
    val events = mutableListOf<String>()

    fun centerOf(tag: String): Offset = test.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center
}

@Composable
private fun RowsContent(rows: Rows) {
    COUITheme {
        var selected by remember { mutableStateOf(-1) }
        Box(Modifier.fillMaxSize()) {
            ListPopupColumn {
                val items = listOf("row0", "row1", "row2")
                items.forEachIndexed { index, tag ->
                    DropdownImpl(
                        item = DropdownItem(
                            text = "Row $index",
                            onLongClick = if (index < 2) {
                                { rows.events += "long $index" }
                            } else {
                                null
                            },
                        ),
                        optionSize = items.size,
                        isSelected = selected == index,
                        index = index,
                        onSelectedIndexChange = {
                            selected = it
                            rows.events += "click $it"
                        },
                        modifier = Modifier.testTag(tag),
                    )
                }
            }
        }
    }
}
