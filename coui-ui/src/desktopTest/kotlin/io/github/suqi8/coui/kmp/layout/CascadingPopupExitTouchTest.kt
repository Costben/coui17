// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A closing cascading popup keeps shrinking into its anchor, but the page beneath already owns the
 * touch stream: a drag that starts while the exit plays — beside the panel or on the shrinking panel
 * itself — scrolls the page instead of being swallowed until the animation settles.
 */
@OptIn(ExperimentalTestApi::class)
class CascadingPopupExitTouchTest {
    @Test
    fun anOpenMenuStillOwnsTheTouchesAroundIt() = runSkikoComposeUiTest(size = Size(400f, 600f), density = Density(1f)) {
        val page = PageDrags()
        setContent { MenuOverPage(show = true, page = page) }
        waitForIdle()
        drag(from = Offset(300f, 120f), by = Offset(0f, 120f))
        assertEquals(0f, page.total, "An open menu is modal: the page must not move beneath it")
    }

    @Test
    fun aDragBesideTheClosingPanelMovesThePage() = runSkikoComposeUiTest(size = Size(400f, 600f), density = Density(1f)) {
        val page = PageDrags()
        var show by mutableStateOf(true)
        var finished = false
        setContent { MenuOverPage(show = show, page = page, onDismissFinished = { finished = true }) }
        waitForIdle()
        mainClock.autoAdvance = false
        show = false
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        drag(from = Offset(300f, 120f), by = Offset(-80f, 120f))
        assertFalse(finished, "The drag has to land while the exit is still running")
        assertTrue(page.total > 100f, "The page has to follow a drag made during the exit, moved ${page.total}")
    }

    @Test
    fun aDragOnTheShrinkingPanelMovesThePageAndSelectsNothing() = runSkikoComposeUiTest(size = Size(400f, 600f), density = Density(1f)) {
        val page = PageDrags()
        var show by mutableStateOf(true)
        var finished = false
        var clicked = false
        setContent {
            MenuOverPage(show = show, page = page, onClick = { clicked = true }, onDismissFinished = { finished = true })
        }
        waitForIdle()
        val row = onNodeWithText("Local").fetchSemanticsNode().boundsInRoot
        mainClock.autoAdvance = false
        show = false
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        drag(from = row.center, by = Offset(0f, -120f))
        assertFalse(finished, "The drag has to land while the exit is still running")
        assertFalse(clicked, "A closing menu must not run a row's action")
        assertTrue(page.total > 100f, "A drag on the closing panel belongs to the page, moved ${page.total}")
    }
}

private class PageDrags {
    var total = 0f
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.drag(from: Offset, by: Offset) {
    onRoot().performTouchInput {
        down(from)
        repeat(6) { moveBy(Offset(by.x / 6f, by.y / 6f), delayMillis = 8) }
        up()
    }
}

@Composable
private fun MenuOverPage(
    show: Boolean,
    page: PageDrags,
    onClick: () -> Unit = {},
    onDismissFinished: () -> Unit = {},
) {
    COUITheme {
        Scaffold {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            page.total += abs(amount.x) + abs(amount.y)
                        }
                    },
            )
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomStart) {
                Box(Modifier.size(56.dp)) {
                    OverlayNavigationPopupMenu(
                        expanded = show,
                        entries = listOf(
                            DropdownEntry(
                                items = listOf(
                                    DropdownItem(text = "Local", onClick = onClick),
                                    DropdownItem(text = "Export", onClick = onClick),
                                    DropdownItem(text = "Model", onClick = onClick),
                                ),
                            ),
                        ),
                        onDismissRequest = {},
                        alignment = PopupPositionProvider.Align.BottomStart,
                        onDismissFinished = onDismissFinished,
                    )
                }
            }
        }
    }
}
