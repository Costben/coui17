// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.Colors
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/** WCAG 2 contrast ratio of [foreground] drawn on [background], both composited over [base]. */
private fun contrast(foreground: Color, background: Color, base: Color): Float {
    val back = background.compositeOver(base)
    val front = foreground.compositeOver(back)
    val a = front.luminance() + 0.05f
    val b = back.luminance() + 0.05f
    return max(a, b) / min(a, b)
}

/** The minimum WCAG ratio for large or bold text; a readable label must clear it comfortably. */
private const val MIN_CONTRAST = 3f

@OptIn(ExperimentalTestApi::class)
class LabelContrastTest {
    private val schemes = listOf("light" to lightColorScheme(), "dark" to darkColorScheme())

    private fun forEachScheme(block: (name: String, scheme: Colors, container: Color, content: Color) -> Unit) = schemes.forEach { (name, scheme) ->
        var container = Color.Unspecified
        var content = Color.Unspecified
        runSkikoComposeUiTest(size = Size(10f, 10f), density = Density(1f)) {
            setContent {
                COUITheme(colors = scheme) {
                    container = TooltipDefaults.plainTooltipContainerColor
                    content = TooltipDefaults.plainTooltipContentColor
                }
            }
            waitForIdle()
        }
        block(name, scheme, container, content)
    }

    @Test
    fun plainTooltipLabelIsReadableOnItsBubble() = forEachScheme { name, scheme, container, content ->
        val ratio = contrast(content, container, scheme.surface)
        assertTrue(ratio >= MIN_CONTRAST, "$name plain tooltip contrast is $ratio")
    }

    @Test
    fun bigRoundLabelIsReadableOnItsRestingFill() = schemes.forEach { (name, scheme) ->
        val colors = InstallLoadProgressColors(
            primaryColor = scheme.primary,
            secondaryColor = scheme.tertiaryContainer,
            textColor = Color.White,
            surpassProgressColor = Color.White,
            disabledColor = scheme.disabledSecondaryVariant,
            textPadding = 0.dp,
        )
        val state = LoadProgressState.Default
        val style = InstallLoadStyle.BigRound
        val fill = InstallLoadProgressDefaults.diskColor(state, style, colors.primaryColor, colors.secondaryColor)
        val label = InstallLoadProgressDefaults.labelColor(state, style, colors)
        val ratio = contrast(label, fill, scheme.surface)
        assertTrue(ratio >= MIN_CONTRAST, "$name big round label contrast is $ratio")
    }
}
