// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

internal actual fun CompositionLocalConsumerModifierNode.performScrollEndHaptic(type: HapticFeedbackType) {
    // COUINestedScrollView.onOverScrolled performs EDGE_LIST_VIBRATE (307); an AOSP build rejects
    // the constant and returns false, which leaves the Compose type as the fallback.
    if (!currentValueOf(LocalView).performHapticFeedback(OPLUS_HAPTIC_EDGE_LIST)) {
        currentValueOf(LocalHapticFeedback).performHapticFeedback(type)
    }
}
