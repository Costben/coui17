// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

private const val LIST_TAG = "scroll-haptic-list"

@OptIn(ExperimentalTestApi::class)
class ScrollEndHapticTest {

    private class RecordingHapticFeedback : HapticFeedback {
        val events = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            events += hapticFeedbackType
        }
    }

    @Test
    fun themeOverscrollBuzzesOnceOnTopAndBottomDrag() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        val haptics = RecordingHapticFeedback()
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                COUITheme {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(LIST_TAG),
                    ) {
                        items(20) {
                            Box(Modifier.fillMaxWidth().height(50.dp))
                        }
                    }
                }
            }
        }
        waitForIdle()

        // Drag down at the top boundary: buzzes once even across multiple move steps.
        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
            moveBy(Offset(0f, 40f))
            moveBy(Offset(0f, -10f))
            moveBy(Offset(0f, 20f))
            up()
        }
        waitForIdle()
        assertEquals(1, haptics.events.size, "dragging past top must buzz once, got ${haptics.events}")

        // Scroll partway into content: must not buzz.
        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, -200f))
            up()
        }
        waitForIdle()
        assertEquals(1, haptics.events.size, "scrolling mid-list must not buzz, got ${haptics.events}")

        // Drag all the way to the bottom boundary and continue past it: buzzes once more.
        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            repeat(12) { moveBy(Offset(0f, -60f)) }
            up()
        }
        waitForIdle()
        assertEquals(2, haptics.events.size, "dragging past bottom must buzz once, got ${haptics.events}")
    }

    @Test
    fun overScrollVerticalBuzzesOnceOnDragAndFlingWithoutThemeFactory() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        val haptics = RecordingHapticFeedback()
        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides haptics,
                LocalOverscrollFactory provides null,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical(isEnabled = { true })
                        .verticalScroll(rememberScrollState())
                        .testTag(LIST_TAG),
                ) {
                    repeat(20) {
                        Box(Modifier.fillMaxWidth().height(50.dp))
                    }
                }
            }
        }
        waitForIdle()

        // Drag past top edge.
        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            repeat(3) { moveBy(Offset(0f, 40f)) }
            up()
        }
        waitForIdle()
        assertEquals(1, haptics.events.size, "overScrollVertical top drag must buzz once, got ${haptics.events}")

        // Fling hard to the bottom boundary.
        onNodeWithTag(LIST_TAG).performTouchInput {
            swipeUp(startY = bottom - 20f, endY = top + 20f, durationMillis = 80L)
        }
        waitForIdle()
        assertEquals(2, haptics.events.size, "overScrollVertical bottom fling must buzz once, got ${haptics.events}")

        // Fling back to the top boundary.
        onNodeWithTag(LIST_TAG).performTouchInput {
            swipeDown(startY = top + 20f, endY = bottom - 20f, durationMillis = 80L)
        }
        waitForIdle()
        assertEquals(3, haptics.events.size, "overScrollVertical top fling must buzz once, got ${haptics.events}")
    }

    @Test
    fun themeOverscrollWithScrollEndHapticAndOverScrollVerticalDoesNotDoubleBuzz() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        val haptics = RecordingHapticFeedback()
        setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                COUITheme {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .scrollEndHaptic()
                            .overScrollVertical(isEnabled = { true })
                            .testTag(LIST_TAG),
                    ) {
                        items(20) {
                            Box(Modifier.fillMaxWidth().height(50.dp))
                        }
                    }
                }
            }
        }
        waitForIdle()

        // Slow drag to top.
        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            repeat(3) { moveBy(Offset(0f, 40f)) }
            up()
        }
        waitForIdle()
        assertEquals(1, haptics.events.size, "combined top drag must buzz once, got ${haptics.events}")

        // Fast fling to bottom.
        onNodeWithTag(LIST_TAG).performTouchInput {
            swipeUp(startY = bottom - 20f, endY = top + 20f, durationMillis = 80L)
        }
        waitForIdle()
        assertEquals(2, haptics.events.size, "combined bottom fling must buzz once, got ${haptics.events}")

        // Fast fling back to top.
        onNodeWithTag(LIST_TAG).performTouchInput {
            swipeDown(startY = top + 20f, endY = bottom - 20f, durationMillis = 80L)
        }
        waitForIdle()
        assertEquals(3, haptics.events.size, "combined top fling must buzz once, got ${haptics.events}")
    }

    @Test
    fun horizontalOverscrollDoesNotTriggerVerticalBoundaryHaptic() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        val haptics = RecordingHapticFeedback()
        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides haptics,
                LocalOverscrollFactory provides null,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollHorizontal(isEnabled = { true })
                        .horizontalScroll(rememberScrollState())
                        .testTag(LIST_TAG),
                ) {
                    repeat(10) {
                        Box(Modifier.width(80.dp).height(100.dp))
                    }
                }
            }
        }
        waitForIdle()

        onNodeWithTag(LIST_TAG).performTouchInput {
            down(center)
            repeat(4) { moveBy(Offset(40f, 0f)) }
            up()
        }
        waitForIdle()
        assertEquals(0, haptics.events.size, "horizontal overscroll must not buzz, got ${haptics.events}")
    }
}
