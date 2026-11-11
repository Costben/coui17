// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import i18n.LocalAppStrings
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.preference.OverlaySpinnerPreference
import io.github.suqi8.coui.kmp.preference.WindowSpinnerPreference
import io.github.suqi8.coui.kmp.squircle.addSquircleRect

/**
 * Renders the "Spinner" gallery section content: one spinner preference whose host, grouping, dialog
 * presentation and enabled state come from the options.
 */
@Composable
fun SpinnerDemo() {
    val enabled = remember { mutableStateOf(true) }
    val windowHost = remember { mutableStateOf(false) }
    val grouped = remember { mutableStateOf(false) }
    val asDialog = remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var group1Selected by remember { mutableIntStateOf(0) }
    var group2Selected by remember { mutableIntStateOf(0) }
    var group3Selected by remember { mutableIntStateOf(0) }
    val s = LocalAppStrings.current
    val spinnerOptions = remember(s) {
        listOf(
            DropdownItem(
                icon = {
                    Icon(
                        RoundedRectanglePainter(),
                        s["Icon"],
                        Modifier.padding(end = 12.dp),
                        Color(0xFFFF5B29),
                    )
                },
                text = s["Option 1"],
                summary = s["Red"],
            ),
            DropdownItem(
                icon = {
                    Icon(
                        RoundedRectanglePainter(),
                        s["Icon"],
                        Modifier.padding(end = 12.dp),
                        Color(0xFF36D167),
                    )
                },
                text = s["Option 2"],
                summary = s["Green"],
            ),
            DropdownItem(
                icon = {
                    Icon(
                        RoundedRectanglePainter(),
                        s["Icon"],
                        Modifier.padding(end = 12.dp),
                        Color(0xFF3482FF),
                    )
                },
                text = s["Option 3"],
                summary = s["Blue"],
            ),
            DropdownItem(
                icon = {
                    Icon(
                        RoundedRectanglePainter(),
                        s["Icon"],
                        Modifier.padding(end = 12.dp),
                        Color(0xFFFFB21D),
                    )
                },
                text = s["Option 4"],
                summary = s["Yellow"],
            ),
        )
    }
    val groupedOptions = remember(spinnerOptions, group1Selected, group2Selected, group3Selected) {
        groupedSpinnerOptions(
            spinnerOptions = spinnerOptions,
            group1SelectedIndex = group1Selected,
            onGroup1SelectedIndexChange = { group1Selected = it },
            group2SelectedIndex = group2Selected,
            onGroup2SelectedIndexChange = { group2Selected = it },
            group3SelectedIndex = group3Selected,
            onGroup3SelectedIndexChange = { group3Selected = it },
        )
    }

    DemoOptions(
        DemoToggle("Enabled", enabled),
        DemoToggle("Window host", windowHost),
        DemoToggle("Grouped options", grouped),
        DemoToggle("Show as dialog", asDialog),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        val title = str("SpinnerPreference")
        val summary = if (expanded) str("Expanded") else str("Collapsed")
        // The dialog presentation is a separate overload that takes the button label.
        when {
            windowHost.value && grouped.value && asDialog.value -> WindowSpinnerPreference(
                title = title,
                summary = summary,
                dialogButtonString = str("OK"),
                entries = groupedOptions,
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            windowHost.value && grouped.value -> WindowSpinnerPreference(
                title = title,
                summary = summary,
                entries = groupedOptions,
                collapseOnSelection = false,
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            windowHost.value && asDialog.value -> WindowSpinnerPreference(
                title = title,
                summary = summary,
                dialogButtonString = str("OK"),
                items = spinnerOptions,
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            windowHost.value -> WindowSpinnerPreference(
                title = title,
                summary = summary,
                items = spinnerOptions,
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            grouped.value && asDialog.value -> OverlaySpinnerPreference(
                title = title,
                summary = summary,
                dialogButtonString = str("OK"),
                entries = groupedOptions,
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            grouped.value -> OverlaySpinnerPreference(
                title = title,
                summary = summary,
                entries = groupedOptions,
                collapseOnSelection = false,
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            asDialog.value -> OverlaySpinnerPreference(
                title = title,
                summary = summary,
                dialogButtonString = str("OK"),
                items = spinnerOptions,
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )

            else -> OverlaySpinnerPreference(
                title = title,
                summary = summary,
                items = spinnerOptions,
                selectedIndex = selected,
                onSelectedIndexChange = { selected = it },
                enabled = enabled.value,
                onExpandedChange = { expanded = it },
            )
        }
    }
}

private fun groupedSpinnerOptions(
    spinnerOptions: List<DropdownItem>,
    group1SelectedIndex: Int,
    onGroup1SelectedIndexChange: (Int) -> Unit,
    group2SelectedIndex: Int,
    onGroup2SelectedIndexChange: (Int) -> Unit,
    group3SelectedIndex: Int,
    onGroup3SelectedIndexChange: (Int) -> Unit,
): List<DropdownEntry> = listOf(
    DropdownEntry(
        items = spinnerOptions.take(2).mapIndexed { index, item ->
            item.copy(
                selected = group1SelectedIndex == index,
                onClick = {
                    onGroup1SelectedIndexChange(index)
                    item.onClick?.invoke()
                },
            )
        },
    ),
    DropdownEntry(
        items = spinnerOptions.drop(2).mapIndexed { index, item ->
            item.copy(
                selected = group2SelectedIndex == index,
                onClick = {
                    onGroup2SelectedIndexChange(index)
                    item.onClick?.invoke()
                },
            )
        },
    ),
    DropdownEntry(
        items = spinnerOptions.mapIndexed { index, item ->
            item.copy(
                enabled = index % 2 == 0,
                selected = group3SelectedIndex == index,
                onClick = {
                    onGroup3SelectedIndexChange(index)
                    item.onClick?.invoke()
                },
            )
        },
    ),
)

private class RoundedRectanglePainter(
    private val cornerRadius: Dp = 6.dp,
) : Painter() {
    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawPath(
            path = Path().apply { addSquircleRect(size.width, size.height, cornerRadius.toPx()) },
            color = Color.White,
        )
    }
}
