// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.blur.highlight.LightPosition
import io.github.suqi8.coui.kmp.blur.highlight.rememberTiltLight
import io.github.suqi8.coui.kmp.blur.sensor.DeviceTilt
import io.github.suqi8.coui.kmp.blur.sensor.rememberDeviceTilt
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DeviceTiltTest {

    @Test
    fun rememberDeviceTiltReturnsZeroOnDesktop() = runSkikoComposeUiTest(
        size = Size(100f, 100f),
        density = Density(1f),
    ) {
        var observed: DeviceTilt? = null
        val base = LightPosition(0.5f, 0.5f, -0.5f)
        var observedPosition: LightPosition? = null

        setContent {
            observed = rememberDeviceTilt().value
            observedPosition = rememberTiltLight(basePosition = base).position
        }
        waitForIdle()

        assertEquals(DeviceTilt.Zero, observed)
        assertEquals(base, observedPosition)
    }
}
