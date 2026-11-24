// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Verifies the upstream COUI 17 top bar additions: settings home metrics, linked search, ToolbarTextButton, and IconButton red dot. */
@OptIn(ExperimentalTestApi::class)
class TopAppBarUpstreamStylesTest {

    @Test
    fun settingsHomeMetricsHonorCustomExpandedHeight() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                TopAppBar(
                    title = "Settings",
                    expandedHeight = TopAppBarDefaults.SettingsHomeExpandedHeight,
                    largeTitleFontSize = TopAppBarDefaults.SettingsHomeLargeTitleFontSize,
                    titlePadding = TopAppBarDefaults.SettingsHomeTitlePadding,
                    largeTitleBottomMargin = TopAppBarDefaults.SettingsHomeLargeTitleBottomMargin,
                    modifier = Modifier.testTag("settings-home-bar"),
                )
            }
        }
        waitForIdle()
        val height = onNodeWithTag("settings-home-bar").fetchSemanticsNode().size.height.toFloat()
        assertEquals(116f, height, 1f, "Settings home bar should expand to 116dp")
    }

    @Test
    fun linkedSearchCollapsesTitleAreaToBottomContentHeight() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        var active by mutableStateOf(false)
        setContent {
            COUITheme {
                val searchState = rememberTopAppBarSearchState(active = active)
                TopAppBar(
                    title = "Settings",
                    searchState = searchState,
                    modifier = Modifier.testTag("linked-search-bar"),
                    bottomContent = {
                        TopAppBarSearchSlot(
                            inputField = {
                                Box(modifier = Modifier.fillMaxWidth().height(40.dp))
                            },
                        )
                    },
                )
            }
        }
        waitForIdle()
        val restHeight = onNodeWithTag("linked-search-bar").fetchSemanticsNode().size.height.toFloat()
        assertEquals(162f, restHeight, 1f, "Expanded bar (110dp) + search slot (52dp) = 162dp at rest")

        active = true
        waitForIdle()
        val activeHeight = onNodeWithTag("linked-search-bar").fetchSemanticsNode().size.height.toFloat()
        assertEquals(52f, activeHeight, 1f, "Active linked search lifts the search slot to 52dp")
    }

    @Test
    fun toolbarTextButtonAndIconButtonRedDotPreserveExpectedBounds() = runSkikoComposeUiTest(
        size = Size(360f, 200f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ToolbarTextButton(
                        text = "Done",
                        onClick = {},
                        modifier = Modifier.testTag("text-btn"),
                    )
                    IconButton(
                        onClick = {},
                        minWidth = 40.dp,
                        minHeight = 40.dp,
                        redDot = 9,
                        modifier = Modifier.testTag("dot-btn"),
                    ) {
                        Box(modifier = Modifier.height(24.dp).width(24.dp))
                    }
                }
            }
        }
        waitForIdle()

        val textSize = onNodeWithTag("text-btn").fetchSemanticsNode().size
        assertTrue(textSize.width in 60..104, "ToolbarTextButton width should stay in 60..104dp, was ${textSize.width}")
        assertEquals(40, textSize.height, "ToolbarTextButton height should be 40dp")

        val dotSize = onNodeWithTag("dot-btn").fetchSemanticsNode().size
        assertEquals(40, dotSize.width, "IconButton width must not expand when redDot is set")
        assertEquals(40, dotSize.height, "IconButton height must not expand when redDot is set")
    }

    @Test
    fun persistentSubtitleClearsNavigationAndActionIconsWhenCollapsed() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        lateinit var behavior: ScrollBehavior
        setContent {
            COUITheme {
                behavior = COUIScrollBehavior(state = rememberTopAppBarState())
                TopAppBar(
                    title = "Top bar",
                    subtitle = "Persistent subtitle text that stays visible on collapse",
                    hideSubtitleOnCollapse = false,
                    scrollBehavior = behavior,
                    navigationIcon = {
                        Box(modifier = Modifier.width(40.dp).height(40.dp).testTag("nav-icon"))
                    },
                    actions = {
                        Box(modifier = Modifier.width(40.dp).height(40.dp).testTag("action-icon"))
                    },
                )
            }
        }
        waitForIdle()

        val delta = Offset(0f, -120f)
        with(behavior.nestedScrollConnection) {
            val pre = onPreScroll(delta, NestedScrollSource.UserInput)
            onPostScroll(delta - pre, Offset.Zero, NestedScrollSource.UserInput)
        }
        waitForIdle()

        val navBounds = onNodeWithTag("nav-icon").fetchSemanticsNode().boundsInRoot
        val actionBounds = onNodeWithTag("action-icon").fetchSemanticsNode().boundsInRoot
        val subtitleBounds = onNodeWithText("Persistent subtitle text that stays visible on collapse")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            subtitleBounds.left >= navBounds.right + 9f,
            "Collapsed subtitle (${subtitleBounds.left}) must clear navigation icon (${navBounds.right})",
        )
        assertTrue(
            subtitleBounds.right <= actionBounds.left - 3f,
            "Collapsed subtitle (${subtitleBounds.right}) must clear action icon (${actionBounds.left})",
        )
    }
}
