// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ChipTabBlurSelectionReproTest {

    @Test
    fun switchingChipTabSelectionBrightensNewlySelectedPillFillOverBackdrop() = runSkikoComposeUiTest(
        size = Size(360f, 96f),
        density = Density(1f),
    ) {
        val tabs = listOf(
            ChipTabData("动态"),
            ChipTabData("应用"),
            ChipTabData("游戏"),
        )
        var selected by mutableIntStateOf(0)
        setContent {
            COUITheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Canvas(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                        drawRect(Color(0xFFEDEDEF))
                    }
                    ChipTabGroup(
                        tabs = tabs,
                        selectedIndex = selected,
                        onSelectedChange = { selected = it },
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        backdrop = backdrop,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(),
                    )
                }
            }
        }
        waitForIdle()
        val initialPixels = onRoot().captureToImage().toPixelMap()

        // Sample the top-center interior of each pill (away from both the 1px rim and text glyphs).
        fun averageLuma(pixels: androidx.compose.ui.graphics.PixelMap, xRange: IntRange, yRange: IntRange): Float {
            var sum = 0f
            var count = 0
            for (y in yRange) {
                for (x in xRange) {
                    val c = pixels[x, y]
                    sum += 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
                    count++
                }
            }
            return sum / count
        }

        val yBand = 36..40
        // Tab 0 starts at x=16, width ~60 -> interior x=24..64; Tab 1 starts around x=84 -> x=94..134
        val tab0Initial = averageLuma(initialPixels, 26..62, yBand)
        val tab1Initial = averageLuma(initialPixels, 94..130, yBand)
        assertTrue(
            tab0Initial > tab1Initial + 0.03f,
            "Initially selected tab 0 ($tab0Initial) must be brighter than unselected tab 1 ($tab1Initial)",
        )

        selected = 1
        waitForIdle()
        val switchedPixels = onRoot().captureToImage().toPixelMap()
        val tab0After = averageLuma(switchedPixels, 26..62, yBand)
        val tab1After = averageLuma(switchedPixels, 94..130, yBand)
        assertTrue(
            tab1After > tab0After + 0.03f,
            "Newly selected tab 1 ($tab1After) must brighten above deselected tab 0 ($tab0After)",
        )
        assertTrue(
            tab1After >= tab0Initial - 0.015f,
            "Newly selected tab 1 ($tab1After) must reach initial selected brightness ($tab0Initial)",
        )
    }
}
