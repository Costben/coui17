// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenu
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenuItem
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Copy
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowListPopup

fun LazyListScope.horizontalPopupMenuSection() {
    item(key = "horizontalPopupMenu") {
        SmallTitle(text = "HorizontalPopupMenu")
        HorizontalPopupMenuDemo()
    }
}

@Composable
private fun HorizontalPopupMenuDemo() {
    var showPopup by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val rows = remember { listOf("Option 1", "Option 2", "Option 3") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            text = "Show HorizontalPopupMenu",
            onClick = { showPopup = true },
        )
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
                        label = "Copy",
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Share,
                        label = "Share",
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Delete,
                        label = "Delete",
                        onClick = { dismiss?.invoke() },
                    )
                }
                // An icon-only block: the item count picks the column count.
                HorizontalPopupMenu {
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Copy,
                        contentDescription = "Copy",
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Share,
                        contentDescription = "Share",
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.Delete,
                        contentDescription = "Delete",
                        onClick = { dismiss?.invoke() },
                    )
                    HorizontalPopupMenuItem(
                        icon = COUIIcons.More,
                        contentDescription = "More",
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
