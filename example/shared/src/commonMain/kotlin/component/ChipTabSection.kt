// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.ChipTabData
import io.github.suqi8.coui.kmp.basic.ChipTabGroup
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.All
import io.github.suqi8.coui.kmp.icon.extended.File
import io.github.suqi8.coui.kmp.icon.extended.GridView
import io.github.suqi8.coui.kmp.icon.extended.Music
import io.github.suqi8.coui.kmp.icon.extended.Photos
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.theme.LocalDismissState

/**
 * Renders the "ChipTab" gallery section content: one chip tab group whose icons, red dots and
 * counts, leading icon chip and surrounding card come from the options.
 *
 * Without the card this is COUIChipTabLayout as Notes lays it out: the scroller spans the window and
 * pads its tabs by 16dp, so they scroll on up to the screen edge.
 */
@Composable
fun ChipTabDemo() {
    val showIcons = remember { mutableStateOf(false) }
    val showHints = remember { mutableStateOf(false) }
    val leadingChip = remember { mutableStateOf(true) }
    val inCard = remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        DemoOptions(
            DemoToggle("Show icons", showIcons),
            DemoToggle("Red dot and count", showHints),
            DemoToggle("Leading icon chip", leadingChip),
            DemoToggle("Inside a card", inCard),
        )
        if (inCard.value) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                cornerRadius = CardDefaults.GroupCornerRadius,
                weight = CardDefaults.GroupWeight,
            ) {
                ChipTabsDemo(showIcons.value, showHints.value, leadingChip.value)
            }
        } else {
            ChipTabsDemo(
                showIcons = showIcons.value,
                showHints = showHints.value,
                leadingChip = leadingChip.value,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun ChipTabsDemo(
    showIcons: Boolean,
    showHints: Boolean,
    leadingChip: Boolean,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val tabs = listOf(
        ChipTabData(
            label = str("All"),
            icon = COUIIcons.All.takeIf { showIcons },
            showRedDot = showHints,
        ),
        ChipTabData(
            label = str("Photos"),
            icon = COUIIcons.Photos.takeIf { showIcons },
            count = 12.takeIf { showHints },
        ),
        ChipTabData(
            label = str("Music"),
            icon = COUIIcons.Music.takeIf { showIcons },
            count = 128.takeIf { showHints },
            showRedDot = showHints,
        ),
        ChipTabData(
            label = str("Files"),
            icon = COUIIcons.File.takeIf { showIcons },
        ),
        ChipTabData(label = str("Unavailable"), enabled = false),
        ChipTabData(label = str("Documents")),
    )

    Box(modifier = modifier) {
        ChipTabGroup(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelectedChange = { selectedIndex = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            leadingIcon = COUIIcons.GridView.takeIf { leadingChip },
            onLeadingIconClick = { menuExpanded = true },
            contentPadding = PaddingValues(horizontal = 16.dp),
        )
        OverlayListPopup(
            show = menuExpanded,
            alignment = PopupPositionProvider.Align.Start,
            onDismissRequest = { menuExpanded = false },
        ) {
            val dismiss = LocalDismissState.current
            ListPopupColumn {
                tabs.forEachIndexed { index, tab ->
                    DropdownImpl(
                        text = tab.label,
                        optionSize = tabs.size,
                        isSelected = index == selectedIndex,
                        index = index,
                        onSelectedIndexChange = { selected ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            selectedIndex = selected
                            dismiss?.invoke() ?: run { menuExpanded = false }
                        },
                    )
                }
            }
        }
    }
}
