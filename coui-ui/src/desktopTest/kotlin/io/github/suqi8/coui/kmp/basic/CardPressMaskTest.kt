// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardPressMaskTest {
    @Test
    fun theLightPressMaskIsAnOpaqueFill() {
        // couiColorCardPressed #E6E6E6 over the white card.
        assertEquals(Color(0xFFE6E6E6), Color.White.maskedBy(Color(0xFFE6E6E6), 1f))
    }

    @Test
    fun theNightPressMaskStaysOpaqueInsteadOfReplacingTheFill() {
        // couiColorCardPressed #33FFFFFF is a 20% mask; lerping towards it would drop the card to
        // 20% alpha, which COUIMaskEffectDrawable never does.
        val rest = Color(0xFF1E1E1E)
        val pressed = rest.maskedBy(Color(0x33FFFFFF), 1f)
        assertEquals(1f, pressed.alpha)
        assertTrue(pressed.red > rest.red && pressed.green > rest.green && pressed.blue > rest.blue)
    }

    @Test
    fun theMaskScalesWithItsProgress() {
        val rest = Color(0xFF1E1E1E)
        assertEquals(rest, rest.maskedBy(Color(0x33FFFFFF), 0f))
        val half = rest.maskedBy(Color(0x33FFFFFF), 0.5f)
        assertTrue(half.red > rest.red)
        assertTrue(half.red < rest.maskedBy(Color(0x33FFFFFF), 1f).red)
    }
}
