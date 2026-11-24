// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.DropdownDefaults
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.preference.ListPreferenceDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DropdownChoiceDialogGroupTest {
    @Test
    fun groupedDialogEntriesUseUniformRowStepWithoutArtificialBand() = runSkikoComposeUiTest(
        size = Size(360f, 600f),
        density = Density(1f),
    ) {
        val entries = listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(text = "Alpha 1", selected = true),
                    DropdownItem(text = "Alpha 2"),
                ),
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(text = "Beta 1", selected = true),
                    DropdownItem(text = "Beta 2"),
                ),
            ),
        )
        setContent {
            COUITheme {
                DropdownChoiceDialogList(
                    entries = entries,
                    show = true,
                    freezeSelection = false,
                    dropdownColors = DropdownDefaults.dialogDropdownColors(),
                    onItemClick = { _, _ -> },
                )
            }
        }
        val a1 = onNodeWithText("Alpha 1").getBoundsInRoot()
        val a2 = onNodeWithText("Alpha 2").getBoundsInRoot()
        val b1 = onNodeWithText("Beta 1").getBoundsInRoot()

        val intraGroupStep = a2.top - a1.top
        val crossGroupStep = b1.top - a2.top
        assertEquals(
            intraGroupStep.value,
            crossGroupStep.value,
            1f,
            "Choice dialog rows must flow uniformly without an 8dp section band ($intraGroupStep vs $crossGroupStep)",
        )
    }

    @Test
    fun titledGroupHeaderAlignsWithDialogRowStartInset() = runSkikoComposeUiTest(
        size = Size(360f, 600f),
        density = Density(1f),
    ) {
        val entries = listOf(
            DropdownEntry(
                title = "Section A",
                items = listOf(DropdownItem(text = "Item 1", selected = true)),
            ),
        )
        setContent {
            COUITheme {
                DropdownChoiceDialogList(
                    entries = entries,
                    show = true,
                    freezeSelection = false,
                    dropdownColors = DropdownDefaults.dialogDropdownColors(),
                    onItemClick = { _, _ -> },
                )
            }
        }
        val header = onNodeWithText("Section A").getBoundsInRoot()
        val row = onNodeWithText("Item 1", useUnmergedTree = true).getBoundsInRoot()
        assertEquals(ListPreferenceDefaults.PanelItemHorizontalPadding.value, header.left.value, 1f)
        assertEquals(row.left.value, header.left.value, 1f)
    }

    @Test
    fun choiceDialogDividerExtendsUnderLeadingIconAt24dpInset() = runSkikoComposeUiTest(
        size = Size(360f, 300f),
        density = Density(1f),
    ) {
        val icon: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier.size(24.dp).background(Color.Red))
        }
        val entries = listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(text = "Option 1", icon = icon, selected = true),
                    DropdownItem(text = "Option 2", icon = icon),
                ),
            ),
        )
        setContent {
            COUITheme {
                Box(Modifier.background(Color.White)) {
                    DropdownChoiceDialogList(
                        entries = entries,
                        show = true,
                        freezeSelection = false,
                        dropdownColors = DropdownDefaults.dialogDropdownColors(),
                        onItemClick = { _, _ -> },
                    )
                }
            }
        }
        waitForIdle()

        val row1 = onNodeWithText("Option 1").getBoundsInRoot()
        val row2 = onNodeWithText("Option 2").getBoundsInRoot()
        val image = onRoot().captureToImage().toPixelMap()
        // Scan between row 1 and row 2 for the horizontal divider line: it must start at 24dp
        // (under the 24dp icon slot, matching coui_select_dialog_singlechoice.xml's match_parent
        // item_divider inside 24dp row padding), not wait until after the 24dp icon + 16dp gap (64dp).
        val scanTop = row1.bottom.value.toInt()
        val scanBottom = row2.top.value.toInt()
        val dividerY = (scanTop..scanBottom).firstOrNull { y ->
            val p = image[180, y]
            abs(p.red - 1f) > 0.01f || abs(p.green - 1f) > 0.01f || abs(p.blue - 1f) > 0.01f
        }
        assertTrue(dividerY != null, "Expected a hairline divider between rows ($scanTop..$scanBottom)")
        val outsideMargin = image[10, dividerY]
        val underIcon = image[30, dividerY]
        assertEquals(Color.White, outsideMargin, "Divider must stay inset by 24dp from the dialog edge")
        assertTrue(
            underIcon != Color.White,
            "Divider must extend beneath the leading icon slot at x=30dp (got $underIcon)",
        )
    }
}
