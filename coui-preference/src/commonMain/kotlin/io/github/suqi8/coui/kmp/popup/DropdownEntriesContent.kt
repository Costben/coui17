// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.popup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.DropdownColors
import io.github.suqi8.coui.kmp.basic.DropdownDefaults
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownHeaderItem
import io.github.suqi8.coui.kmp.basic.DropdownImpl
import io.github.suqi8.coui.kmp.basic.PopupListDivider
import io.github.suqi8.coui.kmp.basic.RadioButton
import io.github.suqi8.coui.kmp.basic.RadioButtonDefaults
import io.github.suqi8.coui.kmp.preference.ListPreferenceColors
import io.github.suqi8.coui.kmp.preference.ListPreferenceDefaults
import io.github.suqi8.coui.kmp.preference.SelectPanelItem
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Renders a list of [DropdownEntry] groups inside a popup container. Computes popup-global
 * first/last per row internally so only the very first and very last rows of the entire popup
 * receive the larger first/last padding; group boundaries fall back to the middle padding.
 *
 * A group that declares a [DropdownEntry.title] gets a non-clickable [DropdownHeaderItem] above its
 * items. Groups are separated by a [PopupListDivider] — matching COUI 17
 * `COUIPopupListWindow.setItemListInternal` (`GROUP_MIN_ITEMS = 3`), only when the menu holds at
 * least [POPUP_GROUP_MIN_ITEMS] items; rows inside the same group share `groupKey = entryIdx` so no
 * intra-group divider is inserted.
 */
@Composable
internal fun DropdownEntriesPopupContent(
    entries: List<DropdownEntry>,
    dropdownColors: DropdownColors,
    onItemClick: (entryIdx: Int, itemIdx: Int) -> Unit,
) {
    val firstEntryIdx = entries.indexOfFirst { it.items.isNotEmpty() }
    val lastEntryIdx = entries.indexOfLast { it.items.isNotEmpty() }
    val showGroupDividers = entries.sumOf { it.items.size } >= POPUP_GROUP_MIN_ITEMS
    entries.forEachIndexed { entryIdx, entry ->
        if (entry.items.isEmpty()) return@forEachIndexed
        val lastItemIdx = entry.items.lastIndex
        val isFirstEntry = entryIdx == firstEntryIdx
        val isLastEntry = entryIdx == lastEntryIdx
        val header = entry.title
        if (header != null) {
            key(entryIdx, "header") {
                DropdownHeaderItem(
                    text = header,
                    color = dropdownColors.headerColor,
                    isFirst = isFirstEntry,
                )
            }
        }
        entry.items.forEachIndexed { itemIdx, option ->
            key(entryIdx, itemIdx) {
                DropdownImpl(
                    item = option,
                    optionSize = entry.items.size,
                    isSelected = option.selected,
                    index = itemIdx,
                    dropdownColors = dropdownColors,
                    enabled = entry.enabled && option.enabled,
                    isFirst = isFirstEntry && header == null && itemIdx == 0,
                    isLast = isLastEntry && itemIdx == lastItemIdx,
                    // Options of one entry are a single COUI group: adjacent rows share the key,
                    // so the column reserves no divider band between them (COUI only inserts a
                    // divider where getGroupId() changes).
                    groupKey = entryIdx,
                    onSelectedIndexChange = { idx -> onItemClick(entryIdx, idx) },
                )
            }
        }
        if (entryIdx != lastEntryIdx && showGroupDividers) {
            key(entryIdx, "divider") {
                PopupListDivider()
            }
        }
    }
}

/** The smallest menu that shows group dividers (COUI `COUIPopupListWindow.GROUP_MIN_ITEMS`). */
internal const val POPUP_GROUP_MIN_ITEMS = 3

/**
 * The list panel of a dropdown / spinner dialog, matching ColorOS 17 `COUIListPreferenceDialogFragment`
 * (`ChoiceListAdapter` with `coui_select_dialog_singlechoice` rows: optional leading icon, label
 * over optional summary, trailing [RadioButton], and a 0.33dp hairline divider inset by the 24dp
 * row padding under every row except the last).
 *
 * Groups follow one another without an artificial section band (matching both ColorOS 17
 * `ChoiceListAdapter` and `upstream/coui17`); a [DropdownEntry.title] adds a [DropdownHeaderItem]
 * aligned with the 24dp row edge.
 *
 * While [freezeSelection] is true the radios keep the selection the dialog opened with while it
 * animates out after a click, matching COUI's single-choice commit-and-dismiss behavior.
 */
@Composable
internal fun DropdownChoiceDialogList(
    entries: List<DropdownEntry>,
    show: Boolean,
    freezeSelection: Boolean,
    dropdownColors: DropdownColors,
    onItemClick: (entryIdx: Int, itemIdx: Int) -> Unit,
) {
    val disabledSelectedColor = COUITheme.colorScheme.disabledPrimary
    val colors = remember(dropdownColors, disabledSelectedColor) {
        ListPreferenceColors(
            itemTextColor = dropdownColors.contentColor,
            disabledItemTextColor = dropdownColors.disabledContentColor,
            itemSummaryColor = dropdownColors.summaryColor,
            disabledItemSummaryColor = dropdownColors.disabledContentColor,
            selectedIndicatorColor = dropdownColors.selectedIndicatorColor,
            disabledSelectedIndicatorColor = disabledSelectedColor,
        )
    }
    val radioColors = RadioButtonDefaults.radioButtonColors(
        selectedColor = colors.selectedIndicatorColor,
        disabledSelectedColor = colors.disabledSelectedIndicatorColor,
    )
    val openedSelection = remember { DropdownDialogOpenedSelection() }
    openedSelection.update(show, entries)
    val lastEntryIdx = entries.indexOfLast { it.items.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = ListPreferenceDefaults.ButtonBarTopPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        entries.forEachIndexed { entryIdx, entry ->
            if (entry.items.isEmpty()) return@forEachIndexed
            entry.title?.let { header ->
                key(entryIdx, "header") {
                    DropdownHeaderItem(
                        text = header,
                        modifier = DialogHeaderModifier,
                        color = dropdownColors.headerColor,
                    )
                }
            }
            entry.items.forEachIndexed { itemIdx, item ->
                key(entryIdx, itemIdx) {
                    val enabled = entry.enabled && item.enabled
                    val selected = if (freezeSelection) {
                        openedSelection.isSelected(entryIdx, itemIdx)
                    } else {
                        item.selected
                    }
                    val icon = item.icon
                    val iconContent: (@Composable () -> Unit)? = if (icon != null) {
                        { Box(DialogIconModifier, contentAlignment = Alignment.Center) { icon(Modifier) } }
                    } else {
                        null
                    }
                    SelectPanelItem(
                        text = item.text,
                        summary = item.summary,
                        selected = selected,
                        checked = null,
                        // COUIListPreferenceDialogFragment shows R.id.item_divider on every row
                        // except the final item (count != 1 && pos != count - 1).
                        showDivider = !(entryIdx == lastEntryIdx && itemIdx == entry.items.lastIndex),
                        colors = colors,
                        enabled = enabled,
                        onClick = { onItemClick(entryIdx, itemIdx) },
                        icon = iconContent,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            enabled = enabled,
                            colors = radioColors,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The selection a dropdown dialog opened with. It is recorded when the dialog starts to show and
 * kept through its exit, so a click that commits and dismisses does not jump the radio.
 */
private class DropdownDialogOpenedSelection {
    private var showing = false
    private var selected: List<List<Boolean>> = emptyList()

    fun update(show: Boolean, entries: List<DropdownEntry>) {
        if (show && !showing) {
            selected = entries.map { entry -> entry.items.map { it.selected } }
        }
        showing = show
    }

    fun isSelected(entryIdx: Int, itemIdx: Int): Boolean = selected.getOrNull(entryIdx)?.getOrNull(itemIdx) == true
}

/** A group header lines up with the row text, which the dialog rows pad by 24dp. */
private val DialogHeaderModifier = Modifier.padding(
    horizontal = ListPreferenceDefaults.PanelItemHorizontalPadding - DropdownDefaults.InsideHorizontalPadding,
)

/**
 * Leading icon slot of `coui_select_dialog_singlechoice.xml` (`alertdialog_choice_icon`): 16dp
 * before the text block (`coui_dialog_layout_margin_horizontal`) and 10dp extra vertical padding
 * over the row's 10dp vertical padding to match `alert_dialog_single_list_icon_padding_vertical` (20dp).
 */
private val DialogIconModifier = Modifier.padding(end = 16.dp, top = 10.dp, bottom = 10.dp)
