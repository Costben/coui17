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

    @Test
    fun plainTooltipCaretAttachesSeamlesslyInAllFourDirections() {
        val cases = listOf(
            TooltipAnchorPosition.Below to Alignment.TopCenter,
            TooltipAnchorPosition.Above to Alignment.BottomCenter,
            TooltipAnchorPosition.Left to Alignment.CenterEnd,
            TooltipAnchorPosition.Right to Alignment.CenterStart,
        )
        for ((position, alignment) in cases) {
            runSkikoComposeUiTest(size = Size(320f, 500f), density = Density(1f)) {
                setContent {
                    COUITheme {
                        Box(Modifier.fillMaxSize(), contentAlignment = alignment) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
                                tooltip = {
                                    PlainTooltip(
                                        modifier = Modifier.testTag(BUBBLE_TAG),
                                        containerColor = Bubble,
                                    ) { Text("Edit") }
                                },
                                state = rememberTooltipState(initialIsVisible = true, isPersistent = true),
                            ) {
                                Box(Modifier.size(40.dp))
                            }
                        }
                    }
                }
                val image = onNodeWithTag(BUBBLE_TAG).captureToImage().toPixelMap()
                val caretPad = TooltipDefaults.CaretPadding.value.toInt()
                when (position) {
                    TooltipAnchorPosition.Below -> {
                        val x = image.width / 2
                        for (y in (caretPad - 4)..(caretPad + 4)) {
                            assertEquals(Bubble, image[x, y], "gap at Below ($x, $y)")
                        }
                    }

                    TooltipAnchorPosition.Above -> {
                        val x = image.width / 2
                        val edgeY = image.height - caretPad
                        for (y in (edgeY - 4)..(edgeY + 4)) {
                            assertEquals(Bubble, image[x, y], "gap at Above ($x, $y)")
                        }
                    }

                    TooltipAnchorPosition.Left -> {
                        val y = image.height / 2
                        val edgeX = image.width - caretPad
                        for (x in (edgeX - 4)..(edgeX + 4)) {
                            assertEquals(Bubble, image[x, y], "gap at Left ($x, $y)")
                        }
                    }

                    TooltipAnchorPosition.Right -> {
                        val y = image.height / 2
                        for (x in (caretPad - 4)..(caretPad + 4)) {
                            assertEquals(Bubble, image[x, y], "gap at Right ($x, $y)")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun edgeAnchoredRichTooltipCaretAttachesWithoutGap() = runSkikoComposeUiTest(size = Size(320f, 500f), density = Density(1f)) {
        setBubble(Alignment.TopEnd)
        val image = onNodeWithTag(BUBBLE_TAG).captureToImage().toPixelMap()
        val caretPad = TooltipDefaults.CaretPadding.value.toInt()
        // Scan across the top caret band to find the caret's x coordinate, then verify every pixel
        // from the caret body straight down into the bubble interior is solid Bubble color.
        val caretY = caretPad - 3
        val caretXs = (0 until image.width).filter { x -> image[x, caretY] == Bubble }
        assertTrue(caretXs.isNotEmpty(), "edge caret must be visible in top padding band")
        val sampleX = caretXs[caretXs.size / 2]
        for (y in caretY..(caretPad + 6)) {
            assertEquals(Bubble, image[sampleX, y], "gap at edge-anchored RichTooltip ($sampleX, $y)")
        }
    }

    @Test
    fun richTooltipStaysCompactOnPhoneWidthWindow() = runSkikoComposeUiTest(size = Size(360f, 600f), density = Density(1f)) {
        setBubble(Alignment.TopCenter)
        val bounds = onNodeWithTag(BUBBLE_TAG).fetchSemanticsNode().boundsInWindow
        assertTrue(
            bounds.width <= TooltipDefaults.RichTooltipMaxWidth.value + 0.5f,
            "RichTooltip width (${bounds.width}) must stay capped at ${TooltipDefaults.RichTooltipMaxWidth}",
        )
        assertTrue(bounds.width <= 260.5f, "RichTooltip must not span full 360dp row: $bounds")
    }

    @Test
    fun richTooltipStartAndEndAlignBesideCenteredAnchorWithoutOverlap() {
        val anchorTag = "anchor"
        for (position in listOf(TooltipAnchorPosition.Start, TooltipAnchorPosition.End)) {
            runSkikoComposeUiTest(size = Size(360f, 600f), density = Density(1f)) {
                setContent {
                    COUITheme {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
                                modifier = Modifier.testTag(anchorTag),
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
                waitForIdle()
                val anchorBounds = onNodeWithTag(anchorTag).fetchSemanticsNode().boundsInWindow
                val bubbleBounds = onNodeWithTag(BUBBLE_TAG).fetchSemanticsNode().boundsInWindow
                if (position == TooltipAnchorPosition.Start) {
                    assertTrue(
                        bubbleBounds.right <= anchorBounds.left,
                        "Start RichTooltip ($bubbleBounds) must sit strictly left of anchor ($anchorBounds)",
                    )
                    assertTrue(bubbleBounds.left >= 15f, "Start RichTooltip must keep left edge margin: $bubbleBounds")
                } else {
                    assertTrue(
                        bubbleBounds.left >= anchorBounds.right,
                        "End RichTooltip ($bubbleBounds) must sit strictly right of anchor ($anchorBounds)",
                    )
                    assertTrue(bubbleBounds.right <= 345f, "End RichTooltip must keep right edge margin: $bubbleBounds")
                }
            }
        }
    }

    @Test
    fun richTooltipAtRightEdgeAnchorClearsAnchorForBothStartAndEnd() {
        val anchorTag = "edge-anchor"
        for (position in listOf(TooltipAnchorPosition.Start, TooltipAnchorPosition.End)) {
            runSkikoComposeUiTest(size = Size(360f, 600f), density = Density(1f)) {
                setContent {
                    COUITheme {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
                                modifier = Modifier.testTag(anchorTag),
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
                waitForIdle()
                val anchorBounds = onNodeWithTag(anchorTag).fetchSemanticsNode().boundsInWindow
                val bubbleBounds = onNodeWithTag(BUBBLE_TAG).fetchSemanticsNode().boundsInWindow
                assertTrue(
                    bubbleBounds.right <= anchorBounds.left,
                    "Right-edge anchor with $position must place RichTooltip ($bubbleBounds) strictly left of anchor ($anchorBounds)",
                )
            }
        }
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
