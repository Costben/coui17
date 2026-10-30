// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The system's material-effect switches, mirrored from `COUIMaterialStateManager`.
 *
 * ColorOS lets the user turn material blur off, and drops it in low-power and super power-saving
 * modes; every COUI view then paints its flat fill instead of sampling the backdrop. Glass surfaces
 * here read [blurEnabled] during composition, so they follow the switch as it changes.
 */
internal object SystemMaterialState {
    /** Whether material blur is allowed. Stays `true` on platforms without such a switch. */
    var blurEnabled by mutableStateOf(true)
}

/** Starts mirroring the platform's material switches into [SystemMaterialState]. */
@Composable
internal expect fun ObserveSystemMaterialState()
