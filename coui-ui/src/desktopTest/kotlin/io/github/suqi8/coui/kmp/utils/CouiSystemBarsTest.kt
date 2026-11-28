// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CouiSystemBarsTest {

    @Test
    fun overlayDeactivatingImmediatelyRestoresUnderlyingStyleBeforeDispose() = runSkikoComposeUiTest {
        val applied = mutableListOf<ResolvedCouiSystemBarsStyle>()
        var showViewer by mutableStateOf(false)
        var viewerExiting by mutableStateOf(false)
        var viewerScrimAlpha by mutableFloatStateOf(1f)
        var viewerChromeVisible by mutableStateOf(true)
        var profileDarkCover by mutableStateOf(false)

        setContent {
            COUITheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CouiSystemBarsHost(applier = { applied.add(it) }) {
                    // Underlying page: light feed (or dark-cover profile).
                    CouiSystemBarsEffect(
                        darkStatusBarIcons = !profileDarkCover,
                        darkNavigationBarIcons = true,
                    )
                    if (showViewer) {
                        CouiSystemBarsEffect(
                            darkStatusBarIcons = false,
                            darkNavigationBarIcons = false,
                            statusBarVisible = viewerChromeVisible,
                            navigationBarVisible = viewerChromeVisible,
                            active = { !viewerExiting && viewerScrimAlpha >= 0.5f },
                        )
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(
            ResolvedCouiSystemBarsStyle(
                darkStatusBarIcons = true,
                darkNavigationBarIcons = true,
                statusBarVisible = true,
                navigationBarVisible = true,
            ),
            applied.last(),
        )

        // Enter dark image viewer: icons flip white.
        showViewer = true
        waitForIdle()
        assertEquals(
            ResolvedCouiSystemBarsStyle(
                darkStatusBarIcons = false,
                darkNavigationBarIcons = false,
                statusBarVisible = true,
                navigationBarVisible = true,
            ),
            applied.last(),
        )

        // Hide viewer chrome: system bars hide together.
        viewerChromeVisible = false
        waitForIdle()
        assertEquals(
            ResolvedCouiSystemBarsStyle(
                darkStatusBarIcons = false,
                darkNavigationBarIcons = false,
                statusBarVisible = false,
                navigationBarVisible = false,
            ),
            applied.last(),
        )

        // Start dragging down past halfway (viewer still mounted!): icons & visibility immediately restore.
        viewerScrimAlpha = 0.4f
        waitForIdle()
        assertEquals(
            ResolvedCouiSystemBarsStyle(
                darkStatusBarIcons = true,
                darkNavigationBarIcons = true,
                statusBarVisible = true,
                navigationBarVisible = true,
            ),
            applied.last(),
        )

        // Cancel drag back into viewer: immediately returns to hidden dark viewer state.
        viewerScrimAlpha = 0.9f
        waitForIdle()
        assertEquals(false, applied.last().darkStatusBarIcons)
        assertEquals(false, applied.last().statusBarVisible)

        // Returning over a dark-cover profile: exiting restores white status bar icons (not light default).
        profileDarkCover = true
        viewerExiting = true
        waitForIdle()
        assertEquals(
            ResolvedCouiSystemBarsStyle(
                darkStatusBarIcons = false,
                darkNavigationBarIcons = true,
                statusBarVisible = true,
                navigationBarVisible = true,
            ),
            applied.last(),
        )
    }
}
