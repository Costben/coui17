// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BAR_TAG = "scroll-to-top-bar"
private const val ACTION_TAG = "scroll-to-top-action"
private const val LIST_TAG = "scroll-to-top-list"

@OptIn(ExperimentalTestApi::class)
class ScrollToTopTest {
    @Test
    fun tappingTopAppBarNonButtonAreaScrollsListToTopAndResetsHeader() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        lateinit var listState: LazyListState
        lateinit var behavior: ScrollBehavior
        var actionClicks = 0

        setContent {
            COUITheme {
                behavior = COUIScrollBehavior(state = rememberTopAppBarState())
                listState = rememberLazyListState()
                Scaffold(
                    topBar = {
                        SmallTopAppBar(
                            title = "Gallery",
                            scrollBehavior = behavior,
                            modifier = Modifier.testTag(BAR_TAG),
                            actions = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag(ACTION_TAG)
                                        .clickable { actionClicks += 1 },
                                )
                            },
                        )
                    },
                ) { padding ->
                    BindScrollToTop(listState)
                    LazyColumn(
                        state = listState,
                        contentPadding = padding,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(behavior.nestedScrollConnection)
                            .testTag(LIST_TAG),
                    ) {
                        items(60) { index ->
                            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("Item $index")
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        waitForIdle()
        assertTrue(
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0,
            "list should be scrolled away from the top",
        )

        onNodeWithTag(ACTION_TAG).performClick()
        waitForIdle()
        assertEquals(1, actionClicks, "action button click must still work")
        assertTrue(
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0,
            "tapping an action button must not trigger scroll-to-top",
        )

        onNodeWithTag(BAR_TAG).performTouchInput {
            click(position = centerLeft.copy(x = 40f))
        }
        waitForIdle()

        assertEquals(0, listState.firstVisibleItemIndex, "tapping non-button top bar must scroll to item 0")
        assertEquals(0, listState.firstVisibleItemScrollOffset, "tapping non-button top bar must reset offset to 0")
        assertEquals(0f, behavior.state.contentOffset, 0.5f, "header contentOffset must reset at top")
    }

    @Test
    fun scrollToTopBlockerSuppressesTrigger() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        lateinit var listState: LazyListState
        var blocked by mutableStateOf(true)

        setContent {
            COUITheme {
                listState = rememberLazyListState()
                Scaffold(
                    topBar = {
                        PinnedTopAppBar(
                            title = "Blocked",
                            modifier = Modifier.testTag(BAR_TAG),
                        )
                    },
                ) { padding ->
                    ScrollToTopBlocker(active = blocked)
                    BindScrollToTop(listState)
                    LazyColumn(
                        state = listState,
                        contentPadding = padding,
                        modifier = Modifier.fillMaxSize().testTag(LIST_TAG),
                    ) {
                        items(40) { index ->
                            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("Row $index")
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
        waitForIdle()
        val indexBefore = listState.firstVisibleItemIndex
        assertTrue(indexBefore > 0)

        onNodeWithTag(BAR_TAG).performTouchInput {
            click(position = centerLeft.copy(x = 40f))
        }
        waitForIdle()
        assertEquals(indexBefore, listState.firstVisibleItemIndex, "blocker active must suppress scroll-to-top")

        blocked = false
        waitForIdle()
        onNodeWithTag(BAR_TAG).performTouchInput {
            click(position = centerLeft.copy(x = 40f))
        }
        waitForIdle()
        assertEquals(0, listState.firstVisibleItemIndex, "clearing blocker restores scroll-to-top")
    }
}
