// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.suqi8.coui.kmp.kit.motion.CouiSpring
import kotlin.math.pow

/** Query and expansion targets are owned by the host; each channel runs independently and disposes with composition. */
@Stable
internal class CouiSearchBarState internal constructor(expanded: Boolean) {
    var expanded by mutableStateOf(expanded)
        private set
    private val width = CouiSpring(if (expanded) 1000f else 0f, .001f)
    private val cancel = CouiSpring(if (expanded) 1000f else 0f, .001f)
    private val page = CouiSpring(if (expanded) 1000f else 0f, .001f)
    private val results = CouiSpring(if (expanded) 1000f else 0f, .001f)
    internal val widthProgress: Float get() = width.value / 1000f
    internal val cancelProgress: Float get() = cancel.value / 1000f
    internal val pageProgress: Float get() = page.value / 1000f
    internal var running by mutableStateOf(false)
    val active: Boolean get() = expanded || running || widthProgress > 0f

    /** Home page fades out cubically on entry and linearly on exit. */
    val contentAlpha: Float get() = (1f - pageProgress).coerceIn(0f, 1f).let { if (expanded) it.pow(3) else it }
    val resultsAlpha: Float get() = (results.value / 1000f).coerceIn(0f, 1f)

    internal fun updateTarget(target: Boolean) {
        if (expanded == target) return
        expanded = target
        running = true
        val end = if (target) 1000f else 0f
        // COUISearchBar width/cancel paths only update target while running, keeping current value and velocity.
        width.animateTo(end, .5f, .25f, onEnd = ::updateRunning)
        cancel.animateTo(end, .5f, .25f, onEnd = ::updateRunning)
        // Page/results retain shared spring adaptation until OEM running setStartValue thresholds are known.
        page.animateTo(end, .35f, onEnd = ::updateRunning)
        results.animateTo(end, if (target) .35f else .1f, onEnd = ::updateRunning)
    }

    internal fun dispose() {
        val end = if (expanded) 1000f else 0f
        width.snapTo(end)
        cancel.snapTo(end)
        page.snapTo(end)
        results.snapTo(end)
        running = false
    }

    private fun updateRunning() {
        running = width.isRunning || cancel.isRunning || page.isRunning || results.isRunning
    }
}

@Composable
internal fun rememberCouiSearchBarState(expanded: Boolean): CouiSearchBarState {
    val state = remember { CouiSearchBarState(expanded) }
    SideEffect { state.updateTarget(expanded) }
    DisposableEffect(state) { onDispose { state.dispose() } }
    return state
}
