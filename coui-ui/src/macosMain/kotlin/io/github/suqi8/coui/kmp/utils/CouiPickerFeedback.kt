// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback

// No OEM linear motor off Android; the velocity-scaled tick degrades to one standard tick.
@Composable
actual fun rememberCouiPickerHaptic(): (velocity: Float, level: CouiVibrateLevel) -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        { _: Float, _: CouiVibrateLevel ->
            haptic.performHapticFeedback(CouiHapticEffect.Stepping.toStandardHaptic())
        }
    }
}

// No audio asset off Android; the picker stays silent.
@Composable
actual fun rememberCouiPickerClick(): () -> Unit = remember { {} }
