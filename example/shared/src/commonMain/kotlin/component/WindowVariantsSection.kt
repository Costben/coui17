// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalAppStrings
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
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
 * [WindowIconCascadingDropdownMenu] for submenus, and [WindowCascadingListPopup] as a multi-level
 * popup menu anchored to a preference row. The toggle switches the cascading menus between three
 * top-level items and a single top-level item.
 */
@Composable
fun WindowIconMenuVariantsDemo() {
    var selected by remember { mutableIntStateOf(0) }
    var lastCascading by remember { mutableStateOf<String?>(null) }
    var popupShown by remember { mutableStateOf(false) }
    val singleRow = remember { mutableStateOf(false) }
    val s = LocalAppStrings.current
    val flatLabels = remember(s) { listOf(s["Option 1"], s["Option 2"], s["Option 3"]) }

    val entries = remember(flatLabels, selected) {
        listOf(
            DropdownEntry(
                items = flatLabels.mapIndexed { index, text ->
                    DropdownItem(
                        text = text,
                        selected = selected == index,
                        onClick = { selected = index },
                    )
                },
            ),
        )
    }
    val cascading = remember(s) {
        listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = s["Section A"],
                        children = listOf(
                            DropdownItem(text = "A-1", onClick = { lastCascading = "A-1" }),
                            DropdownItem(text = "A-2", onClick = { lastCascading = "A-2" }),
                        ),
                    ),
                    DropdownItem(
                        text = s["Section B"],
                        children = listOf(
                            DropdownItem(text = "B-1", onClick = { lastCascading = "B-1" }),
                            DropdownItem(text = "B-2", onClick = { lastCascading = "B-2" }),
                            DropdownItem(text = "B-3", onClick = { lastCascading = "B-3" }),
                        ),
                    ),
                    DropdownItem(
                        text = s["Plain row"],
                        onClick = { lastCascading = s["Plain row"] },
                    ),
                ),
            ),
        )
    }
    val singleRowCascading = remember(s) {
        listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = s["Section A"],
                        children = listOf(
                            DropdownItem(text = "A-1", onClick = { lastCascading = "A-1" }),
                        ),
                    ),
                ),
            ),
        )
    }
    val activeCascading = if (singleRow.value) singleRowCascading else cascading

    DemoOptions(DemoToggle("Single-item cascading menu", singleRow))
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column {
            BasicComponent(
                title = str("Flat icon menu"),
                summary = str("Selected: {}", flatLabels[selected]),
                cardListPosition = CardListPosition.Head,
                endActions = {
                    WindowIconDropdownMenu(
                        entries = entries,
                        content = { Icon(imageVector = COUIIcons.More, contentDescription = str("More")) },
                    )
                },
            )
            BasicComponent(
                title = str("Cascading icon menu"),
                summary = lastCascading?.let { str("Last clicked: {}", it) }
                    ?: str("Submenu triggered from an icon button"),
                cardListPosition = CardListPosition.Middle,
                endActions = {
                    WindowIconCascadingDropdownMenu(
                        entries = activeCascading,
                        content = { Icon(imageVector = COUIIcons.More, contentDescription = str("More")) },
                    )
                },
            )
            Box {
                ArrowPreference(
                    title = str("Cascading popup menu"),
                    summary = str("Multi-level popup where items with arrows open a submenu"),
                    onClick = { popupShown = true },
                    cardListPosition = CardListPosition.Tail,
                )
                WindowCascadingListPopup(
                    show = popupShown,
                    entries = activeCascading,
                    onDismissRequest = { popupShown = false },
                )
            }
        }
    }
}
