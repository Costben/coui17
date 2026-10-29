// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.basic.DropdownColors
import io.github.suqi8.coui.kmp.basic.DropdownDefaults
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuDefaults
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider

/**
 * The ColorOS bottom-navigation overflow menu, drawn on the in-window popup host.
 *
 * Geometry is [io.github.suqi8.coui.kmp.basic.NavigationPopupMenu]'s — [NavigationPopupMenuDefaults.PositionProvider]
 * puts the menu above its anchor, edge-aligned with it, with the bar's own margins, and
 * [NavigationPopupMenuDefaults.MinWidth] / [NavigationPopupMenuDefaults.MaxHeight] bound the card —
 * but the surface is ColorOS 17's popup glass: the blurred backdrop behind the panel, the panel's
 * own drop shadow, and the material rim, i.e. what the Gallery's "More" menu on the bottom bar is.
 *
 * [io.github.suqi8.coui.kmp.basic.NavigationPopupMenu] stays the `PopupWindow` variant: that one has
 * no backdrop to sample, so it keeps the opaque surface the system draws outside the blur path.
 *
 * Compose this inside the composable the menu should anchor to, exactly like the `PopupWindow`
 * variant: COUI measures the parent layout node's bounds to place the popup.
 *
 * @param expanded Whether the menu is shown.
 * @param entries The rows, grouped like [io.github.suqi8.coui.kmp.basic.DropdownEntry]; a row with
 *   non-empty [io.github.suqi8.coui.kmp.basic.DropdownItem.children] opens a submenu.
 * @param onDismissRequest Called when the user dismisses the menu (outside tap or back).
 * @param modifier The modifier to be applied to the popup.
 * @param alignment Which edge of the anchor the menu is aligned to; [PopupPositionProvider.Align.BottomEnd]
 *   keeps the bar's right-edge convention, [PopupPositionProvider.Align.BottomStart] mirrors it for an
 *   anchor at the leading edge.
 * @param popupPositionProvider Position strategy; defaults to the bar's own placement rule.
 * @param minWidth Minimum width of the menu.
 * @param maxHeight Maximum height of the menu; null bounds it by the safe area.
 * @param onDismissFinished Called after the exit animation finishes.
 * @param renderInRootScaffold Whether to render in the outermost `Scaffold`'s popup host.
 * @param dropdownColors Colors used by every row.
 * @param collapseOnSelection When true, selecting any leaf dismisses the menu.
 */
@Composable
fun OverlayNavigationPopupMenu(
    expanded: Boolean,
    entries: List<DropdownEntry>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.BottomEnd,
    popupPositionProvider: PopupPositionProvider = NavigationPopupMenuDefaults.PositionProvider,
    minWidth: Dp = NavigationPopupMenuDefaults.MinWidth,
    maxHeight: Dp? = NavigationPopupMenuDefaults.MaxHeight,
    onDismissFinished: (() -> Unit)? = null,
    renderInRootScaffold: Boolean = true,
    dropdownColors: DropdownColors = DropdownDefaults.dropdownColors(),
    collapseOnSelection: Boolean = true,
) {
    OverlayCascadingListPopup(
        show = expanded,
        entries = entries,
        onDismissRequest = onDismissRequest,
        popupModifier = modifier,
        onDismissFinished = onDismissFinished,
        popupPositionProvider = popupPositionProvider,
        alignment = alignment,
        maxHeight = maxHeight,
        minWidth = minWidth,
        renderInRootScaffold = renderInRootScaffold,
        dropdownColors = dropdownColors,
        collapseOnSelection = collapseOnSelection,
        enableGlass = true,
    )
}
