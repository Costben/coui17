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
import platform.QuartzCore.CAFrameRateRangeMake
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private class SharedDisplayLinkTarget(
    private val onTick: (CADisplayLink) -> Unit,
) : NSObject() {
    @ObjCAction
    @Suppress("unused")
    fun onDisplayLink(link: CADisplayLink) {
        onTick(link)
    }
}

/**
 * Shared persistent ProMotion-aware [CADisplayLink] driver for iOS.
 *
 * Recreating and invalidating a one-shot [CADisplayLink] on every spring frame prevents iOS
 * ProMotion from ramping to 120Hz and adds ObjC/RunLoop churn during toolbar & search animations.
 * Keeping a single link alive while callbacks are queued with [preferredFrameRateRange] 80..120Hz
 * locks COUI springs to the display's full refresh rate.
 */
private object IosCouiFrameDriver {
    private class PendingCallback(
        val onFrame: (Long) -> Unit,
        var cancelled: Boolean = false,
    )

    private val pending = ArrayList<PendingCallback>()
    private var displayLink: CADisplayLink? = null
    private var target: SharedDisplayLinkTarget? = null

    fun post(onFrame: (Long) -> Unit): CouiFrameHandle {
        val entry = PendingCallback(onFrame)
        val enqueue = {
            if (!entry.cancelled) {
                pending.add(entry)
                ensureRunning()
            }
        }
        if (NSThread.isMainThread) {
            enqueue()
        } else {
            dispatch_async(dispatch_get_main_queue(), enqueue)
        }
        return object : CouiFrameHandle {
            override fun cancel() {
                entry.cancelled = true
                val remove = {
                    pending.remove(entry)
                    if (pending.isEmpty()) stop()
                }
                if (NSThread.isMainThread) {
                    remove()
                } else {
                    dispatch_async(dispatch_get_main_queue(), remove)
                }
            }
        }
    }

    private fun ensureRunning() {
        if (displayLink != null) return
        val callbackTarget = SharedDisplayLinkTarget { link -> tick(link) }
        val link = CADisplayLink.displayLinkWithTarget(
            target = callbackTarget,
            selector = NSSelectorFromString("onDisplayLink:"),
        )
        runCatching {
            link.preferredFrameRateRange = CAFrameRateRangeMake(80f, 120f, 120f)
        }
        target = callbackTarget
        displayLink = link
        link.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
    }

    private fun tick(link: CADisplayLink) {
        if (pending.isEmpty()) {
            stop()
            return
        }
        val seconds = if (link.timestamp > 0.0) link.timestamp else NSProcessInfo.processInfo.systemUptime
        val frameTimeNanos = (seconds * 1_000_000_000.0).toLong()
        val batch = ArrayList(pending)
        pending.clear()
        for (i in 0 until batch.size) {
            val cb = batch[i]
            if (!cb.cancelled) cb.onFrame(frameTimeNanos)
        }
        if (pending.isEmpty()) stop()
    }

    private fun stop() {
        displayLink?.invalidate()
        displayLink = null
        target = null
    }
}

internal actual fun postCouiFrameCallback(onFrame: (frameTimeNanos: Long) -> Unit): CouiFrameHandle = IosCouiFrameDriver.post(onFrame)
