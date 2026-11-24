// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop

@Composable
internal actual fun rememberHostWindowBackdrop(active: Boolean): State<Backdrop?> {
    // Dialogs are layered inside the host window, so the inherited backdrop already samples the page.
    val inherited = LocalBackdrop.current
    return rememberUpdatedState(if (active) inherited else null)
}
