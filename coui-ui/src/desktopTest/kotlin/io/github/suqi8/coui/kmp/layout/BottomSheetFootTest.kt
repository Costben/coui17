// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A ColorOS 17 panel's content ends flush with the screen edge.
 *
 * The platform pads the panel frame by 60dp, but its behavior parks those 60dp below the screen,
 * so nothing of them shows. The sheet used to add them as visible padding, which lifted every
 * panel — and the button at its foot — 60dp above where the system puts it.
 */
@OptIn(ExperimentalTestApi::class)
class BottomSheetFootTest {

    @Test
    fun theContentEndsAtTheWindowBottomByDefault() = runSkikoComposeUiTest(
        size = Size(WIDTH_PX, HEIGHT_PX),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                BottomSheetContentLayout(
                    show = true,
                    backgroundColor = Color.White,
                    cornerRadius = 16.dp,
                    sheetMaxWidth = 10_000.dp,
                    outsideMargin = DpSize(0.dp, 0.dp),
                    insideMargin = DpSize(0.dp, 0.dp),
                    dragHandleColor = Color.Gray,
                    popupHost = { visible, content -> if (visible) content() },
                    topInset = 0.dp,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("content"),
                    )
                }
            }
        }
        waitForIdle()

        val contentBottomPx = onNodeWithTag("content").fetchSemanticsNode().boundsInRoot.bottom
        assertEquals(HEIGHT_PX, contentBottomPx, 1f, "the content's foot sits on the window bottom")
    }

    private companion object {
        const val WIDTH_PX = 400f
        const val HEIGHT_PX = 800f
    }
}
