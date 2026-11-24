// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.nav.core

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.LocalSquircleClippedHost
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavSquircleClippedHostTest {
    private data object Root : NavKey

    private data object InnerRoot : NavKey

    @Test
    fun cornerClipConfiguredProvidesSquircleClippedHostTrue() = runComposeUiTest {
        var observed: Boolean? = null
        setContent {
            val stack = navBackStackOf(Root)
            NavDisplay(
                backStack = stack,
                effects = NavDisplayEffects(
                    enableCornerClip = true,
                    cornerClipRadius = 24.dp,
                ),
            ) {
                entry<Root> {
                    observed = LocalSquircleClippedHost.current
                }
            }
        }
        waitForIdle()
        assertTrue(observed == true, "corner-clipping NavDisplay must provide LocalSquircleClippedHost = true")
    }

    @Test
    fun zeroRadiusOrDisabledCornerClipKeepsSquircleClippedHostFalse() = runComposeUiTest {
        var observedZeroRadius: Boolean? = null
        var observedDisabled: Boolean? = null
        setContent {
            val stackA = navBackStackOf(Root)
            NavDisplay(
                backStack = stackA,
                effects = NavDisplayEffects(
                    enableCornerClip = true,
                    cornerClipRadius = 0.dp,
                ),
            ) {
                entry<Root> {
                    observedZeroRadius = LocalSquircleClippedHost.current
                }
            }
            val stackB = navBackStackOf(Root)
            NavDisplay(
                backStack = stackB,
                effects = NavDisplayEffects(
                    enableCornerClip = false,
                    cornerClipRadius = 24.dp,
                ),
            ) {
                entry<Root> {
                    observedDisabled = LocalSquircleClippedHost.current
                }
            }
        }
        waitForIdle()
        assertFalse(observedZeroRadius == true, "0.dp cornerClipRadius must not mark host as squircle-clipped")
        assertFalse(observedDisabled == true, "disabled enableCornerClip must not mark host as squircle-clipped")
    }

    @Test
    fun nestedNavDisplayPreservesOuterSquircleClippedHost() = runComposeUiTest {
        var observedInner: Boolean? = null
        setContent {
            val outerStack = navBackStackOf(Root)
            val innerStack = navBackStackOf(InnerRoot)
            NavDisplay(
                backStack = outerStack,
                effects = NavDisplayEffects(
                    enableCornerClip = true,
                    cornerClipRadius = 24.dp,
                ),
            ) {
                entry<Root> {
                    NavDisplay(
                        backStack = innerStack,
                        effects = NavDisplayEffects(
                            enableCornerClip = false,
                            cornerClipRadius = 0.dp,
                        ),
                    ) {
                        entry<InnerRoot> {
                            observedInner = LocalSquircleClippedHost.current
                        }
                    }
                }
            }
        }
        waitForIdle()
        assertTrue(observedInner == true, "nested NavDisplay must inherit outer LocalSquircleClippedHost = true")
    }
}
