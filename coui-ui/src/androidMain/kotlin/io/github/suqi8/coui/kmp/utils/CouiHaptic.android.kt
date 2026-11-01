// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

// ColorOS linear-motor feedback constants. View.performHapticFeedback accepts arbitrary ints; the
// ColorOS framework maps these to dedicated waveforms, while AOSP builds ignore unknown constants
// (return false).
private const val OPLUS_HAPTIC_SWITCH = 302
private const val OPLUS_HAPTIC_STRENGTH = 308
private const val OPLUS_HAPTIC_LONG = 300
private const val OPLUS_HAPTIC_KEYBOARD = 301
private const val OPLUS_HAPTIC_SHORT = 303
private const val OPLUS_HAPTIC_SHORT_TRIPLE = 304
private const val OPLUS_HAPTIC_STEPPING = 305
private const val OPLUS_HAPTIC_EDGE_SHORT = 306
internal const val OPLUS_HAPTIC_EDGE_LIST = 307
private const val OPLUS_HAPTIC_EDGE_SQUEEZE = 14

// AOSP fallbacks (HapticFeedbackConstants) for non-ColorOS devices.
private const val AOSP_LONG_PRESS = 0 // HapticFeedbackConstants.LONG_PRESS
private const val AOSP_KEYBOARD_TAP = 3 // HapticFeedbackConstants.KEYBOARD_TAP
private const val AOSP_CONTEXT_CLICK = 6 // HapticFeedbackConstants.CONTEXT_CLICK
private const val AOSP_CLOCK_TICK = 4 // HapticFeedbackConstants.CLOCK_TICK

@Composable
actual fun rememberCouiHaptic(): (CouiHapticEffect) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { effect: CouiHapticEffect ->
            val (oplus, fallback) = when (effect) {
                CouiHapticEffect.Switch -> OPLUS_HAPTIC_SWITCH to AOSP_CONTEXT_CLICK
                CouiHapticEffect.Strength -> OPLUS_HAPTIC_STRENGTH to AOSP_CLOCK_TICK
                CouiHapticEffect.Long -> OPLUS_HAPTIC_LONG to AOSP_LONG_PRESS
                CouiHapticEffect.Keyboard -> OPLUS_HAPTIC_KEYBOARD to AOSP_KEYBOARD_TAP
                CouiHapticEffect.Short -> OPLUS_HAPTIC_SHORT to AOSP_CLOCK_TICK
                CouiHapticEffect.ShortTriple -> OPLUS_HAPTIC_SHORT_TRIPLE to AOSP_CLOCK_TICK
                CouiHapticEffect.Stepping -> OPLUS_HAPTIC_STEPPING to AOSP_CLOCK_TICK
                CouiHapticEffect.EdgeShort -> OPLUS_HAPTIC_EDGE_SHORT to AOSP_CONTEXT_CLICK
                CouiHapticEffect.EdgeList -> OPLUS_HAPTIC_EDGE_LIST to AOSP_CONTEXT_CLICK
                CouiHapticEffect.EdgeSqueeze -> OPLUS_HAPTIC_EDGE_SQUEEZE to AOSP_LONG_PRESS
            }
            // Try the OPLUS constant first; if the device doesn't recognise it, fall back to AOSP.
            if (!view.performHapticFeedback(oplus)) {
                view.performHapticFeedback(fallback)
            }
        }
    }
}
