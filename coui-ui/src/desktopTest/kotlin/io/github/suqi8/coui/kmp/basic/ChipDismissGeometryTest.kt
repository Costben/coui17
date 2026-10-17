// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ChipDismissGeometryTest {
    @Test
    fun aDismissibleChipKeepsTheChipHeightAndCentresItsTouchTargetOnTheIcon() = runSkikoComposeUiTest(
        size = Size(400f, 200f),
        density = Density(1f),
    ) {
        var dismissed = false
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Column {
                    Chip(selected = false, onClick = {}, label = "Plain", modifier = Modifier.testTag("plain"))
                    Chip(
                        selected = false,
                        onClick = {},
                        label = "Dismissible",
                        modifier = Modifier.testTag("dismissible"),
                        onDismiss = { dismissed = true },
                    )
                }
            }
        }

        val plain = onNodeWithTag("plain").getBoundsInRoot()
        val chip = onNodeWithTag("dismissible").getBoundsInRoot()
        assertEquals(plain.height, chip.height)
        assertEquals(32.dp, chip.height)

        val touch = onNodeWithContentDescription(ChipDefaults.DISMISS_CONTENT_DESCRIPTION).getBoundsInRoot()
        assertEquals(28.dp, touch.width)
        assertEquals(28.dp, touch.height)
        // The icon sits closeIconEndPadding (9dp) from the end edge, so a 28dp target centred on the
        // 14dp icon ends 2dp inside the chip.
        assertEquals(chip.right - 2.dp, touch.right)
        assertTrue(touch.top >= chip.top && touch.bottom <= chip.bottom)

        // The 28dp target must not stretch the row, so the label stays on the chip's centre line
        // instead of sitting above an oversized close box.
        val label = onNodeWithText("Dismissible").getBoundsInRoot()
        val chipCentre = chip.top.value + chip.height.value / 2
        val labelCentre = label.top.value + label.height.value / 2
        assertEquals(chipCentre, labelCentre, 0.5f)

        onNodeWithContentDescription(ChipDefaults.DISMISS_CONTENT_DESCRIPTION).performClick()
        assertTrue(dismissed)
    }
}
