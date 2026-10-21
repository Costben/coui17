// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchBarStrokeTest {

    private fun strokeFor(dark: Boolean): MaterialStrokeParams {
        var captured: MaterialStrokeParams? = null
        runSkikoComposeUiTest(size = Size(320f, 80f)) {
            setContent {
                COUITheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
                    captured = SearchBarDefaults.searchBarStroke()
                }
            }
            waitForIdle()
        }
        return requireNotNull(captured)
    }

    @Test
    fun lightThemeUsesTheCapsule1LightEdge() {
        assertEquals(MaterialStrokeParams.FrameworkCapsule1Light, strokeFor(dark = false))
    }

    @Test
    fun darkThemeUsesTheCapsule1DarkEdge() {
        assertEquals(MaterialStrokeParams.FrameworkCapsule1Dark, strokeFor(dark = true))
    }
}
