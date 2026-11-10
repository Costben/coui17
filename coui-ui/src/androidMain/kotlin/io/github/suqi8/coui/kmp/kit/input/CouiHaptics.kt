// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.input

import android.view.HapticFeedbackConstants
import android.view.View

/** COUIHapticFeedbackConstants；由当前 ROM 消费，不合成另一套马达波形。 */
internal enum class CouiHapticFeedback(val code: Int) {
    Switch(302),
    ScrollEdge(307),
}

/** 保留 View 和系统触觉设置的门控；不支持该反馈码的 ROM 降级到 AOSP 反馈。 */
internal fun View.performCouiHapticFeedback(feedback: CouiHapticFeedback, enabled: Boolean = true): Boolean {
    if (!enabled || !isAttachedToWindow) return false
    if (performHapticFeedback(feedback.code)) return true
    val fallback = when (feedback) {
        CouiHapticFeedback.Switch -> HapticFeedbackConstants.CONTEXT_CLICK
        CouiHapticFeedback.ScrollEdge -> HapticFeedbackConstants.CONTEXT_CLICK
    }
    return performHapticFeedback(fallback)
}
