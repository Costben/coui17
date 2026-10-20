// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TabRowOpaqueColorsTest {
    private fun colorsFor(dark: Boolean): TabRowColors {
        var colors: TabRowColors? = null
        runSkikoComposeUiTest(size = Size(320f, 80f)) {
            setContent {
                COUITheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
                    colors = TabRowDefaults.opaqueTabRowColors()
                }
            }
            waitForIdle()
        }
        return requireNotNull(colors)
    }

    @Test
    fun lightThemeUsesOpaqueContainerAndWhiteIndicator() {
        val colors = colorsFor(dark = false)
        assertEquals(Color(0xFFE6E6E6), colors.backgroundColor(false))
        assertEquals(Color.White, colors.backgroundColor(true))
    }

    @Test
    fun darkThemeUsesOpaqueContainerAndRaisedIndicator() {
        val colors = colorsFor(dark = true)
        assertEquals(Color(0xFF373737), colors.backgroundColor(false))
        assertEquals(Color(0xFF595959), colors.backgroundColor(true))
    }
}
