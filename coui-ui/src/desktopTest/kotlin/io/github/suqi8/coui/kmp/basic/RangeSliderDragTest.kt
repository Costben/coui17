// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

private const val RANGE_SLIDER_TAG = "range-slider"

/**
 * The range thumbs are mapped with the track thickness, not the 36dp touch target.
 *
 * The touch target is taller than the 20dp track, so measuring the inset from the layout height
 * (36dp * 0.7 = 25.2dp) instead of the track height (20dp * 0.7 = 14dp) made the value lag behind
 * the thumb by about 4% near both ends.
 */
@OptIn(ExperimentalTestApi::class)
class RangeSliderDragTest {

    private fun androidx.compose.ui.test.SkikoComposeUiTest.rangeSlider(): () -> ClosedFloatingPointRange<Float> {
        var value by mutableStateOf(0f..100f)
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Column(Modifier.width(300.dp)) {
                    RangeSlider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag(RANGE_SLIDER_TAG),
                    )
                }
            }
        }
        return { value }
    }

    @Test
    fun theStartThumbFollowsTheTrackInsetNotTheTouchTarget() = runSkikoComposeUiTest {
        val value = rangeSlider()
        onNodeWithTag(RANGE_SLIDER_TAG).performTouchInput {
            down(Offset(20f, centerY))
            advanceEventTime(16)
            moveTo(Offset(40f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        // x = 40 on a 300px track with a 14px inset: (40 - 14) / (300 - 28) = 9.56%.
        val start = value().start
        assertTrue(start > 8f, "the start thumb must not lag behind the finger, was $start")
        assertTrue(start < 11f, "the start thumb must not run ahead of the finger, was $start")
    }

    @Test
    fun theStartThumbStillReachesTheEndOfTheRange() = runSkikoComposeUiTest {
        val value = rangeSlider()
        onNodeWithTag(RANGE_SLIDER_TAG).performTouchInput {
            down(Offset(20f, centerY))
            advanceEventTime(16)
            moveTo(Offset(5f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()
        assertEquals(0f, value().start)
    }
}
