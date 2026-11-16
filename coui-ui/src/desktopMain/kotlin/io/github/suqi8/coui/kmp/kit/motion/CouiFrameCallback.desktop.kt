// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

private val desktopFrameExecutor by lazy {
    Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "coui-kit-frame").apply { isDaemon = true }
    }
}

internal actual fun postCouiFrameCallback(onFrame: (frameTimeNanos: Long) -> Unit): CouiFrameHandle {
    var cancelled = false
    val future = desktopFrameExecutor.schedule(
        {
            if (cancelled) return@schedule
            SwingUtilities.invokeLater {
                if (!cancelled) onFrame(System.nanoTime())
            }
        },
        8L,
        TimeUnit.MILLISECONDS,
    )
    return object : CouiFrameHandle {
        override fun cancel() {
            cancelled = true
            future.cancel(false)
        }
    }
}
