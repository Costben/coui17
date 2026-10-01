// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.preference.OverlaySpinnerPreference
import io.github.suqi8.coui.kmp.preference.WindowSpinnerPreference

fun LazyListScope.spinnerSection() {
    item(key = "spinner") {
        SmallTitle(text = "Spinner")
        SpinnerDemo()
    }
}

/** Renders the "Spinner" gallery section content (a card of overlay and window spinner preferences). */
@Composable
fun SpinnerDemo() {
    val superSpinnerOptionSelected = remember { mutableIntStateOf(0) }
    val windowSpinnerOptionSelected = remember { mutableIntStateOf(1) }
    val superSpinnerOptionSelectedDialog = remember { mutableIntStateOf(2) }
    val windowSpinnerOptionSelectedDialog = remember { mutableIntStateOf(3) }
    var overlayExpanded by remember { mutableStateOf(false) }
    var windowExpanded by remember { mutableStateOf(false) }
    var overlayDialogExpanded by remember { mutableStateOf(false) }
    var windowDialogExpanded by remember { mutableStateOf(false) }
    var overlayGroupedExpanded by remember { mutableStateOf(false) }
    var windowGroupedExpanded by remember { mutableStateOf(false) }
    var overlayGroupedDialogExpanded by remember { mutableStateOf(false) }
    var windowGroupedDialogExpanded by remember { mutableStateOf(false) }
    var overlayGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var overlayGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var overlayGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var windowGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var windowGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var windowGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var overlayDialogGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var overlayDialogGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var overlayDialogGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var windowDialogGroup1SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var windowDialogGroup2SpinnerOptionSelected by remember { mutableIntStateOf(0) }
    var windowDialogGroup3SpinnerOptionSelected by remember { mutableIntStateOf(0) }
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
    val overlayGroupedSpinnerOptions = remember(
        spinnerOptions,
        overlayGroup1SpinnerOptionSelected,
        overlayGroup2SpinnerOptionSelected,
        overlayGroup3SpinnerOptionSelected,
    ) {
        groupedSpinnerOptions(
            spinnerOptions = spinnerOptions,
            group1SelectedIndex = overlayGroup1SpinnerOptionSelected,
            onGroup1SelectedIndexChange = { overlayGroup1SpinnerOptionSelected = it },
            group2SelectedIndex = overlayGroup2SpinnerOptionSelected,
            onGroup2SelectedIndexChange = { overlayGroup2SpinnerOptionSelected = it },
            group3SelectedIndex = overlayGroup3SpinnerOptionSelected,
            onGroup3SelectedIndexChange = { overlayGroup3SpinnerOptionSelected = it },
        )
    }
    val windowGroupedSpinnerOptions = remember(
        spinnerOptions,
        windowGroup1SpinnerOptionSelected,
        windowGroup2SpinnerOptionSelected,
        windowGroup3SpinnerOptionSelected,
    ) {
        groupedSpinnerOptions(
            spinnerOptions = spinnerOptions,
            group1SelectedIndex = windowGroup1SpinnerOptionSelected,
            onGroup1SelectedIndexChange = { windowGroup1SpinnerOptionSelected = it },
            group2SelectedIndex = windowGroup2SpinnerOptionSelected,
            onGroup2SelectedIndexChange = { windowGroup2SpinnerOptionSelected = it },
            group3SelectedIndex = windowGroup3SpinnerOptionSelected,
            onGroup3SelectedIndexChange = { windowGroup3SpinnerOptionSelected = it },
        )
    }
    val overlayGroupedDialogSpinnerOptions = remember(
        spinnerOptions,
        overlayDialogGroup1SpinnerOptionSelected,
        overlayDialogGroup2SpinnerOptionSelected,
        overlayDialogGroup3SpinnerOptionSelected,
    ) {
        groupedSpinnerOptions(
            spinnerOptions = spinnerOptions,
            group1SelectedIndex = overlayDialogGroup1SpinnerOptionSelected,
            onGroup1SelectedIndexChange = { overlayDialogGroup1SpinnerOptionSelected = it },
            group2SelectedIndex = overlayDialogGroup2SpinnerOptionSelected,
            onGroup2SelectedIndexChange = { overlayDialogGroup2SpinnerOptionSelected = it },
            group3SelectedIndex = overlayDialogGroup3SpinnerOptionSelected,
            onGroup3SelectedIndexChange = { overlayDialogGroup3SpinnerOptionSelected = it },
        )
    }
    val windowGroupedDialogSpinnerOptions = remember(
        spinnerOptions,
        windowDialogGroup1SpinnerOptionSelected,
        windowDialogGroup2SpinnerOptionSelected,
        windowDialogGroup3SpinnerOptionSelected,
    ) {
        groupedSpinnerOptions(
            spinnerOptions = spinnerOptions,
            group1SelectedIndex = windowDialogGroup1SpinnerOptionSelected,
            onGroup1SelectedIndexChange = { windowDialogGroup1SpinnerOptionSelected = it },
            group2SelectedIndex = windowDialogGroup2SpinnerOptionSelected,
            onGroup2SelectedIndexChange = { windowDialogGroup2SpinnerOptionSelected = it },
            group3SelectedIndex = windowDialogGroup3SpinnerOptionSelected,
            onGroup3SelectedIndexChange = { windowDialogGroup3SpinnerOptionSelected = it },
        )
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        OverlaySpinnerPreference(
            title = str("SpinnerPref (O)"),
            summary = if (overlayExpanded) str("Expanded") else str("Collapsed"),
            items = spinnerOptions,
            selectedIndex = superSpinnerOptionSelected.value,
            onSelectedIndexChange = { newOption -> superSpinnerOptionSelected.value = newOption },
            onExpandedChange = { overlayExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        WindowSpinnerPreference(
            title = str("SpinnerPref (W)"),
            summary = if (windowExpanded) str("Expanded") else str("Collapsed"),
            items = spinnerOptions,
            selectedIndex = windowSpinnerOptionSelected.value,
            onSelectedIndexChange = { newOption -> windowSpinnerOptionSelected.value = newOption },
            onExpandedChange = { windowExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        OverlaySpinnerPreference(
            title = str("SpinnerPref (O)"),
            summary = str("As Dialog (O)") + if (overlayDialogExpanded) str(" (Expanded)") else str(" (Collapsed)"),
            dialogButtonString = str("OK"),
            items = spinnerOptions,
            selectedIndex = superSpinnerOptionSelectedDialog.value,
            onSelectedIndexChange = { newOption -> superSpinnerOptionSelectedDialog.value = newOption },
            onExpandedChange = { overlayDialogExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        WindowSpinnerPreference(
            title = str("SpinnerPref (W)"),
            summary = str("As Dialog (W)") + if (windowDialogExpanded) str(" (Expanded)") else str(" (Collapsed)"),
            dialogButtonString = str("OK"),
            items = spinnerOptions,
            selectedIndex = windowSpinnerOptionSelectedDialog.value,
            onSelectedIndexChange = { newOption -> windowSpinnerOptionSelectedDialog.value = newOption },
            onExpandedChange = { windowDialogExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        OverlaySpinnerPreference(
            title = str("Grouped SpinnerPref (O)"),
            summary = if (overlayGroupedExpanded) str("Expanded") else str("Collapsed"),
            entries = overlayGroupedSpinnerOptions,
            collapseOnSelection = false,
            onExpandedChange = { overlayGroupedExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        WindowSpinnerPreference(
            title = str("Grouped SpinnerPref (W)"),
            summary = if (windowGroupedExpanded) str("Expanded") else str("Collapsed"),
            entries = windowGroupedSpinnerOptions,
            collapseOnSelection = false,
            onExpandedChange = { windowGroupedExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        OverlaySpinnerPreference(
            title = str("Grouped SpinnerPref (O)"),
            summary = str("As Dialog (O)") + if (overlayGroupedDialogExpanded) str(" (Expanded)") else str(" (Collapsed)"),
            dialogButtonString = str("OK"),
            entries = overlayGroupedDialogSpinnerOptions,
            onExpandedChange = { overlayGroupedDialogExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        WindowSpinnerPreference(
            title = str("Grouped SpinnerPref (W)"),
            summary = str("As Dialog (W)") + if (windowGroupedDialogExpanded) str(" (Expanded)") else str(" (Collapsed)"),
            dialogButtonString = str("OK"),
            entries = windowGroupedDialogSpinnerOptions,
            onExpandedChange = { windowGroupedDialogExpanded = it },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        OverlaySpinnerPreference(
            title = str("Disabled SpinnerPref (O)"),
            summary = str("Collapsed"),
            items = listOf(DropdownItem(text = str("Option 5"))),
            selectedIndex = 0,
            onSelectedIndexChange = {},
            enabled = false,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        WindowSpinnerPreference(
            title = str("Disabled SpinnerPref (W)"),
            summary = str("Collapsed"),
            items = listOf(DropdownItem(text = str("Option 6"))),
            selectedIndex = 0,
            onSelectedIndexChange = {},
            enabled = false,
        )
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
        drawRoundRect(
            color = Color.White,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
}
