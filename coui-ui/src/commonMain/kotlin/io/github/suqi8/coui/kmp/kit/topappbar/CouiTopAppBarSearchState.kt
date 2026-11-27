// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingRenderer
import io.github.suqi8.coui.kmp.kit.effect.isKitSurfaceSupported
import io.github.suqi8.coui.kmp.kit.effect.preferFastAnimatingFloatingSurfaces
import io.github.suqi8.coui.kmp.kit.motion.CouiSearchMotion

@Stable
internal class CouiTopAppBarSearchState internal constructor(
    internal val renderer: CouiFloatingRenderer?,
    expandedState: State<Boolean>,
) {
    internal val motion = CouiSearchMotion(true, expandedState)
    internal var sourceCenter by mutableStateOf<Offset?>(null)
    internal var expandedWidth by mutableFloatStateOf(0f)
    private var cachedPairRenderer: CouiFloatingRenderer? = null

    /** Retains the dual-body floating renderer across expand/collapse cycles so shaders never recompile on reopen. */
    internal fun obtainPairRenderer(): CouiFloatingRenderer = cachedPairRenderer ?: CouiFloatingRenderer.pair().also { cachedPairRenderer = it }
}

@Composable
internal fun rememberCouiTopAppBarSearchState(
    expanded: Boolean,
    renderer: CouiFloatingRenderer? = if (isKitSurfaceSupported()) {
        remember { CouiFloatingRenderer.standalone() }
    } else {
        null
    },
): CouiTopAppBarSearchState {
    val expandedState = rememberUpdatedState(expanded)
    val state = remember(renderer) {
        CouiTopAppBarSearchState(renderer, expandedState).also {
            // Pre-create the dual-body renderer on Skiko/Metal so the first search expand does not
            // stall compiling the ~600-line metaball RuntimeShader on frame 1 of the animation.
            if (preferFastAnimatingFloatingSurfaces && renderer != null) {
                it.obtainPairRenderer()
            }
        }
    }
    SideEffect { state.motion.updateTarget(expanded) }
    DisposableEffect(state) { onDispose { state.motion.dispose() } }
    return state
}
