// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BAR_TAG = "snap-test-bar"

/**
 * The collapsible bar is a snap state, not a scroll follower: once the content leaves the top the
 * title is collapsed into the toolbar row and stays there, no matter which way the content is
 * dragged, and only a return to the top expands the large title again.
 */
@OptIn(ExperimentalTestApi::class)
class TopAppBarSnapBehaviorTest {
    private lateinit var behavior: ScrollBehavior

    @Test
    fun titleStaysCollapsedUntilTheContentReturnsToTheTop() = runSkikoComposeUiTest(
        size = Size(360f, 640f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                behavior = COUIScrollBehavior(state = rememberTopAppBarState())
                Box(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = "Title",
                        largeTitle = "Large title",
                        scrollBehavior = behavior,
                        modifier = Modifier.testTag(BAR_TAG),
                    )
                }
            }
        }
        waitForIdle()

        assertEquals(110f, barHeight(), 0.5f, "the bar starts expanded at the top")

        dragBy(-60f)
        waitForIdle()
        assertEquals(52f, barHeight(), 0.5f, "scrolling up collapses the title into the toolbar row")

        dragBy(30f)
        waitForIdle()
        assertEquals(52f, barHeight(), 0.5f, "dragging down mid-content must not expand the title")

        dragBy(30f)
        waitForIdle()
        assertEquals(110f, barHeight(), 0.5f, "returning to the top expands the large title")
    }

    /**
     * Feeds [delta] to the behavior the way a scrollable would: the list consumes the whole drag,
     * so the post-scroll delta is the same value the pre-scroll one was.
     */
    private fun SkikoComposeUiTest.dragBy(delta: Float) {
        val available = Offset(0f, delta)
        with(behavior.nestedScrollConnection) {
            val pre = onPreScroll(available, NestedScrollSource.UserInput)
            // The list consumes everything that is left after the pre-scroll pass.
            onPostScroll(available - pre, Offset.Zero, NestedScrollSource.UserInput)
        }
    }

    private fun SkikoComposeUiTest.barHeight(): Float = onNodeWithTag(BAR_TAG).fetchSemanticsNode().size.height.toFloat()
}
