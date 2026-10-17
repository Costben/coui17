// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The bottom barriers list popups must stay above, in window pixels.
 *
 * `PopupMenuLocateHelper.findAllBarrierRulesAndExecute` walks the window for views that implement
 * `PopupMenuConfigRule` with type 2 — `COUIBottomFloatingNavigationBar`, `COUINavigationView` and
 * `COUIBottomFloatingToolbar` — and raises the popup's bottom window barrier to the top of each one's
 * visible frame, grown upwards by its outsets (`coui_popup_list_window_gap_to_navigation_view`, 8dp).
 * Composables mark themselves with [popupBottomBarrier]; the list popup reads [bottomBarrierTop]. One
 * registry is shared by every COUI theme in a composition, so window popups, which inherit the
 * caller's composition locals, see the barriers of the page that opened them.
 */
@Stable
internal class PopupBarriers {
    private val tops = mutableStateMapOf<Any, Int>()

    /** The highest registered barrier top in window pixels, outsets applied, or `null` when there is none. */
    val bottomBarrierTop: Int?
        get() = tops.values.minOrNull()

    fun update(owner: Any, top: Int) {
        if (tops[owner] != top) tops[owner] = top
    }

    fun remove(owner: Any) {
        tops.remove(owner)
    }
}

/** The [PopupBarriers] of the current COUI theme, or `null` outside one. */
internal val LocalPopupBarriers = staticCompositionLocalOf<PopupBarriers?> { null }

/**
 * Gives [content] a barrier registry of its own. `findAllBarrierRulesAndExecute` only walks the
 * anchor's own window, so a popup opened from a dialog or a panel ignores the page's navigation view
 * that the dialog already covers.
 */
@Composable
internal fun PopupBarrierBoundary(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPopupBarriers provides remember { PopupBarriers() }, content = content)
}

object PopupBarrierDefaults {
    /** The gap a list popup keeps above a bottom navigation view. COUI `coui_popup_list_window_gap_to_navigation_view`. */
    val GapToNavigationView = 8.dp

    /**
     * The window height a bottom barrier needs before it applies. `getBarrierDirection` returns
     * "gone" when the root view is no taller than
     * `coui_popup_list_window_min_window_height_to_apply_vertical_barrier`.
     */
    val MinWindowHeightToApply = 360.dp
}

/**
 * Marks this element as a bottom barrier: list popups shown in the same COUI theme are kept at least
 * [gap] above its top edge, the way ColorOS keeps popup menus clear of its bottom navigation views.
 *
 * Only the part of the element visible in the window counts; an element scrolled or translated out of
 * the window stops being a barrier.
 *
 * @param gap The clearance kept between the popup and the element's top edge.
 */
fun Modifier.popupBottomBarrier(gap: Dp = PopupBarrierDefaults.GapToNavigationView): Modifier = this then PopupBottomBarrierElement(gap)

private data class PopupBottomBarrierElement(val gap: Dp) : ModifierNodeElement<PopupBottomBarrierNode>() {
    override fun create() = PopupBottomBarrierNode(gap)

    override fun update(node: PopupBottomBarrierNode) {
        node.gap = gap
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "popupBottomBarrier"
        properties["gap"] = gap
    }
}

private class PopupBottomBarrierNode(var gap: Dp) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var registry: PopupBarriers? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val barriers = currentValueOf(LocalPopupBarriers)
        if (registry !== barriers) {
            registry?.remove(this)
            registry = barriers
        }
        barriers ?: return
        val bounds = coordinates.boundsInWindow()
        if (bounds.isEmpty) {
            barriers.remove(this)
            return
        }
        val gapPx = with(requireDensity()) { gap.toPx() }
        barriers.update(this, (bounds.top - gapPx).roundToInt().coerceAtLeast(0))
    }

    override fun onDetach() {
        registry?.remove(this)
        registry = null
    }
}
