// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullPagePullToRefreshTest {
    @Test
    fun gestureUsesHalfDampingAndRetractsBeforeScrollingContent() {
        val state = FullPagePullToRefreshState(CoroutineScope(Dispatchers.Unconfined))
        state.refreshThresholdPx = 100f
        state.maxDragPx = 2_000f

        assertEquals(200f, state.onPostScroll(200f))
        assertEquals(100f, state.dragOffset)
        assertEquals(FullPageRefreshState.ThresholdReached, state.refreshState)

        assertEquals(-150f, state.onPreScroll(-150f))
        assertEquals(25f, state.dragOffset)
        assertEquals(FullPageRefreshState.Pulling, state.refreshState)

        assertEquals(-50f, state.onPreScroll(-100f))
        assertEquals(0f, state.dragOffset)
        assertEquals(FullPageRefreshState.Idle, state.refreshState)
    }

    @Test
    fun shortPullReboundsWithoutRefreshing() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        var isRefreshing by mutableStateOf(false)
        var refreshCount by mutableIntStateOf(0)
        lateinit var state: FullPagePullToRefreshState

        setContent {
            COUITheme {
                state = rememberFullPagePullToRefreshState()
                FullPagePullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        refreshCount++
                        isRefreshing = true
                    },
                    state = state,
                    contentPadding = PaddingValues(top = 56.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 56.dp),
                    ) {
                        items(40) { index -> Text("Item $index") }
                    }
                }
            }
        }

        onRoot().performTouchInput {
            swipe(Offset(180f, 120f), Offset(180f, 180f), durationMillis = 300)
        }
        waitForIdle()

        assertEquals(0, refreshCount)
        assertEquals(FullPageRefreshState.Idle, state.refreshState)
        assertEquals(0f, state.dragOffset, 0.5f)
    }

    @Test
    fun pullBeyondThresholdRefreshesThenHoldsCompletion() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        var isRefreshing by mutableStateOf(false)
        var refreshCount by mutableIntStateOf(0)
        lateinit var state: FullPagePullToRefreshState

        setContent {
            COUITheme {
                state = rememberFullPagePullToRefreshState()
                FullPagePullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        refreshCount++
                        isRefreshing = true
                    },
                    state = state,
                    contentPadding = PaddingValues(top = 56.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 56.dp),
                    ) {
                        items(40) { index -> Text("Item $index") }
                    }
                }
            }
        }

        onRoot().performTouchInput {
            swipe(Offset(180f, 120f), Offset(180f, 440f), durationMillis = 300)
        }
        waitForIdle()

        assertEquals(1, refreshCount)
        assertEquals(FullPageRefreshState.Refreshing, state.refreshState)
        assertEquals(93.187f, state.dragOffset, 1f)

        mainClock.autoAdvance = false
        runOnIdle { isRefreshing = false }
        mainClock.advanceTimeBy(32)
        assertEquals(FullPageRefreshState.RefreshComplete, state.refreshState)
        assertEquals(93.187f, state.dragOffset, 1f)

        mainClock.advanceTimeBy(400)
        assertEquals(FullPageRefreshState.RefreshComplete, state.refreshState)
        assertTrue(state.dragOffset > 80f, "The success hint must hold before rebounding")

        mainClock.advanceTimeBy(1_000)
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(FullPageRefreshState.Idle, state.refreshState)
        assertEquals(0f, state.dragOffset, 0.5f)
    }

    @Test
    fun touchDuringCompletionHoldReboundsImmediately() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        var isRefreshing by mutableStateOf(false)
        lateinit var state: FullPagePullToRefreshState

        setContent {
            COUITheme {
                state = rememberFullPagePullToRefreshState()
                FullPagePullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = { isRefreshing = true },
                    state = state,
                    contentPadding = PaddingValues(top = 56.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 56.dp),
                    ) {
                        items(40) { index -> Text("Item $index") }
                    }
                }
            }
        }

        onRoot().performTouchInput {
            swipe(Offset(180f, 120f), Offset(180f, 440f), durationMillis = 300)
        }
        waitForIdle()
        mainClock.autoAdvance = false
        runOnIdle { isRefreshing = false }
        mainClock.advanceTimeBy(32)
        assertEquals(FullPageRefreshState.RefreshComplete, state.refreshState)

        onRoot().performTouchInput { down(Offset(180f, 300f)) }
        mainClock.advanceTimeBy(120)
        assertTrue(
            state.dragOffset < 80f,
            "A touch during the hold must cancel it and start the rebound",
        )
        onRoot().performTouchInput { up() }
        mainClock.advanceTimeBy(1_000)
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(FullPageRefreshState.Idle, state.refreshState)
        assertEquals(0f, state.dragOffset, 0.5f)
    }
}
