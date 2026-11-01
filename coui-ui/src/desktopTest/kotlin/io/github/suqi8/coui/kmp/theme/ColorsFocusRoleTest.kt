// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorsFocusRoleTest {
    @Test
    fun containerThemeIsTheDefaultThemeContainerBlue() {
        // Theme.COUI couiColorContainerTheme = coui_color_container_theme_blue #0080ff / #1a8cff.
        assertEquals(Color(0xFF0080FF), lightColorScheme().containerTheme)
        assertEquals(Color(0xFF1A8CFF), darkColorScheme().containerTheme)
    }

    @Test
    fun focusRolesFollowTheDefaultThemeInsteadOfTheBlueOverlay() {
        // Theme.COUI couiColorFocus #1f0080ff, couiColorFocusOutline #660080ff.
        val light = lightColorScheme()
        assertEquals(Color(0x1F0080FF), light.focus)
        assertEquals(Color(0x660080FF), light.focusOutline)

        // Theme.COUI.Dark couiColorFocus #331a8cff, couiColorFocusOutline #665c9dff.
        val dark = darkColorScheme()
        assertEquals(Color(0x331A8CFF), dark.focus)
        assertEquals(Color(0x665C9DFF), dark.focusOutline)
    }

    @Test
    fun focusRolesStayIndependentOfContainerTheme() {
        val light = lightColorScheme(containerTheme = Color(0xFF7B1FA2))
        assertEquals(Color(0x1F0080FF), light.focus)
        assertEquals(Color(0x660080FF), light.focusOutline)
    }
}
