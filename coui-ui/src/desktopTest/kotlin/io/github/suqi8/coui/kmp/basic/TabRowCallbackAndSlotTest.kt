// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TabRowCallbackAndSlotTest {

    @Test
    fun tabRowWithContour_invokesCallbacksAndRendersCustomTabContent() = runSkikoComposeUiTest(size = Size(360f, 120f)) {
        val selected = mutableListOf<Int>()
        val reselected = mutableListOf<Int>()
        val clicked = mutableListOf<Int>()
        var current = 0

        setContent {
            COUITheme(colors = lightColorScheme()) {
                TabRowWithContour(
                    tabs = listOf("One", "Two", "Three"),
                    selectedTabIndex = current,
                    onTabSelected = {
                        current = it
                        selected += it
                    },
                    onTabReselected = { reselected += it },
                    onTabClick = { clicked += it },
                    modifier = Modifier.width(240.dp).testTag("bar"),
                    tabModifier = { index -> Modifier.testTag("seg_$index") },
                    tabContent = { _, label, _ ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "$label!")
                        }
                    },
                )
            }
        }
        waitForIdle()

        onNodeWithTag("bar").assertWidthIsEqualTo(240.dp)
        onNodeWithText("One!").assertExists()
        onNodeWithTag("seg_0").performClick()
        waitForIdle()
        assertEquals(emptyList(), selected)
        assertEquals(listOf(0), reselected)
        assertEquals(listOf(0), clicked)

        onNodeWithTag("seg_2").performClick()
        waitForIdle()
        assertEquals(listOf(2), selected)
        assertEquals(listOf(0), reselected)
        assertEquals(listOf(0, 2), clicked)
    }

    @Test
    fun emptyTabs_doesNotCrash() = runSkikoComposeUiTest(size = Size(320f, 80f)) {
        setContent {
            COUITheme(colors = lightColorScheme()) {
                TabRowWithContour(
                    tabs = emptyList(),
                    selectedTabIndex = 0,
                    onTabSelected = {},
                )
            }
        }
        waitForIdle()
    }
}
