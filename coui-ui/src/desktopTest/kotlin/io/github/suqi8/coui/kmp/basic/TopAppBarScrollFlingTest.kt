// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FLING_BAR_TAG = "fling-bar"
private const val FLING_LIST_TAG = "fling-list"

/**
 * A real scrollable under a snapping bar: a downward fling from the middle of the list must leave
 * the title collapsed, because the list has not returned to its top.
 */
@OptIn(ExperimentalTestApi::class)
class TopAppBarScrollFlingTest {

    @Test
    fun downwardFlingMidListKeepsTheTitleCollapsed() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        lateinit var behavior: ScrollBehavior
        lateinit var listState: LazyListState
        setContent {
            COUITheme {
                behavior = COUIScrollBehavior(state = rememberTopAppBarState())
                Box(modifier = Modifier.fillMaxSize()) {
                    listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(FLING_LIST_TAG)
                            .nestedScroll(behavior.nestedScrollConnection),
                    ) {
                        items(200) { index ->
                            Text("Item $index", modifier = Modifier.height(56.dp))
                        }
                    }
                    TopAppBar(
                        title = "Title",
                        largeTitle = "Large title",
                        scrollBehavior = behavior,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .testTag(FLING_BAR_TAG),
                    )
                }
            }
        }
        waitForIdle()
        assertEquals(110f, barHeight(), 0.5f, "the bar starts expanded at the top")

        // Fling the content up, so the list leaves its top and the title collapses.
        onNodeWithTag(FLING_LIST_TAG).performTouchInput {
            swipe(Offset(180f, 560f), Offset(180f, 120f), durationMillis = 200)
        }
        waitForIdle()
        assertTrue(listState.firstVisibleItemIndex > 0, "the fling must leave the list off its top")
        assertEquals(52f, barHeight(), 0.5f, "the title collapses once the list leaves the top")

        // Fling back down, but not far enough to reach the top.
        onNodeWithTag(FLING_LIST_TAG).performTouchInput {
            swipe(Offset(180f, 240f), Offset(180f, 360f), durationMillis = 100)
        }
        waitForIdle()
        assertTrue(
            listState.firstVisibleItemIndex > 0,
            "the second fling must leave the list in the middle, not back at the top",
        )
        assertEquals(
            52f,
            barHeight(),
            0.5f,
            "a downward fling in the middle of the list must not expand the title",
        )
    }

    private fun androidx.compose.ui.test.SkikoComposeUiTest.barHeight(): Float = onNodeWithTag(FLING_BAR_TAG).fetchSemanticsNode().size.height.toFloat()
}
