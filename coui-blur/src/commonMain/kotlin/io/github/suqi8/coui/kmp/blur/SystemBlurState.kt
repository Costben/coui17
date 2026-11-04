// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode

/**
 * Process-wide test hook overriding the platform's system material/power-saving blur switch when
 * non-null. Production code leaves this `null` and reads the platform setting.
 */
internal object SystemBlurState {
    var overrideForTest: Boolean? by mutableStateOf(null)
}

/**
 * Whether the host OS currently permits material blur (e.g. off in Android Low Power Mode, Super
 * Power Save Mode, `disable_window_blurs`, or when ColorOS's system material blur toggle is off).
 * Stays `true` on platforms without such switches unless overridden via [SystemBlurState].
 */
@Composable
@ReadOnlyComposable
internal expect fun isSystemBlurEnabled(): Boolean

/** Non-composable counterpart of [isSystemBlurEnabled] for [Modifier.Node]s. */
internal expect fun CompositionLocalConsumerModifierNode.readSystemBlurEnabled(): Boolean
