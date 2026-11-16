// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.input

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Preserves View and system haptic gating; ROMs without the OEM code fall back to AOSP feedback. */
internal fun View.performCouiHapticFeedback(feedback: CouiHapticFeedback, enabled: Boolean = true): Boolean {
    if (!enabled || !isAttachedToWindow) return false
    if (performHapticFeedback(feedback.code)) return true
    val fallback = when (feedback) {
        CouiHapticFeedback.Switch -> HapticFeedbackConstants.CONTEXT_CLICK
        CouiHapticFeedback.ScrollEdge -> HapticFeedbackConstants.CONTEXT_CLICK
    }
    return performHapticFeedback(fallback)
}

@Composable
internal actual fun rememberCouiKitHaptic(enabled: Boolean): (CouiHapticFeedback) -> Boolean {
    val view = LocalView.current
    return remember(view, enabled) {
        { feedback -> view.performCouiHapticFeedback(feedback, enabled) }
    }
}
