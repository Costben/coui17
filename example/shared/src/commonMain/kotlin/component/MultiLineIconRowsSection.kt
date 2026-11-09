// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.SmallTitle
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

/**
 * One row of every kind with a leading icon and a summary long enough to wrap, so the text block is
 * three or more lines tall. COUI anchors the icon on the centre of the first two lines in that case
 * (`setIconTopInParent`), never above the text; a single page with every row type makes a placement
 * regression easy to spot. The first card keeps a one-line and a two-line row for reference.
 */
@Composable
fun MultiLineIconRowsDemo() {
    val summary = str(
        "This summary is long on purpose, so it wraps onto a second and a third line and the " +
            "leading icon has to follow the first two lines",
    )
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = COUIIcons.Basic.Search,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
    var switchA by remember { mutableStateOf(true) }
    var switchB by remember { mutableStateOf(false) }
    var switchC by remember { mutableStateOf(true) }
    var checkA by remember { mutableStateOf(true) }
    var checkB by remember { mutableStateOf(false) }
    var radio by remember { mutableIntStateOf(0) }
    var markA by remember { mutableStateOf(true) }
    var markB by remember { mutableStateOf(false) }
    var stepper by remember { mutableIntStateOf(3) }
    var slider by remember { mutableFloatStateOf(0.4f) }
    var dropdown by remember { mutableIntStateOf(0) }

    Column {
        SmallTitle(str("Reference: one and two lines"))
        Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
            BasicComponent(
                title = str("One line"),
                startAction = icon,
                cardListPosition = CardListPosition.Head,
            )
            BasicComponent(
                title = str("Two lines"),
                summary = str("Short summary"),
                startAction = icon,
                cardListPosition = CardListPosition.Tail,
            )
        }

        SmallTitle(str("Three or more lines"))
        Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
            BasicComponent(
                title = str("BasicComponent"),
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Head,
            )
            ArrowPreference(
                title = str("ArrowPreference"),
                summary = summary,
                startAction = icon,
                onClick = {},
                cardListPosition = CardListPosition.Middle,
            )
            SwitchPreference(
                checked = switchA,
                onCheckedChange = { switchA = it },
                title = str("SwitchPreference"),
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Middle,
            )
            SwitchLoadingPreference(
                checked = switchB,
                onCheckedChange = { switchB = it },
                title = str("SwitchLoadingPreference"),
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Middle,
            )
            SwitchWithDividerPreference(
                checked = switchC,
                onCheckedChange = { switchC = it },
                title = str("SwitchWithDividerPreference"),
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Middle,
            )
            StatusSwitchingPreference(
                title = str("StatusSwitchingPreference"),
                status = StatusSwitchingContent.Text(str("On")),
                summary = summary,
                startAction = icon,
                onClick = {},
                cardListPosition = CardListPosition.Tail,
            )
        }

        Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
            CheckboxPreference(
                title = str("CheckboxPreference"),
                checked = checkA,
                onCheckedChange = { checkA = it },
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Head,
            )
            CheckBoxWithDividerPreference(
                title = str("CheckBoxWithDividerPreference"),
                checked = checkB,
                onCheckedChange = { checkB = it },
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Middle,
            )
            RadioButtonPreference(
                title = str("RadioButtonPreference"),
                selected = radio == 0,
                onClick = { radio = 0 },
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Middle,
            )
            MarkPreference(
                title = str("MarkPreference"),
                checked = markA,
                onClick = { markA = !markA },
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Middle,
            )
            MarkWithDividerPreference(
                title = str("MarkWithDividerPreference"),
                checked = markB,
                onClick = { markB = !markB },
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Tail,
            )
        }

        Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            ButtonPreference(
                title = str("ButtonPreference"),
                buttonText = str("Open"),
                onButtonClick = {},
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Head,
            )
            StepperPreference(
                value = stepper,
                onValueChange = { stepper = it },
                title = str("StepperPreference"),
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Middle,
            )
            OverlayDropdownPreference(
                items = listOf(str("Option 1"), str("Option 2"), str("Option 3")),
                selectedIndex = dropdown,
                title = str("OverlayDropdownPreference"),
                summary = summary,
                startAction = icon,
                onSelectedIndexChange = { dropdown = it },
                cardListPosition = CardListPosition.Middle,
            )
            SliderPreference(
                value = slider,
                onValueChange = { slider = it },
                title = str("SliderPreference"),
                summary = summary,
                startAction = icon,
                cardListPosition = CardListPosition.Tail,
            )
        }
    }
}
