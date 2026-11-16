// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.input

import androidx.compose.ui.input.pointer.PointerEvent

/** Returns true when this [PointerEvent] represents a Compose-synthesized touch cancellation. */
internal expect fun PointerEvent.isCouiTouchCancelled(): Boolean

internal fun PointerEvent.hasCouiSyntheticCancelChanges(): Boolean = changes.any { it.previousPressed } && changes.all {
    !it.pressed && (
        !it.previousPressed ||
            (
                it.isConsumed && it.uptimeMillis == it.previousUptimeMillis &&
                    it.position == it.previousPosition
                )
        )
}
