// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

/**
 * The alert-dialog button bar geometry, pinned to the ColorOS 17 measurements.
 *
 * A dialog panel is 392dp wide (`coui_dialog_max_width`), so at density 1 the numbers below are
 * directly comparable with a screenshot of the real dialog: the capsules are 44dp tall, 16dp in
 * from the panel sides, 4dp below the content above them, 16dp above the panel's bottom edge, and
 * 8dp apart — a 64dp-tall bar, not a row of buttons that fills it.
 *
 * That 16/8 pair is `COUIButtonBarLayout`'s cell padding: 16dp from the panel
 * (`coui_alert_dialog_button_parent_padding_horizontal`) and 4dp from each other
 * (`..._horizontal_inside`), and the capsule fills its cell edge to edge — measured on the device,
 * the capsule's left edge sits 16.5dp from the panel's side and the two capsules are 8.6dp apart.
 */
@OptIn(ExperimentalTestApi::class)
class DialogButtonBarGeometryTest {
    private val panelWidth = 392.dp

    @Test
    fun twoButtonsSitInOneRowOfInsetCapsules() = runSkikoComposeUiTest(
        size = Size(392f, 200f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Box(modifier = Modifier.width(panelWidth)) {
                    DialogButtonBar(
                        negative = DialogButtonBarAction(text = "取消", onClick = {}),
                        positive = DialogButtonBarAction(text = "删除", onClick = {}),
                        modifier = Modifier.testTag("bar"),
                    )
                }
            }
        }

        val cancel = onNodeWithText("取消").getBoundsInRoot()
        val confirm = onNodeWithText("删除").getBoundsInRoot()

        // coui_alert_dialog_button_height = 44dp, parent_padding_vertical = 4dp.
        assertEquals(44.dp, cancel.height)
        assertEquals(44.dp, confirm.height)
        assertEquals(4.dp, cancel.top)

        // coui_alert_dialog_button_parent_padding_horizontal = 16dp on the outer edge of each cell.
        assertEquals(16.dp, cancel.left)
        assertEquals(panelWidth - 16.dp, confirm.right)

        // The two 4dp inner insets meet at the panel centre line, leaving the capsules 8dp apart.
        assertEquals(8.dp, confirm.left - cancel.right)
        assertEquals(panelWidth / 2 - 4.dp, cancel.right)
        assertEquals(panelWidth / 2 + 4.dp, confirm.left)

        // parent_padding_bottom = 16dp: the bar is 4 + 44 + 16 = 64dp tall, so the capsules clear
        // the panel's bottom corner instead of running over it.
        assertEquals(64.dp, cancel.bottom + 16.dp)
        assertEquals(64.dp, onNodeWithTag("bar").getBoundsInRoot().height)
    }

    @Test
    fun threeButtonsStackAsFullWidthCapsules() = runSkikoComposeUiTest(
        size = Size(392f, 300f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Box(modifier = Modifier.width(panelWidth)) {
                    DialogButtonBar(
                        negative = DialogButtonBarAction(text = "取消", onClick = {}),
                        positive = DialogButtonBarAction(text = "确定", onClick = {}),
                        neutral = DialogButtonBarAction(text = "稍后", onClick = {}),
                        modifier = Modifier.testTag("bar"),
                    )
                }
            }
        }

        val neutral = onNodeWithText("稍后").getBoundsInRoot()
        val positive = onNodeWithText("确定").getBoundsInRoot()
        val negative = onNodeWithText("取消").getBoundsInRoot()

        // Equal capsules, no dividers: the same 44dp capsule turned 90 degrees, inset 16dp a side.
        assertEquals(panelWidth - 32.dp, neutral.width)
        assertEquals(44.dp, neutral.height)
        assertEquals(44.dp, positive.height)
        assertEquals(44.dp, negative.height)
        assertEquals(16.dp, neutral.left)

        // resortButton puts neutral first, then positive, then negative. `setVerButtonsParentPadding`
        // gives every non-bottom cell 4dp above and below, so two capsules end up 8dp apart.
        assertEquals(8.dp, positive.top - neutral.bottom)
        assertEquals(8.dp, negative.top - positive.bottom)
        assertTrue(neutral.top < positive.top && positive.top < negative.top)

        // 4dp above + three capsules + two 8dp gaps + 16dp below.
        assertEquals(4.dp + 44.dp * 3 + 8.dp * 2 + 16.dp, onNodeWithTag("bar").getBoundsInRoot().height)
    }

    @Test
    fun aLabelWiderThanItsCellStacksTheBar() = runSkikoComposeUiTest(
        size = Size(392f, 200f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Box(modifier = Modifier.width(panelWidth)) {
                    DialogButtonBar(
                        negative = DialogButtonBarAction(text = "取消", onClick = {}),
                        positive = DialogButtonBarAction(
                            text = "把这条非常非常长的记录彻底删除掉",
                            onClick = {},
                        ),
                        modifier = Modifier.testTag("bar"),
                    )
                }
            }
        }

        val confirm = onNodeWithText("把这条非常非常长的记录彻底删除掉").getBoundsInRoot()
        // needSetButVertical: a label that does not fit getHorizontalButtonContentWidth stacks the
        // bar, and a stacked capsule spans the panel width minus the two 16dp side insets.
        assertEquals(panelWidth - 32.dp, confirm.width)
        // In the stack the label fits on one line, so the capsule keeps the single-line height.
        assertEquals(44.dp, confirm.height)
    }

    @Test
    fun aStackedLabelThatWrapsGrowsItsCapsule() = runSkikoComposeUiTest(
        size = Size(392f, 200f),
        density = Density(1f),
    ) {
        val wrappingLabel = "把这条非常非常长的记录以及它的全部本地副本一起彻底删除掉"
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Box(modifier = Modifier.width(panelWidth)) {
                    DialogButtonBar(
                        negative = DialogButtonBarAction(text = "取消", onClick = {}),
                        positive = DialogButtonBarAction(text = wrappingLabel, onClick = {}),
                        modifier = Modifier.testTag("bar"),
                    )
                }
            }
        }

        val confirm = onNodeWithText(wrappingLabel).getBoundsInRoot()
        // coui_alert_dialog_button_min_height_multiline = 56dp: the stacked capsule grows instead of
        // clipping the second line.
        assertEquals(56.dp, confirm.height)
        assertEquals(panelWidth - 32.dp, confirm.width)
    }
}
