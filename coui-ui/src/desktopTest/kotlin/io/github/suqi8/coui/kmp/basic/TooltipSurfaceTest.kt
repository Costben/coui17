// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BUBBLE_TAG = "bubble"
private val Bubble = Color(0xFFFF00FF)
private val LongText = "A rich tooltip with enough supporting text to want every pixel it can get. ".repeat(4)

/** The bubble of [RichTooltip] on the 320dp window of a ColorOS phone at its largest display size. */
@OptIn(ExperimentalTestApi::class)
class TooltipSurfaceTest {
    @Test
    fun theBubbleKeepsItsMarginOnAWindowNarrowerThanMaxWidth() = runSkikoComposeUiTest(size = Size(320f, 500f), density = Density(1f)) {
        setBubble(Alignment.TopCenter)
        val width = Density(1f).run { onNodeWithTag(BUBBLE_TAG).getBoundsInRoot().width.toPx() }
        assertTrue(width <= 288f, "bubble is $width wide")
    }

    @Test
    fun theCaretIsNotDimmedByTheBubbleShadow() = runSkikoComposeUiTest(size = Size(320f, 500f), density = Density(1f)) {
        setBubble(Alignment.TopCenter)
        val image = onNodeWithTag(BUBBLE_TAG).captureToImage().toPixelMap()
        // The anchor is centred, and the arrow's body sits in the padding above the bubble.
        val caret = image[image.width / 2, TooltipDefaults.CaretPadding.value.toInt() - 2]
        assertEquals(Bubble, caret)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.setBubble(alignment: Alignment) {
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize(), contentAlignment = alignment) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = {
                            RichTooltip(
                                modifier = Modifier.testTag(BUBBLE_TAG),
                                colors = TooltipDefaults.richTooltipColors().copy(containerColor = Bubble),
                            ) { Text(LongText) }
                        },
                        state = rememberTooltipState(initialIsVisible = true, isPersistent = true),
                    ) {
                        Box(Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}
