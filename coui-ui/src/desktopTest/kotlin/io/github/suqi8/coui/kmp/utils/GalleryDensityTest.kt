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
 * The Gallery density is `round(shortEdgePx / 360 * 160) / 160`, capped at the platform density.
 * The reference points are the PLK110 (1272px, 565 dpi) and the 1216px device (540 dpi) the
 * ColorOS 17 Gallery was measured on.
 */
@OptIn(ExperimentalTestApi::class)
class GalleryDensityTest {

    private val platform620dpi = 620f / 160f

    @Test
    fun plk110ShortEdgeIs565Dpi() {
        assertEquals(3.53125f, GalleryDensityDefaults.density(1272, platform620dpi))
    }

    @Test
    fun narrowerDeviceIs540Dpi() {
        assertEquals(540f / 160f, GalleryDensityDefaults.density(1216, platform620dpi))
    }

    @Test
    fun densityNeverExceedsThePlatformDensity() {
        // A 2560px desktop short edge would derive 5.16x; the 2x platform wins.
        assertEquals(2f, GalleryDensityDefaults.density(1600, 2f))
        // A phone whose own density is already below the Gallery's keeps it.
        assertEquals(3f, GalleryDensityDefaults.density(1272, 3f))
    }

    @Test
    fun unknownWindowFallsBackToThePlatformDensity() {
        assertEquals(2.75f, GalleryDensityDefaults.density(0, 2.75f))
        assertEquals(2.75f, GalleryDensityDefaults.density(-1, 2.75f))
    }

    @Test
    fun rememberKeepsTheFontScaleAndAppliesTheDensity() = runSkikoComposeUiTest(
        size = Size(1272f, 2772f),
        density = Density(1f),
    ) {
        lateinit var result: Density
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 3.875f, fontScale = 1.3f)) {
                result = rememberGalleryDensity()
            }
        }
        waitForIdle()
        assertEquals(3.53125f, result.density)
        assertEquals(1.3f, result.fontScale)
    }
}
