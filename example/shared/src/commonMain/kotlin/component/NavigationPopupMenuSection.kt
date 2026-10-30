// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Search
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.overlay.OverlayNavigationPopupMenu

/**
 * Renders the "NavigationPopupMenu" gallery section content: a button that opens the bottom-bar
 * overflow menu on the in-window popup host, so the card samples the page behind it and wears the
 * glass, rim and drop shadow the Gallery's own "More" menu has.
 */
@Composable
fun NavigationPopupMenuDemo() {
    var expanded by remember { mutableStateOf(false) }
    val searchLabel = str("Search")
    val settingsLabel = str("Settings")
    val aboutLabel = str("About")
    val entries = listOf(
        DropdownEntry(
            items = listOf(
                DropdownItem(
                    text = searchLabel,
                    icon = { Icon(imageVector = COUIIcons.Search, contentDescription = null, modifier = it) },
                    onClick = { expanded = false },
                ),
            ),
        ),
        DropdownEntry(
            items = listOf(
                DropdownItem(
                    text = settingsLabel,
                    icon = { Icon(imageVector = COUIIcons.Settings, contentDescription = null, modifier = it) },
                    onClick = { expanded = false },
                ),
            ),
        ),
        DropdownEntry(
            items = listOf(
                DropdownItem(
                    text = aboutLabel,
                    enabled = false,
                    icon = { Icon(imageVector = COUIIcons.More, contentDescription = null, modifier = it) },
                ),
            ),
        ),
    )

    // The popup anchors to the bounds of its parent layout node. Keep that node around the
    // three-dot action so the menu opens above the same material button as the system bar.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box {
            HighlightedIconButton(
                onClick = { expanded = !expanded },
            ) {
                Icon(imageVector = COUIIcons.More, contentDescription = str("More"))
            }
            OverlayNavigationPopupMenu(
                expanded = expanded,
                entries = entries,
                onDismissRequest = { expanded = false },
            )
        }
    }
}
