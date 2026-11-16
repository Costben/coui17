// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Continuous expand/collapse motion between top app bar search and normal toolbar; holds neither query nor renderer. */
@Stable
internal class CouiSearchMotion(
    private val enabled: Boolean,
    private val expandedState: State<Boolean>,
) {
    private var animationTarget = expandedState.value
    private val initialValue = if (expandedState.value) 1000f else 0f
    private val searchSpring = CouiSpring(initialValue, minimumChange = 0.001f)
    private val toolbarSpring = CouiSpring(initialValue, minimumChange = 0.001f)
    private var retained by mutableStateOf(expandedState.value)
    private var animationRunning by mutableStateOf(false)
    val expanded: Boolean get() = expandedState.value
    val present: Boolean get() = expanded || retained
    val running: Boolean get() = expanded != animationTarget || animationRunning
    var progress by mutableFloatStateOf(if (expandedState.value) 1f else 0f)
        private set
    var toolbarAlpha by mutableFloatStateOf(if (expandedState.value) 0f else 1f)
        private set
    val actionBoundsTransform = BoundsTransform { _, _ ->
        // When collapsing, the search surface yields first while persistent actions follow the toolbar return rhythm.
        val active = if (expanded) searchSpring else toolbarSpring
        spring(
            dampingRatio = active.dampingRatio,
            stiffness = active.stiffness,
        )
    }
    val toolbarScale: Float get() {
        val end = if (expanded) 0.8f else 0.5f
        return end + (1f - end) * toolbarAlpha
    }

    fun updateTarget(value: Boolean) {
        if (animationTarget == value) return
        animationTarget = value
        if (!enabled) return
        retained = true
        animationRunning = true
        val target = if (value) 1000f else 0f
        if (!searchSpring.isRunning) searchSpring.setStartValue(progress * 1000f)
        if (!toolbarSpring.isRunning) toolbarSpring.setStartValue((1f - toolbarAlpha) * 1000f)
        // Only update target while running so velocity is preserved without cancelling.
        searchSpring.animateTo(
            target = target,
            response = if (value) 0.55f else 0.45f,
            bounce = 0.2f,
            minimumChange = 0.001f,
            onFrame = { next -> progress = next / 1000f },
            onEnd = {
                progress = if (expanded) 1f else 0f
                retained = expanded
                animationRunning = false
            },
        )
        toolbarSpring.animateTo(
            target = target,
            response = if (value) 0.40f else 0.50f,
            bounce = if (value) 0.3f else 0.2f,
            minimumChange = 0.001f,
            onFrame = { next -> toolbarAlpha = (1f - next / 1000f).coerceIn(0f, 1f) },
            onEnd = {
                toolbarAlpha = if (expanded) 0f else 1f
            },
        )
    }

    fun finishCollapse() {
        if (expanded) return
        dispose()
        searchSpring.snapTo(0f)
        toolbarSpring.snapTo(0f)
        progress = 0f
        toolbarAlpha = 1f
        retained = false
        animationRunning = false
        animationTarget = false
    }

    fun dispose() {
        searchSpring.cancelComplete()
        toolbarSpring.cancelComplete()
    }
}
