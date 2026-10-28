// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The dialog capsule is `TYPE_CONTENT_SECONDARY_BUTTON`: a white layer over the panel whose
 * strength depends on the theme. The light value in dark theme paints a (185,185,185) capsule over
 * a (43,43,43) panel, against the (86,86,86) a ColorOS 17 dark dialog shows, and the white label
 * on it all but vanishes.
 */
@OptIn(ExperimentalTestApi::class)
class DialogButtonFillTest {

    private fun fillsFor(dark: Boolean): Pair<Color, Color> {
        var captured: Pair<Color, Color>? = null
        runSkikoComposeUiTest(size = Size(320f, 80f)) {
            setContent {
                COUITheme(colors = if (dark) darkColorScheme() else lightColorScheme()) {
                    captured = DialogButtonBarDefaults.buttonFill() to DialogButtonBarDefaults.disabledButtonFill()
                }
            }
            waitForIdle()
        }
        return requireNotNull(captured)
    }

    @Test
    fun lightThemeKeepsTheMeasuredWhiteLayer() {
        val (fill, disabled) = fillsFor(dark = false)
        assertEquals(Color(0xAAFFFFFF), fill)
        // coui_color_container8_light
        assertEquals(Color(0x14000000), disabled)
    }

    @Test
    fun darkThemeUsesTheDarkSecondaryButtonRecipe() {
        val (fill, disabled) = fillsFor(dark = true)
        // CONTENT_SECONDARY_BUTTON_PARAMS_DARK: 25px blur + #33FFFFFF SRC_OVER.
        assertEquals(Color(0x33FFFFFF), fill)
        // coui_color_container8_dark
        assertEquals(Color(0x1AFFFFFF), disabled)
    }
}
