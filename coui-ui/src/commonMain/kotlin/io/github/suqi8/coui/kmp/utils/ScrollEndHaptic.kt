// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Velocity

private const val PRE_SCROLL_RESET_THRESHOLD = 1.0f
private const val POST_FLING_AVAILABLE_VELOCITY_THRESHOLD = 1.0f

/**
 * A [NestedScrollConnection] that provides haptic feedback when a scrollable container
 * is flung to its start or end boundaries.
 *
 * @param hapticFeedback The [HapticFeedback] instance to perform feedback.
 * @param hapticFeedbackType The type of haptic feedback to perform.
 */
private class ScrollEndHapticNode(
    private var hapticFeedbackType: HapticFeedbackType,
) : DelegatingNode(),
    CompositionLocalConsumerModifierNode,
    NestedScrollConnection {

    private enum class ScrollEndHapticState {
        /** Not scrolled to the boundary. */
        Idle,

        /** Scrolled to the top boundary. */
        TopBoundaryHit,

        /** Scrolled to the bottom boundary. */
        BottomBoundaryHit,
    }

    private var scrollEndHapticState = ScrollEndHapticState.Idle

    /** Fires once per boundary hit, the way `COUINestedScrollView.onOverScrolled` calls it. */
    private fun boundaryHit(top: Boolean) {
        val state = if (top) ScrollEndHapticState.TopBoundaryHit else ScrollEndHapticState.BottomBoundaryHit
        if (scrollEndHapticState != state) {
            performScrollEndHaptic(hapticFeedbackType)
            scrollEndHapticState = state
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!isAttached) return Offset.Zero
        // Reset state when scrolling from a boundary into content.
        if (scrollEndHapticState == ScrollEndHapticState.TopBoundaryHit && available.y < -PRE_SCROLL_RESET_THRESHOLD) {
            scrollEndHapticState = ScrollEndHapticState.Idle
        } else if (scrollEndHapticState == ScrollEndHapticState.BottomBoundaryHit && available.y > PRE_SCROLL_RESET_THRESHOLD) {
            scrollEndHapticState = ScrollEndHapticState.Idle
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (!isAttached) return Offset.Zero
        // `COUINestedScrollView.onOverScrolled` fires the boundary feedback on the frame the content
        // stops moving, so a slow drag to the edge buzzes just like a fling does. `available` is what
        // no scrollable consumed, which is exactly the overscroll at the boundary.
        if (available.y > 0f) {
            boundaryHit(top = true)
        } else if (available.y < 0f) {
            boundaryHit(top = false)
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!isAttached) return Velocity.Zero
        // A fling that arrives with unhandled velocity in the boundary direction is overscrolling.
        if (available.y > POST_FLING_AVAILABLE_VELOCITY_THRESHOLD) {
            boundaryHit(top = true)
        } else if (available.y < -POST_FLING_AVAILABLE_VELOCITY_THRESHOLD) {
            boundaryHit(top = false)
        }
        return Velocity.Zero
    }

    override fun onAttach() {
        delegate(nestedScrollModifierNode(this, null))
    }

    override fun onDetach() {
        super.onDetach()
        scrollEndHapticState = ScrollEndHapticState.Idle
    }

    fun update(hapticFeedbackType: HapticFeedbackType) {
        this.hapticFeedbackType = hapticFeedbackType
    }
}

private data class ScrollEndHapticElement(
    private val hapticFeedbackType: HapticFeedbackType,
) : ModifierNodeElement<ScrollEndHapticNode>() {
    override fun create() = ScrollEndHapticNode(hapticFeedbackType)

    override fun update(node: ScrollEndHapticNode) {
        node.update(hapticFeedbackType)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollEndHaptic"
        properties["hapticFeedbackType"] = hapticFeedbackType
    }
}

/**
 * Applies a haptic feedback effect when a scrollable container is flung to its boundaries.
 *
 * `COUINestedScrollView.onOverScrolled` performs
 * `COUIHapticFeedbackConstants.EDGE_LIST_VIBRATE` (OPLUS 307) on every boundary hit; on Android the
 * port asks the view for that waveform first. [hapticFeedbackType] is the Compose type performed
 * when the device does not recognise the OEM constant, and the only type performed off Android.
 *
 * @param hapticFeedbackType The type of haptic feedback to perform where the COUI waveform is
 *   unavailable.
 */
fun Modifier.scrollEndHaptic(
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
): Modifier = this.then(ScrollEndHapticElement(hapticFeedbackType))

/**
 * Performs the boundary feedback of a scroll view.
 *
 * Android tries the ColorOS edge-list waveform through `View.performHapticFeedback`, which takes a
 * raw OEM constant that [HapticFeedbackType] cannot carry; every other target performs [type].
 */
internal expect fun CompositionLocalConsumerModifierNode.performScrollEndHaptic(type: HapticFeedbackType)
