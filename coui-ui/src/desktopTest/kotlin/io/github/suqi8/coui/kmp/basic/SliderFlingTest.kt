// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val FLING_SLIDER_TAG = "fling-slider"
private const val FLING_VERTICAL_SLIDER_TAG = "fling-vertical-slider"

/**
 * The fling after a release reads the animated fraction, not the [Slider]'s own `value`.
 *
 * Those two are the same only for the 0..1 default range. On a percent range, reading `value`
 * turned every fling frame into `fractionToValue(42f)`, i.e. the fraction 1, so a short fast drag
 * slammed the thumb to the end and a fast drag away from the end jumped straight back.
 *
 * Key-point sliders take COUI's other path: `COUISectionSeekBar` overrides `handleMotionEventUp`
 * and snaps to the section nearest the finger without running the physics fling, so a fast drag
 * there must land on a tick and stay on it.
 */
@OptIn(ExperimentalTestApi::class)
class SliderFlingTest {

    private fun androidx.compose.ui.test.SkikoComposeUiTest.slider(
        initialValue: Float,
        steps: Int = 0,
        valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    ): () -> Float {
        var value by mutableFloatStateOf(initialValue)
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Column(Modifier.width(300.dp)) {
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = valueRange,
                        steps = steps,
                        modifier = Modifier.testTag(FLING_SLIDER_TAG),
                    )
                }
            }
        }
        return { value }
    }

    @Test
    fun aFastDragUpGlidesPastTheReleaseInsteadOfJumpingToTheEnd() = runSkikoComposeUiTest {
        val value = slider(initialValue = 0f)
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(30f, centerY))
            advanceEventTime(16)
            moveTo(Offset(80f, centerY))
            advanceEventTime(16)
            moveTo(Offset(130f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        // The finger stops around 42%; the fling may carry further but must not reach the end.
        assertTrue(value() > 50f, "the fling must keep sliding past the release, was ${value()}")
        assertTrue(value() < 95f, "a short fast drag must not slam the value to the end, was ${value()}")
    }

    @Test
    fun aFastDragDownGlidesPastTheReleaseInsteadOfSnappingBack() = runSkikoComposeUiTest {
        val value = slider(initialValue = 100f)
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(270f, centerY))
            advanceEventTime(16)
            moveTo(Offset(220f, centerY))
            advanceEventTime(16)
            moveTo(Offset(170f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        // The finger stops around 57%; the thumb must follow it down, not spring back to 100.
        assertTrue(value() < 60f, "the thumb must not spring back to the end, was ${value()}")
        assertTrue(value() > 5f, "the fling must not slam the value to the start, was ${value()}")
    }

    @Test
    fun aNewDragCancelsTheFlingFromThePreviousRelease() = runSkikoComposeUiTest {
        val value = slider(initialValue = 0f)
        mainClock.autoAdvance = false
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(30f, centerY))
            advanceEventTime(16)
            moveTo(Offset(80f, centerY))
            advanceEventTime(16)
            moveTo(Offset(130f, centerY))
            advanceEventTime(16)
            up()
        }
        mainClock.advanceTimeBy(32)
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(120f, centerY))
            advanceEventTime(16)
            moveTo(Offset(100f, centerY))
            advanceEventTime(16)
            up()
        }
        mainClock.autoAdvance = true
        waitForIdle()
        // The second drag ends around 32%; the old fling must not carry the thumb back to ~80.
        assertTrue(value() < 40f, "the previous fling must stop when a new drag starts, was ${value()}")
    }

    @Test
    fun aSteppedSliderStopsOnTheKeyPointUnderTheFinger() = runSkikoComposeUiTest {
        val value = slider(initialValue = 0f, steps = 7)
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(30f, centerY))
            advanceEventTime(16)
            moveTo(Offset(80f, centerY))
            advanceEventTime(16)
            moveTo(Offset(130f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        // COUISectionSeekBar snaps to the section nearest the finger instead of flinging:
        // the release sits at 42.6%, so the 8 ticks (12.5 apart) land on 37.5 and stay there.
        assertEquals(37.5f, value(), 0.01f)
    }

    @Test
    fun aSteppedSliderDoesNotSpringBackToTheEnd() = runSkikoComposeUiTest {
        val value = slider(initialValue = 100f, steps = 7)
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(270f, centerY))
            advanceEventTime(16)
            moveTo(Offset(220f, centerY))
            advanceEventTime(16)
            moveTo(Offset(170f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        // The finger stops at 57.4%; the nearest tick is 62.5, and no inertia pulls it back to 100.
        assertEquals(62.5f, value(), 0.01f)
    }

    @Test
    fun aSteppedDragTrailsTheFingerInsteadOfJumpingToTheNextTick() = runSkikoComposeUiTest {
        val value = slider(initialValue = 0f, steps = 7)
        // The 300dp track keeps a 14px cap inset, so its 8 sections are 34px wide each.
        // COUISectionSeekBar only advances once the finger has travelled a whole section from the
        // anchor, so a finger most of the way to the next tick still reports the current one.
        mainClock.autoAdvance = false
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(30f, centerY))
            advanceEventTime(16)
            moveTo(Offset(60f, centerY))
            advanceEventTime(16)
        }
        mainClock.advanceTimeBy(64)
        val afterFirstMove = value()
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            moveTo(Offset(80f, centerY))
            advanceEventTime(16)
        }
        mainClock.advanceTimeBy(64)
        val afterSecondMove = value()
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput { up() }
        mainClock.autoAdvance = true
        waitForIdle()
        // 60px already reaches the second tick, so the drag is live before the second move.
        assertEquals(12.5f, afterFirstMove, 0.01f)
        // 80px is 1.9 sections in: the value must still trail on the second tick.
        assertEquals(12.5f, afterSecondMove, 0.01f)
        // Releasing snaps to the section nearest the finger, so the value catches up at the end.
        assertEquals(25f, value(), 0.01f)
    }

    @Test
    fun aFineSteppedDragLandsOnAWholeStep() = runSkikoComposeUiTest {
        // The gallery's "Steps" slider: 0..200 with 199 steps, so a section is 1.36px wide.
        val value = slider(initialValue = 100f, steps = 199, valueRange = 0f..200f)
        onNodeWithTag(FLING_SLIDER_TAG).performTouchInput {
            down(Offset(30f, centerY))
            advanceEventTime(16)
            moveTo(Offset(80f, centerY))
            advanceEventTime(16)
            moveTo(Offset(130f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        // 130px is 42.6% of the track, i.e. 85.3 of the 200 sections: the release rounds to 85.
        assertEquals(85f, value(), 0.01f)
    }

    @Test
    fun aVerticalFlingGlidesPastTheRelease() = runSkikoComposeUiTest {
        var value by mutableFloatStateOf(0f)
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Box(Modifier.height(300.dp)) {
                    VerticalSlider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag(FLING_VERTICAL_SLIDER_TAG),
                    )
                }
            }
        }
        // Value 0 sits at the bottom, so dragging up raises it: the finger stops around 42.6%.
        onNodeWithTag(FLING_VERTICAL_SLIDER_TAG).performTouchInput {
            down(Offset(centerX, 270f))
            advanceEventTime(16)
            moveTo(Offset(centerX, 220f))
            advanceEventTime(16)
            moveTo(Offset(centerX, 170f))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        assertTrue(value > 50f, "the vertical fling must keep sliding past the release, was $value")
        assertTrue(value < 95f, "a short fast drag must not slam the value to the end, was $value")
    }
}
