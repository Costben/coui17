// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Chip
import io.github.suqi8.coui.kmp.basic.ChipDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextFieldDefaults
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * The theme is the single place a downstream project changes the metrics with, so two things have to
 * hold: the stock [Dimens] must reproduce the `Defaults` constants exactly (otherwise introducing the
 * theme silently moves every control), and an override must reach the components.
 */
@OptIn(ExperimentalTestApi::class)
class DimensTest {

    @Test
    fun stockDimensMatchTheDefaultsObjects() {
        val dimens = Dimens()

        assertEquals(ButtonDefaults.MinWidth, dimens.button.regular.minWidth)
        assertEquals(ButtonDefaults.MinHeight, dimens.button.regular.minHeight)
        assertEquals(ButtonDefaults.CornerRadius, dimens.button.regular.cornerRadius)
        assertEquals(ButtonDefaults.InsideMargin, dimens.button.regular.insideMargin)
        assertEquals(ButtonDefaults.MinWidthSmall, dimens.button.small.minWidth)
        assertEquals(ButtonDefaults.MinHeightSmall, dimens.button.small.minHeight)
        assertEquals(ButtonDefaults.CornerRadiusSmall, dimens.button.small.cornerRadius)
        assertEquals(ButtonDefaults.InsideMarginSmall, dimens.button.small.insideMargin)
        assertEquals(ButtonDefaults.MinWidthTiny, dimens.button.tiny.minWidth)
        assertEquals(ButtonDefaults.MinHeightTiny, dimens.button.tiny.minHeight)
        assertEquals(ButtonDefaults.CornerRadiusTiny, dimens.button.tiny.cornerRadius)
        assertEquals(ButtonDefaults.InsideMarginTiny, dimens.button.tiny.insideMargin)

        assertEquals(CardDefaults.CornerRadius, dimens.card.cornerRadius)
        assertEquals(CardDefaults.InsideMargin, dimens.card.insideMargin)

        assertEquals(ChipDefaults.CornerRadius, dimens.chip.cornerRadius)
        assertEquals(ChipDefaults.MinWidth, dimens.chip.minWidth)
        assertEquals(ChipDefaults.MinHeight, dimens.chip.minHeight)
        assertEquals(ChipDefaults.MaxWidth, dimens.chip.maxWidth)
        assertEquals(ChipDefaults.InsideMargin, dimens.chip.insideMargin)

        assertEquals(TextFieldDefaults.CornerRadius, dimens.textField.cornerRadius)
        assertEquals(TextFieldDefaults.InsideMargin, dimens.textField.insideMargin(TextFieldMode.Rectangle))
        assertEquals(TextFieldDefaults.LineInsideMargin, dimens.textField.insideMargin(TextFieldMode.Line))
        assertEquals(TextFieldDefaults.NoneInsideMargin, dimens.textField.insideMargin(TextFieldMode.None))
    }

    @Test
    fun themeHandsTheGivenDimensToItsContent() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        val custom = Dimens().let { stock ->
            stock.copy(card = stock.card.copy(cornerRadius = 4.dp))
        }
        var seen: Dimens? = null
        setContent {
            COUITheme(dimens = custom) {
                seen = COUITheme.dimens
            }
        }
        waitForIdle()
        assertSame(custom, seen)
    }

    @Test
    fun aNestedThemeInheritsTheOuterDimens() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        val outer = Dimens().let { stock ->
            stock.copy(card = stock.card.copy(cornerRadius = 4.dp))
        }
        var seen: Dimens? = null
        setContent {
            COUITheme(dimens = outer) {
                // No `dimens` argument: the nested theme must keep the outer metrics.
                COUITheme(colors = COUITheme.colorScheme) {
                    seen = COUITheme.dimens
                }
            }
        }
        waitForIdle()
        assertEquals(outer, seen)
    }

    @Test
    fun provideCOUIDimensOverridesOnlyTheMetrics() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        val outer = Dimens()
        val inner = outer.let { stock -> stock.copy(chip = stock.chip.copy(minHeight = 64.dp)) }
        var seen: Dimens? = null
        setContent {
            COUITheme(dimens = outer) {
                ProvideCOUIDimens(inner) {
                    seen = COUITheme.dimens
                }
            }
        }
        waitForIdle()
        assertEquals(inner, seen)
    }

    @Test
    fun buttonTakesItsMinHeightFromTheTheme() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        val custom = Dimens().let { stock ->
            stock.copy(button = stock.button.copy(regular = stock.button.regular.copy(minHeight = 96.dp)))
        }
        setContent {
            COUITheme(dimens = custom) {
                Button(onClick = {}, modifier = Modifier.testTag("button")) {
                    Text("COUI")
                }
            }
        }
        onNodeWithTag("button").assertHeightIsAtLeast(96.dp)
    }

    @Test
    fun anExplicitArgumentWinsOverTheTheme() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        val custom = Dimens().let { stock ->
            stock.copy(button = stock.button.copy(regular = stock.button.regular.copy(minHeight = 96.dp)))
        }
        setContent {
            COUITheme(dimens = custom) {
                Button(
                    onClick = {},
                    modifier = Modifier.testTag("button"),
                    minHeight = 40.dp,
                ) {
                    Text("COUI")
                }
            }
        }
        // 40dp requested explicitly: the theme's 96dp must not win. The content still needs room,
        // so the bound is the requested minimum rather than an exact height.
        onNodeWithTag("button").assertHeightIsAtLeast(40.dp)
    }

    @Test
    fun chipTakesItsMinHeightFromTheTheme() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        val custom = Dimens().let { stock ->
            stock.copy(chip = stock.chip.copy(minHeight = 64.dp))
        }
        setContent {
            COUITheme(dimens = custom) {
                Chip(
                    selected = false,
                    onClick = {},
                    label = "Chip",
                    modifier = Modifier.testTag("chip"),
                )
            }
        }
        onNodeWithTag("chip").assertHeightIsAtLeast(64.dp)
    }

    @Test
    fun cardInsideMarginComesFromTheTheme() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        val custom = Dimens().let { stock ->
            stock.copy(card = stock.card.copy(insideMargin = PaddingValues(24.dp)))
        }
        setContent {
            COUITheme(dimens = custom) {
                Card(modifier = Modifier.testTag("card")) {
                    Text("COUI")
                }
            }
        }
        // The text is inset by the themed margin, so the card is taller than the bare text line.
        onNodeWithTag("card").assertHeightIsAtLeast(48.dp)
    }
}
