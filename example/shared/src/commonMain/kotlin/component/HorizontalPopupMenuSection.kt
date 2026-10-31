// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalAppStrings
import i18n.str
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenu
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenuItem
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Copy
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowListPopup

/** Renders the "HorizontalPopupMenu" gallery section content (a button opening a popup with labelled and icon-only menus). */
@Composable
fun HorizontalPopupMenuDemo() {
    var showPopup by remember { mutableStateOf(false) }
    // The shortest menu the popup draws: one row, so the fixed 28dp corner radius is exactly half
    // the panel height. Both hosts are covered because the window host is a separate window and
    // the overlay host composes into the root scaffold.
    var showSingleRowWindow by remember { mutableStateOf(false) }
    var showSingleRowOverlay by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val s = LocalAppStrings.current
    val rows = remember(s) { listOf(s["Option 1"], s["Option 2"], s["Option 3"]) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                text = str("Show HorizontalPopupMenu"),
                onClick = { showPopup = true },
            )
            TextButton(
                text = str("Show single-row WindowListPopup"),
                onClick = { showSingleRowWindow = true },
            )
            TextButton(
                text = str("Show single-row OverlayListPopup"),
                onClick = { showSingleRowOverlay = true },
            )
        }
        WindowListPopup(
            show = showSingleRowWindow,
            onDismissRequest = { showSingleRowWindow = false },
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                DropdownImpl(
                    text = str("Only row"),
                    optionSize = 1,
                    isSelected = false,
                    index = 0,
                    onSelectedIndexChange = { dismiss?.invoke() },
                )
            }
        }
        OverlayListPopup(
            show = showSingleRowOverlay,
            onDismissRequest = { showSingleRowOverlay = false },
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                DropdownImpl(
                    text = str("Only row"),
                    optionSize = 1,
                    isSelected = false,
                    index = 0,
                    onSelectedIndexChange = { dismiss?.invoke() },
                )
            }
        }
        WindowListPopup(
            show = showPopup,
            onDismissRequest = { showPopup = false },
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                // A labelled block: one label in the block selects the three-column grid.
                HorizontalPopupMenu {
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Copy,
                        label = str("Copy"),
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Share,
                        label = str("Share"),
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Delete,
                        label = str("Delete"),
                        onClick = { dismiss?.invoke() },
                    )
                }
                // An icon-only block: the item count picks the column count.
                HorizontalPopupMenu {
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Copy,
                        contentDescription = str("Copy"),
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Share,
                        contentDescription = str("Share"),
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Delete,
                        contentDescription = str("Delete"),
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.More,
                        contentDescription = str("More"),
                        onClick = { dismiss?.invoke() },
                    )
                }
                rows.forEachIndexed { index, text ->
                    DropdownImpl(
                        text = text,
                        optionSize = rows.size,
                        isSelected = selectedIndex == index,
                        index = index,
                        onSelectedIndexChange = { newIndex ->
                            selectedIndex = newIndex
                            dismiss?.invoke()
                        },
                    )
                }
            }
        }
    }
}
