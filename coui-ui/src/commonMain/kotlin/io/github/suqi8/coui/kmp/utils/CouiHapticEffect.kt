// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * A COUI (ColorOS) haptic effect, mapped to OPLUS linear-motor feedback constants on Android and to
 * the closest standard haptic on every other platform.
 *
 * The numeric values mirror the OEM `performHapticFeedback` constants used by the real COUI widgets
 * (`COUIHapticFeedbackConstants`).
 */
enum class CouiHapticEffect {
    /** Switch / button / checkbox toggle confirm (OPLUS 302 GRANULAR_SHORT_VIBRATE). */
    Switch,

    /** Stepper / picker discrete tick (OPLUS 308 GRANULAR_SHORT_VIBRATE_SYNC). */
    Strength,

    /** Long press confirm, e.g. holding a drag handle (OPLUS 300 LONG_VIBRATE). */
    Long,

    /** A single key press on a COUI keyboard (OPLUS 301 KEYBOARD_TOUCH_FEEDBACK). */
    Keyboard,

    /** Short accent used for small state changes (OPLUS 303 SHORT_VIBRATE). */
    Short,

    /** Three rapid shorts, used for a rejected or repeated gesture (OPLUS 304 SHORT_TRIPLE_VIBRATE). */
    ShortTriple,

    /** One step of a stepped control such as a wheel picker (OPLUS 305 STEPPING_SHORT_VIBRATE). */
    Stepping,

    /** A scrollable view reaching its boundary (OPLUS 306 EDGE_SHORT_VIBRATE). */
    EdgeShort,

    /** A list or scroll view overscrolling at its boundary (OPLUS 307 EDGE_LIST_VIBRATE). */
    EdgeList,

    /** The side-key squeeze gesture (OPLUS 14 EDGE_SQUEEZE). */
    EdgeSqueeze,
}

/**
 * Performs a [CouiHapticEffect]. On OPLUS/ColorOS devices this invokes the matching linear-motor
 * waveform through `View.performHapticFeedback`; on other Android builds and on non-Android targets
 * it falls back to the closest standard Compose haptic.
 *
 * Returns a callback so the effect can be triggered from outside the composition (e.g. inside an
 * animation or gesture callback) without re-resolving platform handles each time.
 */
@Composable
expect fun rememberCouiHaptic(): (CouiHapticEffect) -> Unit

/**
 * The standard Compose haptic closest to a COUI effect, for platforms without an OEM linear motor.
 */
internal fun CouiHapticEffect.toStandardHaptic(): HapticFeedbackType = when (this) {
    CouiHapticEffect.Switch -> HapticFeedbackType.ToggleOn
    CouiHapticEffect.Strength -> HapticFeedbackType.SegmentTick
    CouiHapticEffect.Long -> HapticFeedbackType.LongPress
    CouiHapticEffect.Keyboard -> HapticFeedbackType.SegmentTick
    CouiHapticEffect.Short -> HapticFeedbackType.ContextClick
    CouiHapticEffect.ShortTriple -> HapticFeedbackType.SegmentTick
    CouiHapticEffect.Stepping -> HapticFeedbackType.SegmentTick
    CouiHapticEffect.EdgeShort -> HapticFeedbackType.GestureThresholdActivate
    CouiHapticEffect.EdgeList -> HapticFeedbackType.GestureThresholdActivate
    CouiHapticEffect.EdgeSqueeze -> HapticFeedbackType.LongPress
}
