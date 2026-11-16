// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import io.github.suqi8.coui.kmp.kit.input.isCouiTouchCancelled

@Composable
internal fun rememberCouiTouchMotion(expanded: Boolean): CouiTouchMotion {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val motion = remember(scope, density) { CouiTouchMotion(scope, density) }
    LaunchedEffect(expanded, motion) {
        if (!expanded) motion.release(Offset.Zero)
    }
    DisposableEffect(motion) {
        onDispose { motion.dispose() }
    }
    return motion
}

/** Releases normal press motion with the node; keep constructor for explicit CoroutineScope lifetimes. */
@Composable
internal fun rememberCouiPressMotion(): CouiPressMotion {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val press = remember(scope, density) { CouiPressMotion(scope, density) }
    DisposableEffect(press) { onDispose { press.dispose() } }
    return press
}

/** Observes press and cancel only; scrollable children can release after parent consumes move until touch ends. */
internal fun Modifier.couiPressMotion(
    press: CouiPressMotion,
    enabled: Boolean = true,
    cancelOnConsumedMove: Boolean = false,
): Modifier = pointerInput(press, enabled, cancelOnConsumedMove) {
    if (!enabled) return@pointerInput
    var pressed = false
    var cancelled = false
    try {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val hasPointers = event.changes.any { it.pressed }
                val nowPressed = !cancelled && !event.isCouiTouchCancelled() && hasPointers
                if (nowPressed && !pressed) press.press(size)
                if (!nowPressed && pressed) press.release()
                pressed = nowPressed
                if (!hasPointers) cancelled = false
                if (cancelOnConsumedMove) {
                    val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                    if (pressed && finalEvent.changes.any {
                            it.isConsumed && it.pressed && it.position != it.previousPosition
                        }
                    ) {
                        press.release()
                        pressed = false
                        cancelled = true
                    }
                }
            }
        }
    } finally {
        if (pressed) press.release()
    }
}

/** Feeds COUI matrix-transformed pointer events into the deformation unit without stealing child gestures. */
internal fun Modifier.couiTouchMotion(
    motion: CouiTouchMotion,
    expanded: Boolean,
    targetSize: ((Offset) -> IntSize?)? = null,
): Modifier = couiTouchMotion(motion, expanded, cancelOnConsumedMove = false, targetSize = targetSize)

/** Popups cancel on parent scroll; bottom bars and FABs take over drag past slop while public entry only observes. */
internal fun Modifier.couiTouchMotion(
    motion: CouiTouchMotion,
    expanded: Boolean,
    cancelOnConsumedMove: Boolean,
    targetSize: ((Offset) -> IntSize?)? = null,
    consumeDrag: Boolean = false,
): Modifier = onGloballyPositioned { motion.coordinates = it }
    .pointerInput(motion, expanded, cancelOnConsumedMove, consumeDrag) {
        if (!expanded) return@pointerInput
        val trackers = mutableMapOf<PointerId, CouiVelocityTracker>()
        var downTime = 0L
        try {
            awaitPointerEventScope {
                var active: PointerId? = null
                var velocityPointer: PointerId? = null
                var cancelled = false
                var dragOrigin = Offset.Zero
                var ownsDrag = false

                fun cancelTouch() {
                    motion.release(Offset.Zero)
                    trackers.values.forEach { it.recycle() }
                    trackers.clear()
                    active = null
                    velocityPointer = null
                    ownsDrag = false
                }

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.isCouiTouchCancelled()) {
                        cancelTouch()
                        cancelled = false
                        continue
                    }
                    if (cancelled) {
                        if (event.changes.none { it.pressed }) cancelled = false
                        continue
                    }
                    val coordinates = motion.coordinates?.takeIf { it.isAttached } ?: continue
                    val newPointer = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                    val oldPointer = event.changes.firstOrNull { it.id == active }
                    val pointer = newPointer ?: if (oldPointer?.pressed == false) {
                        event.changes.firstOrNull { it.pressed } ?: oldPointer
                    } else {
                        oldPointer ?: if (targetSize != null) event.changes.firstOrNull { it.pressed } else null
                    } ?: continue
                    // Hit target decides whether deformation starts; active drags continue outside bounds until UP/CANCEL.
                    val bounds = if (active == null && targetSize != null) {
                        targetSize(pointer.position) ?: continue
                    } else {
                        size
                    }
                    if (active == null) {
                        downTime = pointer.uptimeMillis
                        velocityPointer = pointer.id
                    }
                    for (change in event.changes) {
                        trackers.getOrPut(change.id) { CouiVelocityTracker(downTime) }.add(
                            change,
                            coordinates,
                            start = active == null && change.id == pointer.id,
                        )
                    }
                    val position = coordinates.localToWindow(pointer.position)
                    if (active == null) {
                        dragOrigin = position
                        ownsDrag = false
                        motion.begin(position, bounds)
                    } else if (pointer.id != active) {
                        val previous = oldPointer?.let { coordinates.localToWindow(it.position) }
                        if (previous != null) dragOrigin += position - previous
                        motion.switchPointer(previous, position)
                    }
                    if (consumeDrag && pointer.pressed &&
                        (ownsDrag || (position - dragOrigin).getDistance() > viewConfiguration.touchSlop)
                    ) {
                        ownsDrag = true
                        // Take over drag on Initial pass so parent scroll and child clicks cancel this frame without affecting taps.
                        event.changes.forEach { change ->
                            if (change.pressed && change.position != change.previousPosition) change.consume()
                        }
                    }
                    if (newPointer != null) velocityPointer = newPointer.id
                    if (event.changes.any { it.id == velocityPointer && !it.pressed } &&
                        event.changes.any { it.pressed }
                    ) {
                        // Vendor velocity helper picks last pointer while motion unit picks first; preserve both semantics.
                        velocityPointer = event.changes.lastOrNull { it.pressed }?.id
                    }
                    active = pointer.id
                    if (pointer.pressed) {
                        if (newPointer == null && oldPointer?.pressed == true) motion.move(position)
                    } else {
                        motion.release(trackers[velocityPointer]?.releaseVelocity() ?: Offset.Zero)
                        trackers.values.forEach { it.recycle() }
                        trackers.clear()
                        active = null
                        velocityPointer = null
                        ownsDrag = false
                    }
                    if (cancelOnConsumedMove) {
                        val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                        if (active != null && finalEvent.changes.any {
                                it.isConsumed && it.pressed && it.position != it.previousPosition
                            }
                        ) {
                            cancelTouch()
                            cancelled = true
                        }
                    }
                }
            }
        } finally {
            trackers.values.forEach { it.recycle() }
            motion.release(Offset.Zero)
        }
    }
