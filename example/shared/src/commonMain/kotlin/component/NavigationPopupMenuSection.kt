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
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenu
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuItem
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Search
import io.github.suqi8.coui.kmp.icon.extended.Settings

/** Renders the "NavigationPopupMenu" gallery section content: a button that opens a navigation popup menu. */
@Composable
fun NavigationPopupMenuDemo() {
    var expanded by remember { mutableStateOf(false) }

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
            NavigationPopupMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                NavigationPopupMenuItem(
                    label = str("Search"),
                    icon = COUIIcons.Search,
                    onClick = { expanded = false },
                )
                NavigationPopupMenuItem(
                    label = str("Settings"),
                    icon = COUIIcons.Settings,
                    onClick = { expanded = false },
                )
                NavigationPopupMenuItem(
                    label = str("About"),
                    icon = COUIIcons.More,
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}
