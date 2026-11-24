// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenu
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuItem
import io.github.suqi8.coui.kmp.basic.PopupListDivider
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Search
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.overlay.OverlayNavigationPopupMenu

/**
 * Renders the "NavigationPopupMenu" gallery section content: host and divider options above a
 * single trigger card that anchors the bottom-bar overflow menu to a three-dot material action
 * button.
 */
@Composable
fun NavigationPopupMenuDemo() {
    val hostIndex = remember { mutableIntStateOf(0) }
    val sectionDivider = remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var lastClicked by remember { mutableStateOf<String?>(null) }
    val searchLabel = str("Search")
    val settingsLabel = str("Settings")
    val aboutLabel = str("About")
    val searchItem = DropdownItem(
        text = searchLabel,
        icon = { Icon(imageVector = COUIIcons.Search, contentDescription = null, modifier = it) },
        onClick = {
            lastClicked = searchLabel
            expanded = false
        },
    )
    val settingsItem = DropdownItem(
        text = settingsLabel,
        icon = { Icon(imageVector = COUIIcons.Settings, contentDescription = null, modifier = it) },
        onClick = {
            lastClicked = settingsLabel
            expanded = false
        },
    )
    val aboutItem = DropdownItem(
        text = aboutLabel,
        enabled = false,
        icon = { Icon(imageVector = COUIIcons.More, contentDescription = null, modifier = it) },
    )
    val entries = if (sectionDivider.value) {
        listOf(
            DropdownEntry(items = listOf(searchItem, settingsItem)),
            DropdownEntry(items = listOf(aboutItem)),
        )
    } else {
        listOf(
            DropdownEntry(items = listOf(searchItem, settingsItem, aboutItem)),
        )
    }

    DemoOptions(
        DemoChoice("Host", listOf("Overlay", "Window"), hostIndex),
        DemoToggle("Section divider", sectionDivider),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        BasicComponent(
            title = str("Show NavigationPopupMenu"),
            summary = lastClicked?.let { str("Last clicked: {}", it) }
                ?: if (hostIndex.intValue == 0) str("Overlay") else str("Window"),
            onClick = { expanded = !expanded },
            cardListPosition = CardListPosition.Full,
            endActions = {
                // The popup anchors to the bounds of its parent layout node. Keep that node around
                // the three-dot action so the menu opens above the same material button as the bar.
                Box {
                    HighlightedIconButton(
                        onClick = { expanded = !expanded },
                    ) {
                        Icon(imageVector = COUIIcons.More, contentDescription = str("More"))
                    }
                    if (hostIndex.intValue == 0) {
                        OverlayNavigationPopupMenu(
                            expanded = expanded,
                            entries = entries,
                            onDismissRequest = { expanded = false },
                        )
                    } else {
                        NavigationPopupMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            NavigationPopupMenuItem(
                                label = searchLabel,
                                icon = COUIIcons.Search,
                                onClick = {
                                    lastClicked = searchLabel
                                    expanded = false
                                },
                            )
                            NavigationPopupMenuItem(
                                label = settingsLabel,
                                icon = COUIIcons.Settings,
                                onClick = {
                                    lastClicked = settingsLabel
                                    expanded = false
                                },
                            )
                            if (sectionDivider.value) {
                                PopupListDivider()
                            }
                            NavigationPopupMenuItem(
                                label = aboutLabel,
                                icon = COUIIcons.More,
                                enabled = false,
                                onClick = {},
                            )
                        }
                    }
                }
            },
        )
    }
}
