// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
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
import io.github.suqi8.coui.kmp.theme.LocalPlatformFontWeightAdjustment
import io.github.suqi8.coui.kmp.theme.ThemeController
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SegmentedControlLabelRasterTest {

    @Test
    fun glassTabRowLabelsStayVisibleAcrossSwitchesWithBoldText() = runSkikoComposeUiTest(
        size = Size(402f, 100f),
        density = Density(1f),
    ) {
        val tabs = listOf("推荐", "热门", "排行榜", "直播")
        var selected by mutableIntStateOf(0)
        setContent {
            COUITheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalPlatformFontWeightAdjustment provides 100) {
                    val backdrop = rememberLayerBackdrop()
                    Box(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                            drawRect(Color(0xFFF3F4F6))
                        }
                        TabRowWithContour(
                            tabs = tabs,
                            selectedTabIndex = selected,
                            onTabSelected = { selected = it },
                            backdrop = backdrop,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
        for (target in listOf(0, 1, 2, 3, 2, 0, 2)) {
            selected = target
            waitForIdle()
            val pixels = onRoot().captureToImage().toPixelMap()
            // Inner capsule spans x = 16..386 (width 370), 4 equal segments of ~92px each.
            for (seg in tabs.indices) {
                val xStart = 16 + seg * 92 + 6
                val xEnd = 16 + (seg + 1) * 92 - 6
                var darkPixels = 0
                for (y in 32 until 68) {
                    for (x in xStart until xEnd) {
                        val c = pixels[x, y]
                        if (c.red < 0.6f && c.green < 0.6f && c.blue < 0.6f) darkPixels++
                    }
                }
                assertTrue(
                    darkPixels > 12,
                    "Segment $seg ('${tabs[seg]}') must remain visible when selected=$target, darkPixels=$darkPixels",
                )
            }
        }
    }
}
