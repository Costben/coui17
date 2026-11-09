// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatingActionButtonMaterialTest {
    @Test
    fun theCausticLightKeepsTheHueAtALumaOf80() {
        val caustic = FloatingActionButtonDefaults.causticColor(Color(0xFF0080FF))
        val luma = 0.299f * caustic.red + 0.587f * caustic.green + 0.114f * caustic.blue
        assertTrue(abs(luma * 255f - 80f) < 1f, "luma ${luma * 255f}")
        assertTrue(caustic.blue > caustic.green && caustic.green > caustic.red)
        assertEquals(153f / 255f, caustic.alpha, 1e-3f)
    }

    @Test
    fun blackAndWhiteFallBackToTheNativeGreys() {
        assertEquals(Color(0xFF828282).copy(alpha = 153f / 255f), FloatingActionButtonDefaults.causticColor(Color.Black))
        assertEquals(Color(0xFF0B0B0B).copy(alpha = 153f / 255f), FloatingActionButtonDefaults.causticColor(Color.White))
    }

    @Test
    fun theFabLightIsOpaqueSmall1() {
        assertEquals(0.6f, SpotLightDefaults.opaqueSmallParams1(isDark = false).endIntensity)
        assertEquals(0.5f, SpotLightDefaults.opaqueSmallParams1(isDark = true).endIntensity)
    }
}
