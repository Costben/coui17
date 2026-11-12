// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates

/** Preserves platform event types and historical samples so UP never duplicates MOVE for velocity estimation. */
internal class CouiVelocityTracker(private val downTime: Long) {
    private val tracker = VelocityTracker.obtain()

    fun add(change: PointerInputChange, coordinates: LayoutCoordinates, start: Boolean = false) {
        for (sample in change.historical) {
            addMovement(sample.uptimeMillis, MotionEvent.ACTION_MOVE, coordinates.localToWindow(sample.position))
        }
        val action = when {
            !change.previousPressed || start -> MotionEvent.ACTION_DOWN
            !change.pressed -> MotionEvent.ACTION_UP
            else -> MotionEvent.ACTION_MOVE
        }
        addMovement(change.uptimeMillis, action, coordinates.localToWindow(change.position))
    }

    /** Native release velocity uses px/ms scaled by 0.3. */
    fun releaseVelocity(): Offset {
        tracker.computeCurrentVelocity(1, Float.MAX_VALUE)
        return Offset(tracker.xVelocity * 0.3f, tracker.yVelocity * 0.3f)
    }

    fun recycle() {
        tracker.recycle()
    }

    private fun addMovement(time: Long, action: Int, position: Offset) {
        val event = MotionEvent.obtain(downTime, time, action, position.x, position.y, 0)
        try {
            tracker.addMovement(event)
        } finally {
            event.recycle()
        }
    }
}
