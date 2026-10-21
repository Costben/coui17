// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
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
    private fun colorsFor(
        dark: Boolean,
        colors: @Composable () -> TabRowColors = { TabRowDefaults.opaqueTabRowColors() },
    ): TabRowColors {
        var captured: TabRowColors? = null
        runSkikoComposeUiTest(size = Size(320f, 80f)) {
            setContent {
                COUITheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
                    captured = colors()
                }
            }
            waitForIdle()
        }
        return requireNotNull(captured)
    }

    @Test
    fun lightThemeUsesOpaqueContainerAndDefaultIndicator() {
        val colors = colorsFor(dark = false)
        assertEquals(Color(0xFFE2E3E4), colors.backgroundColor(false))
        assertEquals(Color(0xB2FFFFFF), colors.backgroundColor(true))
        assertEquals(lightColorScheme().onSurface, colors.contentColor(false))
        assertEquals(lightColorScheme().onSurface, colors.contentColor(true))
        assertEquals(Color(0x0D000000), colors.indicatorShadowColor())
    }

    @Test
    fun darkThemeUsesOpaqueContainerAndDefaultIndicator() {
        val colors = colorsFor(dark = true)
        assertEquals(Color(0xFF353535), colors.backgroundColor(false))
        assertEquals(Color(0x33FFFFFF), colors.backgroundColor(true))
        assertEquals(darkColorScheme().onSurface, colors.contentColor(false))
        assertEquals(darkColorScheme().onSurface, colors.contentColor(true))
        assertEquals(Color(0x14000000), colors.indicatorShadowColor())
    }

    @Test
    fun explicitColorsOverrideTheSampledDefaults() {
        val colors = colorsFor(dark = false) {
            TabRowDefaults.opaqueTabRowColors(
                backgroundColor = Color.Red,
                contentColor = Color.Green,
                selectedBackgroundColor = Color.Blue,
                selectedContentColor = Color.Yellow,
                indicatorShadowColor = Color.Magenta,
            )
        }
        assertEquals(Color.Red, colors.backgroundColor(false))
        assertEquals(Color.Blue, colors.backgroundColor(true))
        assertEquals(Color.Green, colors.contentColor(false))
        assertEquals(Color.Yellow, colors.contentColor(true))
        assertEquals(Color.Magenta, colors.indicatorShadowColor())
    }
}
