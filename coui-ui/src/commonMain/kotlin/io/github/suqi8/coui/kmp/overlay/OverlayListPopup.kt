// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.ListPopupDefaults
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.PreciseClickState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.layout.ListPopupLayout
import io.github.suqi8.coui.kmp.layout.popupPhysicalDensity
import io.github.suqi8.coui.kmp.utils.COUIPopupUtils.Companion.PopupLayout

/**
 * A popup with a list of items.
 *
 * @param show Whether the [OverlayListPopup] is shown.
 * @param popupModifier The modifier to be applied to the [OverlayListPopup].
 * @param popupPositionProvider The [PopupPositionProvider] of the [OverlayListPopup].
 * @param alignment The alignment of the [OverlayListPopup].
 * @param enableWindowDim Whether to enable window dimming when the [OverlayListPopup] is shown.
 *   Defaults to false: `COUIPopupListWindow` calls `setBackgroundDrawable(null)` and never dims,
 *   so a COUI dropdown leaves the content behind it untouched.
 * @param onDismissRequest The callback when the [OverlayListPopup] is dismissed.
 * @param onDismissFinished The callback when the [OverlayListPopup] is completely dismissed (after exit animation).
 * @param maxHeight The maximum height of the [OverlayListPopup]. If null, the height will be calculated automatically.
 * @param minWidth The minimum width of the [OverlayListPopup].
 * @param renderInRootScaffold Whether to render the popup in the root (outermost) Scaffold.
 *   When true (default), the popup covers the full screen. When false, it renders within the
 *   current Scaffold's bounds with position compensation.
 * @param preciseClickState Opens the popup at the last point recorded by
 *   [io.github.suqi8.coui.kmp.basic.preciseClickAnchor] on the anchor instead of centring it on the
 *   anchor, matching COUI `PreciseClickHelper`. Null (default) keeps anchor-centred placement.
 * @param content The [Composable] content of the [OverlayListPopup]. You should use the [ListPopupColumn] in general.
 */
@Composable
fun OverlayListPopup(
    show: Boolean,
    popupModifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    enableWindowDim: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxHeight: Dp? = null,
    minWidth: Dp = ListPopupDefaults.MinWidth,
    renderInRootScaffold: Boolean = true,
    preciseClickState: PreciseClickState? = null,
    content: @Composable () -> Unit,
) {
    // Fallback only: the popup composes in the root Scaffold's overlay, where the host publishes the
    // backdrop the *screen* is recorded into. That one is the correct source — a menu samples what
    // the user sees behind it — so the call site's backdrop is used only when the host has none.
    // Publishing it unconditionally made every menu opened from a page sample that page's wallpaper
    // layer instead, and a white page frosted into the wallpaper's cyan and yellow.
    val backdrop = LocalBackdrop.current
    // Same reason for the density: ColorOS menus render at the panel's density, not the display-size
    // override a third-party app follows, so the popup must re-provide it where it actually composes.
    val density = LocalDensity.current
    val popupDensity = remember(density) { popupPhysicalDensity(density) }
    ListPopupLayout(
        show = show,
        popupHost = { visible, passTouches, hostContent ->
            val visibleState = remember { mutableStateOf(false) }
            visibleState.value = visible
            PopupLayout(
                visible = visibleState,
                enableWindowDim = false,
                enableBackHandler = false,
                enterTransition = EnterTransition.None,
                exitTransition = ExitTransition.None,
                renderInRootScaffold = renderInRootScaffold,
                passTouches = passTouches,
            ) {
                CompositionLocalProvider(LocalDensity provides popupDensity) {
                    PopupBackdrop(hostBackdrop = backdrop) {
                        hostContent()
                    }
                }
            }
        },
        popupModifier = popupModifier,
        popupPositionProvider = popupPositionProvider,
        alignment = alignment,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        maxHeight = maxHeight,
        minWidth = minWidth,
        preciseClickState = preciseClickState,
        enableGlass = true,
        content = content,
    )
}

/**
 * Publishes the backdrop a popup should sample: the one the host already provides, falling back to
 * the call site's only when the host has none.
 *
 * The host publishes the layer the whole screen is recorded into, which is what a menu opened over a
 * page must blur. The call site's backdrop is the layer *that page* records, and in an app whose
 * pages keep their background transparent over a wallpaper it carries the wallpaper: the menu then
 * frosts into the wallpaper's colours instead of the page it covers.
 */
@Composable
internal fun PopupBackdrop(hostBackdrop: Backdrop?, content: @Composable () -> Unit) {
    val ambient = LocalBackdrop.current
    CompositionLocalProvider(LocalBackdrop provides (ambient ?: hostBackdrop)) {
        content()
    }
}
