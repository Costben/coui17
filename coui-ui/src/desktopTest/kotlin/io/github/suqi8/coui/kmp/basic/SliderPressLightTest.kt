// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The slider press feedback follows `COUISeekBar`: a touch alone (no drag) presses the slider,
 * the thumb wears `TexturedThumbRenderer`'s frosted look while held and turns solid again after
 * release.
 */
@OptIn(ExperimentalTestApi::class)
class SliderPressLightTest {

    @Test
    fun innerGlowFadesInwardFromTheEdge() {
        // A 10px band blurred with sigma 5 * 0.57735 + 0.5: 0.86 at the rim, half at 5px, gone by 12px.
        assertEquals(0.86f, sliderThumbInnerGlowAlpha(0f), 0.01f)
        assertEquals(0.5f, sliderThumbInnerGlowAlpha(5f), 0.01f)
        assertTrue(sliderThumbInnerGlowAlpha(12f) < 0.03f)
    }

    @Test
    fun theLightKeepsFollowingAFingerThatNeverStops() {
        // The finger moves 12px every 16ms frame for half a second and never pauses.
        var position = 0f
        var velocity = 0f
        var target = 0f
        repeat(30) {
            target += 12f
            val step = sliderLightFollowStep(position, velocity, target, 0.016f)
            position = step.position
            velocity = step.velocity
        }
        // A 750px/s target trails by about v * 2 * 0.85 / (2 * PI / 0.35) = 71px on the move spring.
        assertTrue(position > 250f, "light stalled at $position while the finger reached $target")
        assertTrue(target - position < 90f)
    }

    @Test
    fun theLightSettlesOnTheFingerOnceItStops() {
        var step = SliderLightFollow(0f, 0f)
        repeat(60) { step = sliderLightFollowStep(step.position, step.velocity, 100f, 0.016f) }
        assertEquals(100f, step.position, 0.5f)
    }

    @Test
    fun rangePressPicksTheThumbADragWouldTake() {
        // 1000px wide, 10px cap inset: thumbs at 206 (0.2) and 794 (0.8).
        fun picks(x: Float, last: Boolean = true) = rangePressPicksStart(x, 1000, 10f, 0.2f, 0.8f, isRtl = false, lastDraggedIsStart = last)
        assertTrue(picks(210f))
        assertFalse(picks(790f))
        assertTrue(picks(400f))
        assertFalse(picks(600f))
        // RTL mirrors the positions.
        assertFalse(rangePressPicksStart(210f, 1000, 10f, 0.2f, 0.8f, isRtl = true, lastDraggedIsStart = true))
    }

    @Test
    fun aTouchWithoutDragPressesAndTexturesTheThumb() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var light: SliderPressLight
        setContent {
            light = remember { SliderPressLight() }
            Box(Modifier.size(200.dp, 36.dp).testTag("slider").sliderPressLight(light, enabled = true))
        }
        waitForIdle()
        assertFalse(light.isPressed)
        assertFalse(light.texturedAt())

        onNodeWithTag("slider").performTouchInput { down(center) }
        mainClock.advanceTimeBy(600)
        assertTrue(light.isPressed)
        assertTrue(light.texturedAt())

        onNodeWithTag("slider").performTouchInput { up() }
        mainClock.advanceTimeBy(1_000)
        assertFalse(light.isPressed)
        assertFalse(light.texturedAt())
    }

    private fun SliderPressLight.texturedAt(): Boolean {
        var drawn = false
        val size = Size(40f, 40f)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(ImageBitmap(40, 40)), size) {
            drawn = drawTexturedThumb(this, Offset(20f, 20f), 16f)
        }
        return drawn
    }
}
