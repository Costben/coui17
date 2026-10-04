// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ListPopupInteractionLifecycleTest {
    @Test
    fun topAndBottomActionsOpenTowardTheAvailableSpace() {
        for (top in listOf(true, false)) {
            runSkikoComposeUiTest(size = Size(500f, 600f), density = Density(1f)) {
                setContent { AnchoredMenu(show = true, top = top) }
                waitForIdle()
                val anchor = onNodeWithTag("anchor").fetchSemanticsNode().boundsInRoot
                val row = onNodeWithTag("action").fetchSemanticsNode().boundsInRoot
                assertTrue(if (top) row.top > anchor.bottom else row.bottom < anchor.top, "The menu opens away from the toolbar: anchor=$anchor row=$row")
                assertTrue(row.left >= 0f && row.right <= 500f && row.top >= 0f && row.bottom <= 600f)
            }
        }
    }

    @Test
    fun dismissalDisablesActionsWhileTheHostFinishesAnimating() = runSkikoComposeUiTest(size = Size(500f, 600f), density = Density(1f)) {
        var show by mutableStateOf(true)
        var finished = false
        setContent { AnchoredMenu(show = show, onDismissFinished = { finished = true }) }
        waitForIdle()
        onNodeWithTag("action").assertIsEnabled()
        mainClock.autoAdvance = false
        show = false
        mainClock.advanceTimeByFrame()
        onNodeWithTag("action").assertIsNotEnabled()
        assertFalse(finished)
        mainClock.advanceTimeBy(900)
        assertTrue(finished)
    }

    @Test
    fun reopeningDuringExitRestoresTheSamePlacementAndCancelsDismissal() = runSkikoComposeUiTest(size = Size(500f, 600f), density = Density(1f)) {
        var show by mutableStateOf(true)
        var finished = false
        setContent { AnchoredMenu(show = show, onDismissFinished = { finished = true }) }
        waitForIdle()
        val rest = onNodeWithTag("action").fetchSemanticsNode().boundsInRoot
        mainClock.autoAdvance = false
        show = false
        mainClock.advanceTimeBy(120)
        show = true
        mainClock.advanceTimeBy(1200)
        onNodeWithTag("action").assertIsEnabled()
        assertEquals(rest, onNodeWithTag("action").fetchSemanticsNode().boundsInRoot)
        assertFalse(finished, "The cancelled exit must not dismiss a reopened menu")
    }
}

@Composable
private fun AnchoredMenu(show: Boolean, top: Boolean = true, onDismissFinished: () -> Unit = {}) {
    COUITheme {
        Scaffold {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = if (top) Alignment.TopEnd else Alignment.BottomEnd) {
                Box(Modifier.size(40.dp).testTag("anchor")) {
                    OverlayListPopup(
                        show = show,
                        alignment = PopupPositionProvider.Align.End,
                        onDismissRequest = {},
                        onDismissFinished = onDismissFinished,
                    ) {
                        ListPopupColumn {
                            DropdownImpl(
                                item = DropdownItem(text = "Action"),
                                optionSize = 1,
                                isSelected = false,
                                index = 0,
                                role = Role.Button,
                                modifier = Modifier.testTag("action"),
                            )
                        }
                    }
                }
            }
        }
    }
}
