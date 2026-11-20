// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

private const val ECHO_SLIDER_TAG = "echo-slider"

/**
 * Callers that keep an integer preference used to feed the rounded value straight back in.
 *
 * `CouiKitSlider` re-syncs that value on every recomposition, and the rounded echo never equals
 * the float the gesture produced — so `syncValue` treated it as an outside change, cancelled the
 * gesture and stopped the animations. The drag froze after one step, and because `press()` had
 * already been cleared, `release()` returned early: `onValueChangeFinished` never ran and the new
 * value was never persisted, even though the label showed it while the page stayed open.
 */
@OptIn(ExperimentalTestApi::class)
class SliderExternalEchoTest {

    private fun androidx.compose.ui.test.SkikoComposeUiTest.integerSlider(
        initialValue: Int,
        onValueChangeFinished: () -> Unit,
    ): () -> Int {
        var value by mutableIntStateOf(initialValue)
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Column(Modifier.width(300.dp)) {
                    Slider(
                        value = value.toFloat(),
                        onValueChange = { value = it.roundToInt() },
                        onValueChangeFinished = onValueChangeFinished,
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag(ECHO_SLIDER_TAG),
                    )
                }
            }
        }
        return { value }
    }

    @Test
    fun aRoundedEchoMustNotStopTheDragHalfway() = runSkikoComposeUiTest {
        var finished = 0
        val value = integerSlider(initialValue = 10, onValueChangeFinished = { finished++ })

        onNodeWithTag(ECHO_SLIDER_TAG).performTouchInput {
            down(Offset(30f, centerY))
            advanceEventTime(16)
            moveTo(Offset(90f, centerY))
            advanceEventTime(16)
            moveTo(Offset(150f, centerY))
            advanceEventTime(16)
            moveTo(Offset(210f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()

        // Every move has to keep counting; the rounded echo must not kill the gesture mid-way.
        assertTrue(value() > 60, "the drag must follow the finger, was ${value()}")
        assertTrue(value() <= 100, "the drag must stay inside the range, was ${value()}")
    }

    @Test
    fun aRoundedEchoMustNotSwallowTheFinishCallback() = runSkikoComposeUiTest {
        var finished = 0
        val value = integerSlider(initialValue = 10, onValueChangeFinished = { finished++ })

        onNodeWithTag(ECHO_SLIDER_TAG).performTouchInput {
            down(Offset(30f, centerY))
            advanceEventTime(16)
            moveTo(Offset(90f, centerY))
            advanceEventTime(16)
            moveTo(Offset(150f, centerY))
            advanceEventTime(16)
            up()
        }
        waitForIdle()

        assertTrue(finished >= 1, "release must report the finished drag so the caller can persist it")
        assertTrue(value() > 40, "the drag must follow the finger, was ${value()}")
    }
}
