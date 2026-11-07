// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where the hairline between two card rows starts. COUIPreferenceItemDecoration.getDividerInsetStart
 * aligns it with the title view, so it sits under the text: 16dp in on a plain row, and past the icon
 * and its 16dp margin on a row with a leading icon. It used to start 32dp in, half a character past
 * the text.
 */
@OptIn(ExperimentalTestApi::class)
class BasicComponentDividerPaintTest {

    @Test
    fun theHairlineStartsUnderTheTitleOfAPlainRow() {
        assertEquals(16 to WIDTH_PX - 16, paintedHairline(withIcon = false))
    }

    @Test
    fun theHairlineStartsUnderTheTitleOfARowWithAnIcon() {
        assertEquals(16 + ICON_DP + 16 to WIDTH_PX - 16, paintedHairline(withIcon = true))
    }

    private fun paintedHairline(withIcon: Boolean): Pair<Int, Int> {
        var result = -1 to -1
        runSkikoComposeUiTest(size = Size(WIDTH_PX.toFloat(), HEIGHT_PX.toFloat()), density = Density(1f)) {
            setContent { Harness(withIcon) }
            waitForIdle()
            val image = onRoot().captureToImage().toAwtImage()
            // The first row's hairline is its bottom pixel row; search a few rows around it.
            val rows = (0 until image.height).filter { y ->
                (0 until image.width).count { x -> image.getRGB(x, y) and 0xFFFFFF != 0xFFFFFF } in 100..WIDTH_PX
            }
            val y = rows.first()
            val painted = (0 until image.width).filter { x -> image.getRGB(x, y) and 0xFFFFFF != 0xFFFFFF }
            result = painted.first() to painted.last() + 1
        }
        return result
    }

    @Composable
    private fun Harness(withIcon: Boolean) {
        COUITheme {
            Column(Modifier.fillMaxSize().background(Color.White)) {
                BasicComponent(
                    startAction = if (withIcon) {
                        { Box(Modifier.size(ICON_DP.dp)) }
                    } else {
                        null
                    },
                    cardListPosition = CardListPosition.Head,
                )
                BasicComponent(cardListPosition = CardListPosition.Tail)
            }
        }
    }

    private companion object {
        const val WIDTH_PX = 300
        const val HEIGHT_PX = 200
        const val ICON_DP = 24
    }
}
