// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenu
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuItem
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Search
import io.github.suqi8.coui.kmp.icon.extended.Settings

fun LazyListScope.navigationPopupMenuSection() {
    item(key = "navigationPopupMenu") {
        SmallTitle(text = "NavigationPopupMenu")
        NavigationPopupMenuDemo()
    }
}

/** Renders the "NavigationPopupMenu" gallery section content: a button that opens a navigation popup menu. */
@Composable
fun NavigationPopupMenuDemo() {
    var expanded by remember { mutableStateOf(false) }

    // The popup anchors to the bounds of its parent layout node, so the menu is composed inside the
    // box it should open above.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            text = "Show NavigationPopupMenu",
            onClick = { expanded = true },
        )
        NavigationPopupMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            NavigationPopupMenuItem(
                label = "Search",
                icon = COUIIcons.Search,
                onClick = { expanded = false },
            )
            NavigationPopupMenuItem(
                label = "Settings",
                icon = COUIIcons.Settings,
                onClick = { expanded = false },
            )
            NavigationPopupMenuItem(
                label = "About",
                icon = COUIIcons.More,
                enabled = false,
                onClick = {},
            )
        }
    }
}
