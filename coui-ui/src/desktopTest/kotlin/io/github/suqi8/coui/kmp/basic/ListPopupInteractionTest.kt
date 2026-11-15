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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asSkiaPath
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
import io.github.suqi8.coui.kmp.squircle.SquircleDefaults
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
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

    @Test
    fun twoLineRowKeepsSmoothRoundedPressMaskWithoutCircleBlend() = runSkikoComposeUiTest(
        size = Size(500f, 500f),
        density = Density(1f),
    ) {
        val items = listOf(
            DropdownItem(text = "Single"),
            DropdownItem(text = "First line\nSecond line"),
        )
        setContent { Menu(items = items) }
        waitForIdle()

        val twoLine = rowBounds(1)
        assertTrue(twoLine.height > 42f && twoLine.height < 72f, "Expected two-line row height, got ${twoLine.height}")

        val rest = onRoot().captureToImage().toPixelMap()
        mainClock.autoAdvance = false

        // Press the two-line row: 28dp is close to half its height (~29dp), so capsuleCircleBlend = false
        // plus WeightLarge must keep the smooth squircle corner filled at (8dp inset + 8dp, top + 8dp),
        // which lies outside a circular R=28 arc (distance sqrt(20^2 + 20^2) = 28.28 > 28).
        onRoot().performTouchInput { down(twoLine.center) }
        mainClock.advanceTimeBy(300)
        val pressed = onRoot().captureToImage().toPixelMap()
        val edgeX = twoLine.left.toInt() + 2
        val cornerX = twoLine.left.toInt() + 16
        val cornerY = twoLine.top.toInt() + 8
        assertEquals(
            rest[edgeX, twoLine.center.y.toInt()].red,
            pressed[edgeX, twoLine.center.y.toInt()].red,
            0.01f,
            "The press mask keeps its 8dp horizontal margin on two-line rows",
        )
        assertTrue(
            abs(rest[cornerX, cornerY].red - pressed[cornerX, cornerY].red) > 0.02f,
            "Two-line row corner must stay a WeightLarge smooth squircle rather than circle-blending into a capsule",
        )
        runOnUiThread { scene.cancelPointerInput() }
    }

    @Test
    fun multiLineRowKeepsFixed28dpSmoothRoundedPressMask() = runSkikoComposeUiTest(
        size = Size(500f, 500f),
        density = Density(1f),
    ) {
        val items = listOf(
            DropdownItem(text = "Single"),
            DropdownItem(text = "Line 1\nLine 2\nLine 3", summary = "Summary 1\nSummary 2"),
        )
        setContent { Menu(items = items) }
        waitForIdle()

        val multiLine = rowBounds(1)
        assertTrue(multiLine.height >= 80f, "Expected multi-line row height >= 80dp, got ${multiLine.height}")

        val rest = onRoot().captureToImage().toPixelMap()
        mainClock.autoAdvance = false

        // Press the multi-line row: corner radius stays capped at 28dp instead of growing to height / 2,
        // so both the 28dp corner diagonal and the straight vertical side near mid-height are lit.
        onRoot().performTouchInput { down(multiLine.center) }
        mainClock.advanceTimeBy(300)
        val pressed = onRoot().captureToImage().toPixelMap()
        val cornerX = multiLine.left.toInt() + 16
        val cornerY = multiLine.top.toInt() + 8
        val sideX = multiLine.left.toInt() + 10
        val sideY = multiLine.center.y.toInt()
        val cornerDelta = abs(rest[cornerX, cornerY].red - pressed[cornerX, cornerY].red)
        val sideDelta = abs(rest[sideX, sideY].red - pressed[sideX, sideY].red)
        val centerDelta = abs(rest[multiLine.center.x.toInt(), sideY].red - pressed[multiLine.center.x.toInt(), sideY].red)
        assertTrue(
            cornerDelta > 0.02f,
            "Multi-line row press mask must keep a fixed 28dp corner instead of growing to a height/2 capsule (cornerDelta=$cornerDelta, sideDelta=$sideDelta, centerDelta=$centerDelta, bounds=$multiLine)",
        )
        assertTrue(
            sideDelta > 0.02f,
            "Multi-line row press mask must preserve straight vertical edges inside the 8dp inset (sideDelta=$sideDelta)",
        )
        runOnUiThread { scene.cancelPointerInput() }
    }

    @Test
    fun squircleToCapsuleMorphPathMatchesEndpointsAndInterpolatesMonotonically() {
        val width = 220f
        val tallHeight = 58f
        val capsuleHeight = 40f

        val squircleRef = Path().apply {
            addSquircleRect(
                width = width,
                height = tallHeight,
                cornerRadius = 28f,
                squircleEnabled = true,
                capsuleCircleBlend = false,
                weight = SquircleDefaults.WeightLarge,
            )
        }.asSkiaPath()
        val morphZero = Path().apply {
            addSquircleToCapsuleMorphRect(
                width = width,
                height = tallHeight,
                cornerRadius = 28f,
                capsuleFraction = 0f,
            )
        }.asSkiaPath()

        val capsuleRef = Path().apply {
            addSegmentCapsule(Rect(0f, 0f, width, capsuleHeight))
        }.asSkiaPath()
        val morphOne = Path().apply {
            addSquircleToCapsuleMorphRect(
                width = width,
                height = capsuleHeight,
                cornerRadius = 20f,
                capsuleFraction = 1f,
            )
        }.asSkiaPath()

        // Both endpoints must trace identical containment across the corner tile.
        for (x in 0..40) {
            for (y in 0..30) {
                val fx = x + 0.5f
                val fy = y + 0.5f
                assertEquals(squircleRef.contains(fx, fy), morphZero.contains(fx, fy), "Mismatch at fraction=0 ($fx, $fy)")
                assertEquals(capsuleRef.contains(fx, fy), morphOne.contains(fx, fy), "Mismatch at fraction=1 ($fx, $fy)")
            }
        }

        // Mid-transition at a fixed height must sit smoothly between the WeightLarge squircle corner
        // and the full capsule corner without collapsing into a smaller radius.
        val morphHalf = Path().apply {
            addSquircleToCapsuleMorphRect(
                width = width,
                height = 49f,
                cornerRadius = 24f,
                capsuleFraction = 0.5f,
            )
        }.asSkiaPath()
        assertTrue(morphHalf.contains(20f, 20f))
        assertTrue(!morphHalf.contains(1f, 1f))
    }

    @Test
    fun slidingFromMultiLineRowToSingleLineRowMorphsCornerSmoothlyMidSlide() = runSkikoComposeUiTest(
        size = Size(500f, 500f),
        density = Density(1f),
    ) {
        val items = listOf(
            DropdownItem(text = "Single"),
            DropdownItem(text = "First line\nSecond line\nThird line"),
        )
        setContent { Menu(items = items) }
        waitForIdle()

        val single = rowBounds(0)
        val tall = rowBounds(1)
        val rest = onRoot().captureToImage().toPixelMap()
        mainClock.autoAdvance = false

        onRoot().performTouchInput {
            down(tall.center)
            advanceEventTime(300)
        }
        mainClock.advanceTimeBy(300)

        // Drag up onto the single-line row and stop mid-spring (~32ms) so the mask height is still
        // taller than 40dp, confirming the corner radius and weight interpolate during the slide
        // instead of waiting for the final frame to snap into a capsule.
        onRoot().performTouchInput {
            moveTo(single.center, delayMillis = 16)
        }
        mainClock.advanceTimeBy(32)
        val midSlide = onRoot().captureToImage().toPixelMap()
        val midY = ((single.center.y + tall.center.y) * 0.5f).toInt()
        assertTrue(
            abs(rest[single.center.x.toInt(), midY].red - midSlide[single.center.x.toInt(), midY].red) > 0.02f,
            "Mid-slide mask must remain visible between the tall and single-line rows",
        )

        // After settling, the mask clears the tall row bottom and settles inside the 40dp single-line capsule.
        mainClock.advanceTimeBy(300)
        val settled = onRoot().captureToImage().toPixelMap()
        val settledSingle = rowBounds(0)
        val settledTall = rowBounds(1)
        assertEquals(
            rest[tall.center.x.toInt(), tall.bottom.toInt() - 4].red,
            settled[settledTall.center.x.toInt(), settledTall.bottom.toInt() - 4].red,
            0.01f,
            "Settled mask must clear the previous multi-line row",
        )
        val sampleX = settledSingle.left.toInt() + 14
        val sampleY = settledSingle.center.y.toInt()
        val delta = abs(rest[single.left.toInt() + 14, single.center.y.toInt()].red - settled[sampleX, sampleY].red)
        val centerDelta = abs(rest[single.center.x.toInt(), single.center.y.toInt()].red - settled[settledSingle.center.x.toInt(), settledSingle.center.y.toInt()].red)
        assertTrue(
            delta > 0.02f,
            "Settled mask must light the single-line capsule row (delta=$delta, centerDelta=$centerDelta, before=$single, after=$settledSingle)",
        )
        runOnUiThread { scene.cancelPointerInput() }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun SkikoComposeUiTest.rowBounds(index: Int): Rect = onNodeWithTag("row-$index").fetchSemanticsNode().boundsInRoot

@Composable
private fun Menu(
    dark: Boolean = false,
    disabledSecond: Boolean = false,
    glass: Boolean = false,
    items: List<DropdownItem> = listOf(DropdownItem(text = "Item 0"), DropdownItem(text = "Item 1")),
    onSelect: (Int) -> Unit = {},
) {
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
                    items.forEachIndexed { index, item ->
                        DropdownImpl(
                            item = item,
                            optionSize = items.size,
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
