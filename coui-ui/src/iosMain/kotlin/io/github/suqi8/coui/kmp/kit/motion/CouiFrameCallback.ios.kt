// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.suqi8.coui.kmp.kit.motion

import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSThread
import platform.QuartzCore.CADisplayLink
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private class OneShotDisplayLinkTarget(
    private val onTick: (CADisplayLink) -> Unit,
) : NSObject() {
    @ObjCAction
    @Suppress("unused")
    fun onDisplayLink(link: CADisplayLink) {
        onTick(link)
    }
}

internal actual fun postCouiFrameCallback(onFrame: (frameTimeNanos: Long) -> Unit): CouiFrameHandle {
    var cancelled = false
    var displayLink: CADisplayLink? = null
    var target: OneShotDisplayLinkTarget? = null

    fun scheduleOnMain() {
        if (cancelled) return
        val callbackTarget = OneShotDisplayLinkTarget { link ->
            link.invalidate()
            displayLink = null
            target = null
            if (!cancelled) {
                val seconds = if (link.timestamp > 0.0) link.timestamp else NSProcessInfo.processInfo.systemUptime
                onFrame((seconds * 1_000_000_000.0).toLong())
            }
        }
        val link = CADisplayLink.displayLinkWithTarget(
            target = callbackTarget,
            selector = NSSelectorFromString("onDisplayLink:"),
        )
        target = callbackTarget
        displayLink = link
        link.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
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
            val active = displayLink
            displayLink = null
            target = null
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
