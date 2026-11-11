// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.menu.WindowIconCascadingDropdownMenu
import io.github.suqi8.coui.kmp.menu.WindowIconDropdownMenu
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.window.WindowCascadingListPopup

/**
 * The `Window` icon-menu family: [WindowIconDropdownMenu] for a flat popup,
 * [WindowIconCascadingDropdownMenu] for submenus, and [WindowCascadingListPopup] as the popup the
 * cascading menu hosts. The `Overlay` twins are covered elsewhere; these keep the window host shown.
 * The option swaps the hosted popup's primary panel between the full list and a single row.
 */
@Composable
fun WindowIconMenuVariantsDemo() {
    var selected by remember { mutableIntStateOf(0) }
    var popupShown by remember { mutableStateOf(false) }
    val singleRow = remember { mutableStateOf(false) }
    val entries = remember(selected) {
        listOf(
            DropdownEntry(
                items = listOf("Option 1", "Option 2", "Option 3").mapIndexed { index, text ->
                    DropdownItem(
                        text = text,
                        selected = selected == index,
                        onClick = { selected = index },
                    )
                },
            ),
        )
    }
    val cascading = remember {
        listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = "Section A",
                        children = listOf(
                            DropdownItem(text = "A-1"),
                            DropdownItem(text = "A-2"),
                        ),
                    ),
                    DropdownItem(
                        text = "Section B",
                        children = listOf(
                            DropdownItem(text = "B-1"),
                            DropdownItem(text = "B-2"),
                            DropdownItem(text = "B-3"),
                        ),
                    ),
                    DropdownItem(text = "Plain row"),
                ),
            ),
        )
    }

    val singleRowCascading = remember {
        listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = "Section A",
                        children = listOf(DropdownItem(text = "A-1")),
                    ),
                ),
            ),
        )
    }

    DemoOptions(DemoToggle("Single-row primary panel", singleRow))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column {
            Row(modifier = Modifier.padding(16.dp)) {
                WindowIconDropdownMenu(
                    entries = entries,
                    content = { Icon(imageVector = COUIIcons.More, contentDescription = null) },
                )
                Spacer(modifier = Modifier.width(24.dp))
                WindowIconCascadingDropdownMenu(
                    entries = cascading,
                    content = { Icon(imageVector = COUIIcons.More, contentDescription = null) },
                )
                Spacer(modifier = Modifier.width(24.dp))
                ArrowPreference(
                    title = str("WindowCascadingListPopup"),
                    summary = str("The popup the cascading icon menu hosts"),
                    onClick = { popupShown = true },
                )
            }
            // The option swaps the primary panel's entry list, so the one-row geometry is reachable
            // on the same row instead of a second copy.
        }
    }

    WindowCascadingListPopup(
        show = popupShown,
        entries = if (singleRow.value) singleRowCascading else cascading,
        onDismissRequest = { popupShown = false },
    )
}
