// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [PinnedScrollBehavior] only measures: it accumulates the content's scroll into `contentOffset`
 * (clamped to `<= 0`) and never touches `heightOffset`, the pre-scroll phase or the fling.
 *
 * The behavior is created in a composition because [PinnedScrollBehavior] is composable and
 * remembers its implementation. The connection is then driven directly, which isolates the offset
 * arithmetic from the gesture and layout machinery.
 */
@OptIn(ExperimentalTestApi::class)
class PinnedScrollBehaviorTest {

    private fun scrollUp(behavior: ScrollBehavior, dy: Float): Offset = behavior.nestedScrollConnection.onPostScroll(
        consumed = Offset(0f, dy),
        available = Offset.Zero,
        source = NestedScrollSource.UserInput,
    )

    private inline fun withBehavior(
        state: TopAppBarState = TopAppBarState(-200f, 0f, 0f),
        crossinline canScroll: () -> Boolean = { true },
        crossinline block: (ScrollBehavior) -> Unit,
    ) = runSkikoComposeUiTest(size = Size(320f, 480f), density = Density(1f)) {
        lateinit var behavior: ScrollBehavior
        setContent { behavior = PinnedScrollBehavior(state = state, canScroll = { canScroll() }) }
        waitForIdle()
        block(behavior)
    }

    @Test
    fun isPinnedWithoutSnapOrFling() = withBehavior { behavior ->
        assertTrue(behavior.isPinned)
        assertNull(behavior.snapAnimationSpec)
        assertNull(behavior.flingAnimationSpec)
    }

    @Test
    fun contentOffsetAccumulatesUpwardScroll() {
        val state = TopAppBarState(-200f, 0f, 0f)
        withBehavior(state) { behavior ->
            scrollUp(behavior, -40f)
            scrollUp(behavior, -25f)
            assertEquals(-65f, state.contentOffset)
        }
    }

    @Test
    fun contentOffsetIsClampedAtZeroWhenScrollingBackDown() {
        val state = TopAppBarState(-200f, 0f, 0f)
        withBehavior(state) { behavior ->
            scrollUp(behavior, -30f)
            scrollUp(behavior, 50f)
            assertEquals(0f, state.contentOffset)
            // An overscroll at the top must not leave a positive offset behind either.
            scrollUp(behavior, 80f)
            assertEquals(0f, state.contentOffset)
            scrollUp(behavior, -10f)
            assertEquals(-10f, state.contentOffset)
        }
    }

    @Test
    fun heightOffsetIsNeverWritten() {
        val state = TopAppBarState(-200f, 0f, 0f)
        withBehavior(state) { behavior ->
            behavior.nestedScrollConnection.onPostScroll(
                consumed = Offset(0f, -60f),
                available = Offset(0f, -20f),
                source = NestedScrollSource.UserInput,
            )
            scrollUp(behavior, 15f)
            assertEquals(0f, state.heightOffset)
            assertEquals(-200f, state.heightOffsetLimit)
            // `0f / -200f` is negative zero, which `assertEquals` on boxed floats tells apart from `0f`.
            assertTrue(state.collapsedFraction == 0f)
        }
    }

    @Test
    fun preScrollAndPostScrollConsumeNothing() {
        val state = TopAppBarState(-200f, 0f, 0f)
        withBehavior(state) { behavior ->
            val connection = behavior.nestedScrollConnection
            assertEquals(
                Offset.Zero,
                connection.onPreScroll(Offset(0f, -80f), NestedScrollSource.UserInput),
            )
            assertEquals(
                Offset.Zero,
                connection.onPostScroll(Offset(0f, -80f), Offset(0f, -10f), NestedScrollSource.UserInput),
            )
            assertEquals(0f, state.heightOffset)
        }
    }

    @Test
    fun canScrollFalseFreezesTheContentOffset() {
        val state = TopAppBarState(-200f, 0f, 0f)
        var enabled = true
        withBehavior(state, canScroll = { enabled }) { behavior ->
            scrollUp(behavior, -20f)
            enabled = false
            scrollUp(behavior, -20f)
            assertEquals(-20f, state.contentOffset)
        }
    }
}
