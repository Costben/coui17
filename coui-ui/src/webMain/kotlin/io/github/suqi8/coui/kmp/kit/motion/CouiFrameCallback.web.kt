// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

private external fun requestAnimationFrame(callback: (Double) -> Unit): Int

private external fun cancelAnimationFrame(handle: Int)

internal actual fun postCouiFrameCallback(onFrame: (frameTimeNanos: Long) -> Unit): CouiFrameHandle {
    var cancelled = false
    val handle = requestAnimationFrame { timestampMs ->
        if (!cancelled) {
            onFrame((timestampMs * 1_000_000.0).toLong())
        }
    }
    return object : CouiFrameHandle {
        override fun cancel() {
            if (cancelled) return
            cancelled = true
            cancelAnimationFrame(handle)
        }
    }
}
