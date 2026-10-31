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
import androidx.compose.runtime.LaunchedEffect
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
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.menu.WindowIconCascadingDropdownMenu
import io.github.suqi8.coui.kmp.menu.WindowIconDropdownMenu
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.window.WindowAlertDialog
import io.github.suqi8.coui.kmp.window.WindowCascadingListPopup
import io.github.suqi8.coui.kmp.window.WindowLoadingDialog
import io.github.suqi8.coui.kmp.window.WindowProgressDialog
import io.github.suqi8.coui.kmp.window.WindowSecurityDialog
import kotlinx.coroutines.delay

/**
 * The window-hosted dialog composables: platform-`Dialog`-hosted siblings of the `Overlay*Dialog`
 * family that the "Dialog" entry already covers — [WindowLoadingDialog], [WindowAlertDialog],
 * [WindowProgressDialog] and [WindowSecurityDialog]. They share the content layouts and only differ
 * in the host, so this entry exists to keep both hosts demonstrated.
 */
@Composable
fun WindowDialogVariantsDemo() {
    var showLoading by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }
    var progressValue by remember { mutableStateOf<Float?>(null) }

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
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("WindowAlertDialog"),
            summary = str("Centered warning dialog in a platform Dialog"),
            onClick = { showAlert = true },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("WindowProgressDialog"),
            summary = str("Horizontal progress dialog in a platform Dialog"),
            onClick = { showProgress = true },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
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
    WindowAlertDialog(
        show = showAlert,
        onConfirm = { showAlert = false },
        onCancel = { showAlert = false },
        title = str("Delete this backup?"),
        summary = str("The backup will be removed from the cloud and from every synced device."),
        confirmText = str("Delete"),
        cancelText = str("Cancel"),
    )

    // Run the progress dialog through its indefinite and determinate modes like a real download.
    LaunchedEffect(showProgress) {
        if (showProgress) {
            progressValue = null
            delay(1200)
            repeat(40) { step ->
                progressValue = step / 40f
                delay(60)
            }
            progressValue = 1f
            delay(400)
            showProgress = false
        }
    }
    WindowProgressDialog(
        show = showProgress,
        progress = progressValue,
        text = if (progressValue == null) str("Connecting...") else str("Downloading..."),
        onDismissRequest = { showProgress = false },
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
    var singleRowPopupShown by remember { mutableStateOf(false) }
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

    Card(
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
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            // A cascading menu whose primary panel carries a single row, so the one-row geometry is
            // reachable on the cascading path too.
            ArrowPreference(
                title = str("WindowCascadingListPopup (single row)"),
                summary = str("A one-row primary panel"),
                onClick = { singleRowPopupShown = true },
            )
        }
    }

    WindowCascadingListPopup(
        show = popupShown,
        entries = cascading,
        onDismissRequest = { popupShown = false },
    )

    WindowCascadingListPopup(
        show = singleRowPopupShown,
        entries = singleRowCascading,
        onDismissRequest = { singleRowPopupShown = false },
    )
}
