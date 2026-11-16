// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import android.view.Choreographer

internal actual fun postCouiFrameCallback(onFrame: (frameTimeNanos: Long) -> Unit): CouiFrameHandle {
    val choreographer = Choreographer.getInstance()
    var cancelled = false
    val callback = Choreographer.FrameCallback { frameTimeNanos ->
        if (!cancelled) onFrame(frameTimeNanos)
    }
    choreographer.postFrameCallback(callback)
    return object : CouiFrameHandle {
        override fun cancel() {
            if (cancelled) return
            cancelled = true
            choreographer.removeFrameCallback(callback)
        }
    }
}
