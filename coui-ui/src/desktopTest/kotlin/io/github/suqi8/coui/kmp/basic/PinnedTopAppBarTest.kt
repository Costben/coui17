// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CONTENT_TAG = "pinned-content"
private const val TITLE = "Photos"
private const val ACTION_TAG = "pinned-action"

/**
 * The bar keeps its title and actions on one 52dp row and never moves; the pinned behavior only
 * reports how far the content has scrolled under it.
 */
@OptIn(ExperimentalTestApi::class)
class PinnedTopAppBarTest {

    private val density = Density(2f)

    @Test
    fun titleAndActionsKeepTheirBoundsAfterTheContentScrolls() = runSkikoComposeUiTest(
        size = Size(720f, 1200f),
        density = density,
    ) {
        lateinit var behavior: ScrollBehavior
        setContent {
            behavior = PinnedScrollBehavior()
            Box(Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection)) {
                Column(
                    Modifier
                        .testTag(CONTENT_TAG)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Box(Modifier.height(PinnedTopAppBarDefaults.Height))
                    repeat(40) { Box(Modifier.fillMaxWidth().height(48.dp)) }
                }
                PinnedTopAppBar(
                    title = TITLE,
                    scrollBehavior = behavior,
                    defaultWindowInsetsPadding = false,
                    actions = {
                        Box(Modifier.couiTopAppBarActionItem().testTag(ACTION_TAG))
                        Box(Modifier.couiTopAppBarActionItem())
                    },
                )
            }
        }
        waitForIdle()

        val titleBefore = onNodeWithText(TITLE).fetchSemanticsNode().boundsInRoot
        val actionBefore = onNodeWithTag(ACTION_TAG).fetchSemanticsNode().boundsInRoot
        assertEquals(0f, behavior.state.contentOffset)

        onNodeWithTag(CONTENT_TAG).performTouchInput { swipeUp() }
        waitForIdle()

        val scrolled = -behavior.state.contentOffset
        assertTrue(scrolled > 0f, "swiping the content must report scroll under the bar, was $scrolled")
        assertEquals(0f, behavior.state.heightOffset)
        assertEquals(titleBefore, onNodeWithText(TITLE).fetchSemanticsNode().boundsInRoot)
        assertEquals(actionBefore, onNodeWithTag(ACTION_TAG).fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun rowGeometryMatchesTheGalleryMeasurement() = runSkikoComposeUiTest(
        size = Size(720f, 400f),
        density = density,
    ) {
        setContent {
            Box(Modifier.fillMaxSize()) {
                PinnedTopAppBar(
                    title = TITLE,
                    modifier = Modifier.testTag("bar"),
                    defaultWindowInsetsPadding = false,
                    actions = {
                        Box(Modifier.couiTopAppBarActionItem().testTag("first"))
                        Box(Modifier.couiTopAppBarActionItem().testTag("second"))
                    },
                )
            }
        }
        waitForIdle()

        val bar = onNodeWithTag("bar").fetchSemanticsNode().boundsInRoot
        val first = onNodeWithTag("first").fetchSemanticsNode().boundsInRoot
        val second = onNodeWithTag("second").fetchSemanticsNode().boundsInRoot
        val title = onNodeWithText(TITLE).fetchSemanticsNode().boundsInRoot

        // The bar reports its row height only; the 24dp material band takes no layout space.
        assertEquals(52f * 2f, bar.height)
        // 40dp square actions, 10dp apart, 16dp from the end edge.
        assertEquals(80f, first.width)
        assertEquals(80f, first.height)
        assertEquals(20f, second.left - first.right)
        assertEquals(720f - 32f, second.right)
        // Vertically centred in the row.
        assertEquals(bar.center.y, first.center.y)
        assertEquals(bar.center.y, title.center.y, 1f)
        // With no navigation icon the title starts at the 16dp start inset.
        assertEquals(32f, title.left)
    }

    @Test
    fun navigationIconPushesTheTitleByTheGapOnlyWhenPresent() = runSkikoComposeUiTest(
        size = Size(720f, 400f),
        density = density,
    ) {
        setContent {
            Column {
                PinnedTopAppBar(
                    title = "With",
                    defaultWindowInsetsPadding = false,
                    navigationIcon = { Box(Modifier.couiTopAppBarActionItem()) },
                )
                PinnedTopAppBar(title = "Without", defaultWindowInsetsPadding = false)
            }
        }
        waitForIdle()

        val with: Rect = onNodeWithText("With").fetchSemanticsNode().boundsInRoot
        val without: Rect = onNodeWithText("Without").fetchSemanticsNode().boundsInRoot
        // 16dp inset + 40dp icon + 10dp gap, against the 16dp inset alone.
        assertEquals(66f * 2f, with.left)
        assertEquals(16f * 2f, without.left)
    }
}
