// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The row of a checkbox preference is one click target that toggles the box by default, and two
 * targets once `onMainLayoutClick` is given — `COUICheckBoxWithDividerPreference`'s
 * `main_layout` / `check_box_layout` split.
 */
@OptIn(ExperimentalTestApi::class)
class CheckboxPreferenceClickTest {

    @Test
    fun theRowTogglesTheBoxByDefault() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var changes = 0
        setContent {
            COUITheme {
                CheckboxPreference(
                    title = "Wi-Fi",
                    checked = false,
                    onCheckedChange = { changes++ },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        waitForIdle()

        onNodeWithText("Wi-Fi").performClick()

        assertEquals(1, changes, "the row click must toggle the box")
    }

    @Test
    fun aMainLayoutCallbackTakesTheRowClickAndLeavesTheBoxToggleable() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var changes = 0
        var mainClicks = 0
        setContent {
            COUITheme {
                CheckboxPreference(
                    title = "Wi-Fi",
                    checked = false,
                    onCheckedChange = { changes++ },
                    modifier = Modifier.fillMaxWidth(),
                    onMainLayoutClick = { mainClicks++ },
                )
            }
        }
        waitForIdle()

        onNodeWithText("Wi-Fi").performClick()
        assertEquals(1, mainClicks, "the main area reports to onMainLayoutClick")
        assertEquals(0, changes, "the main area must not toggle the box")

        onNode(isToggleable()).performClick()
        assertEquals(1, changes, "the box keeps its own toggle target")
    }
}
