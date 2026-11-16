// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates

/** Preserves platform event types and historical samples so UP never duplicates MOVE for velocity estimation. */
internal class CouiVelocityTracker(
    @Suppress("unused") private val downTime: Long,
) {
    private val tracker = VelocityTracker()

    fun add(change: PointerInputChange, coordinates: LayoutCoordinates, start: Boolean = false) {
        if (!change.previousPressed || start) {
            tracker.resetTracking()
            tracker.addPosition(change.uptimeMillis, coordinates.localToWindow(change.position))
            return
        }
        for (sample in change.historical) {
            tracker.addPosition(sample.uptimeMillis, coordinates.localToWindow(sample.position))
        }
        if (change.pressed) {
            tracker.addPosition(change.uptimeMillis, coordinates.localToWindow(change.position))
        }
    }

    /** Native release velocity uses px/ms scaled by 0.3. */
    fun releaseVelocity(): Offset {
        val velocity = tracker.calculateVelocity()
        return Offset(velocity.x * 0.3f / 1000f, velocity.y * 0.3f / 1000f)
    }

    fun recycle() {
        tracker.resetTracking()
    }
}
