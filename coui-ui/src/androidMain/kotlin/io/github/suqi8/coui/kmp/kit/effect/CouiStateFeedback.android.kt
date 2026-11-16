// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun ObserveWindowFocusLost(
    key: Any?,
    onFocusLost: () -> Unit,
) {
    val currentOnFocusLost by rememberUpdatedState(onFocusLost)
    val view = LocalView.current
    DisposableEffect(key, view) {
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
            if (!focused) currentOnFocusLost()
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            }
        }
    }
}
