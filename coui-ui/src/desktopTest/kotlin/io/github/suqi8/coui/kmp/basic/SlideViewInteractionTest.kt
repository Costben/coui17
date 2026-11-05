// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class, ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlideViewInteractionTest {
    @Test
    fun deleteSlidesTheContentOutAndCollapsesTheRow() = runSkikoComposeUiTest(size = Size(360f, 200f), density = Density(1f)) {
        val state = SlideViewState(initiallyOpen = true)
        var scope: CoroutineScope? = null
        var finished = false
        setContent {
            scope = rememberCoroutineScope()
            COUITheme {
                Card(insideMargin = PaddingValues(0.dp)) {
                    SlideView(
                        menuItems = listOf(SlideMenuItem("Delete")),
                        onMenuItemClick = {},
                        modifier = Modifier.testTag("row"),
                        state = state,
                    ) {
                        BasicComponent(title = "4 hours before", cardListPosition = CardListPosition.Head)
                    }
                    Box(Modifier.fillMaxWidth().height(56.dp).background(Color.Green).testTag("next"))
                }
            }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        scope!!.launch {
            state.delete()
            finished = true
        }
        mainClock.advanceTimeBy(200)
        val midway = onNodeWithTag("row").getBoundsInRoot().let { (it.bottom - it.top).value }
        assertTrue(midway in 1f..55f, "The row must be collapsing half-way through, was $midway")
        // The row below has moved up into the collapsing row's old space and must not be painted
        // over by the delete fill, which belongs to the shrinking row only.
        val next = onNodeWithTag("next").getBoundsInRoot()
        val pixel = onRoot().captureToImage().toPixelMap()[180, next.top.value.toInt() + 4]
        assertTrue(pixel.green > 0.8f && pixel.red < 0.2f, "The next row must show through, was $pixel")
        mainClock.advanceTimeBy(400)
        waitForIdle()
        assertEquals(0f, onNodeWithTag("row").getBoundsInRoot().let { (it.bottom - it.top).value }, 0.5f)
        assertTrue(finished, "delete() must return once the row has collapsed")
        assertEquals(360f, state.scroll.value, 0.5f)
    }

    @Test
    fun aSwipeOpensTheMenuAtItsWidth() = runSkikoComposeUiTest(size = Size(360f, 100f), density = Density(1f)) {
        val state = SlideViewState()
        setContent {
            COUITheme {
                SlideView(
                    menuItems = listOf(SlideMenuItem("Delete")),
                    onMenuItemClick = {},
                    modifier = Modifier.testTag("row"),
                    state = state,
                ) {
                    Box(Modifier.fillMaxWidth().height(56.dp).background(Color.White))
                }
            }
        }
        onNodeWithTag("row").performTouchInput { swipeLeft(startX = 340f, endX = 200f, durationMillis = 300) }
        waitForIdle()
        assertTrue(state.isOpen)
        assertEquals(80f, state.scroll.value, 0.5f)
    }
}
