// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.graphics.Color
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import kotlin.test.Test
import kotlin.test.assertEquals

/** The opt-in blurred presets must carry `COUIMaterialBlurEffect` / `COUIMaterialThemeBlurEffect`'s values. */
class NativeBlurMaterialTest {

    private val accent = Color(0xFF0066FF)

    @Test
    fun secondaryButtonUsesContentSecondaryButtonParams() {
        val light = ButtonDefaults.glassMaterial(Color.Gray, isAccent = false, isDefaultSecondary = true, isDark = false, enabled = true)
        assertEquals(25f, light.blurSigmaPx)
        assertEquals(listOf(BlendColorEntry(Color(0xCCFFFFFF))), light.layers)
        val dark = ButtonDefaults.glassMaterial(Color.Gray, isAccent = false, isDefaultSecondary = true, isDark = true, enabled = true)
        assertEquals(listOf(BlendColorEntry(Color(0x33FFFFFF))), dark.layers)
    }

    @Test
    fun primaryButtonUsesContentPrimaryButtonParams() {
        val light = ButtonDefaults.glassMaterial(accent, isAccent = true, isDefaultSecondary = false, isDark = false, enabled = true)
        assertEquals(10f, light.blurSigmaPx)
        assertEquals(accent.copy(alpha = 229 / 255f), light.layers[0].color)
        assertEquals(BlendColorEntry(Color(0x00FFFFFF), BlurBlendMode.Overlay), light.layers[1])
        val disabled = ButtonDefaults.glassMaterial(accent, isAccent = true, isDefaultSecondary = false, isDark = false, enabled = false)
        assertEquals(accent.copy(alpha = 77 / 255f), disabled.layers[0].color)
        assertEquals(BlendColorEntry(Color(0x19FFFFFF), BlurBlendMode.Overlay), disabled.layers[1])
    }

    @Test
    fun customFillKeepsItsOwnColour() {
        val custom = Color(0x80FF0000)
        val material = ButtonDefaults.glassMaterial(custom, isAccent = false, isDefaultSecondary = false, isDark = false, enabled = true)
        assertEquals(listOf(BlendColorEntry(custom)), material.layers)
    }

    @Test
    fun accentFillIgnoresAlpha() {
        assertEquals(true, ButtonDefaults.isAccentFill(accent.copy(alpha = 0.5f), accent))
        assertEquals(false, ButtonDefaults.isAccentFill(Color.Red, accent))
    }

    @Test
    fun segmentAndFabUseTheirNativeLayers() {
        assertEquals(
            listOf(
                BlendColorEntry(Color(0x7F333333), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x99EFEFEF), BlurBlendMode.Luminosity),
            ),
            TabRowDefaults.segmentBlurColors(isDark = false),
        )
        assertEquals(
            listOf(
                BlendColorEntry(accent.copy(alpha = 230 / 255f)),
                BlendColorEntry(Color(0x19FFFFFF), BlurBlendMode.Overlay),
            ),
            FloatingActionButtonDefaults.fabBlurColors(accent, isDark = true),
        )
    }
}
