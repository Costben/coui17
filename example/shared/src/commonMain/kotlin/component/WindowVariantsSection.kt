// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

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
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.menu.WindowIconCascadingDropdownMenu
import io.github.suqi8.coui.kmp.menu.WindowIconDropdownMenu
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.window.WindowCascadingListPopup
import io.github.suqi8.coui.kmp.window.WindowLoadingDialog
import io.github.suqi8.coui.kmp.window.WindowSecurityDialog

/**
 * The two `Window*Dialog` composables: platform-`Dialog`-hosted siblings of the `Overlay*Dialog`
 * family that the "Dialog" entry already covers. They share the content layouts and only differ in
 * the host, so this entry exists to keep both hosts demonstrated.
 */
@Composable
fun WindowDialogVariantsDemo() {
    var showLoading by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ArrowPreference(
            title = str("WindowLoadingDialog"),
            summary = str("Platform Dialog host for the loading content"),
            onClick = { showLoading = true },
        )
        ArrowPreference(
            title = str("WindowSecurityDialog"),
            summary = str("Statement, link and checkbox in a platform Dialog"),
            onClick = { showSecurity = true },
        )
    }

    WindowLoadingDialog(
        show = showLoading,
        text = str("Loading..."),
        onDismissRequest = { showLoading = false },
    )
    WindowSecurityDialog(
        show = showSecurity,
        title = str("Security Notice"),
        summary = str("This feature needs to connect to the network."),
        statement = str("Tap and view Privacy Policy for more information."),
        statementLinkText = str("Privacy Policy"),
        onLinkClick = { },
        onConfirm = { _ -> showSecurity = false },
        onCancel = { showSecurity = false },
    )
}

/**
 * The `Window` icon-menu family: [WindowIconDropdownMenu] for a flat popup,
 * [WindowIconCascadingDropdownMenu] for submenus, and [WindowCascadingListPopup] as the popup the
 * cascading menu hosts. The `Overlay` twins are covered elsewhere; these keep the window host shown.
 */
@Composable
fun WindowIconMenuVariantsDemo() {
    var selected by remember { mutableIntStateOf(0) }
    var popupShown by remember { mutableStateOf(false) }
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

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
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
    }

    WindowCascadingListPopup(
        show = popupShown,
        entries = cascading,
        onDismissRequest = { popupShown = false },
    )
}
