// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class, ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The Center dialog's transition, measured off the rendered surface frame by frame.
 *
 * ColorOS declares it in `res/anim/coui_center_dialog_enter.xml` / `_exit.xml`: the window scales
 * from 0.8 to 1 about its own centre and fades from 0 to 1 over 250ms on
 * `pathInterpolator(0.3, 0, 0.1, 1)`, and on dismissal it only fades, over 150ms on
 * `pathInterpolator(0.3, 0, 1, 1)`. Those properties — the scale runs with the fade on the way in,
 * nothing scales on the way out, and it is a fade rather than a cut at either end — are what the
 * tests pin; the exact per-frame values belong to the interpolator, not to this layout.
 *
 * The dialog runs without a window dim so the panel's own opacity can be read straight off the
 * pixels: over a fixed backdrop, `alpha = (panel - backdrop) / (opaque panel - backdrop)`.
 */
@OptIn(ExperimentalTestApi::class)
class AlertDialogCenterAnimationTest {

    @Test
    fun theCenterEnterFadesInWhileItScalesUp() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        var show by mutableStateOf(false)
        setContent { CenterDialogHarness(show) { show = it } }
        mainClock.autoAdvance = false
        mainClock.advanceTimeByFrame()

        runOnIdle { show = true }
        val frames = frames(ENTER_FRAMES)

        assertTrue(frames.first().width == 0f, "the panel starts invisible: ${frames.first()}")
        val visible = frames.filter { it.width > 0f }
        assertTrue(visible.size >= 8, "the enter was drawn frame by frame: ${visible.size} painted frames")
        assertTrue(
            visible.zipWithNext().all { (a, b) -> b.width >= a.width - 0.01f },
            "the panel only grows: $visible",
        )
        assertTrue(
            visible.zipWithNext().all { (a, b) -> b.alpha >= a.alpha - 0.02f },
            "and only becomes more opaque: $visible",
        )
        assertTrue(
            visible.any { it.width < 0.95f && it.alpha > 0.2f && it.alpha < 0.85f },
            "the scale runs with the fade instead of before or after it: $visible",
        )
        val last = visible.last()
        assertTrue(abs(last.width - 1f) < 0.02f, "the enter lands on 1.0 scale: $last")
        assertTrue(last.alpha > 0.95f, "and on a fully opaque panel: $last")
    }

    @Test
    fun theCenterExitOnlyFadesOut() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        var show by mutableStateOf(false)
        setContent { CenterDialogHarness(show) { show = it } }
        mainClock.autoAdvance = false
        mainClock.advanceTimeByFrame()
        runOnIdle { show = true }
        frames(ENTER_FRAMES)

        runOnIdle { show = false }
        val frames = frames(EXIT_FRAMES)

        val visible = frames.filter { it.width > 0f }
        assertTrue(visible.first().alpha > 0.9f, "the exit starts opaque: ${visible.first()}")
        assertTrue(
            visible.all { abs(it.width - 1f) < 0.02f },
            "nothing scales on the way out: $visible",
        )
        assertTrue(
            visible.zipWithNext().all { (a, b) -> b.alpha <= a.alpha + 0.02f },
            "the panel only becomes more transparent: $visible",
        )
        assertTrue(
            visible.any { it.alpha > 0.2f && it.alpha < 0.85f },
            "the panel is caught mid-fade, so the exit is not a cut: $visible",
        )
        assertTrue(frames.last().width == 0f, "and it is gone by the end of the run: ${frames.last()}")
    }

    /** Measures the panel once per display frame, [count] frames in total. */
    private fun ComposeUiTest.frames(count: Int): List<Sample> = buildList {
        repeat(count) {
            mainClock.advanceTimeByFrame()
            add(sample())
        }
    }

    /**
     * The panel's width relative to its resting width, and its opacity, read off the middle scanline
     * of the rendered surface. The panel is white over a blue page, so its red channel separates the
     * two no matter how far the fade has gone; a sample of `0f` means nothing was painted.
     */
    private fun ComposeUiTest.sample(): Sample {
        val map = onRoot().captureToImage().toPixelMap()
        val row = WINDOW_HEIGHT.toInt() / 2
        fun red(x: Int) = map[x, row].red * 255f

        var left = -1
        var right = -1
        for (x in 0 until map.width) {
            if (red(x) > BACKDROP_RED + 2f) {
                if (left < 0) left = x
                right = x
            }
        }
        if (left < 0) return Sample(0f, 0f)
        val center = map[(left + right) / 2, row].red * 255f
        val alpha = ((center - BACKDROP_RED) / (OPAQUE_RED - BACKDROP_RED)).coerceIn(0f, 1f)
        return Sample((right - left + 1) / PANEL_WIDTH, alpha)
    }

    private data class Sample(val width: Float, val alpha: Float)
}

/** A white panel over a flat page, with the window dim switched off so opacity reads off the pixels. */
@Composable
private fun CenterDialogHarness(show: Boolean, onShowChange: (Boolean) -> Unit) {
    COUITheme {
        Scaffold {
            Box(Modifier.fillMaxSize().background(Color(PAGE))) {
                OverlayAlertDialog(
                    show = show,
                    onConfirm = { onShowChange(false) },
                    onCancel = { onShowChange(false) },
                    title = "Delete backup?",
                    summary = "This cannot be undone.",
                    colors = AlertDialogColors(
                        titleColor = Color.Black,
                        summaryColor = Color.Gray,
                        backgroundColor = Color.White,
                    ),
                    enableWindowDim = false,
                )
            }
        }
    }
}

/** The page the dialog opens over; its red channel is what the panel's opacity is read against. */
private val PAGE = 0xFF3366CC

/** The page's red channel, `0x33`. */
private const val BACKDROP_RED = 0x33 * 1f

/** The panel's red channel once fully painted, read off the resting frame. */
private const val OPAQUE_RED = 255f

private const val WINDOW_WIDTH = 400f
private const val WINDOW_HEIGHT = 800f

/** `coui_dialog_max_width` (392dp) minus the 16dp the panel background insets on both sides. */
private const val PANEL_WIDTH = 360f

/** 250ms at 60Hz, with a frame of slack at each end. */
private const val ENTER_FRAMES = 18

/** 150ms at 60Hz, with enough frames after it for the dialog to leave the composition. */
private const val EXIT_FRAMES = 14
