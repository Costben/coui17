// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LoadProgressButtonDelegationTest {

    private fun capture(content: @Composable () -> Unit): PixelMap {
        var pixels: PixelMap? = null
        runSkikoComposeUiTest(density = Density(3f)) {
            setContent {
                COUITheme(colors = lightColorScheme()) {
                    Box(Modifier.testTag(TAG).background(Color.White)) { content() }
                }
            }
            waitForIdle()
            pixels = onNodeWithTag(TAG).captureToImage().toPixelMap()
        }
        return pixels!!
    }

    private fun assertSamePixels(expected: PixelMap, actual: PixelMap) {
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                assertEquals(expected[x, y], actual[x, y], "pixel $x,$y")
            }
        }
    }

    @Test
    fun everyShapeTierDrawsTheInstallLoadProgressButton() {
        listOf(
            LoadProgressButtonSize.Small to InstallLoadStyle.Default,
            LoadProgressButtonSize.Large to InstallLoadStyle.BigRound,
            LoadProgressButtonSize.Circle to InstallLoadStyle.Circle,
        ).forEach { (size, style) ->
            LoadProgressState.entries.forEach { state ->
                val button = capture {
                    LoadProgressButton(onClick = {}, state = state, progress = 40, text = "40%", size = size)
                }
                val install = capture {
                    InstallLoadProgress(onClick = {}, state = state, progress = 40, text = "40%", style = style)
                }
                assertSamePixels(install, button)
            }
        }
    }

    @Test
    fun theLabelOverTheFillFollowsTheFillInRightToLeft() {
        val surpass = Color(0xFF00FF00)
        val pixels = capture {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                InstallLoadProgress(
                    onClick = {},
                    state = LoadProgressState.Loading,
                    progress = 100,
                    max = 200,
                    text = "HHHHHHHHHHHHHHHHHHHHHHHHHHHHHH",
                    style = InstallLoadStyle.BigRound,
                    colors = InstallLoadProgressDefaults.colors(
                        primaryColor = Color(0xFFFF0000),
                        secondaryColor = Color.White,
                        surpassProgressColor = surpass,
                    ),
                )
            }
        }
        fun greenPixels(columns: IntRange): Int {
            var count = 0
            for (x in columns) {
                for (y in 0 until pixels.height) {
                    if (pixels[x, y].green > 0.9f && pixels[x, y].red < 0.1f) count++
                }
            }
            return count
        }
        val middle = pixels.width / 2
        assertTrue(greenPixels(middle until pixels.width) > 0, "the flipped label belongs on the filled, right half")
        assertEquals(0, greenPixels(0 until middle), "the left half is not filled yet")
    }

    private companion object {
        const val TAG = "root"
    }
}
