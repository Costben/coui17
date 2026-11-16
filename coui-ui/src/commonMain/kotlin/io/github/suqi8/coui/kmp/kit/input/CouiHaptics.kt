// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.input

import androidx.compose.runtime.Composable

/** COUIHapticFeedbackConstants consumed by the active ROM without synthesizing alternate waveforms. */
internal enum class CouiHapticFeedback(val code: Int) {
    Switch(302),
    ScrollEdge(307),
}

/** Performs [CouiHapticFeedback] when enabled, preserving OEM constants on Android and bridging on Skiko. */
@Composable
internal expect fun rememberCouiKitHaptic(enabled: Boolean = true): (CouiHapticFeedback) -> Boolean
