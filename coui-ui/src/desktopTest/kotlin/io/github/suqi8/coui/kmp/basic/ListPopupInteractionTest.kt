// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.BezierGravityPathGeometry
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, InternalComposeUiApi::class)
class ListPopupInteractionTest {
    @Test
    fun draggingDeformsTheWholeMenuAndReleaseRestoresIt() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        setContent { Menu() }
        waitForIdle()
        val before = rowBounds(0)
        mainClock.autoAdvance = false
        onRoot().performTouchInput {
            down(before.center)
            moveTo(before.center + Offset(60f, 0f), delayMillis = 32)
        }
        mainClock.advanceTimeBy(300)
        val dragged = rowBounds(0)
        assertTrue(dragged.width > before.width + 1f, "The menu must stretch horizontally while dragging: $before -> $dragged")
        assertTrue(dragged.height < before.height, "The menu must compress on the other axis")
        val second = rowBounds(1)
        assertEquals(dragged.width, second.width, 0.01f, "Both rows share one surface transform")
        onRoot().performTouchInput { up() }
        mainClock.advanceTimeBy(1800)
        assertEquals(before.width, rowBounds(0).width, 0.1f)
        assertEquals(before.center.x, rowBounds(0).center.x, 0.1f)
    }

    @Test
    fun holdingStillDoesNotApplyButtonPressShrink() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        setContent { Menu() }
        waitForIdle()
        val before = rowBounds(0)
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(before.center) }
        mainClock.advanceTimeBy(300)
        assertEquals(before, rowBounds(0), "COUI popup disables its press-scale unit")
        runOnUiThread { scene.cancelPointerInput() }
    }

    @Test
    fun draggingSelectsTheReleaseRow() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        var chosen = -1
        setContent { Menu(onSelect = { chosen = it }) }
        waitForIdle()
        val first = rowBounds(0)
        val second = rowBounds(1)
        onRoot().performTouchInput {
            down(first.center)
            moveTo(second.center, delayMillis = 32)
            up()
        }
        waitForIdle()
        assertEquals(1, chosen)
    }

    @Test
    fun leavingTheHorizontalBoundsCancelsSelection() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        var chosen = -1
        setContent { Menu(onSelect = { chosen = it }) }
        waitForIdle()
        val first = rowBounds(0)
        onRoot().performTouchInput {
            down(first.center)
            moveTo(Offset(first.right + 80f, first.center.y), delayMillis = 32)
            up()
        }
        waitForIdle()
        assertEquals(-1, chosen, "Release outside the menu must not activate its row")
    }

    @Test
    fun returningDuringTheSameGestureRestoresSelection() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        var chosen = -1
        setContent { Menu(onSelect = { chosen = it }) }
        waitForIdle()
        val first = rowBounds(0)
        onRoot().performTouchInput {
            down(first.center)
            moveTo(Offset(first.right + 80f, first.center.y), delayMillis = 32)
            moveTo(first.center, delayMillis = 32)
            up()
        }
        waitForIdle()
        assertEquals(0, chosen)
    }

    @Test
    fun aSecondPointerCancelsSelectionWithoutStartingAnotherRowGesture() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        var chosen = -1
        setContent { Menu(onSelect = { chosen = it }) }
        waitForIdle()
        val first = rowBounds(0)
        onRoot().performTouchInput {
            down(0, first.center)
            down(1, first.center + Offset(10f, 0f))
            up(1)
            moveTo(0, first.center + Offset(5f, 0f), delayMillis = 32)
            up(0)
        }
        waitForIdle()
        assertEquals(-1, chosen)
    }

    @Test
    fun aCancelledGestureClearsThePressMaskWithoutActivatingARow() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        var chosen = -1
        setContent { Menu(onSelect = { chosen = it }) }
        waitForIdle()
        val first = rowBounds(0)
        val probe = first.topLeft + Offset(12f, first.height / 2f)
        val rest = onRoot().captureToImage().toPixelMap()[probe.x.toInt(), probe.y.toInt()]
        mainClock.autoAdvance = false
        onRoot().performTouchInput { down(first.center) }
        mainClock.advanceTimeBy(300)
        val pressed = onRoot().captureToImage().toPixelMap()[probe.x.toInt(), probe.y.toInt()]
        assertTrue(abs(pressed.red - rest.red) > 0.02f, "The held row must draw its press mask")
        // SkikoInputDispatcher.enqueueCancel is a no-op. Send a real platform cancellation to
        // the scene so the production pointer chain receives the consumed synthetic UP.
        runOnUiThread { scene.cancelPointerInput() }
        mainClock.advanceTimeBy(1000)
        val released = onRoot().captureToImage().toPixelMap()[probe.x.toInt(), probe.y.toInt()]
        assertEquals(rest.red, released.red, 0.01f, "Cancellation must not leave a stuck highlight")
        assertEquals(-1, chosen, "Cancellation must not activate the highlighted row")
    }

    @Test
    fun disabledRowsAreSkippedDuringDragSelection() = runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
        var chosen = -1
        setContent { Menu(disabledSecond = true, onSelect = { chosen = it }) }
        waitForIdle()
        val first = rowBounds(0)
        val second = rowBounds(1)
        onRoot().performTouchInput {
            down(first.center)
            moveTo(second.center, delayMillis = 32)
            up()
        }
        waitForIdle()
        assertEquals(-1, chosen)
    }

    @Test
    fun pressMaskIsInsetInBothThemes() {
        for (dark in listOf(false, true)) {
            runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
                setContent { Menu(dark = dark) }
                waitForIdle()
                val row = rowBounds(0)
                val rest = onRoot().captureToImage().toPixelMap()
                mainClock.autoAdvance = false
                onRoot().performTouchInput { down(row.center) }
                mainClock.advanceTimeBy(300)
                val pressed = onRoot().captureToImage().toPixelMap()
                val y = row.center.y.toInt()
                val edge = row.left.toInt() + 2
                val inner = row.left.toInt() + 14
                assertEquals(rest[edge, y].red, pressed[edge, y].red, 0.01f, "The mask leaves an 8dp horizontal margin")
                assertTrue(abs(rest[inner, y].red - pressed[inner, y].red) > 0.02f, "The mask lights the inner row in both themes")
                runOnUiThread { scene.cancelPointerInput() }
            }
        }
    }

    @Test
    fun glassRetainsTheBackdropAndDrawsItsRimInBothThemes() {
        for (dark in listOf(false, true)) {
            runSkikoComposeUiTest(size = Size(500f, 500f), density = Density(1f)) {
                setContent {
                    val backdrop = rememberLayerBackdrop { drawContent() }
                    CompositionLocalProvider(LocalBackdrop provides backdrop) {
                        Box(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.fillMaxSize().layerBackdrop(backdrop).drawBehind {
                                    drawRect(Color.Red, size = Size(size.width / 2f, size.height))
                                    drawRect(Color.Blue, topLeft = Offset(size.width / 2f, 0f), size = Size(size.width / 2f, size.height))
                                },
                            )
                            Menu(dark = dark, glass = true)
                        }
                    }
                }
                waitForIdle()
                val row = rowBounds(0)
                val pixels = onRoot().captureToImage().toPixelMap()
                val y = row.center.y.toInt()
                val left = pixels[row.left.toInt() + 30, y]
                val right = pixels[row.right.toInt() - 30, y]
                // The asymmetric material rim also changes brightness, so compare color dominance
                // within each sample rather than assuming both sides receive equal white light.
                assertTrue(left.red > left.blue + 0.02f && right.blue > right.red + 0.02f, "The glass must retain the backdrop through its native tint layers: $left / $right")
                // Native top rim is a gradient inside the 28dp silhouette, distinct from the fill.
                val top = row.top.toInt() - 8
                val rim = pixels[250, top + 2]
                val inner = pixels[250, top + 6]
                assertTrue(abs(rim.red - inner.red) + abs(rim.blue - inner.blue) > 0.01f, "The menu must draw its material edge in both themes")
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.rowBounds(index: Int): Rect = onNodeWithTag("row-$index").fetchSemanticsNode().boundsInRoot

@Composable
private fun Menu(dark: Boolean = false, disabledSecond: Boolean = false, glass: Boolean = false, onSelect: (Int) -> Unit = {}) {
    COUITheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ListPopupContent(
                popupContentSize = IntSize.Zero,
                onPopupContentSizeChange = {},
                fractionProgress = { 1f },
                alphaProgress = { 1f },
                pathProgress = { 1f },
                gravityPath = BezierGravityPathGeometry(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                popupLayoutPosition = PopupLayoutPosition(showBelow = true, showAbove = false, isRightAligned = true),
                localTransformOrigin = TransformOrigin(1f, 0f),
                modifier = Modifier.width(236.dp),
                enableGlass = glass,
            ) {
                ListPopupColumn {
                    for (index in 0..1) {
                        DropdownImpl(
                            item = DropdownItem(text = "Item $index"),
                            optionSize = 2,
                            isSelected = false,
                            index = index,
                            modifier = Modifier.testTag("row-$index"),
                            enabled = index != 1 || !disabledSecond,
                            groupKey = "actions",
                            onSelectedIndexChange = onSelect,
                        )
                    }
                }
            }
        }
    }
}
