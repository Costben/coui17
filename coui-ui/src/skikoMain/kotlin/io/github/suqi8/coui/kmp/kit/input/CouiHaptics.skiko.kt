// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.suqi8.coui.kmp.utils.CouiHapticEffect
import io.github.suqi8.coui.kmp.utils.rememberCouiHaptic

@Composable
internal actual fun rememberCouiKitHaptic(enabled: Boolean): (CouiHapticFeedback) -> Boolean {
    val performHaptic = rememberCouiHaptic()
    return remember(performHaptic, enabled) {
        { feedback ->
            if (!enabled) {
                false
            } else {
                val effect = when (feedback) {
                    CouiHapticFeedback.Switch -> CouiHapticEffect.Switch
                    CouiHapticFeedback.ScrollEdge -> CouiHapticEffect.EdgeList
                }
                performHaptic(effect)
                true
            }
        }
    }
}
