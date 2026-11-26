// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSThread
import platform.Foundation.NSTimer
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal actual fun postCouiFrameCallback(onFrame: (frameTimeNanos: Long) -> Unit): CouiFrameHandle {
    var cancelled = false
    var timer: NSTimer? = null

    fun scheduleOnMain() {
        if (cancelled) return
        val scheduled = NSTimer.timerWithTimeInterval(
            interval = 1.0 / 120.0,
            repeats = false,
            block = { _ ->
                if (!cancelled) {
                    val nanos = (NSProcessInfo.processInfo.systemUptime * 1_000_000_000.0).toLong()
                    onFrame(nanos)
                }
            },
        )
        timer = scheduled
        NSRunLoop.mainRunLoop.addTimer(scheduled, NSRunLoopCommonModes)
    }

    if (NSThread.isMainThread) {
        scheduleOnMain()
    } else {
        dispatch_async(dispatch_get_main_queue()) {
            scheduleOnMain()
        }
    }

    return object : CouiFrameHandle {
        override fun cancel() {
            cancelled = true
            val active = timer
            timer = null
            if (active != null) {
                if (NSThread.isMainThread) {
                    active.invalidate()
                } else {
                    dispatch_async(dispatch_get_main_queue()) {
                        active.invalidate()
                    }
                }
            }
        }
    }
}
