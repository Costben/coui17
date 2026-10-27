// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The dim layer must fade in, never paint at full strength on the frame the dialog appears.
 *
 * `DialogDimEnter` is a 300ms `fadeIn`, so the page behind the panel has to darken over several
 * frames. A first frame that is already fully dimmed is a one-frame flash: the panel arrives on a
 * background that is suddenly much darker than it was, and the eye reads it as the backdrop
 * blinking. This walks the entrance frame by frame and checks the ramp is monotone and starts near
 * the undimmed value.
 */
@OptIn(ExperimentalTestApi::class)
class DialogDimRampTest {

    @Test
    fun theDimFadesInInsteadOfJumping() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        mainClock.autoAdvance = false
        var show by mutableStateOf(false)
        setContent {
            Harness(show = show, onShowChange = { show = it })
        }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        val undimmed = pageLuminance()
        println("UNDIMMED $undimmed")

        show = true
        val ramp = mutableListOf<Int>()
        repeat(FRAMES) {
            mainClock.advanceTimeByFrame()
            ramp += pageLuminance()
        }
        println("DIM RAMP $ramp")

        // The page starts undimmed and only darkens as the 300ms fade runs.
        assertTrue(
            ramp.first() >= undimmed - 2,
            "the first frame must not be dimmed yet: first=${ramp.first()} undimmed=$undimmed",
        )
        // Monotone non-increasing: a mid-entrance brightening would be a flash in the other
        // direction, and an early jump followed by a recovery is the one the user reported.
        val rises = ramp.zipWithNext().count { (a, b) -> b > a + 1 }
        assertTrue(rises == 0, "the dim ramp must not brighten again mid-entrance: $ramp")

        // And it does reach the dimmed value, so the test is not passing on a no-op.
        assertTrue(
            ramp.last() <= undimmed - 20,
            "the dim must still reach its target: last=${ramp.last()} undimmed=$undimmed",
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.pageLuminance(): Int {
        val image = onRoot().captureToImage().toAwtImage()
        val pixels = IntArray(image.width * image.height).also {
            image.getRGB(0, 0, image.width, image.height, it, 0, image.width)
        }
        // A point in the top-left corner: the panel is centred, so this only sees the dim.
        val c = pixels[6 * image.width + 6]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /**
     * The panel itself must not blink either: with a backdrop behind it the glass samples the page,
     * and if that texture is not ready on the first frame the panel paints its flat fallback and
     * then swaps to the glass — a visible one-frame change of the panel's own background.
     */
    @Test
    fun thePanelBackgroundDoesNotBlink() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        mainClock.autoAdvance = false
        var show by mutableStateOf(false)
        setContent {
            COUITheme {
                val backdrop = rememberLayerBackdrop { drawContent() }
                Scaffold(containerColor = Color.White) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier.fillMaxSize()
                                .layerBackdrop(backdrop)
                                .background(Color(0xFFD8D8D8)),
                        )
                        CompositionLocalProvider(LocalBackdrop provides backdrop) {
                            OverlayAlertDialog(
                                show = show,
                                onConfirm = { show = false },
                                onCancel = { show = false },
                                title = "Delete backup?",
                                summary = "This cannot be undone.",
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        show = true
        val ramp = mutableListOf<Int>()
        repeat(FRAMES) {
            mainClock.advanceTimeByFrame()
            ramp += panelLuminance()
        }
        println("PANEL RAMP $ramp")

        // The panel fades in, so its interior rises; it must never darken again on the way in.
        val drops = ramp.zipWithNext().count { (a, b) -> b < a - 2 }
        assertTrue(drops == 0, "the panel background must not blink during the entrance: $ramp")
    }

    private fun androidx.compose.ui.test.ComposeUiTest.panelLuminance(): Int {
        val image = onRoot().captureToImage().toAwtImage()
        val pixels = IntArray(image.width * image.height).also {
            image.getRGB(0, 0, image.width, image.height, it, 0, image.width)
        }
        val c = pixels[(image.height / 2) * image.width + image.width / 2]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    @Composable
    private fun Harness(show: Boolean, onShowChange: (Boolean) -> Unit) {
        COUITheme {
            Scaffold(containerColor = Color.White) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    OverlayAlertDialog(
                        show = show,
                        onConfirm = { onShowChange(false) },
                        onCancel = { onShowChange(false) },
                        title = "Delete backup?",
                        summary = "This cannot be undone.",
                    )
                }
            }
        }
    }

    private companion object {
        const val WINDOW_WIDTH = 400f
        const val WINDOW_HEIGHT = 600f
        const val FRAMES = 40
    }
}
