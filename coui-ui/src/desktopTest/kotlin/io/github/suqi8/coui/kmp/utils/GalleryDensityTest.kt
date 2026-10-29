// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Gallery dpi is the panel's own density, or `(shortEdgePx / 360 * 160).toInt()` on a device
 * that can switch resolution. The reference points are the PLK110 (1272px, resolution switch,
 * 565 dpi), the PKT110 (1240px, physical 560 dpi, no resolution switch, Gallery at 560 dpi) and the
 * 1216px device (540 dpi) the ColorOS 17 Gallery was measured on.
 */
@OptIn(ExperimentalTestApi::class)
class GalleryDensityTest {

    @Test
    fun resolutionSwitchPinsA360dpShortEdge() {
        assertEquals(565, GalleryDensityDefaults.densityDpi(560, 1272, resolutionSwitch = true))
        assertEquals(540, GalleryDensityDefaults.densityDpi(560, 1216, resolutionSwitch = true))
    }

    @Test
    fun withoutResolutionSwitchThePanelDensityWins() {
        // A 360dp short edge would give 551 dpi on the PKT110; Gallery stays at the panel's 560.
        assertEquals(560, GalleryDensityDefaults.densityDpi(560, 1240, resolutionSwitch = false))
    }

    @Test
    fun unknownDisplayFallsBackToThePanelDensity() {
        assertEquals(560, GalleryDensityDefaults.densityDpi(560, 0, resolutionSwitch = true))
        assertEquals(560, GalleryDensityDefaults.densityDpi(560, -1, resolutionSwitch = true))
    }

    @Test
    fun rememberKeepsThePlatformDensityWithoutAnOverride() = runSkikoComposeUiTest(
        size = Size(1272f, 2772f),
        density = Density(1f),
    ) {
        lateinit var result: Density
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.3f)) {
                result = rememberGalleryDensity()
            }
        }
        waitForIdle()
        assertEquals(2f, result.density)
        assertEquals(1.3f, result.fontScale)
    }
}
