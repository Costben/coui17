// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.cardListPositionOf
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.ButtonPreference
import io.github.suqi8.coui.kmp.preference.CheckBoxWithDividerPreference
import io.github.suqi8.coui.kmp.preference.CheckboxPreference
import io.github.suqi8.coui.kmp.preference.MarkPreference
import io.github.suqi8.coui.kmp.preference.MarkWithDividerPreference
import io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference
import io.github.suqi8.coui.kmp.preference.RadioButtonPreference
import io.github.suqi8.coui.kmp.preference.SliderPreference
import io.github.suqi8.coui.kmp.preference.StatusSwitchingContent
import io.github.suqi8.coui.kmp.preference.StatusSwitchingPreference
import io.github.suqi8.coui.kmp.preference.StepperPreference
import io.github.suqi8.coui.kmp.preference.SwitchLoadingPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.preference.SwitchWithDividerPreference
import io.github.suqi8.coui.kmp.theme.COUITheme

private val RowTypes = listOf(
    "BasicComponent",
    "ArrowPreference",
    "SwitchPreference",
    "SwitchLoadingPreference",
    "SwitchWithDividerPreference",
    "StatusSwitchingPreference",
    "CheckboxPreference",
    "CheckBoxWithDividerPreference",
    "RadioButtonPreference",
    "MarkPreference",
    "MarkWithDividerPreference",
    "ButtonPreference",
    "StepperPreference",
    "OverlayDropdownPreference",
    "SliderPreference",
)

private val ItemRowCountOptions = listOf(
    "1 row",
    "2 rows",
    "3 rows",
)

private val LineCountOptions = listOf(
    "One line",
    "Two lines",
    "Three or more lines",
)

private const val LONG_MULTI_LINE_SUMMARY =
    "This summary is long on purpose so it wraps onto a second and a third line on any phone " +
        "screen, letting you verify that the leading icon stays anchored to the centre of the " +
        "first two lines"

/**
 * Interactive multi-row showcase for leading icon alignment across one, two, and three-or-more
 * text lines: COUI anchors the icon on the centre of the first two lines when a summary wraps to
 * three or more lines (`setIconTopInParent`), and grouped rows share matching style/parameters
 * with proper [cardListPositionOf] dividers between them.
 */
@Composable
fun MultiLineIconRowsDemo() {
    val rowType = remember { mutableIntStateOf(0) }
    val itemRowCount = remember { mutableIntStateOf(1) }
    val lineCount = remember { mutableIntStateOf(2) }
    val showIcon = remember { mutableStateOf(true) }
    val enabled = remember { mutableStateOf(true) }

    val switchChecked = remember { mutableStateListOf(true, true, true) }
    val checkChecked = remember { mutableStateListOf(true, true, true) }
    var radioSelectedIndex by remember { mutableIntStateOf(0) }
    val markChecked = remember { mutableStateListOf(true, true, true) }
    val stepperValues = remember { mutableStateListOf(3, 3, 3) }
    val sliderValues = remember { mutableStateListOf(0.4f, 0.4f, 0.4f) }
    val dropdownIndices = remember { mutableStateListOf(0, 0, 0) }

    DemoOptions(
        DemoChoice("Row type", RowTypes, rowType),
        DemoChoice("Item rows", ItemRowCountOptions, itemRowCount),
        DemoChoice("Line count", LineCountOptions, lineCount),
        DemoToggle("Show leading icon", showIcon),
        DemoToggle("Enabled", enabled),
    )

    val rowCount = itemRowCount.intValue + 1
    if (radioSelectedIndex >= rowCount) {
        radioSelectedIndex = 0
    }
    val summary = when (lineCount.intValue) {
        0 -> null
        1 -> str("Short summary")
        else -> str(LONG_MULTI_LINE_SUMMARY)
    }
    val startIcon: (@Composable () -> Unit)? = if (showIcon.value) {
        {
            Icon(
                imageVector = COUIIcons.Basic.Search,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled.value) {
                    COUITheme.colorScheme.onBackground
                } else {
                    COUITheme.colorScheme.disabledOnSecondaryVariant
                },
            )
        }
    } else {
        null
    }

    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        val label = str(RowTypes[rowType.intValue])
        val dropdownItems = listOf(str("Option 1"), str("Option 2"), str("Option 3"))
        for (index in 0 until rowCount) {
            val position = cardListPositionOf(index, rowCount)
            when (rowType.intValue) {
                0 -> BasicComponent(
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                1 -> ArrowPreference(
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    onClick = {},
                    cardListPosition = position,
                )

                2 -> SwitchPreference(
                    checked = switchChecked[index],
                    onCheckedChange = { switchChecked[index] = it },
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                3 -> SwitchLoadingPreference(
                    checked = switchChecked[index],
                    onCheckedChange = { switchChecked[index] = it },
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                4 -> SwitchWithDividerPreference(
                    checked = switchChecked[index],
                    onCheckedChange = { switchChecked[index] = it },
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                5 -> StatusSwitchingPreference(
                    title = label,
                    status = StatusSwitchingContent.Text(str("On")),
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    onClick = {},
                    cardListPosition = position,
                )

                6 -> CheckboxPreference(
                    title = label,
                    checked = checkChecked[index],
                    onCheckedChange = { checkChecked[index] = it },
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                7 -> CheckBoxWithDividerPreference(
                    title = label,
                    checked = checkChecked[index],
                    onCheckedChange = { checkChecked[index] = it },
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                8 -> RadioButtonPreference(
                    title = label,
                    selected = radioSelectedIndex == index,
                    onClick = {
                        radioSelectedIndex = if (rowCount == 1 && radioSelectedIndex == index) -1 else index
                    },
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                9 -> MarkPreference(
                    title = label,
                    checked = markChecked[index],
                    onClick = { markChecked[index] = !markChecked[index] },
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                10 -> MarkWithDividerPreference(
                    title = label,
                    checked = markChecked[index],
                    onClick = { markChecked[index] = !markChecked[index] },
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                11 -> ButtonPreference(
                    title = label,
                    buttonText = str("Open"),
                    onButtonClick = {},
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                12 -> StepperPreference(
                    value = stepperValues[index],
                    onValueChange = { stepperValues[index] = it },
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )

                13 -> OverlayDropdownPreference(
                    items = dropdownItems,
                    selectedIndex = dropdownIndices[index],
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    onSelectedIndexChange = { dropdownIndices[index] = it },
                    cardListPosition = position,
                )

                else -> SliderPreference(
                    value = sliderValues[index],
                    onValueChange = { sliderValues[index] = it },
                    title = label,
                    summary = summary,
                    startAction = startIcon,
                    enabled = enabled.value,
                    cardListPosition = position,
                )
            }
        }
    }
}
