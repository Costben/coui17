// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalHapticFeedback

internal actual fun CompositionLocalConsumerModifierNode.performScrollEndHaptic(type: HapticFeedbackType) {
    // No OEM constant to ask for off Android, so the Compose type is the whole effect.
    currentValueOf(LocalHapticFeedback).performHapticFeedback(type)
}
