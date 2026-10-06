// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.window.WindowListPopup

private val NavigationPopupMenuGroupKey = Any()

/**
 * The ColorOS bottom-navigation overflow menu.
 *
 * It shows the navigation items that do not fit in the bar. It is a popup anchored to the menu view
 * with a top/end drop-down gravity (0x800035), so it opens **above** the bar and aligns to the bar's
 * end edge, and it is offset by the popup's horizontal and vertical margins. Its window background
 * is the same popup surface this library already draws in `ListPopupContent` (28dp smooth-corner
 * glass); the rows carry the popup list's top / center / bottom press-state selectors.
 *
 * Compose this inside the composable it should anchor to, exactly like [WindowListPopup]: the anchor
 * is the bounds of the popup's parent layout node.
 *
 * @param expanded Whether the menu is shown.
 * @param onDismissRequest Called when the user dismisses the menu (outside tap or back).
 * @param modifier The modifier to be applied to the popup.
 * @param content The rows, usually [NavigationPopupMenuItem]s.
 */
@Composable
fun NavigationPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    WindowListPopup(
        show = expanded,
        popupModifier = modifier,
        popupPositionProvider = NavigationPopupMenuDefaults.PositionProvider,
        alignment = PopupPositionProvider.Align.BottomEnd,
        maxHeight = NavigationPopupMenuDefaults.MaxHeight,
        minWidth = NavigationPopupMenuDefaults.MinWidth,
        onDismissRequest = onDismissRequest,
    ) {
        // The native popup calls `listView.setDivider(null)`. Sharing one group
        // key keeps the rounded press mask while suppressing divider bands.
        ListPopupColumn {
            content()
        }
    }
}

/**
 * A row of a [NavigationPopupMenu].
 *
 * A row is a horizontal `LinearLayout` with a 12dp `paddingTop` / `paddingBottom`, a minimum width,
 * an icon and a bold label. The icon is inset from the row start by the popup's horizontal margin,
 * and the label is inset by the same amount on both sides.
 *
 * The label takes the bar's secondary-neutral colour (tertiary when disabled) and the icon is tinted
 * with the bar's icon-tint list (primary when enabled, tertiary when disabled).
 *
 * @param label The row title.
 * @param icon The row icon.
 * @param onClick Called when the row is activated.
 * @param modifier The modifier to be applied to the row.
 * @param enabled Whether the row can be activated.
 */
@Composable
fun NavigationPopupMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colorScheme = COUITheme.colorScheme
    val labelColor = if (enabled) colorScheme.onSurfaceSecondary else colorScheme.disabledOnSurface
    val iconTint = if (enabled) colorScheme.onSurfaceContainer else colorScheme.disabledOnSurface
    Row(
        modifier = modifier
            .widthIn(min = NavigationPopupMenuDefaults.MinWidth)
            .popupListItem(
                selected = false,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                groupKey = NavigationPopupMenuGroupKey,
            )
            .padding(vertical = NavigationPopupMenuDefaults.ItemPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .padding(start = NavigationPopupMenuDefaults.HorizontalMargin)
                .size(NavigationPopupMenuDefaults.IconSize),
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconTint),
        )
        Spacer(Modifier.width(NavigationPopupMenuDefaults.HorizontalMargin))
        Text(
            modifier = Modifier.padding(end = NavigationPopupMenuDefaults.HorizontalMargin),
            text = label,
            color = labelColor,
            fontSize = NavigationPopupMenuDefaults.TextSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Contains default values used by [NavigationPopupMenu] and [NavigationPopupMenuItem]. */
object NavigationPopupMenuDefaults {

    /** The minimum row width. */
    val MinWidth = 208.dp

    /** The maximum menu height. */
    val MaxHeight = 324.dp

    /**
     * The horizontal margin between the menu and the bar's end edge, and the inset of a row's icon
     * and label.
     */
    val HorizontalMargin = 16.dp

    /** The vertical margin between the menu and the bar. */
    val VerticalMargin = 8.dp

    /** The row title size. */
    val TextSize = 16.sp

    /**
     * The row's top and bottom padding. With the 24dp icon this makes a 48dp row.
     */
    val ItemPaddingVertical = 12.dp

    /** The row icon size. */
    val IconSize = 24.dp

    /**
     * The space the menu reserves for a row's icon and margins when it measures its width:
     * `mPopupWidth = max(mPopupWidth, titleWidth + itemUsedSpace)`.
     */
    val ItemUsedSpace = 72.dp

    /**
     * Places the menu above the anchor, aligned to the anchor's end edge, with [HorizontalMargin] /
     * [VerticalMargin] to spare.
     *
     * This reproduces the menu's own placement: `setDropDownGravity(0x800035)` puts the
     * menu above the bar, `setHorizontalOffset(-horizontalMargin + padding.right)` and
     * `setVerticalOffset(padding.bottom - verticalMargin)` account for the 9-patch padding, and
     * `setContentWidth(min(mPopupWidth, screenWidth - 2 * horizontalMargin))` keeps the menu clear of
     * the screen edges.
     */
    val PositionProvider: PopupPositionProvider = object : PopupPositionProvider {
        private val margins = PaddingValues(
            horizontal = HorizontalMargin,
            vertical = VerticalMargin,
        )

        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset {
            val endAligned = when (alignment.resolvedAlign(layoutDirection)) {
                PopupPositionProvider.Align.End,
                PopupPositionProvider.Align.TopEnd,
                PopupPositionProvider.Align.BottomEnd,
                -> true

                else -> false
            }
            val offsetX = if (endAligned) {
                anchorBounds.right - popupContentSize.width - popupMargin.right
            } else {
                anchorBounds.left + popupMargin.left
            }
            val offsetY = anchorBounds.top - popupContentSize.height - popupMargin.bottom
            val minX = windowBounds.left + popupMargin.left
            val maxX = (windowBounds.right - popupContentSize.width - popupMargin.right).coerceAtLeast(minX)
            val minY = windowBounds.top + popupMargin.top
            val maxY = (windowBounds.bottom - popupContentSize.height - popupMargin.bottom).coerceAtLeast(minY)
            return IntOffset(
                x = offsetX.coerceIn(minX, maxX),
                y = offsetY.coerceIn(minY, maxY),
            )
        }

        override fun getMargins(): PaddingValues = margins
    }
}

// `PopupPositionProvider.Align.resolve` is file-private in ListPopup.kt, so the start/end mirroring
// that `rememberListPopupLayoutInfo` applies is repeated here for the anchored case.
private fun PopupPositionProvider.Align.resolvedAlign(layoutDirection: LayoutDirection): PopupPositionProvider.Align {
    if (layoutDirection == LayoutDirection.Ltr) return this
    return when (this) {
        PopupPositionProvider.Align.Start -> PopupPositionProvider.Align.End
        PopupPositionProvider.Align.End -> PopupPositionProvider.Align.Start
        PopupPositionProvider.Align.TopStart -> PopupPositionProvider.Align.TopEnd
        PopupPositionProvider.Align.TopEnd -> PopupPositionProvider.Align.TopStart
        PopupPositionProvider.Align.BottomStart -> PopupPositionProvider.Align.BottomEnd
        PopupPositionProvider.Align.BottomEnd -> PopupPositionProvider.Align.BottomStart
    }
}
