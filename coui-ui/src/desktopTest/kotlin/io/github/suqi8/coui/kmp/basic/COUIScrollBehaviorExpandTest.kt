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

/**
 * [COUIScrollBehavior]'s `expandOnContentScroll`: by default the bar only expands once the list has
 * reached its top; when enabled it follows the content's downward drag anywhere in the list.
 *
 * The behavior is created in a composition because [COUIScrollBehavior] is composable and remembers
 * its scroll behavior. The connection is then driven directly, which isolates the offset arithmetic
 * from the gesture and layout machinery.
 */
@OptIn(ExperimentalTestApi::class)
class COUIScrollBehaviorExpandTest {

    private fun initialState() = TopAppBarState(
        initialHeightOffsetLimit = -200f,
        initialHeightOffset = -200f,
        initialContentOffset = 1000f,
    )

    @Test
    fun contentScrollDoesNotExpandByDefault() = runSkikoComposeUiTest(
        size = Size(320f, 480f),
        density = Density(1f),
    ) {
        val state = initialState()
        lateinit var behavior: ScrollBehavior
        setContent {
            behavior = COUIScrollBehavior(
                state = state,
                snapAnimationSpec = null,
                flingAnimationSpec = null,
            )
        }
        waitForIdle()

        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 50f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )

        assertEquals(-200f, state.heightOffset)
        assertEquals(1050f, state.contentOffset)
    }

    @Test
    fun contentScrollExpandsWhenEnabled() = runSkikoComposeUiTest(
        size = Size(320f, 480f),
        density = Density(1f),
    ) {
        val state = initialState()
        lateinit var behavior: ScrollBehavior
        setContent {
            behavior = COUIScrollBehavior(
                state = state,
                snapAnimationSpec = null,
                flingAnimationSpec = null,
                expandOnContentScroll = true,
            )
        }
        waitForIdle()

        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 50f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )

        assertEquals(-150f, state.heightOffset)
        assertEquals(1050f, state.contentOffset)
    }

    @Test
    fun expansionStopsAtTheFullyExpandedBar() = runSkikoComposeUiTest(
        size = Size(320f, 480f),
        density = Density(1f),
    ) {
        val state = initialState()
        lateinit var behavior: ScrollBehavior
        setContent {
            behavior = COUIScrollBehavior(
                state = state,
                snapAnimationSpec = null,
                flingAnimationSpec = null,
                expandOnContentScroll = true,
            )
        }
        waitForIdle()

        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 50f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 500f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )

        assertEquals(0f, state.heightOffset)
        assertEquals(1550f, state.contentOffset)
    }
}
