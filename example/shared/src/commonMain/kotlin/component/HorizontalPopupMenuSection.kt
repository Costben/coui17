// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

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
import i18n.LocalAppStrings
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenu
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenuItem
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.PopupListDivider
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Copy
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowListPopup

/**
 * Renders the "HorizontalPopupMenu" gallery section content: host and bar-style options above a
 * single trigger card that opens a popup pairing either a 3-column labelled action bar or a
 * 4-column icon-only action bar with standard list items.
 */
@Composable
fun HorizontalPopupMenuDemo() {
    var showPopup by remember { mutableStateOf(false) }
    val hostIndex = remember { mutableIntStateOf(0) }
    val iconOnly = remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf<String?>(null) }
    val s = LocalAppStrings.current
    val rows = remember(s) { listOf(s["Option 1"], s["Option 2"], s["Option 3"]) }

    DemoOptions(
        DemoChoice("Host", listOf("Window", "Overlay"), hostIndex),
        DemoToggle("Icon-only action bar", iconOnly),
    )
    Card(
        cornerRadius = CardDefaults.GroupCornerRadius,
        weight = CardDefaults.GroupWeight,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Box {
            ArrowPreference(
                title = str("Show HorizontalPopupMenu"),
                summary = lastAction?.let { str("Last clicked: {}", it) }
                    ?: if (iconOnly.value) {
                        str("Icon-only action bar with list items")
                    } else {
                        str("Labelled action bar with list items")
                    },
                onClick = { showPopup = true },
                cardListPosition = CardListPosition.Full,
            )
            val popupContent: @Composable () -> Unit = {
                val dismiss = LocalDismissState.current
                val copyText = str("Copy")
                val shareText = str("Share")
                val deleteText = str("Delete")
                val moreText = str("More")
                ListPopupColumn {
                    if (iconOnly.value) {
                        // An icon-only block: the item count picks the 4-column compact grid.
                        HorizontalPopupMenu {
                            HorizontalPopupMenuItem(
                                icon = COUIIcons.Copy,
                                contentDescription = copyText,
                                onClick = {
                                    lastAction = copyText
                                    dismiss?.invoke()
                                },
                            )
                            HorizontalPopupMenuItem(
                                icon = COUIIcons.Share,
                                contentDescription = shareText,
                                onClick = {
                                    lastAction = shareText
                                    dismiss?.invoke()
                                },
                            )
                            HorizontalPopupMenuItem(
                                icon = COUIIcons.Delete,
                                contentDescription = deleteText,
                                onClick = {
                                    lastAction = deleteText
                                    dismiss?.invoke()
                                },
                            )
                            HorizontalPopupMenuItem(
                                icon = COUIIcons.More,
                                contentDescription = moreText,
                                onClick = {
                                    lastAction = moreText
                                    dismiss?.invoke()
                                },
                            )
                        }
                    } else {
                        // A labelled block: labels select the 3-column grid with icons above text.
                        HorizontalPopupMenu {
                            HorizontalPopupMenuItem(
                                icon = COUIIcons.Copy,
                                label = copyText,
                                onClick = {
                                    lastAction = copyText
                                    dismiss?.invoke()
                                },
                            )
                            HorizontalPopupMenuItem(
                                icon = COUIIcons.Share,
                                label = shareText,
                                onClick = {
                                    lastAction = shareText
                                    dismiss?.invoke()
                                },
                            )
                            HorizontalPopupMenuItem(
                                icon = COUIIcons.Delete,
                                label = deleteText,
                                onClick = {
                                    lastAction = deleteText
                                    dismiss?.invoke()
                                },
                            )
                        }
                    }
                    PopupListDivider()
                    rows.forEachIndexed { index, text ->
                        DropdownImpl(
                            item = DropdownItem(text = text),
                            optionSize = rows.size,
                            isSelected = selectedIndex == index,
                            index = index,
                            groupKey = 0,
                            onSelectedIndexChange = { newIndex ->
                                selectedIndex = newIndex
                                lastAction = rows[newIndex]
                                dismiss?.invoke()
                            },
                        )
                    }
                }
            }
            if (hostIndex.intValue == 0) {
                WindowListPopup(
                    show = showPopup,
                    onDismissRequest = { showPopup = false },
                    content = popupContent,
                )
            } else {
                OverlayListPopup(
                    show = showPopup,
                    onDismissRequest = { showPopup = false },
                    content = popupContent,
                )
            }
        }
    }
}
