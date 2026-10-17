// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TooltipPositionTest {
    private val window = IntSize(400, 800)
    private val bubble = IntSize(200, 60)

    private fun provider(): PopupPositionProvider {
        lateinit var result: PopupPositionProvider
        runSkikoComposeUiTest(size = Size(10f, 10f), density = Density(1f)) {
            setContent { result = TooltipDefaults.rememberTooltipPositionProvider() }
            waitForIdle()
        }
        return result
    }

    @Test
    fun aTooltipAtTheRightEdgeKeepsTheBubbleMarginFromTheWindowEdge() {
        val anchor = IntRect(left = 360, top = 100, right = 400, bottom = 140)
        val x = provider().calculatePosition(anchor, window, LayoutDirection.Ltr, bubble).x
        assertEquals(window.width - bubble.width - 16, x)
    }

    @Test
    fun aTooltipAtTheLeftEdgeKeepsTheBubbleMarginFromTheWindowEdge() {
        val anchor = IntRect(left = 0, top = 100, right = 40, bottom = 140)
        val x = provider().calculatePosition(anchor, window, LayoutDirection.Ltr, bubble).x
        assertEquals(16, x)
    }

    @Test
    fun aBubbleWiderThanTheMarginsAllowIsCentred() {
        val wide = IntSize(390, 60)
        val anchor = IntRect(left = 360, top = 100, right = 400, bottom = 140)
        val x = provider().calculatePosition(anchor, window, LayoutDirection.Ltr, wide).x
        assertEquals((window.width - wide.width) / 2, x)
    }
}
