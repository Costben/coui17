// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.runtime.State
import io.github.suqi8.coui.kmp.kit.motion.CouiSpring

/** Raw progress and minimum short-press progress for StateEffectAnimator; state selection and colors stay with consumers. */
internal class CouiStateProgress(initial: Float = 0f) {
    private var active = initial > 0f
    private var pendingRelease = false
    private var onHidden: (() -> Unit)? = null
    private val spring: CouiSpring = CouiSpring(initial * ProgressMax) {
        if (pendingRelease && spring.value > MinimumPress && spring.value < ProgressMax) {
            pendingRelease = false
            animate(0f)
        }
    }

    val progress: State<Float> = object : State<Float> {
        override val value: Float get() = spring.value / ProgressMax
    }
    val value: Float get() = progress.value

    fun setActive(value: Boolean, minimumPress: Boolean = false, onHidden: (() -> Unit)? = null) {
        this.onHidden = onHidden
        if (active == value) {
            if (!value && !spring.isRunning) onHidden?.invoke()
            return
        }
        active = value
        if (!value && minimumPress && spring.isRunning && spring.value <= MinimumPress) {
            pendingRelease = true
        } else {
            pendingRelease = false
            animate(if (value) ProgressMax else 0f)
        }
    }

    fun dispose() {
        pendingRelease = false
        onHidden = null
        spring.cancel()
    }

    /** Clears session state and velocity on disable or explicit reset; the instance remains reusable. */
    fun reset() {
        active = false
        pendingRelease = false
        onHidden = null
        spring.snapTo(0f)
    }

    private fun animate(target: Float) {
        spring.animateTo(target, response = 0.3f, onEnd = {
            // Frame drops may reach the target directly; wait for the prior frame to end before consuming pending release.
            if (pendingRelease) {
                pendingRelease = false
                animate(0f)
            } else if (!active && spring.value == 0f) {
                onHidden?.invoke()
            }
        })
    }

    private companion object {
        const val ProgressMax = 10000f
        const val MinimumPress = 7000f
    }
}
