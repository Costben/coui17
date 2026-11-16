// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
internal actual fun ObserveWindowFocusLost(
    key: Any?,
    onFocusLost: () -> Unit,
) {
    val currentOnFocusLost by rememberUpdatedState(onFocusLost)
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(key, windowInfo) {
        var wasFocused = windowInfo.isWindowFocused
        snapshotFlow { windowInfo.isWindowFocused }.collect { focused ->
            if (wasFocused && !focused) {
                currentOnFocusLost()
            }
            wasFocused = focused
        }
    }
}
